package com.stealth.calcbox.vault

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager

/**
 * Manages stealth safeguards, screen capture prevention (FLAG_SECURE),
 * auto-lock lifecycle grace periods, clipboard purging, and flip-to-lock detection.
 */
class AntiForensicsManager(
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    val flipSensorListener: FlipToLockSensorListener by lazy { FlipToLockSensorListener() }

    var pauseTimeMillis: Long? = null
        private set

    /**
     * Records timestamp when app enters background/paused state.
     */
    fun recordPauseTime(currentTimeMillis: Long = clock()): Long {
        pauseTimeMillis = currentTimeMillis
        return currentTimeMillis
    }

    /**
     * Resets the recorded pause time.
     */
    fun resetPauseTime() {
        pauseTimeMillis = null
    }

    /**
     * Checks if the app should auto-lock upon returning to foreground.
     *
     * Rules:
     * - If no pause time was recorded: returns false (not returning from pause).
     * - If [timeoutSeconds] <= 0: returns true (instant auto-lock on pause).
     * - If [timeoutSeconds] > 0: returns true if (resumeTimeMillis - pauseTimeMillis) >= timeoutSeconds * 1000L.
     */
    fun shouldAutoLock(
        resumeTimeMillis: Long = clock(),
        timeoutSeconds: Int
    ): Boolean {
        val paused = pauseTimeMillis ?: return false
        if (timeoutSeconds <= 0) {
            return true
        }
        return (resumeTimeMillis - paused) >= (timeoutSeconds * 1000L)
    }

    /**
     * Overload for convenience when using current clock for resumeTimeMillis.
     */
    fun shouldAutoLock(timeoutSeconds: Int): Boolean {
        return shouldAutoLock(clock(), timeoutSeconds)
    }

    /**
     * Enables FLAG_SECURE on activity window to prevent screenshots, screen recordings,
     * and Recent Apps task switcher snapshots.
     */
    fun enableSecureWindow(activity: Activity) {
        Companion.enableSecureWindow(activity)
    }

    /**
     * Clears FLAG_SECURE from activity window.
     */
    fun disableSecureWindow(activity: Activity) {
        Companion.disableSecureWindow(activity)
    }

    /**
     * Purges clipboard contents to prevent leaking sensitive credentials or copied vault data.
     */
    fun purgeClipboard(context: Context) {
        Companion.purgeClipboard(context)
    }

    /**
     * Registers flip-to-lock sensor listener.
     */
    fun registerFlipSensor(context: Context, onFaceDown: () -> Unit) {
        flipSensorListener.register(context, onFaceDown)
    }

    /**
     * Unregisters flip-to-lock sensor listener.
     */
    fun unregisterFlipSensor() {
        flipSensorListener.unregister()
    }

    companion object {
        fun enableSecureWindow(activity: Activity) {
            try {
                activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (_: Exception) {
            }
        }

        fun disableSecureWindow(activity: Activity) {
            try {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (_: Exception) {
            }
        }

        fun purgeClipboard(context: Context) {
            try {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboardManager.clearPrimaryClip()
                } else {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * SensorEventListener that monitors device orientation via accelerometer.
 * When the device is turned face-down (e.g. negative Z acceleration),
 * triggers an instant callback to immediately lock the vault.
 */
class FlipToLockSensorListener : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var onFaceDown: (() -> Unit)? = null
    @Volatile
    private var isFaceDown: Boolean = false

    companion object {
        const val FACE_DOWN_Z_THRESHOLD = -7.0f
        const val FACE_UP_Z_RESET_THRESHOLD = -2.0f
    }

    fun register(context: Context, onFaceDown: () -> Unit) {
        unregister()
        this.onFaceDown = onFaceDown
        this.isFaceDown = false
        try {
            val appContext = context.applicationContext ?: context
            val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                ?: return
            this.sensorManager = sm

            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {
        }
    }

    fun unregister() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
        } finally {
            sensorManager = null
            accelerometer = null
            onFaceDown = null
            isFaceDown = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values.getOrNull(2) ?: return
            if (z < FACE_DOWN_Z_THRESHOLD) {
                if (!isFaceDown) {
                    isFaceDown = true
                    onFaceDown?.invoke()
                }
            } else if (z > FACE_UP_Z_RESET_THRESHOLD) {
                isFaceDown = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Testing hook to simulate face-down detection without hardware sensors.
     */
    fun simulateFaceDown() {
        isFaceDown = true
        onFaceDown?.invoke()
    }

    fun isCurrentlyFaceDown(): Boolean = isFaceDown
}
