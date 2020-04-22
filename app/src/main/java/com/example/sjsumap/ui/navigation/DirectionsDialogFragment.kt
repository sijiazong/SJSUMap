package com.example.sjsumap.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.example.sjsumap.R


class DirectionsDialogFragment : DialogFragment() {
    var originText: EditText? = null
    var destinationText: EditText? = null
    var modeGroup: RadioGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val directionView: View = inflater.inflate(R.layout.fragment_directions, container, false)
        originText = directionView.findViewById(R.id.origin)
        destinationText = directionView.findViewById(R.id.destination)
        modeGroup = directionView.findViewById(R.id.mode)

        val btnOk: Button = directionView.findViewById(R.id.btnOk)
        btnOk.setOnClickListener {
            val origin = originText!!.text
            val destination = destinationText!!.text
            val selectedId = modeGroup!!.checkedRadioButtonId
            val mode = directionView.findViewById<RadioButton>(selectedId).text.toString().toLowerCase()
//            Directions: Davidson College of Engineering & North Parking Garage & walking
            Toast.makeText(activity, "$origin, $destination, $mode", Toast.LENGTH_LONG).show()
            val args = Bundle()
            args.putString("param1", "Directions: $origin & $destination & $mode")
            activity!!.findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_to_nav_map,
                args
            )
            dismiss()
        }
        val btnCancel: Button = directionView.findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener{
            dismiss()
        }
        return directionView
    }
}