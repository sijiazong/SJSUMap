package com.example.sjsumap.ui.navigation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.sjsumap.R

private const val ARG_DEST = "destination"

class DirectionsFragment : Fragment() {
    private var originText: EditText? = null
    private var destinationText: EditText? = null
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
        destinationText!!.setText(destination)
        modeGroup = directionView.findViewById(R.id.mode)

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
            }
        }
        val btnCancel: Button = directionView.findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener {
//            dismiss()
        }
        return directionView
    }
}

//fun hideKeyboardFrom(context: Context, view: View) {
//    val imm: InputMethodManager =
//        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
//    imm.hideSoftInputFromWindow(view.windowToken, 0)
//}
//}