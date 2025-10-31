package com.example.intouch

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class RequestsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requests)

        initializeViews()
        setupToolbar()
        setupViewPager()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = RequestsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Requested Me"
                1 -> "I Requested"
                else -> ""
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_requests, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_incoming -> {
                // Find current instance of RequestedMeFragment and clear
                val fragment = supportFragmentManager.findFragmentByTag("f0") as? RequestedMeFragment
                    ?: currentRequestedMe()
                fragment?.clearAllIncomingRequests()
                true
            }
            R.id.action_clear_outgoing -> {
                val fragment = supportFragmentManager.findFragmentByTag("f1") as? IRequestedFragment
                    ?: currentIRequested()
                fragment?.clearAllOutgoingRequests()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun currentRequestedMe(): RequestedMeFragment? {
        return supportFragmentManager.fragments.firstOrNull { it is RequestedMeFragment } as? RequestedMeFragment
    }

    private fun currentIRequested(): IRequestedFragment? {
        return supportFragmentManager.fragments.firstOrNull { it is IRequestedFragment } as? IRequestedFragment
    }

    private class RequestsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RequestedMeFragment()
                1 -> IRequestedFragment()
                else -> RequestedMeFragment()
            }
        }
    }
}
