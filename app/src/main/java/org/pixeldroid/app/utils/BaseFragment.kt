package org.pixeldroid.app.utils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

/**
 * Base Fragment, for dependency injection and other things common to a lot of the fragments
 */
@AndroidEntryPoint
open class BaseFragment: Fragment() {

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    @Inject
    lateinit var db: AppDatabase

    internal val requestPermissionDownloadPic =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setMessage(R.string.write_permission_download_pic)
                        .setNegativeButton(android.R.string.ok) { _, _ -> }
                        .show()
                }

            }
        }
}
