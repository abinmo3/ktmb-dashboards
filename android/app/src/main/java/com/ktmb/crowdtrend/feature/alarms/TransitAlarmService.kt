package com.ktmb.crowdtrend.feature.alarms

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ktmb.crowdtrend.core.util.TransitAlarmNotificationHelper
import com.ktmb.crowdtrend.data.repository.TransitAlarmRepository

/**
 * Foreground Service that monitors device location and fires notifications
 * when the user approaches any active transit alarm's target station.
 *
 * Uses [LocationManager] with GPS provider via the classic [LocationListener] API,
 * which works on all API levels from 26 onward.
 */
class TransitAlarmService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var alarmRepo: TransitAlarmRepository

    private var isRunning = false
    private val triggered = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        alarmRepo = TransitAlarmRepository(this)
        TransitAlarmNotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        if (isRunning) return
        isRunning = true

        val activeCount = alarmRepo.alarms.value.count { it.isActive }
        val notification = TransitAlarmNotificationHelper.buildForegroundNotification(this, activeCount)
        ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        startLocationUpdates()
    }

    @Suppress("DEPRECATION")
    private fun stop() {
        isRunning = false
        locationManager.removeUpdates(locationListener)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) { /* permission revoked */ }
        catch (_: Exception) { /* GPS disabled */ }

        // Also register network provider as fallback (lower accuracy but works indoors)
        try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper(),
            )
        } catch (_: Exception) { /* network provider may be unavailable */ }
    }

    @Suppress("DEPRECATION")
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            checkProximity(location)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private fun checkProximity(current: Location) {
        val activeAlarms = alarmRepo.alarms.value.filter { it.isActive }
        for (alarm in activeAlarms) {
            if (alarm.id in triggered) continue

            val results = FloatArray(1)
            Location.distanceBetween(
                current.latitude, current.longitude,
                alarm.latitude, alarm.longitude,
                results,
            )
            if (results[0] <= alarm.radiusMeters) {
                triggered.add(alarm.id)
                TransitAlarmNotificationHelper.showAlarmNotification(
                    this@TransitAlarmService,
                    alarm.stationName,
                    alarm.label,
                )
            }
        }

        // Update foreground notification badge
        val activeCount = activeAlarms.size
        val notification = TransitAlarmNotificationHelper.buildForegroundNotification(this, activeCount)
        ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.ktmb.crowdtrend.alarm.START"
        const val ACTION_STOP = "com.ktmb.crowdtrend.alarm.STOP"

        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 30_000L
        private const val MIN_DISTANCE_METERS = 50f
    }
}
