package com.h.pixeldroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.h.pixeldroid.Pixeldroid
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.di.PixelfedAPIHolder
import javax.inject.Inject

/**
 * Base Fragment, for dependency injection and other things common to a lot of the fragments
 */
open class BaseFragment: Fragment() {

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    @Inject
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as Pixeldroid).getAppComponent().inject(this)
    }

}
