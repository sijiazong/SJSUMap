package com.example.sjsumap.ui.navigation

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.sjsumap.R
import com.example.sjsumap.utilities.Helper

private const val ARG_DEST = "destination"

class DirectionsFragment : Fragment() {
    private var originText: AutoCompleteTextView? = null
    private var destinationText: AutoCompleteTextView? = null
    private var modeGroup: RadioGroup? = null
    private var destination: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("directions_info", "onCreate: arguments $arguments")
        super.onCreate(savedInstanceState)
        arguments?.let {
            destination = it.getString(ARG_DEST)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val directionView: View = inflater.inflate(R.layout.fragment_directions, container, false)
        originText = directionView.findViewById(R.id.origin)
        destinationText = directionView.findViewById(R.id.destination)
        modeGroup = directionView.findViewById(R.id.mode)
        configureAutoComplete(originText)
        configureAutoComplete(destinationText)
        destinationText!!.setText(destination)


        val btnOk: Button = directionView.findViewById(R.id.btnOk)
        btnOk.setOnClickListener {
            val origin = originText!!.text
            val destination = destinationText!!.text
            if (TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination)) {
                if (TextUtils.isEmpty(origin)) {
                    originText!!.error = "Origin is required!"
                }
                if (TextUtils.isEmpty(destination)) {
                    destinationText!!.error = "Destination is required!"
                }
            } else {
                val selectedId = modeGroup!!.checkedRadioButtonId
                val mode = directionView.findViewById<RadioButton>(selectedId).text.toString()
                    .toLowerCase()
//            Directions: Davidson College of Engineering & North Parking Garage & walking
                Log.i("directions_info", "$origin, $destination, $mode")
                val args = Bundle()
                args.putString("param1", "Directions: $origin & $destination & $mode")
                activity!!.findNavController(R.id.nav_host_fragment).navigate(
                    R.id.action_to_nav_map,
                    args
                )
                Helper.hideSoftKeyboard(activity!!)
            }
        }
//        val btnCancel: Button = directionView.findViewById(R.id.btnCancel)
//        btnCancel.setOnClickListener {}
        return directionView
    }

    private fun configureAutoComplete(view: AutoCompleteTextView?) {
        val suggestions = resources.getStringArray(R.array.building_list)
        val myAdapter = ArrayAdapter(
            activity!!.applicationContext,
            android.R.layout.simple_list_item_1,
            suggestions
        )
        view!!.threshold = 1
        view!!.setAdapter(myAdapter)
    }
}