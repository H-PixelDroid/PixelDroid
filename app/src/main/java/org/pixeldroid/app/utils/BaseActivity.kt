package org.pixeldroid.app.utils

import dagger.hilt.android.AndroidEntryPoint
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : org.pixeldroid.common.ThemedActivity() {

    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}