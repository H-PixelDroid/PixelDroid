package org.pixeldroid.app.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
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
