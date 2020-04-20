package com.example.sjsumap.ui.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.example.sjsumap.R
import com.example.sjsumap.utilities.FileHelper
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
        val text = FileHelper.getTextFromResources(activity!!.applicationContext, R.raw.buildings)
        val buildings = JSONArray(text)
        for (i in 0 until buildings.length()) {
            val building = buildings.getJSONObject(i)
            val name = building.getString("name")
            if (name == buildingName){
                view.findViewById<TextView>(R.id.description).text = building.getString("building desc")
                view.findViewById<TextView>(R.id.services).text = building.getString("service desc")
                val imageName = building.getString("img").replace(".jpg","")
                val resourceId = resources.getIdentifier(imageName, "drawable", activity!!.packageName)
                view.findViewById<ImageView>(R.id.image).setImageResource(resourceId)
            }
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
