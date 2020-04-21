package com.example.sjsumap.ui.explore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.sjsumap.R
import com.example.sjsumap.utilities.FileHelper
import org.json.JSONObject


/**
 * A simple [Fragment] subclass.
 */
class ExploreFragment : Fragment() {
    companion object {
        var services_list = mutableListOf<String>()
        val services_data = HashMap<String, MutableList<String>>()
        val services_icon = HashMap<String, String>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (services_data.isEmpty()) {
            getServiceData()
        }
        val listView = inflater.inflate(R.layout.fragment_explore, container, false)
        val myAdapter = ArrayAdapter<String>(
            activity!!.applicationContext,
            android.R.layout.simple_list_item_1,
            services_list
        )
        val list = listView.findViewById<ListView>(R.id.list)
        list.adapter = myAdapter
        setOnItemClickListener(list)
        return listView
    }

    private fun getServiceData() {
        val text = FileHelper.getTextFromResources(activity!!.applicationContext, R.raw.services)
        val servicesJson = JSONObject(text)
        services_list = servicesJson.keys().asSequence().toMutableList()
        for (service in services_list) {
            val serviceJson = servicesJson.getJSONObject(service)
            val buildingsJson = serviceJson.getJSONArray("buildings")
            val buildingList = mutableListOf<String>()
            for (i in 0 until buildingsJson.length()) {
                buildingList.add(buildingsJson[i].toString())
            }
            services_data[service] = buildingList
            services_icon[service] = serviceJson.getString("icon")
        }
        Log.i("service_info", services_data.toString())
        Log.i("service_icon", services_icon.toString())
    }

    private fun setOnItemClickListener(list: ListView) {
        list.setOnItemClickListener { parent, _, position, _ ->
            // Get the selected item text from ListView
            val selectedItem = parent.getItemAtPosition(position) as String
            Log.i("service_info", selectedItem)
            val args = Bundle()
            args.putString("param1", "Service: $selectedItem")
            activity!!.findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_search_location,
                args
            )
        }
    }
}
