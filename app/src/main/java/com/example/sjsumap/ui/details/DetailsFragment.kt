package com.example.sjsumap.ui.details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.sjsumap.R
import com.example.sjsumap.utilities.Helper
import org.json.JSONArray

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "building_name"

class DetailsFragment : Fragment() {
    private var buildingName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            buildingName = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_details, container, false)
        view.findViewById<TextView>(R.id.title).text = buildingName
        val text = Helper.getTextFromResources(activity!!.applicationContext, R.raw.buildings)
        val buildings = JSONArray(text)
        for (i in 0 until buildings.length()) {
            val building = buildings.getJSONObject(i)
            val name = building.getString("name")
            if (name == buildingName) {
                view.findViewById<TextView>(R.id.description).text =
                    building.getString("building desc")
                view.findViewById<TextView>(R.id.services).text = building.getString("service desc")
                val imageName = building.getString("img").replace(".jpg", "")
                val resourceId =
                    resources.getIdentifier(imageName, "drawable", activity!!.packageName)
                view.findViewById<ImageView>(R.id.image).setImageResource(resourceId)
            }
        }
        view.findViewById<Button>(R.id.go_button).setOnClickListener {
            val args = Bundle()
            args.putString("destination", buildingName)
            Log.i("details", buildingName!!)
            activity!!.findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_nav_details_to_nav_directions, args)
        }
        return view
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}
