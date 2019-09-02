package com.pierdr.pierluigidallarosa.myactivity

import android.content.Intent
import androidx.test.rule.ActivityTestRule
import com.pierdr.tramontana.ui.TramontanaActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GivenNoActivityTest {
    @get:Rule
    val activityTestRule = ActivityTestRule(TramontanaActivity::class.java, false, false)

    @Test
    fun whenActivityStarts_theServerStartsToo() {
        activityTestRule.launchActivity(Intent())

        val latch = CountDownLatch(1)
        val client = TestClient(doOnOpen = { latch.countDown() })
        client.connect()
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun whenActivityPauses_theServerStops() {
        activityTestRule.launchActivity(Intent())
        activityTestRule.finishActivity()

        val latch = CountDownLatch(1)
        val client = TestClient(doOnError = { latch.countDown() })
        client.connect()
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS))
    }
}
