package com.h.pixeldroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.fragments.ProfileFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG

class ProfileActivity : AppCompatActivity() {
    lateinit var profileFragment : ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set profile according to given account
        val account = intent.getSerializableExtra(ACCOUNT_TAG) as Account

        profileFragment = ProfileFragment()
        val arguments = Bundle()
        arguments.putSerializable("profileTag", account)
        profileFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.profileFragmentSingle, profileFragment).commit()
    }
}