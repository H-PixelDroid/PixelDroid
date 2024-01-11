package org.pixeldroid.app.directMessages.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.pixeldroid.app.R

class ConversationsFragment : Fragment() {

    companion object {
        fun newInstance() = ConversationsFragment()
    }

    private lateinit var viewModel: ConversationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ConversationsViewModel::class.java)
        // TODO: Use the ViewModel to watch the variable containing DM data and show it
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

}