package com.h.pixeldroid.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.di.PixelfedAPIHolder
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
        (requireActivity().application as PixelDroidApplication).getAppComponent().inject(this)
    }

}
