package com.example.sjsumap.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sjsumap.R

/**
 * A simple [Fragment] subclass.
 */
class ExploreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)
//        val textView: TextView = view.findViewById(R.id.text_gallery)
//        textView.text = "this is explore fragment"
        return view
    }
}