package com.pierdr.pierluigidallarosa.myactivity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import com.pierdr.pierluigidallarosa.myactivity.util.MockableClient
import com.pierdr.pierluigidallarosa.myactivity.util.ViewVisibilityIdlingResource
import com.pierdr.pierluigidallarosa.myactivity.util.WebSocketEndpoint
import com.pierdr.tramontana.ui.TramontanaActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import processing.data.JSONObject
import kotlin.test.assertTrue

class GivenConnectedTest {
    @get:Rule
    val activityTestRule = ActivityTestRule(TramontanaActivity::class.java, true, true)

    private lateinit var sketchVisibilityIdlingResource: ViewVisibilityIdlingResource
    private val endpoint: WebSocketEndpoint = mockk(relaxed = true)
    private val client = MockableClient(endpoint)

    @Before
    fun setUp() {
        client.connect()
        verify(timeout = 1000) { endpoint.onOpen(any()) }

        sketchVisibilityIdlingResource = ViewVisibilityIdlingResource(activityTestRule.activity.findViewById(R.id.sketch_container))
        IdlingRegistry.getInstance().register(sketchVisibilityIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(sketchVisibilityIdlingResource)
    }

    @Test
    fun whenTouchIsRegistered_andScreenIsTouched_thenTouchIsReported() {
        client.send(JSONObject()
                .setString("m", "registerTouch")
                .toString())

        for (i in 1 until 10) {
            onView(withId(R.id.sketch_container)).perform(click())

            // note: doesn't actually verify message order: "verifyOrder" doesn't support timeout.
            verify(timeout = 1000) { endpoint.onMessage(match { isMessageOfType(it, "touchedDown") }) }
            verify(timeout = 1000) { endpoint.onMessage(match { isMessageOfType(it, "touched") }) }
        }
    }

    private fun isMessageOfType(message: String, type: String): Boolean {
        val json = JSONObject.parse(message)
        assertTrue(json.hasKey("m"))
        return json.getString("m") == type
    }
}