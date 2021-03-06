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
import com.example.sjsumap.MainActivity
import com.example.sjsumap.R


/**
 * A simple [Fragment] subclass.
 */
class ExploreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (servicesData.isEmpty()) {
            getServiceData()
        }
        val listView = inflater.inflate(R.layout.fragment_explore, container, false)
        val myAdapter = ArrayAdapter(
            activity!!.applicationContext,
            android.R.layout.simple_list_item_1,
            servicesList
        )
        val list = listView.findViewById<ListView>(R.id.list)
        list.adapter = myAdapter
        setOnItemClickListener(list)
        return listView
    }

    private fun getServiceData() {
//        val text = Helper.getTextFromResources(activity!!.applicationContext, R.raw.services)
//        val servicesJson = JSONObject(text)
        val servicesJson = MainActivity.servicesJson.getJSONObject(0)
//        servicesList = servicesJson.keys().asSequence().toMutableList()
        servicesList = resources.getStringArray(R.array.service_list)
        for (service in servicesList) {
            val serviceJson = servicesJson.getJSONObject(service)
            val buildingsJson = serviceJson.getJSONArray("buildings")
            val buildingList = mutableListOf<String>()
            for (i in 0 until buildingsJson.length()) {
                buildingList.add(buildingsJson[i].toString())
            }
            servicesData[service] = buildingList
            servicesIcon[service] = serviceJson.getString("icon")
        }
        Log.i("service_info", servicesData.toString())
        Log.i("service_icon", servicesIcon.toString())
    }

    private fun setOnItemClickListener(list: ListView) {
        list.setOnItemClickListener { parent, _, position, _ ->
            // Get the selected item text from ListView
            val selectedItem = parent.getItemAtPosition(position) as String
            Log.i("service_info", selectedItem)
            val args = Bundle()
            args.putString("type", "Service")
            args.putString("query", selectedItem)
            activity!!.findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_to_nav_map,
                args
            )
        }
    }

    companion object {
        lateinit var servicesList: Array<String>
        val servicesData = HashMap<String, MutableList<String>>()
        val servicesIcon = HashMap<String, String>()
    }
}
