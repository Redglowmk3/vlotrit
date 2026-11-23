package nl.ritbon.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class RouteTrackingService : Service(), LocationListener {
    private lateinit var manager: LocationManager
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(if (prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)) "VLOTRIT zakelijke werkdag" else "Vlotrit registreert je rit")
            .setContentText(if (prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)) "Ritten starten en stoppen automatisch" else "GPS-route wordt bijgehouden")
            .setOngoing(true)
            .build()
        startForeground(21, notification)
        manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return
        }
        val hasPrecise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val provider = if (hasPrecise) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        try {
            if (!manager.isProviderEnabled(provider)) throw IllegalStateException("Locatiebron uitgeschakeld")
            manager.requestLocationUpdates(provider, 2000L, 1f, this)
        } catch (_: Exception) {
            try { manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 1f, this) }
            catch (_: Exception) { prefs.edit().putString(KEY_GPS_ERROR, "Zet locatie aan en geef Vlotrit locatietoegang").apply() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_WORKDAY) {
            saveAutomaticBusinessTrip(prefs)
            prefs.edit().putBoolean(KEY_WORKDAY_ACTIVE, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        val hasPrecise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (location.accuracy > if (hasPrecise) 90f else 250f) return
        if (prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)) {
            handleBusinessLocation(location)
            return
        }
        if (!prefs.getBoolean(KEY_RUNNING, false)) return
        recordTripLocation(location)
    }

    private fun handleBusinessLocation(location: Location) {
        val oldLat = prefs.getFloat(KEY_LAST_LAT, Float.NaN).toDouble()
        val oldLon = prefs.getFloat(KEY_LAST_LON, Float.NaN).toDouble()
        val deltaKm = if (!oldLat.isNaN() && !oldLon.isNaN()) {
            val prior = Location("prior").apply { latitude = oldLat; longitude = oldLon }
            prior.distanceTo(location).toDouble() / 1000.0
        } else 0.0
        val moving = (location.hasSpeed() && location.speed >= 2.8f) || (deltaKm >= 0.015 && deltaKm < 0.5)
        val now = System.currentTimeMillis()
        val tripActive = prefs.getBoolean(KEY_BUSINESS_TRIP_ACTIVE, false)
        if (!tripActive) {
            movementStreak = if (moving) movementStreak + 1 else 0
            if (movementStreak >= 2) {
                val routeStart = if (oldLat.isNaN() || oldLon.isNaN()) "" else "$oldLat,$oldLon"
                prefs.edit().putBoolean(KEY_BUSINESS_TRIP_ACTIVE, true).putBoolean(KEY_RUNNING, true)
                    .putFloat(KEY_START_ODO, prefs.getFloat(KEY_LAST_ODO, 0f)).putFloat(KEY_TRACKED_KM, 0f)
                    .putString(KEY_ROUTE, routeStart).putLong(KEY_STARTED_AT, now).putLong(KEY_BUSINESS_LAST_MOVING_AT, now).apply()
                recordTripLocation(location)
            } else {
                prefs.edit().putFloat(KEY_LAST_LAT, location.latitude.toFloat()).putFloat(KEY_LAST_LON, location.longitude.toFloat()).apply()
            }
            return
        }
        movementStreak = if (moving) movementStreak + 1 else 0
        val confirmedMoving = movementStreak >= 2
        if (confirmedMoving) prefs.edit().putLong(KEY_BUSINESS_LAST_MOVING_AT, now).apply()
        recordTripLocation(location)
        val lastMovingAt = prefs.getLong(KEY_BUSINESS_LAST_MOVING_AT, prefs.getLong(KEY_STARTED_AT, now))
        if (!moving && !confirmedMoving && now - lastMovingAt >= BUSINESS_STOP_AFTER_IDLE_MS) {
            saveAutomaticBusinessTrip(prefs)
            movementStreak = 0
        }
    }

    private fun recordTripLocation(location: Location) {
        var km = prefs.getFloat(KEY_TRACKED_KM, 0f).toDouble()
        val oldLat = prefs.getFloat(KEY_LAST_LAT, Float.NaN).toDouble()
        val oldLon = prefs.getFloat(KEY_LAST_LON, Float.NaN).toDouble()
        if (!oldLat.isNaN() && !oldLon.isNaN()) {
            val prior = Location("prior").apply { latitude = oldLat; longitude = oldLon }
            val delta = prior.distanceTo(location).toDouble() / 1000.0
            if (delta >= 0.003 && delta < 0.5) km += delta
        }
        val route = prefs.getString(KEY_ROUTE, "").orEmpty()
        val point = "${location.latitude},${location.longitude}"
        // Limit stored route detail while retaining a clear, useful track.
        val points = route.split(';').filter { it.isNotBlank() }
        val updatedRoute = if (points.size < 700 && (points.isEmpty() || km != prefs.getFloat(KEY_TRACKED_KM, 0f).toDouble())) "$route${if (route.isEmpty()) "" else ";"}$point" else route
        prefs.edit().putFloat(KEY_TRACKED_KM, km.toFloat()).putFloat(KEY_LAST_LAT, location.latitude.toFloat())
            .putFloat(KEY_LAST_LON, location.longitude.toFloat()).putString(KEY_ROUTE, updatedRoute).remove(KEY_GPS_ERROR).apply()
    }

    @Deprecated("Deprecated by Android") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) { prefs.edit().putString(KEY_GPS_ERROR, "GPS uitgeschakeld").apply() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { if (::manager.isInitialized) manager.removeUpdates(this); super.onDestroy() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Ritregistratie", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private var movementStreak = 0
    companion object {
        private const val CHANNEL = "ritbon_tracking"
        private const val BUSINESS_STOP_AFTER_IDLE_MS = 5 * 60 * 1000L
    }
}

