package com.andrerinas.openheadunit.connection

import android.app.Application
import android.content.Context
import com.andrerinas.openheadunit.decoder.AudioDecoder
import com.andrerinas.openheadunit.decoder.VideoDecoder
import com.andrerinas.openheadunit.utils.Settings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * CarLink: [CommManager] lives in AppComponent for the whole process, and AapService calls
 * [CommManager.destroy] at the end of every session while the ":aa" process stays alive. A destroy
 * that cancelled the scope left every later session unable to launch its teardown: no ByeBye, and
 * the socket to the phone's head unit server stayed open until the next connect() closed it, after
 * the new dial had been accepted.
 */
@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [32], manifest = Config.NONE)
class CommManagerDestroyTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val loopback: InetAddress = InetAddress.getLoopbackAddress()

    @Before fun freshSettings() {
        context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun manager(): CommManager {
        val settings = Settings(context)
        return CommManager(context, settings, AudioDecoder(), VideoDecoder(settings))
    }

    /** A connected car-side socket and the phone's end of it. */
    private fun ServerSocket.dial(): Pair<Socket, Socket> {
        val car = Socket(loopback, localPort)
        return car to accept()
    }

    @Test fun destroyClosesTheLiveConnection() = runBlocking {
        val manager = manager()
        ServerSocket(0, 1, loopback).use { server ->
            val (car, phone) = server.dial()
            phone.use {
                manager.connect(car)
                manager.destroy()
                assertTrue("the service's destroy closes the socket", car.isClosed)
            }
        }
    }

    @Test fun aSessionAfterDestroyStillClosesItsSocketOnDisconnect() = runBlocking {
        val manager = manager()
        ServerSocket(0, 1, loopback).use { server ->
            // The first session ends with the service destroyed, as every CarLink exit does.
            val (firstCar, firstPhone) = server.dial()
            firstPhone.use {
                manager.connect(firstCar)
                manager.destroy()
            }

            // The second session, in the same process and the same CommManager.
            val (car, phone) = server.dial()
            phone.use {
                manager.connect(car)
                manager.disconnect(sendByeBye = false, isUserExit = false)
                withTimeout(5_000) { manager.awaitDisconnectComplete() }
                assertTrue("disconnect() closes the socket the phone's server is holding", car.isClosed)
            }
        }
    }

    @Test fun aLinkLossTeardownAfterDestroyStillClosesItsSocket() = runBlocking {
        val manager = manager()
        ServerSocket(0, 1, loopback).use { server ->
            val (firstCar, firstPhone) = server.dial()
            firstPhone.use {
                manager.connect(firstCar)
                manager.destroy()
            }

            val (car, phone) = server.dial()
            phone.use {
                manager.connect(car)
                // disconnectForLinkLoss is the same launch on the same scope (a link going away).
                manager.disconnectForLinkLoss(5_000)
                assertTrue("the link-loss teardown closes the socket", car.isClosed)
                assertEquals(
                    "and leaves the manager disconnected",
                    CommManager.ConnectionState.Disconnected(isClean = false, isUserExit = false),
                    manager.connectionState.value
                )
            }
        }
    }
}
