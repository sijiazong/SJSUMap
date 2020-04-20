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

    private var mMap: GoogleMap? = null
    private var query: String? = null
    private val locateZoom = 18.5F
    private var marker: Marker? = null
    private val polygonColor = "#E5A823"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("map_info", "in onCreate")
        Log.i("map_info", "arguments $arguments")
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
        if (mMap == null) {
            initMap(this)
        }
        if (query != null) {
            mapView.findViewById<TextView>(R.id.map_text).text = query
        }
        Log.i("map_info", "query: $query")
        return mapView
    }

    private fun initMap(obj: MapFragment) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        Log.i("map_info", "before map async")
        mapFragment.getMapAsync(obj)
        Log.i("map_info", "after map async")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("map_info", "initiate map call back")
        mMap = googleMap
        getPolygonData()
        Log.i("map_info", "on map ready")
        if (query != null) {
            geoLocate(query as String)
            Log.i("map_info", "text: $query")
        }
        mMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
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
        mMap?.setOnMarkerClickListener {
            if (it != null) {
                it.showInfoWindow()
                Toast.makeText(activity, "Marker ${it.title}", Toast.LENGTH_LONG).show()
            }
            true
        }
    }


    private fun getPolygonData() {
        val text = FileHelper.getTextFromResources(activity!!.applicationContext, R.raw.buildings)
        val buildings = JSONArray(text)
        for (i in 0 until buildings.length()) {
            val building = buildings.getJSONObject(i)
            val buildingName = building.getString("name")
            val polygonOptions = getPolygonOptions(building)
            val polygon = drawPolygons(polygonOptions)
            polygon.tag = buildingName
        }
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
            val point = LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng"))
            points.add(point)
        }
        return points
    }

    private fun drawPolygons(options: PolygonOptions): Polygon {
        val polygon: Polygon = mMap!!.addPolygon(options)
        applyPolygonStyle(polygon)
        mMap!!.setOnPolygonClickListener { polygon ->
            val args = Bundle()
            args.putString("building_name", polygon.tag.toString())
            activity!!.findNavController(R.id.nav_host_fragment).navigate(R.id.action_nav_map_to_detailsFragment, args)
        }
        return polygon
    }

    private fun applyPolygonStyle(polygon: Polygon) {
        polygon.apply {
            fillColor = Color.parseColor(polygonColor)
            strokeColor = Color.parseColor(polygonColor)
        }
    }

    private fun geoLocate(param1: String) {
        val gc = Geocoder(activity)
        val addresses = gc.getFromLocationName(param1, 1)
        if (addresses.isNotEmpty()) {
            val add = addresses[0]
            val lat = add.latitude
            val lng = add.longitude
            val name = add.getAddressLine(0)
            Log.i("map_info", "geolocate: $add")
            moveToLocation(lat, lng, locateZoom)
            marker = mMap!!.addMarker(
                MarkerOptions().position(LatLng(lat, lng)).title(name).snippet(add.locality)
            )
        } else {
            Toast.makeText(activity, "Error: Location not found!", Toast.LENGTH_LONG).show()
        }

    }

    private fun moveToLocation(lat: Double, lng: Double, zoom: Float) {
        val loc = LatLng(lat, lng)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom))
    }
}