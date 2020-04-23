package com.example.sjsumap.ui.map

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.sjsumap.R
import com.example.sjsumap.ui.explore.ExploreFragment
import com.example.sjsumap.utilities.Helper
import com.example.sjsumap.utilities.PermissionUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 */

//OnMyLocationClickListener
class MapFragment : Fragment(), OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var mPermissionDenied = false

    //variable to determine fragment handle
    private var query: String? = null
    private var type: String? = null

    private var marker: Marker? = null
    private var mMap: GoogleMap? = null
    private var isRouteVisible: Boolean = false
    private var instructions = mutableListOf<String>()
    private var instructionRow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("map_info", "onCreate: arguments $arguments")
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString("type")
            query = it.getString("query")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mapView = inflater.inflate(R.layout.fragment_map, container, false)
        Log.i("map_info", "mMap: ${mMap.toString()}")
        initMap(this)
        Log.i("map_info", "onCreateView: query: $type")
        Log.i("map_info", "onCreateView: query: $query")
        val fab: FloatingActionButton = mapView.findViewById(R.id.directions)
        fab.setOnClickListener {
            if (isRouteVisible) {
                showTextInstructions(inflater, container)
            } else {
                val args = Bundle()
                activity!!.findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_nav_map_to_nav_directions, args)
            }
        }
        return mapView
    }

    private fun showTextInstructions(inflater: LayoutInflater, container: ViewGroup?) {
        val alertDialog: AlertDialog = AlertDialog.Builder(activity).create()
        alertDialog.setTitle("Directions Instruction")
        val dialogView = inflater.inflate(R.layout.instructions_dialog, container, false)
        val lv: ListView = dialogView.findViewById(R.id.list)
        val myAdapter = ArrayAdapter(
            activity!!.applicationContext,
            android.R.layout.simple_list_item_1,
            instructions
        )
        lv.adapter = myAdapter
        alertDialog.setView(dialogView)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            "Back To Map"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Clear Directions") { dialog, _ ->
            val args = Bundle()
            activity!!.findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_to_nav_map,
                args
            )
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun initMap(obj: MapFragment) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        Log.i("map_info", "before map async")
        mapFragment.getMapAsync(obj)
        Log.i("map_info", "after map async")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("map_info", "onMapReady: initiate map call back")
        mMap = googleMap
        if (polygonsData.isEmpty()) {
            getPolygonData()
        }
        addPolygonsToMap()
        query?.let {
//            Log.i("service_info", query.toString())
            when (type) {
                "Service" -> {
                    val serviceType = query!!.trim()
                    if (serviceType in ExploreFragment.servicesList) {
                        addServiceMarker(serviceType, mMap!!)
                    } else {
                        Toast.makeText(activity, "Error: Can not find service!", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                "Directions" -> {
                    val params = query!!.trim().split(" & ")
                    //                Directions: Davidson College of Engineering & 47236 Cavanaugh Cmn, Fremont, CA
                    //                Directions: Davidson College of Engineering & South Parking Garage
                    val origin = params[0]
                    val destination = params[1]
                    val mode = params[2]
                    //        walking, driving, transit
                    renderDirections(origin, destination, mode)
                    //        setInfoWindow(mMap!!)
                    //        setOnMarkerClickListener(mMap!!)
                }
                "Search" -> {
                    if (resources.getStringArray(R.array.building_list).contains(query)) {
                        val center = buildingCenterData[query!!]!!
                        moveAndMarkLocation(center, query, "Campus Building")
                    } else {
                        geoLocate(query as String)
                        Log.i("map_info", "text: $query")
                    }
                }
                else -> {
                    Log.i("map_info", "Invalid request")
                }
            }
        }
        enableMyLocation()
    }

    private fun enableMyLocation() {
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(
                activity!!.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (mMap != null) {
                mMap!!.isMyLocationEnabled = true
                mMap!!.uiSettings.isMyLocationButtonEnabled = true
            }
        } else {
            if (!hasRequestedPermission) {
//               Toast.makeText(activity, "My location is not enabled currently", Toast.LENGTH_LONG).show()
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                hasRequestedPermission = true
            }
        }
    }


    private fun renderDirections(origin: String, destination: String, mode: String) {
        val start = getRequestParam(origin)
        val end = getRequestParam(destination)
//        parameter format location name or origin=41.43206,-81.3899
        val url = generateRequestUrl(start, end, mode)
        Log.i("url", url)
        val queue = Volley.newRequestQueue(activity!!.applicationContext)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                onDirectionsDataReady(response)
            },
            Response.ErrorListener { error ->
                Log.i("url", "Request Error: $error")
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun getRequestParam(input: String): String {
        var param = input
        /** if campus building, use lat lng to search directions**/
        if (input in buildingCenterData) {
            val place = buildingCenterData[input]
            param = "${place!!.latitude},${place.longitude}"
        }
        return param
    }

    private fun onDirectionsDataReady(response: JSONObject) {
        // When API call is done, create parser and convert into JsonObjec
        // get to the correct element in JsonObject
        val routes = response.getJSONArray("routes")
        if (routes.length() > 0) {

            /**
             * use the first route if routes available
             * use bounds to set camera
             * **/
            val boundsJson = routes.getJSONObject(0).getJSONObject("bounds")
            val northeastJson = boundsJson.getJSONObject("northeast")
            val southwestJson = boundsJson.getJSONObject("southwest")
            val northeast = jsonObjectToLatLng(northeastJson)
            val southwest = jsonObjectToLatLng(southwestJson)
            val bounds = LatLngBounds(southwest, northeast)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val path = arrayListOf<HashMap<String, Double>>()
            /** Traversing all legs */
            for (i in 0 until legs.length()) {
                val steps = legs.getJSONObject(i).getJSONArray("steps")

                /** set up start and end marker **/
                val startAddress = legs.getJSONObject(i).getString("start_address")
                val startLocation = legs.getJSONObject(i).getJSONObject("start_location")
                val endAddress = legs.getJSONObject(i).getString("end_address")
                val endLocation = legs.getJSONObject(i).getJSONObject("end_location")
                val startOptions = MarkerOptions().position(jsonObjectToLatLng(startLocation))
                    .snippet(startAddress).title("Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                val endOptions = MarkerOptions().position(jsonObjectToLatLng(endLocation))
                    .snippet(endAddress).title("Destination")
                mMap!!.addMarker(startOptions)
                mMap!!.addMarker(endOptions)
                /** Traversing all steps to draw polyline **/
                for (j in 0 until steps.length()) {
                    val htmlContent = HtmlCompat.fromHtml(
                        steps.getJSONObject(j).getString("html_instructions"),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                    instructionRow += 1
                    instructions.add("$instructionRow. $htmlContent")
                    val polylinePoints =
                        steps.getJSONObject(j).getJSONObject("polyline").getString("points")
                    val decodedPoints: List<LatLng> = PolyUtil.decode(polylinePoints)
                    /** Traversing all points  */
                    for (point in decodedPoints) {
                        val hm: HashMap<String, Double> = HashMap()
                        hm["lat"] = point.latitude
                        hm["lng"] = point.longitude
                        path.add(hm)
                    }
                }
            }
            val points = arrayListOf<LatLng>()
            for (point in path) {
                val lat = point["lat"]
                val lng = point["lng"]
                val position = LatLng(lat!!, lng!!)
                points.add(position)
            }
            val lineOptions = PolylineOptions().addAll(points)
            lineOptions.color(Color.parseColor("#45A5F5"))
            mMap!!.addPolyline(lineOptions)
            Log.i("html", "instructions: $instructions")
//            mMap!!.setOnPolylineClickListener {
//                showTextInstructions()
//                Log.i("html", "clicked polyline")
//            }
            isRouteVisible = true
        } else {
            Log.i("url", "Error finding routes")
        }
    }

    private fun jsonObjectToLatLng(pointJson: JSONObject) =
        LatLng(pointJson.getDouble("lat"), pointJson.getDouble("lng"))

    private fun generateRequestUrl(origin: String, destination: String, mode: String): String {
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("maps.googleapis.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("directions")
            .appendPath("json")
            .appendQueryParameter("origin", origin)
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("mode", mode)
            .appendQueryParameter("key", resources.getString(R.string.google_maps_key))
        return builder.build().toString()
    }

    private fun addPolygonsToMap() {
        Log.i("map_info", "add polygon to map")
        for ((name, options) in polygonsData) {
            val polygon = drawPolygon(options)
            polygon.tag = name
        }
    }

    private fun addServiceMarker(serviceType: String, mMap: GoogleMap) {
        //if first time generate markers for the service, get all service markers for that service
        if (!serviceMarkers.containsKey(serviceType)) {
            Log.i("map_info", "generate marker for $serviceType")
            val buildings = ExploreFragment.servicesData[serviceType]!!
            val icon = ExploreFragment.servicesIcon[serviceType]
            val allOptions = mutableListOf<MarkerOptions>()
            val iconResourceId =
                resources.getIdentifier(icon, "drawable", activity!!.packageName)
            for (building_name in buildings) {
                val center = buildingCenterData[building_name]
                val markerOptions =
                    MarkerOptions().position(center!!).title(building_name).snippet(serviceType)
                markerOptions.icon(BitmapDescriptorFactory.fromResource(iconResourceId))
                allOptions.add(markerOptions)
            }
            serviceMarkers[serviceType] = allOptions
        }

        val markersOptions = serviceMarkers[serviceType]
        for (options in markersOptions!!) {
            mMap.addMarker(options)
        }
    }

//    private fun setOnMarkerClickListener(map: GoogleMap) {
//        map.setOnMarkerClickListener {
//            if (it != null) {
//                it.showInfoWindow()
//                Toast.makeText(activity, "Marker ${it.title}", Toast.LENGTH_LONG).show()
//            }
//            true
//        }
//    }

//    private fun setInfoWindow(map: GoogleMap) {
//        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
//            override fun getInfoContents(mk: Marker?): View {
//                val infoWindow = layoutInflater.inflate(R.layout.info_window, null)
//                infoWindow.findViewById<TextView>(R.id.info_title).text = mk?.title
//                infoWindow.findViewById<TextView>(R.id.info_description).text = mk?.snippet
//                return infoWindow
//            }
//
//            override fun getInfoWindow(p0: Marker?): View? {
//                return null
//            }
//        })
//    }

    private fun getPolygonData() {
        Log.i("map_info", "get polygons data")
        val text = Helper.getTextFromResources(activity!!.applicationContext, R.raw.buildings)
        val buildings = JSONArray(text)
        for (i in 0 until buildings.length()) {
            Log.i("map_info", "get data from json: building $i")
            val building = buildings.getJSONObject(i)
            val buildingName = building.getString("name")
            val polygonOptions = getPolygonOptions(building)
            polygonsData[buildingName] = polygonOptions
            val buildingCenter = building.getJSONObject("center")
            buildingCenterData[buildingName] = createPointFromJson(buildingCenter)
        }
        Log.i("buildings", polygonsData.keys.toString())
    }

    private fun getPolygonOptions(building: JSONObject): PolygonOptions {
        val pointsOuter = getBuildingPoints(building, "outer")
        val pointsInner = getBuildingPoints(building, "inner")
        val polygonOptions = PolygonOptions()
        polygonOptions.clickable(true).addAll(pointsOuter)
        if (pointsInner.isNotEmpty()) {
            polygonOptions.addHole(pointsInner)
        }
        return polygonOptions
    }

    private fun getBuildingPoints(building: JSONObject, typeTag: String): ArrayList<LatLng> {
        val points = arrayListOf<LatLng>()
        val coordinates = building.getJSONArray(typeTag)
        for (i in 0 until coordinates.length()) {
            val coordinate = coordinates.getJSONObject(i)
            val point = createPointFromJson(coordinate)
            points.add(point)
        }
        return points
    }

    private fun createPointFromJson(coordinate: JSONObject): LatLng {
        val lat = coordinate.getDouble("lat")
        val lng = coordinate.getDouble("lng")
        return LatLng(lat, lng)
    }

    private fun drawPolygon(options: PolygonOptions): Polygon {
        Log.i("map_info", "draw polygons")
        val polygon: Polygon = mMap!!.addPolygon(options)
        applyPolygonStyle(polygon)
        mMap!!.setOnPolygonClickListener {
            val args = Bundle()
            args.putString("building_name", it.tag.toString())
            Log.i("polygon", "click ${it.tag}")
            activity!!.findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_nav_map_to_detailsFragment, args)
        }
        return polygon
    }

    private fun applyPolygonStyle(polygon: Polygon) {
        polygon.apply {
            fillColor = Color.parseColor(POLYGON_FILL_COLOR)
            strokeColor = Color.parseColor(POLYGON_STROKE_COLOR)
        }
    }

    private fun geoLocate(query: String) {
        val gc = Geocoder(activity)
        val addresses = gc.getFromLocationName(query, 1)
        if (addresses.isNotEmpty()) {
            val add = addresses[0]
            Log.i("map_info", "geolocate: $add")
            val latLng = LatLng(add.latitude, add.longitude)
            moveAndMarkLocation(latLng, add.getAddressLine(0), add.locality)
        } else {
            Toast.makeText(activity, "Error: Cannot find location!", Toast.LENGTH_LONG).show()
        }
    }

    private fun moveAndMarkLocation(
        latLng: LatLng,
        info_title: String?,
        info_snippet: String?
    ) {
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, LOCATE_ZOOM))
        marker = mMap!!.addMarker(
            MarkerOptions().position(latLng).title(info_title).snippet(info_snippet)
        )
    }


    /**
     * Code to get user permission to access current location
     * Source: Google Maps SDK for Android Code samples: MyLocationDemoActivity
     * https://github.com/googlemaps/android-samples/blob/master/ApiDemos/java/app/src/main/java/com/example/mapdemo/MyLocationDemoActivity.java
     * **/

//    override fun onMyLocationButtonClick(): Boolean {
//        Toast.makeText(activity, "onMyLocationButtonClick\n", Toast.LENGTH_LONG).show()
//        return false
//    }
//
//    override fun onMyLocationClick(p0: Location) {
//        Toast.makeText(activity, "onMyLocationClick", Toast.LENGTH_SHORT).show()
//    }

    // [START maps_check_location_permission_result]
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true
            // [END_EXCLUDE]
        }
    }

    // [END maps_check_location_permission_result]
    companion object Polygons {
        private const val POLYGON_FILL_COLOR = "#80E5A823"
        private const val POLYGON_STROKE_COLOR = "#BFE5A823"
        private const val LOCATE_ZOOM = 18.5F

        private val polygonsData = HashMap<String, PolygonOptions>()
        private val buildingCenterData = HashMap<String, LatLng>()
        private val serviceMarkers = HashMap<String, MutableList<MarkerOptions>>()
        private var hasRequestedPermission = false
    }
}