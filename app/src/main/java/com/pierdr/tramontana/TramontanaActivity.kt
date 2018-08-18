package com.pierdr.tramontana

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pierdr.pierluigidallarosa.myactivity.Directive
import com.pierdr.pierluigidallarosa.myactivity.R

private const val READY_TAG = "ready"
private const val SHOWTIME_TAG = "showtime"

class TramontanaActivity : AppCompatActivity(), MainView {
    private val TAG = javaClass.simpleName

    private val presenter = MainPresenter(this)

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
        showtimeFragment.eventSink = presenter
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, showtimeFragment, SHOWTIME_TAG)
                .commit()
    }

    override fun runDirective(directive: Directive) {
        val fragmentWithShowtimeTag = supportFragmentManager?.findFragmentByTag(SHOWTIME_TAG)
                ?: throw IllegalStateException("got directive $directive but no showtime fragment present")

        val showtimeFragment = fragmentWithShowtimeTag as ShowtimeFragment
        showtimeFragment.runDirective(directive)
    }
}

interface MainView {
    fun showReadyFragment()
    fun showShowtimeFragment()
    fun runDirective(directive: Directive)
}

