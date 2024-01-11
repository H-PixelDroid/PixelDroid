package org.pixeldroid.app.directMessages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.pixeldroid.app.R
import org.pixeldroid.app.directMessages.ui.main.ConversationsFragment

class ConversationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConversationsFragment.newInstance())
                .commitNow()
        }
    }
}