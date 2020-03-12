package com.h.pixeldroid.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.h.pixeldroid.LoginActivity

import com.h.pixeldroid.R


class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)

        val loginButton: Button = view.findViewById(R.id.button_start_login)
        loginButton.setOnClickListener {
            startActivity(Intent(view.context, LoginActivity::class.java))
        }

        return view
    }
}
