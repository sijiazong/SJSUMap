package com.example.sjsumap.ui.map

import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sjsumap.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * A simple [Fragment] subclass.
 */

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var query: String? = null
    private val locateZoom = 18.5F
    private var marker: Marker? = null

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
//        mMap!!.isMyLocationEnabled = true
        Log.i("map_info", "on map ready")
        if (query != null) {
            geoLocate(query as String)
            Log.i("map_info", "text: $query")
        }
        mMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter{
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

    private fun geoLocate(param1: String) {
        val gc = Geocoder(activity)
        val addresses = gc.getFromLocationName(param1, 1)
        if (addresses.size > 0) {
            val add = addresses[0]
            val lat = add.latitude
            val lng = add.longitude
            val name = add.getAddressLine(0)
            Log.i("map_info", "geolocate: $add")
            moveToLocation(lat, lng, locateZoom)
            marker = mMap!!.addMarker(MarkerOptions().position(LatLng(lat,lng)).title(name).snippet(add.locality))
        }else{
            Toast.makeText(activity, "Error: Location not found!", Toast.LENGTH_LONG).show()
        }

    }

    private fun moveToLocation(lat: Double, lng: Double, zoom: Float) {
        val loc = LatLng(lat, lng)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom))
    }
}