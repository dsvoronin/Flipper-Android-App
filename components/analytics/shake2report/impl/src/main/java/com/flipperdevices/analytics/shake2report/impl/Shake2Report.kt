package com.flipperdevices.analytics.shake2report.impl

import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat
import com.flipperdevices.analytics.shake2report.impl.activity.Shake2ReportActivity
import com.flipperdevices.analytics.shake2report.impl.helper.ScreenshotHelper
import com.flipperdevices.analytics.shake2report.impl.listener.FlipperShakeDetector
import com.squareup.seismic.ShakeDetector
import fr.bipi.tressence.file.FileLoggerTree
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import java.io.File
import timber.log.Timber

private const val FILE_LOG_DIR = "log"
private const val FILE_LOG_SIZE = 1024 * 1024 // 1 MB
private const val FILE_LOG_LIMIT = 20
private const val VIBRATOR_TIME_MS = 500L

internal class Shake2Report(
    private val application: Application
) : ShakeDetector.Listener {
    private val shakeDetector = FlipperShakeDetector(this, application)
    private var vibrator = ContextCompat.getSystemService(application, Vibrator::class.java)
    private var screenshotHelper = ScreenshotHelper(application)
    private var logDir: File = File(application.cacheDir, FILE_LOG_DIR)
    private var screenshot: Bitmap? = null

    fun register() {
        SentryAndroid.init(application) { options ->
            options.addIntegration(
                SentryTimberIntegration(
                    minEventLevel = SentryLevel.FATAL,
                    minBreadcrumbLevel = SentryLevel.DEBUG
                )
            )
        }

        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val fileLoggerTree = FileLoggerTree.Builder()
            .withDir(logDir)
            .withFileName("timber-${System.currentTimeMillis()}-%g.log")
            .withSizeLimit(FILE_LOG_SIZE)
            .withFileLimit(FILE_LOG_LIMIT)
            .withMinPriority(Log.VERBOSE)
            .appendToFile(false)
            .build()
        Timber.plant(fileLoggerTree)

        shakeDetector.register()

        screenshotHelper.register()
    }

    internal fun getLogDir(): File {
        return logDir
    }

    internal fun getScreenshotAndReset(): Bitmap? {
        val screenshotToReturn = screenshot
        screenshot = null
        return screenshotToReturn
    }

    override fun hearShake() {
        vibrate()
        screenshot = screenshotHelper.takeScreenshot()

        application.startActivity(
            Intent(
                application,
                Shake2ReportActivity::class.java
            ).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    VIBRATOR_TIME_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            // deprecated in API 26 
            vibrator?.vibrate(VIBRATOR_TIME_MS)
        }
    }
}
