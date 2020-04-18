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
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 */

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var query: String? = null
    private val locateZoom = 18.5F

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
        Log.i("map_info", "on map ready")
        if (query != null) {
            geoLocate(query as String)
            Log.i("map_info", "text: $query")
        }
    }

    private fun geoLocate(param1: String) {
        val gc = Geocoder(activity)
        try{
            val addresses = gc.getFromLocationName(param1, 1)
            if (addresses.size > 0) {
                val lat = addresses[0].latitude
                val lng = addresses[0].longitude
                val name = addresses[0].getAddressLine(0)
                Log.i("map_info", "geolocate: $name")
                moveToLocation(lat, lng, locateZoom, name)
            }
        } catch (e: IOException){
            Log.i("map_info", "can not find location")
            Toast.makeText(activity, "Error :Can not find location!", Toast.LENGTH_LONG).show()
        } catch (e: Exception){
            Log.i("map_info", e.message)
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun moveToLocation(lat: Double, lng: Double, zoom: Float, name: String) {
        val loc = LatLng(lat, lng)
        mMap!!.addMarker(MarkerOptions().position(loc).title(name))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom))
        Toast.makeText(activity, name, Toast.LENGTH_LONG).show()
    }
}