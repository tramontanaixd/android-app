package com.pierdr.tramontana.ui

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pierdr.pierluigidallarosa.myactivity.R
import org.koin.android.ext.android.inject

private const val READY_TAG = "ready"
private const val SHOWTIME_TAG = "showtime"

class TramontanaActivity : AppCompatActivity(), MainView {
    private val presenter: MainPresenter by inject()

    init {
        lifecycle.addObserver(presenter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        showReadyFragment()
    }

    override fun showReadyFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, ReadyFragment(), READY_TAG)
                .commit()
    }

    override fun showShowtimeFragment() {
        val showtimeFragment = ShowtimeFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, showtimeFragment, SHOWTIME_TAG)
                .commit()
    }
}

interface MainView : LifecycleOwner {
    fun showReadyFragment()
    fun showShowtimeFragment()
}

