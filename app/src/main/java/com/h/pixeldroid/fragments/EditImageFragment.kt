package com.h.pixeldroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.h.pixeldroid.R
import com.h.pixeldroid.interfaces.EditImageFragmentListener

class EditImageFragment : Fragment() {

    private var listener: EditImageFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_image, container, false)
    }
}
