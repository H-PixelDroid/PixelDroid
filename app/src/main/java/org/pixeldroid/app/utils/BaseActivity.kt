package org.pixeldroid.app.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (this.application as PixelDroidApplication).getAppComponent().inject(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}