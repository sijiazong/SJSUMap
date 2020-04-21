package com.example.sjsumap.ui.map

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.sjsumap.R
import com.example.sjsumap.ui.explore.ExploreFragment
import com.example.sjsumap.utilities.FileHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 */

class MapFragment : Fragment(), OnMapReadyCallback {

    private var query: String? = null
    private var marker: Marker? = null
    private var mMap: GoogleMap? = null

    companion object Polygons {
        private const val POLYGON_FILL_COLOR = "#80E5A823"
        private const val POLYGON_STROKE_COLOR = "#BFE5A823"
        private const val LOCATE_ZOOM = 18.5F

        val polygonsData = HashMap<String, PolygonOptions>()
        val buildingCenterData = HashMap<String, LatLng>()
        val serviceMarkers = HashMap<String, MutableList<MarkerOptions>>()

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("map_info", "onCreate: arguments $arguments")
        super.onCreate(savedInstanceState)
        arguments?.let {
            query = it.getString("param1")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mapView = inflater.inflate(R.layout.fragment_map, container, false)
        Log.i("map_info", "mMap: ${mMap.toString()}")
        initMap(this)
        Log.i("map_info", "onCreateView: query: $query")
        return mapView
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
            Log.i("service_info", query.toString())
            if (query!!.startsWith("Service:", ignoreCase = true)) {
                val serviceType = query!!.replace("Service:", "").trim()
                if (serviceType in ExploreFragment.servicesList) {
                    addServiceMarker(serviceType, mMap!!)
                } else {
                    Toast.makeText(activity, "Error: Can not find service!", Toast.LENGTH_LONG)
                        .show()
                }
            } else if (resources.getStringArray(R.array.building_list).contains(query)) {
                val center = buildingCenterData[query!!]!!
                moveAndMarkLocation(center, query, "Campus Building")
            } else {
                geoLocate(query as String)
                Log.i("map_info", "text: $query")
            }
        }
//        setInfoWindow(mMap!!)
//        setOnMarkerClickListener(mMap!!)
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

    private fun setOnMarkerClickListener(map: GoogleMap) {
        map.setOnMarkerClickListener {
            if (it != null) {
                it.showInfoWindow()
                Toast.makeText(activity, "Marker ${it.title}", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    private fun setInfoWindow(map: GoogleMap) {
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(mk: Marker?): View {
                val infoWindow = layoutInflater.inflate(R.layout.info_window, null)
                infoWindow.findViewById<TextView>(R.id.info_title).text = mk?.title
                infoWindow.findViewById<TextView>(R.id.info_description).text = mk?.snippet
                return infoWindow
            }

            override fun getInfoWindow(p0: Marker?): View? {
                return null
            }
        })
    }

    private fun getPolygonData() {
        Log.i("map_info", "get polygons data")
        val text = FileHelper.getTextFromResources(activity!!.applicationContext, R.raw.buildings)
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

    private fun geoLocate(param1: String) {
        val gc = Geocoder(activity)
        val addresses = gc.getFromLocationName(param1, 1)
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
}