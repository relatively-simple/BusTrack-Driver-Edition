package com.bustrack.driver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val database = FirebaseDatabase.getInstance().reference

    private var currentRouteId: String = ""
    private var currentBusId: String = ""
    private var currentStatusCode: Int = 1 // 1 = Active
    private var currentDelayTime: Long = 0L // Epoch seconds

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val routeId = intent?.getStringExtra("EXTRA_ROUTE_ID")
        val busId = intent?.getStringExtra("EXTRA_BUS_ID")
        val statusCode = intent?.getIntExtra("EXTRA_STATUS_CODE", 1) ?: 1
        val delayTime = intent?.getLongExtra("EXTRA_DELAY_TIME", 0L) ?: 0L

        if (routeId != null && busId != null) {
            currentRouteId = routeId
            currentBusId = busId
            currentStatusCode = statusCode
            currentDelayTime = delayTime
        }

        val isActionStop = intent?.getBooleanExtra("EXTRA_STOP_SERVICE", false) ?: false
        if (isActionStop) {
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()
        startLocationUpdates()
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "bus_broadcaster_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Broadcaster Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bus Broadcaster Active")
            .setContentText("Broadcasting location to Firebase...")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationTrackingService", "Missing location permissions", e)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun processLocation(location: Location) {
        if (currentRouteId.isEmpty() || currentBusId.isEmpty()) return

        // Velocity deadband < 4 km/h
        // location.speed is in meters/second. 4 km/h = 4000 / 3600 = 1.111 m/s
        val thresholdMpS = 4.0 / 3.6
        val speed = if (location.hasSpeed() && location.speed < thresholdMpS) {
            0.0f
        } else {
            location.speed
        }

        // Format: "lat,lng,speed,statusCode,delayTime"
        val payload = "${location.latitude},${location.longitude},$speed,$currentStatusCode,$currentDelayTime"
        
        database.child("live").child(currentRouteId).child(currentBusId).setValue(payload)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
