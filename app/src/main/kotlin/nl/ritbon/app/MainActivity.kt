package nl.ritbon.app

import android.Manifest
import android.app.PendingIntent
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.text.Html
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

const val PREFS = "ritbon_prefs"
const val KEY_LAST_ODO = "last_odo"
const val KEY_START_ODO = "start_odo"
const val KEY_END_ODO = "end_odo"
const val KEY_TRACKED_KM = "tracked_km"
const val KEY_LAST_LAT = "last_lat"
const val KEY_LAST_LON = "last_lon"
const val KEY_ROUTE = "route_points"
const val KEY_GPS_ERROR = "gps_error"
const val KEY_RUNNING = "running"
const val KEY_CONSUMPTION = "consumption"
const val KEY_PRICE = "price"
const val KEY_NAME = "trip_name"
const val KEY_STARTED_AT = "started_at"
const val KEY_STOPPED_AT = "stopped_at"
const val KEY_HISTORY = "trip_history"
const val KEY_HISTORY_MIGRATED = "trip_history_migrated"
const val KEY_CHARGE_HISTORY = "charge_history"
const val KEY_PRICE_UPDATED_AT = "price_updated_at"
const val KEY_ELECTRIC_CONSUMPTION = "electric_consumption"
const val KEY_ELECTRIC_PRICE = "electric_price"
const val KEY_TRIP_ELECTRIC = "trip_electric"
const val KEY_FUEL_DEPRECIATION = "fuel_depreciation"
const val KEY_ELECTRIC_DEPRECIATION = "electric_depreciation"
const val KEY_DEPRECIATION_RATE = "depreciation_rate"
const val KEY_WORKDAY_ACTIVE = "business_workday_active"
const val KEY_BUSINESS_TRIP_ACTIVE = "business_trip_active"
const val KEY_BUSINESS_LABEL = "business_label"
const val KEY_BUSINESS_TRIP_COUNT = "business_trip_count"
const val KEY_BUSINESS_STARTED_AT = "business_started_at"
const val KEY_BUSINESS_LAST_MOVING_AT = "business_last_moving_at"
const val ACTION_START_WORKDAY = "nl.ritbon.app.START_WORKDAY"
const val ACTION_STOP_WORKDAY = "nl.ritbon.app.STOP_WORKDAY"
private const val KEY_VEHICLES = "vehicle_profiles"
private const val KEY_SELECTED_VEHICLE = "selected_vehicle"
private const val KEY_ACTIVE_VEHICLE_ID = "active_vehicle_id"
private const val KEY_ACTIVE_VEHICLE_NAME = "active_vehicle_name"
private const val KEY_ACTIVE_FUEL_TYPE = "active_fuel_type"
private const val KEY_ACTIVE_CONSUMPTION = "active_consumption"
private const val KEY_ACTIVE_PRICE = "active_price"
private const val KEY_ACTIVE_DEPRECIATION = "active_depreciation"
private const val KEY_ACTIVE_DEPRECIATION_RATE = "active_depreciation_rate"
private const val KEY_MAINTENANCE_REMINDERS = "maintenance_reminders"

private val Forest = Color(0xFF176DB3)
private val Ink = Color(0xFF14283F)
private val Muted = Color(0xFF68798D)
private val Paper = Color(0xFFF3F7FC)
private val BlueTint = Color(0xFFE7F1FB)
private const val UPDATE_REPOSITORY = "redglowmk3/vlotrit"
private const val UPDATE_INSTALL_STATUS_ACTION = "nl.ritbon.app.UPDATE_INSTALL_STATUS"

private fun isElectricFuelType(type: String) = type.equals("EV", true) || type.equals("Elektrisch", true)
private fun fuelTypeLabel(type: String) = if (isElectricFuelType(type)) "EV" else type

internal data class VehicleProfile(
    val id: String,
    val name: String,
    val fuelType: String,
    val consumption: Float,
    val price: Float,
    val depreciationPerKm: Float,
    val depreciationEnabled: Boolean,
    val licensePlate: String = ""
) {
    val electric get() = isElectricFuelType(fuelType)
    val energyUnit get() = if (electric) "kWh" else "L"
}

private data class TripRecord(
    val id: Long, val name: String, val startKm: Float, val endKm: Float, val distanceKm: Float,
    val consumption: Float, val price: Float, val priceUpdatedAt: Long, val startedAt: Long, val stoppedAt: Long, val route: String,
    val electric: Boolean = false, val startLocation: String = "", val endLocation: String = "",
    val depreciationEnabled: Boolean = false, val depreciationPerKm: Float = 0.15f,
    val vehicleId: String = "", val vehicleName: String = "", val fuelType: String = "", val actualEnergy: Float? = null,
    val category: String = "Privé", val passengers: List<String> = emptyList()
) {
    val estimatedEnergyAmount get() = distanceKm * consumption / 100f
    val energyAmount get() = actualEnergy ?: estimatedEnergyAmount
    val energyUnit get() = if (electric) "kWh" else "L"
    val consumptionUnit get() = if (electric) "kWh/100 km" else "L/100 km"
    val priceUnit get() = if (electric) "€/kWh" else "€/L"
    val energyCost get() = energyAmount * price
    val depreciationCost get() = if (depreciationEnabled) distanceKm * depreciationPerKm else 0f
    val cost get() = energyCost + depreciationCost
}

private data class ChargeSession(
    val id: Long,
    val vehicleId: String,
    val vehicleName: String,
    val kwh: Float,
    val pricePerKwh: Float,
    val location: String,
    val timestamp: Long
) {
    val cost get() = kwh * pricePerKwh
}

private data class InsightBucket(val label: String, val km: Float, val cost: Float, val trips: Int)

private data class NearbyStation(
    val name: String,
    val kind: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val details: String,
    val priceE10: Float? = null,
    val priceE5: Float? = null,
    val priceDiesel: Float? = null,
    val priceCheckedAt: Long = 0L,
    val priceSource: String = "",
    val website: String = "",
    val phone: String = "",
    val openingHours: String = "",
    val city: String = ""
)

private fun readChargeSessions(prefs: android.content.SharedPreferences): List<ChargeSession> = try {
    val array = JSONArray(prefs.getString(KEY_CHARGE_HISTORY, "[]"))
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        ChargeSession(o.optLong("id"), o.optString("vehicleId"), o.optString("vehicleName"),
            o.optDouble("kwh").toFloat(), o.optDouble("pricePerKwh").toFloat(), o.optString("location"), o.optLong("timestamp"))
    }.sortedByDescending { it.timestamp }
} catch (_: Exception) { emptyList() }

private fun writeChargeSessions(prefs: android.content.SharedPreferences, sessions: List<ChargeSession>) {
    val array = JSONArray()
    sessions.forEach { s -> array.put(JSONObject().put("id", s.id).put("vehicleId", s.vehicleId).put("vehicleName", s.vehicleName)
        .put("kwh", s.kwh).put("pricePerKwh", s.pricePerKwh).put("location", s.location).put("timestamp", s.timestamp)) }
    prefs.edit().putString(KEY_CHARGE_HISTORY, array.toString()).apply()
}

private fun buildInsightBuckets(trips: List<TripRecord>, range: String, today: LocalDate = LocalDate.now()): List<InsightBucket> {
    val zone = ZoneId.systemDefault()
    return when (range) {
        "Week" -> (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayTrips = trips.filter { Instant.ofEpochMilli(it.stoppedAt).atZone(zone).toLocalDate() == date }
            InsightBucket(date.format(DateTimeFormatter.ofPattern("EE", Locale("nl", "NL"))), dayTrips.sumOf { it.distanceKm.toDouble() }.toFloat(),
                dayTrips.sumOf { it.cost.toDouble() }.toFloat(), dayTrips.size)
        }
        "Jaar" -> (4 downTo 0).map { offset ->
            val year = today.year - offset
            val yearTrips = trips.filter { Instant.ofEpochMilli(it.stoppedAt).atZone(zone).year == year }
            InsightBucket(year.toString(), yearTrips.sumOf { it.distanceKm.toDouble() }.toFloat(), yearTrips.sumOf { it.cost.toDouble() }.toFloat(), yearTrips.size)
        }
        else -> (5 downTo 0).map { offset ->
            val month = YearMonth.from(today).minusMonths(offset.toLong())
            val monthTrips = trips.filter { YearMonth.from(Instant.ofEpochMilli(it.stoppedAt).atZone(zone).toLocalDate()) == month }
            val label = month.format(DateTimeFormatter.ofPattern("MMM", Locale("nl", "NL"))).replaceFirstChar { it.titlecase(Locale("nl", "NL")) }
            InsightBucket(label, monthTrips.sumOf { it.distanceKm.toDouble() }.toFloat(), monthTrips.sumOf { it.cost.toDouble() }.toFloat(), monthTrips.size)
        }
    }
}

private fun readHistory(prefs: android.content.SharedPreferences): List<TripRecord> = try {
    val array = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
    val trips = (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        TripRecord(o.optLong("id"), o.optString("name"), o.optDouble("startKm").toFloat(), o.optDouble("endKm").toFloat(),
            o.optDouble("distanceKm").toFloat(), o.optDouble("consumption", 11.0).toFloat(), o.optDouble("price", 2.092).toFloat(),
            o.optLong("priceUpdatedAt"), o.optLong("startedAt"), o.optLong("stoppedAt"), o.optString("route"), o.optBoolean("electric", false),
            o.optString("startLocation"), o.optString("endLocation"), o.optBoolean("depreciationEnabled", false), o.optDouble("depreciationPerKm", 0.15).toFloat(),
            o.optString("vehicleId"), o.optString("vehicleName"), fuelTypeLabel(o.optString("fuelType", if (o.optBoolean("electric", false)) "EV" else "E10")),
            if (o.isNull("actualEnergy")) null else o.optDouble("actualEnergy").toFloat(),
            o.optString("category", "Privé"), (o.optJSONArray("passengers") ?: JSONArray()).let { names -> (0 until names.length()).map { names.optString(it) }.filter { it.isNotBlank() } })
    }
    if (trips.isNotEmpty()) {
        prefs.edit().putBoolean(KEY_HISTORY_MIGRATED, true).apply()
        trips
    } else if (!prefs.getBoolean(KEY_HISTORY_MIGRATED, false) && prefs.getLong(KEY_STOPPED_AT, 0L) > 0L) {
        val end = prefs.getFloat(KEY_END_ODO, 0f)
        val start = prefs.getFloat(KEY_START_ODO, end)
        val legacyTrip = TripRecord(prefs.getLong(KEY_STOPPED_AT, 0L), prefs.getString(KEY_NAME, "").orEmpty(), start, end,
            prefs.getFloat(KEY_TRACKED_KM, (end - start).coerceAtLeast(0f)), prefs.getFloat(KEY_CONSUMPTION, 11f),
            prefs.getFloat(KEY_PRICE, 2.092f), prefs.getLong(KEY_PRICE_UPDATED_AT, 0L), prefs.getLong(KEY_STARTED_AT, 0L), prefs.getLong(KEY_STOPPED_AT, 0L),
            prefs.getString(KEY_ROUTE, "").orEmpty(), prefs.getBoolean(KEY_TRIP_ELECTRIC, false))
        writeHistory(prefs, listOf(legacyTrip))
        prefs.edit().putBoolean(KEY_HISTORY_MIGRATED, true).apply()
        listOf(legacyTrip)
    } else {
        prefs.edit().putBoolean(KEY_HISTORY_MIGRATED, true).apply()
        emptyList()
    }
} catch (_: Exception) { emptyList() }

private fun writeHistory(prefs: android.content.SharedPreferences, trips: List<TripRecord>) {
    val array = JSONArray()
    trips.forEach { t -> array.put(JSONObject().put("id", t.id).put("name", t.name).put("startKm", t.startKm).put("endKm", t.endKm)
        .put("distanceKm", t.distanceKm).put("consumption", t.consumption).put("price", t.price)
        .put("priceUpdatedAt", t.priceUpdatedAt).put("startedAt", t.startedAt).put("stoppedAt", t.stoppedAt).put("route", t.route).put("electric", t.electric)
        .put("startLocation", t.startLocation).put("endLocation", t.endLocation)
        .put("depreciationEnabled", t.depreciationEnabled).put("depreciationPerKm", t.depreciationPerKm)
        .put("vehicleId", t.vehicleId).put("vehicleName", t.vehicleName).put("fuelType", t.fuelType)
        .put("actualEnergy", t.actualEnergy ?: JSONObject.NULL).put("category", t.category).put("passengers", JSONArray(t.passengers))) }
    prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
}

internal fun saveAutomaticBusinessTrip(prefs: android.content.SharedPreferences): Boolean {
    if (!prefs.getBoolean(KEY_BUSINESS_TRIP_ACTIVE, false)) return false
    val km = prefs.getFloat(KEY_TRACKED_KM, 0f).coerceAtLeast(0f)
    if (km < 0.03f) {
        prefs.edit().putBoolean(KEY_BUSINESS_TRIP_ACTIVE, false).putBoolean(KEY_RUNNING, false).apply()
        return false
    }
    val stoppedAt = System.currentTimeMillis()
    val nextCount = prefs.getInt(KEY_BUSINESS_TRIP_COUNT, 0) + 1
    val workLabel = prefs.getString(KEY_BUSINESS_LABEL, "").orEmpty().trim()
    val name = "${workLabel.ifBlank { "Zakelijke rit" }} · rit $nextCount"
    val startKm = prefs.getFloat(KEY_START_ODO, prefs.getFloat(KEY_LAST_ODO, 0f))
    val endKm = startKm + km
    val route = prefs.getString(KEY_ROUTE, "").orEmpty()
    val endpoints = routeEndpoints(route)
    val electric = prefs.getBoolean(KEY_TRIP_ELECTRIC, false)
    val fuelType = fuelTypeLabel(prefs.getString(KEY_ACTIVE_FUEL_TYPE, if (electric) "EV" else "E10").orEmpty())
    val record = TripRecord(
        stoppedAt, name, startKm, endKm, km,
        prefs.getFloat(KEY_ACTIVE_CONSUMPTION, if (electric) 18f else 11f),
        prefs.getFloat(KEY_ACTIVE_PRICE, if (electric) 0.30f else 2.092f),
        if (fuelType == "E10") prefs.getLong(KEY_PRICE_UPDATED_AT, 0L) else 0L,
        prefs.getLong(KEY_STARTED_AT, stoppedAt), stoppedAt, route, electric,
        endpoints.first?.let(::coordinateLabel).orEmpty(), endpoints.second?.let(::coordinateLabel).orEmpty(),
        prefs.getBoolean(KEY_ACTIVE_DEPRECIATION, false), prefs.getFloat(KEY_ACTIVE_DEPRECIATION_RATE, 0.15f),
        prefs.getString(KEY_ACTIVE_VEHICLE_ID, "").orEmpty(), prefs.getString(KEY_ACTIVE_VEHICLE_NAME, "").orEmpty(), fuelType,
        category = "Zakelijk"
    )
    writeHistory(prefs, listOf(record) + readHistory(prefs))
    prefs.edit().putBoolean(KEY_BUSINESS_TRIP_ACTIVE, false).putBoolean(KEY_RUNNING, false)
        .putFloat(KEY_END_ODO, endKm).putFloat(KEY_LAST_ODO, endKm).putLong(KEY_STOPPED_AT, stoppedAt)
        .putInt(KEY_BUSINESS_TRIP_COUNT, nextCount).apply()
    return true
}

internal fun startBusinessWorkday(context: Context, prefs: android.content.SharedPreferences, vehicle: VehicleProfile, label: String, startOdometer: Float? = null) {
    val initialOdometer = startOdometer ?: prefs.getFloat(KEY_LAST_ODO, prefs.getFloat(KEY_START_ODO, 0f))
    prefs.edit().putBoolean(KEY_WORKDAY_ACTIVE, true).putBoolean(KEY_BUSINESS_TRIP_ACTIVE, false).putBoolean(KEY_RUNNING, false)
        .putString(KEY_BUSINESS_LABEL, label.trim()).putInt(KEY_BUSINESS_TRIP_COUNT, 0).putLong(KEY_BUSINESS_STARTED_AT, System.currentTimeMillis())
        .putFloat(KEY_LAST_ODO, initialOdometer).putFloat(KEY_START_ODO, initialOdometer)
        .putString(KEY_ACTIVE_VEHICLE_ID, vehicle.id).putString(KEY_ACTIVE_VEHICLE_NAME, vehicle.name).putString(KEY_ACTIVE_FUEL_TYPE, vehicle.fuelType)
        .putBoolean(KEY_TRIP_ELECTRIC, vehicle.electric).putFloat(KEY_ACTIVE_CONSUMPTION, vehicle.consumption).putFloat(KEY_ACTIVE_PRICE, vehicle.price)
        .putBoolean(KEY_ACTIVE_DEPRECIATION, vehicle.depreciationEnabled).putFloat(KEY_ACTIVE_DEPRECIATION_RATE, vehicle.depreciationPerKm)
        .putLong(KEY_STOPPED_AT, 0L).putFloat(KEY_LAST_LAT, Float.NaN).putFloat(KEY_LAST_LON, Float.NaN).apply()
    ContextCompat.startForegroundService(context, Intent(context, RouteTrackingService::class.java).setAction(ACTION_START_WORKDAY))
}

internal fun stopBusinessWorkday(context: Context, prefs: android.content.SharedPreferences) {
    prefs.edit().putBoolean(KEY_WORKDAY_ACTIVE, false).apply()
    context.stopService(Intent(context, RouteTrackingService::class.java))
    saveAutomaticBusinessTrip(prefs)
    prefs.edit().putBoolean(KEY_BUSINESS_TRIP_ACTIVE, false).apply()
}

private fun readVehicleProfiles(prefs: android.content.SharedPreferences): List<VehicleProfile> = try {
    val saved = prefs.getString(KEY_VEHICLES, null)
    if (saved != null) {
        val array = JSONArray(saved)
        (0 until array.length()).map { i -> array.getJSONObject(i).let { o ->
            VehicleProfile(o.optString("id"), o.optString("name"), fuelTypeLabel(o.optString("fuelType", "E10")),
                o.optDouble("consumption", 11.0).toFloat(), o.optDouble("price", 2.092).toFloat(),
                o.optDouble("depreciationPerKm", 0.15).toFloat(), o.optBoolean("depreciationEnabled", false), o.optString("licensePlate"))
        } }.ifEmpty { defaultVehicleProfiles(prefs) }
    } else defaultVehicleProfiles(prefs)
} catch (_: Exception) { defaultVehicleProfiles(prefs) }

private fun defaultVehicleProfiles(prefs: android.content.SharedPreferences) = listOf(
    VehicleProfile("default-fuel", "Mijn auto", "E10", prefs.getFloat(KEY_CONSUMPTION, 11f), prefs.getFloat(KEY_PRICE, 2.092f),
        prefs.getFloat(KEY_DEPRECIATION_RATE, 0.15f), prefs.getBoolean(KEY_FUEL_DEPRECIATION, false)),
    VehicleProfile("default-electric", "Mijn elektrische auto", "EV", prefs.getFloat(KEY_ELECTRIC_CONSUMPTION, 18f),
        prefs.getFloat(KEY_ELECTRIC_PRICE, 0.30f), prefs.getFloat(KEY_DEPRECIATION_RATE, 0.15f), prefs.getBoolean(KEY_ELECTRIC_DEPRECIATION, false))
)

private fun writeVehicleProfiles(prefs: android.content.SharedPreferences, profiles: List<VehicleProfile>) {
    val array = JSONArray()
    profiles.forEach { p -> array.put(JSONObject().put("id", p.id).put("name", p.name).put("fuelType", p.fuelType)
        .put("consumption", p.consumption).put("price", p.price).put("depreciationPerKm", p.depreciationPerKm)
        .put("depreciationEnabled", p.depreciationEnabled).put("licensePlate", p.licensePlate)) }
    prefs.edit().putString(KEY_VEHICLES, array.toString()).apply()
}

private data class RdwVehicleData(val makeModel: String, val apkDate: String, val consumption: Float?, val electric: Boolean)

private fun lookupRdwVehicle(licensePlate: String): RdwVehicleData {
    val plate = licensePlate.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    require(plate.length in 5..8) { "Controleer het kenteken en probeer opnieuw." }
    fun fetch(dataset: String): JSONObject? {
        val url = URL("https://opendata.rdw.nl/resource/$dataset.json?kenteken=${URLEncoder.encode(plate, "UTF-8")}&\$limit=1")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 12000; requestMethod = "GET"
            setRequestProperty("User-Agent", "VLOTRIT/1.23 (RDW voertuiggegevens)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val result = connection.inputStream.bufferedReader().use { it.readText() }
            JSONArray(result).optJSONObject(0)
        } finally { connection.disconnect() }
    }
    val general = fetch("m9d7-ebf2")
    val inspection = fetch("m4ep-j23h")
    val fuel = fetch("8ys7-d773")
    val energy = (fuel?.optString("brandstof_omschrijving").orEmpty()).contains("elektr", true)
    fun number(vararg keys: String): Float? = keys.firstNotNullOfOrNull { key ->
        fuel?.optString(key)?.replace(',', '.')?.toFloatOrNull()?.takeIf { it > 0f }
    }
    val consumption = if (energy) {
        number("elektrisch_verbruik_enkel_elektrisch_wltp")?.times(0.1f)
    } else {
        number("brandstof_verbruik_gecombineerd_wltp", "brandstofverbruik_gecombineerd")
    }
    val rawDate = inspection?.optString("vervaldatum_apk_dt")?.takeIf { it.isNotBlank() && it != "null" }
        ?: inspection?.optString("vervaldatum_apk")?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    val digits = rawDate.take(10).filter(Char::isDigit)
    val apk = if (digits.length >= 8) "${digits.take(4)}-${digits.substring(4, 6)}-${digits.substring(6, 8)}" else ""
    val title = listOf(general?.optString("merk").orEmpty(), general?.optString("handelsbenaming").orEmpty())
        .filter { it.isNotBlank() && it != "null" }.distinct().joinToString(" ")
    if (general == null && inspection == null && fuel == null) error("Geen voertuig gevonden bij de RDW. Controleer het kenteken.")
    return RdwVehicleData(title, apk, consumption, energy)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RitbonApp() } }
}

@Composable
private fun RitbonApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var profiles by remember { mutableStateOf(readVehicleProfiles(prefs)) }
    var selectedVehicleId by remember { mutableStateOf(prefs.getString(KEY_SELECTED_VEHICLE, null)
        ?: profiles.firstOrNull { it.electric == prefs.getBoolean(KEY_TRIP_ELECTRIC, false) }?.id ?: profiles.first().id) }
    val selectedVehicle = profiles.firstOrNull { it.id == selectedVehicleId } ?: profiles.first()
    var startOdo by remember { mutableStateOf((if (prefs.getBoolean(KEY_RUNNING, false)) prefs.getFloat(KEY_START_ODO, 0f) else prefs.getFloat(KEY_LAST_ODO, prefs.getFloat(KEY_START_ODO, 0f))).formatInput()) }
    var distance by remember { mutableStateOf(prefs.getFloat(KEY_TRACKED_KM, 0f).formatInput()) }
    var consumption by remember { mutableStateOf(prefs.getFloat(KEY_CONSUMPTION, 11f).formatInput()) }
    var price by remember { mutableStateOf(prefs.getFloat(KEY_PRICE, 2.092f).formatPrice()) }
    var electricConsumption by remember { mutableStateOf(prefs.getFloat(KEY_ELECTRIC_CONSUMPTION, 18f).formatInput()) }
    var electricPrice by remember { mutableStateOf(prefs.getFloat(KEY_ELECTRIC_PRICE, 0.30f).formatPrice()) }
    var tripName by remember { mutableStateOf(prefs.getString(KEY_NAME, "").orEmpty()) }
    var running by remember { mutableStateOf(prefs.getBoolean(KEY_RUNNING, false)) }
    var workdayActive by remember { mutableStateOf(prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)) }
    var businessTripActive by remember { mutableStateOf(prefs.getBoolean(KEY_BUSINESS_TRIP_ACTIVE, false)) }
    var businessLabel by remember { mutableStateOf(prefs.getString(KEY_BUSINESS_LABEL, "").orEmpty()) }
    var businessStartOdo by remember { mutableStateOf(prefs.getFloat(KEY_LAST_ODO, prefs.getFloat(KEY_START_ODO, 0f)).formatInput()) }
    var pendingBusinessStart by remember { mutableStateOf(false) }
    var endOdo by remember { mutableStateOf(prefs.getFloat(KEY_END_ODO, 0f)) }
    var route by remember { mutableStateOf(prefs.getString(KEY_ROUTE, "").orEmpty()) }
    var history by remember { mutableStateOf(readHistory(prefs)) }
    var selectedHistoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTab by remember { mutableIntStateOf(if (prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)) 4 else if (prefs.getBoolean(KEY_RUNNING, false) && prefs.getBoolean(KEY_TRIP_ELECTRIC, false)) 1 else 0) }
    var fuelDepreciation by remember { mutableStateOf(prefs.getBoolean(KEY_FUEL_DEPRECIATION, false)) }
    var electricDepreciation by remember { mutableStateOf(prefs.getBoolean(KEY_ELECTRIC_DEPRECIATION, false)) }
    var depreciationRate by remember { mutableStateOf(prefs.getFloat(KEY_DEPRECIATION_RATE, 0.15f).formatPrice()) }
    var gpsError by remember { mutableStateOf(prefs.getString(KEY_GPS_ERROR, "").orEmpty()) }
    var priceInfo by remember { mutableStateOf("E10 prijs ophalen…") }
    var info by remember { mutableStateOf("") }
    var latestRelease by remember { mutableStateOf<AppRelease?>(null) }
    var availableRelease by remember { mutableStateOf<AppRelease?>(null) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var logoConfettiBurst by remember { mutableIntStateOf(0) }
    var showLogoConfetti by remember { mutableStateOf(false) }
    var showFreeFuelDialog by remember { mutableStateOf(false) }
    var showGoodNightDialog by remember { mutableStateOf(false) }
    var showUpsideDownDialog by remember { mutableStateOf(false) }
    var upsideDownShown by remember { mutableStateOf(false) }
    var selectedShakeTrip by remember { mutableStateOf<TripRecord?>(null) }
    var noShakeTrips by remember { mutableStateOf(false) }
    val latestHistory by rememberUpdatedState(history)
    val openingGreeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour in listOf(23, 0, 1, 2, 3, 4) -> "Nog laat onderweg? Houd je ogen op de weg."
            hour in 5..6 -> "Vroeg uit de veren! De koffie staat hopelijk al klaar."
            else -> ""
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    suspend fun checkForAppUpdate() {
        if (checkingUpdates) return
        checkingUpdates = true
        try {
            val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
            val latest = withContext(Dispatchers.IO) { fetchLatestRelease(UPDATE_REPOSITORY, currentVersion) }
            latestRelease = latest
            availableRelease = latest.takeIf { compareVersionNames(it.versionName, currentVersion) > 0 }
        } catch (_: Exception) {
            latestRelease = null
            availableRelease = null
        } finally {
            prefs.edit().putLong("updates_last_check", System.currentTimeMillis()).apply()
            checkingUpdates = false
        }
    }

    LaunchedEffect(logoConfettiBurst) {
        if (logoConfettiBurst > 0) {
            showLogoConfetti = true
            playEasterEggSound()
            delay(1800)
            showLogoConfetti = false
        }
    }

    DisposableEffect(context, selectedTab, running, workdayActive) {
        if (selectedTab != 0 || running || workdayActive) {
            onDispose { }
        } else {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastShakeAt = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                    val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                    val now = System.currentTimeMillis()
                    if (magnitude > 20f && now - lastShakeAt > 1600L) {
                        lastShakeAt = now
                        val trip = latestHistory.randomOrNull()
                        if (trip == null) noShakeTrips = true else selectedShakeTrip = trip
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { manager.unregisterListener(listener) }
        }
    }

    DisposableEffect(context, selectedTab) {
        if (selectedTab != 0) onDispose { }
        else {
            val listener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (!upsideDownShown && orientation in 165..195) {
                        upsideDownShown = true
                        showUpsideDownDialog = true
                    }
                }
            }
            if (listener.canDetectOrientation()) listener.enable()
            onDispose { listener.disable() }
        }
    }

    fun selectVehicle(profile: VehicleProfile, navigateToTripTab: Boolean = true) {
        selectedVehicleId = profile.id
        prefs.edit().putString(KEY_SELECTED_VEHICLE, profile.id).apply()
        consumption = profile.consumption.formatInput(); price = profile.price.formatPrice()
        electricConsumption = profile.consumption.formatInput(); electricPrice = profile.price.formatPrice()
        depreciationRate = profile.depreciationPerKm.formatPrice()
        if (profile.electric) {
            electricDepreciation = profile.depreciationEnabled
            prefs.edit().putFloat(KEY_ELECTRIC_CONSUMPTION, profile.consumption).putFloat(KEY_ELECTRIC_PRICE, profile.price)
                .putBoolean(KEY_ELECTRIC_DEPRECIATION, profile.depreciationEnabled).apply()
            if (navigateToTripTab) selectedTab = 1
        } else {
            fuelDepreciation = profile.depreciationEnabled
            prefs.edit().putFloat(KEY_CONSUMPTION, profile.consumption).putFloat(KEY_PRICE, profile.price)
                .putBoolean(KEY_FUEL_DEPRECIATION, profile.depreciationEnabled).apply()
            if (navigateToTripTab) selectedTab = 0
        }
        prefs.edit().putFloat(KEY_DEPRECIATION_RATE, profile.depreciationPerKm).apply()
    }
    fun updateSelectedVehicle(profile: VehicleProfile) {
        profiles = profiles.map { if (it.id == profile.id) profile else it }
        writeVehicleProfiles(prefs, profiles)
    }
    fun saveVehicleProfile(profile: VehicleProfile) {
        profiles = profiles.filterNot { it.id == profile.id } + profile
        writeVehicleProfiles(prefs, profiles)
        selectVehicle(profile, navigateToTripTab = selectedTab != 10)
        if (profile.licensePlate.isNotBlank()) coroutineScope.launch {
            try {
                val rdw = withContext(Dispatchers.IO) { lookupRdwVehicle(profile.licensePlate) }
                val updated = profile.copy(
                    name = if (profile.name == "Mijn auto" && rdw.makeModel.isNotBlank()) rdw.makeModel else profile.name,
                    fuelType = if (rdw.electric) "EV" else profile.fuelType,
                    price = if (rdw.electric && !profile.electric) 0.30f else profile.price,
                    consumption = rdw.consumption ?: profile.consumption
                )
                profiles = profiles.map { if (it.id == updated.id) updated else it }
                writeVehicleProfiles(prefs, profiles)
                if (selectedVehicleId == updated.id) selectVehicle(updated, navigateToTripTab = selectedTab != 10)
                info = if (rdw.consumption != null) "RDW-testverbruik voor ${profile.licensePlate} ingevuld (${rdw.consumption.formatNl()} ${if (rdw.electric) "kWh/100 km" else "L/100 km"})." else "Kenteken gevonden; RDW heeft geen verbruikswaarde voor dit voertuig."
            } catch (e: Exception) { info = e.message ?: "RDW-gegevens konden niet worden opgehaald." }
        }
    }
    fun deleteVehicleProfile(profile: VehicleProfile) {
        if (profiles.size <= 1) return
        profiles = profiles.filterNot { it.id == profile.id }
        writeVehicleProfiles(prefs, profiles)
        if (selectedVehicleId == profile.id) {
            val replacement = profiles.first()
            selectVehicle(replacement, navigateToTripTab = selectedTab != 10)
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.getString(KEY_VEHICLES, null) == null) writeVehicleProfiles(prefs, profiles)
        prefs.edit().putString(KEY_SELECTED_VEHICLE, selectedVehicleId).apply()
    }
    LaunchedEffect(Unit) { checkForAppUpdate() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (pendingBusinessStart) {
            pendingBusinessStart = false
            if (ok) {
                startBusinessWorkday(context, prefs, selectedVehicle, businessLabel, businessStartOdo.replace(',', '.').toFloatOrNull())
                workdayActive = true
                businessTripActive = false
                info = "Werkdag gestart. Ritdetectie loopt zolang de VLOTRIT-melding zichtbaar is."
            } else info = "Zakelijk registreren heeft locatietoegang nodig."
        } else {
            val isElectric = selectedTab == 1
            startTrip(context, prefs, startOdo, if (isElectric) electricConsumption else consumption,
                if (isElectric) electricPrice else price, tripName, isElectric, selectedVehicle)
            if (!ok) info = "Geen locatietoegang: vul de gereden afstand handmatig in."
            else if (grants[Manifest.permission.ACCESS_FINE_LOCATION] != true) info = "Voor een nauwkeurige route: kies bij Locatietoegang voor Precieze locatie."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            running = prefs.getBoolean(KEY_RUNNING, false)
            workdayActive = prefs.getBoolean(KEY_WORKDAY_ACTIVE, false)
            businessTripActive = prefs.getBoolean(KEY_BUSINESS_TRIP_ACTIVE, false)
            businessLabel = prefs.getString(KEY_BUSINESS_LABEL, businessLabel).orEmpty()
            distance = prefs.getFloat(KEY_TRACKED_KM, 0f).formatInput()
            endOdo = prefs.getFloat(KEY_END_ODO, 0f)
            route = prefs.getString(KEY_ROUTE, "").orEmpty()
            history = readHistory(prefs)
            gpsError = prefs.getString(KEY_GPS_ERROR, "").orEmpty()
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) { fetchE10Price() }
            val fetchedAt = System.currentTimeMillis()
            if (!prefs.getBoolean(KEY_RUNNING, false)) {
                price = result.first.formatPrice()
                prefs.edit().putFloat(KEY_PRICE, result.first).putLong(KEY_PRICE_UPDATED_AT, fetchedAt).apply()
                if (selectedVehicle.fuelType == "E10") updateSelectedVehicle(selectedVehicle.copy(price = result.first))
                priceInfo = "ANWB · prijs opgehaald ${Date(fetchedAt).dateTime()}"
            } else {
                priceInfo = "Rit gebruikt de ANWB-prijs die bij de start was opgeslagen."
            }
        } catch (_: Exception) { priceInfo = "Prijs ophalen lukte niet · je kunt hem aanpassen" }
    }

    val onTripAction: () -> Unit = {
        val electricTab = selectedTab == 1
        if (!running) {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) startTrip(context, prefs, startOdo, if (electricTab) electricConsumption else consumption,
                if (electricTab) electricPrice else price, tripName, electricTab, selectedVehicle)
            else permissionLauncher.launch(permissions.toTypedArray())
        } else {
            val km = (distance.toFloatOrNull() ?: 0f).coerceAtLeast(0f)
            val start = startOdo.toFloatOrNull() ?: prefs.getFloat(KEY_LAST_ODO, 0f)
            val tripIsElectric = prefs.getBoolean(KEY_TRIP_ELECTRIC, false)
            val tripFuelType = fuelTypeLabel(prefs.getString(KEY_ACTIVE_FUEL_TYPE, if (tripIsElectric) "EV" else "E10").orEmpty())
            val tripConsumption = prefs.getFloat(KEY_ACTIVE_CONSUMPTION, prefs.getFloat(if (tripIsElectric) KEY_ELECTRIC_CONSUMPTION else KEY_CONSUMPTION, if (tripIsElectric) 18f else 11f))
            val tripPrice = prefs.getFloat(KEY_ACTIVE_PRICE, prefs.getFloat(if (tripIsElectric) KEY_ELECTRIC_PRICE else KEY_PRICE, if (tripIsElectric) 0.30f else 2.092f))
            endOdo = start + km
            val stoppedAt = System.currentTimeMillis()
            val savedRoute = prefs.getString(KEY_ROUTE, "").orEmpty()
            val endpoints = routeEndpoints(savedRoute)
            val record = TripRecord(stoppedAt, tripName, start, endOdo, km, tripConsumption,
                tripPrice, if (tripFuelType == "E10") prefs.getLong(KEY_PRICE_UPDATED_AT, 0L) else 0L, prefs.getLong(KEY_STARTED_AT, stoppedAt), stoppedAt,
                savedRoute, tripIsElectric, endpoints.first?.let(::coordinateLabel).orEmpty(), endpoints.second?.let(::coordinateLabel).orEmpty(),
                prefs.getBoolean(KEY_ACTIVE_DEPRECIATION, prefs.getBoolean(if (tripIsElectric) KEY_ELECTRIC_DEPRECIATION else KEY_FUEL_DEPRECIATION, false)),
                prefs.getFloat(KEY_ACTIVE_DEPRECIATION_RATE, prefs.getFloat(KEY_DEPRECIATION_RATE, 0.15f)),
                prefs.getString(KEY_ACTIVE_VEHICLE_ID, "").orEmpty(), prefs.getString(KEY_ACTIVE_VEHICLE_NAME, "").orEmpty(), tripFuelType,
                category = "Privé")
            history = listOf(record) + readHistory(prefs)
            writeHistory(prefs, history)
            if (endpoints.first != null || endpoints.second != null) coroutineScope.launch {
                val startAddress = endpoints.first?.let { withContext(Dispatchers.IO) { reverseGeocode(context, it) } }.orEmpty()
                val endAddress = endpoints.second?.let { withContext(Dispatchers.IO) { reverseGeocode(context, it) } }.orEmpty()
                val enriched = history.map { trip -> if (trip.id == stoppedAt) trip.copy(
                    startLocation = startAddress.ifBlank { trip.startLocation }, endLocation = endAddress.ifBlank { trip.endLocation }) else trip }
                history = enriched; writeHistory(prefs, enriched)
            }
            prefs.edit().putBoolean(KEY_RUNNING, false).putFloat(KEY_TRACKED_KM, km).putFloat(KEY_END_ODO, endOdo)
                .putFloat(KEY_LAST_ODO, endOdo).putFloat(if (tripIsElectric) KEY_ELECTRIC_CONSUMPTION else KEY_CONSUMPTION, tripConsumption)
                .putFloat(if (tripIsElectric) KEY_ELECTRIC_PRICE else KEY_PRICE, tripPrice).putString(KEY_NAME, tripName)
                .putLong(KEY_STOPPED_AT, stoppedAt).apply()
            context.stopService(Intent(context, RouteTrackingService::class.java)); running = false
            startOdo = endOdo.formatInput(); info = "Eindstand ${endOdo.formatNl()} km opgeslagen als je volgende beginstand."
            val localStop = java.util.Calendar.getInstance().apply { timeInMillis = stoppedAt }
            if ((localStop.get(java.util.Calendar.HOUR_OF_DAY) == 23 && localStop.get(java.util.Calendar.MINUTE) >= 30) || localStop.get(java.util.Calendar.HOUR_OF_DAY) in 0..4) showGoodNightDialog = true
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Color(0xFF39A9D6), background = Paper, surface = Color.White, onSurface = Ink)) {
      ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
          ModalDrawerSheet {
              Text("VLOTRIT", modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
              HorizontalDivider()
              listOf("Brandstof", "EV", "Ritten", "Planner", "Zakelijk", "Inzicht", "Laadbeurten", "Stations", "Onderhoud", "Voertuigen", "Info").forEachIndexed { index, label ->
                  if (index == 10) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                  NavigationDrawerItem(label = { Text(label) }, selected = selectedTab == index, onClick = {
                      when (index) {
                          0 -> if (running || workdayActive) { if (!prefs.getBoolean(KEY_TRIP_ELECTRIC, false)) selectedTab = 0 } else profiles.firstOrNull { !it.electric }?.let { selectVehicle(it) }
                          1 -> if (running || workdayActive) { if (prefs.getBoolean(KEY_TRIP_ELECTRIC, false)) selectedTab = 1 } else profiles.firstOrNull { it.electric }?.let { selectVehicle(it) }
                          else -> selectedTab = index
                      }
                      coroutineScope.launch { drawerState.close() }
                  }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
              }
          }
      }) {
        Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                Canvas(Modifier.align(Alignment.CenterStart).size(38.dp).clickable { coroutineScope.launch { drawerState.open() } }) {
                    val stroke = 2.dp.toPx(); val left = size.width * .18f; val right = size.width * .82f
                    listOf(.28f, .5f, .72f).forEach { y -> drawLine(Forest, androidx.compose.ui.geometry.Offset(left, size.height * y), androidx.compose.ui.geometry.Offset(right, size.height * y), strokeWidth = stroke, cap = StrokeCap.Round) }
                }
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(R.drawable.vlotrit_logo), contentDescription = "VLOTRIT-logo", modifier = Modifier.size(39.dp), contentScale = ContentScale.Crop)
                        if (showLogoConfetti) {
                            Text("€", Modifier.align(Alignment.TopStart).offset((-5).dp, (-7).dp), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("⛽", Modifier.align(Alignment.TopEnd).offset(4.dp, (-8).dp), fontSize = 13.sp)
                            Text("€", Modifier.align(Alignment.BottomEnd).offset(3.dp, 5.dp), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("⛽", Modifier.align(Alignment.BottomStart).offset((-5).dp, 4.dp), fontSize = 12.sp)
                        }
                    }
                    Text("VLOTRIT", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Ink, letterSpacing = 1.1.sp)
                }
                val isActive = workdayActive || running
                Surface(Modifier.align(Alignment.CenterEnd), color = if (isActive) BlueTint else Color.White, shape = RoundedCornerShape(50), shadowElevation = 1.dp) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Canvas(Modifier.size(7.dp)) { drawCircle(if (isActive) Forest else Color(0xFF9AA9B7)) }
                        Text(if (workdayActive) "Werkdag" else if (running) "Rit loopt" else "Startklaar", color = if (isActive) Forest else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (selectedTab !in 7..10) VehicleProfilesCard(profiles, selectedVehicle.id, enabled = !running && !workdayActive,
                onSelect = { selectVehicle(it) }, onPlateEasterEgg = { logoConfettiBurst++ }, onHornEasterEgg = { playEasterEggSound(horn = true) })
            HeroBanner(selectedTab, running, gpsError, workdayActive, onTripAction)
            if (selectedTab == 0 && openingGreeting.isNotBlank()) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BlueTint)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (openingGreeting.startsWith("Nog laat")) "🦉" else "☕", fontSize = 22.sp)
                        Text(openingGreeting, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (availableRelease != null) {
                Card(Modifier.fillMaxWidth().clickable { selectedTab = 10 }, shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueTint)) {
                    Text("Er is een nieuwe update beschikbaar. Werk VLOTRIT bij via het kopje Info om de nieuwe versie te gebruiken.",
                        Modifier.padding(horizontal = 15.dp, vertical = 12.dp), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (showFreeFuelDialog) AlertDialog(
                onDismissRequest = { showFreeFuelDialog = false },
                title = { Text("Gratis tankbeurt!") },
                text = { Text("Gefeliciteerd, je hebt een pomphouder gevonden die nog in 1995 leeft!") },
                confirmButton = { TextButton(onClick = { showFreeFuelDialog = false }) { Text("Haha") } }
            )
            if (showGoodNightDialog) AlertDialog(
                onDismissRequest = { showGoodNightDialog = false },
                title = { Text("Welterusten") },
                text = { Text("Rit veilig opgeslagen. Welterusten voor straks!") },
                confirmButton = { TextButton(onClick = { showGoodNightDialog = false }) { Text("Dank je") } }
            )
            if (showUpsideDownDialog) AlertDialog(
                onDismissRequest = { showUpsideDownDialog = false },
                title = { Text("VLOTRIT") },
                text = { Text("Alles goed daar? De auto staat hopelijk nog wel op vier wielen.") },
                confirmButton = { TextButton(onClick = { showUpsideDownDialog = false }) { Text("Ja hoor") } }
            )
            selectedShakeTrip?.let { picked -> AlertDialog(
                onDismissRequest = { selectedShakeTrip = null },
                title = { Text("Deze rit kiest vandaag voor jou.") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(picked.name.ifBlank { picked.vehicleName.ifBlank { "Je rit" } })
                    Text("${picked.distanceKm.formatNl()} km · ${picked.cost.euro()}", color = Muted)
                    if (picked.route.isNotBlank()) RouteMap(picked.route, Modifier.fillMaxWidth().height(150.dp))
                } },
                confirmButton = { TextButton(onClick = { selectedHistoryId = picked.id; selectedTab = 2; selectedShakeTrip = null }) { Text("Bekijk rit") } },
                dismissButton = { TextButton(onClick = { selectedShakeTrip = null }) { Text("Sluiten") } }
            ) }
            if (noShakeTrips) AlertDialog(
                onDismissRequest = { noShakeTrips = false }, title = { Text("Deze rit kiest vandaag voor jou.") },
                text = { Text("Sla eerst een rit op, dan kan VLOTRIT er willekeurig een uitlichten.") },
                confirmButton = { TextButton(onClick = { noShakeTrips = false }) { Text("Begrepen") } }
            )
            if (selectedTab == 2) {
                TripHistory(context, prefs, history, selectedHistoryId, selectedVehicle,
                    onSelect = { selectedHistoryId = it }, onTripsChanged = { history = it },
                    onApplyAverage = { average ->
                        val updated = selectedVehicle.copy(consumption = average)
                        updateSelectedVehicle(updated)
                        if (updated.electric) electricConsumption = average.formatInput() else consumption = average.formatInput()
                        prefs.edit().putFloat(if (updated.electric) KEY_ELECTRIC_CONSUMPTION else KEY_CONSUMPTION, average).apply()
                        info = "Gemiddeld verbruik bijgewerkt naar ${average.formatNl()} ${if (updated.electric) "kWh/100 km" else "L/100 km"}."
                    })
            } else if (selectedTab == 3) {
                RoutePlanner(context, prefs, selectedVehicle)
            } else if (selectedTab == 4) {
                BusinessWorkdayCard(workdayActive, businessTripActive, prefs.getInt(KEY_BUSINESS_TRIP_COUNT, 0),
                    prefs.getFloat(KEY_TRACKED_KM, 0f), selectedVehicle, businessLabel, businessStartOdo, info,
                    onLabelChange = { businessLabel = it; prefs.edit().putString(KEY_BUSINESS_LABEL, it).apply() },
                    onStartOdometerChange = { businessStartOdo = it },
                    onStart = {
                        if (running) {
                            info = "Stop eerst je handmatige rit voordat je de zakelijke werkdag start."
                        } else {
                        val locationPermissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) locationPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasLocation) {
                            startBusinessWorkday(context, prefs, selectedVehicle, businessLabel, businessStartOdo.replace(',', '.').toFloatOrNull())
                            workdayActive = true
                            businessTripActive = false
                            info = "Werkdag gestart. Ritdetectie loopt zolang de VLOTRIT-melding zichtbaar is."
                        } else {
                            pendingBusinessStart = true
                            permissionLauncher.launch(locationPermissions.toTypedArray())
                        }
                        }
                    },
                    onStop = {
                        stopBusinessWorkday(context, prefs)
                        workdayActive = false
                        businessTripActive = false
                        running = false
                        history = readHistory(prefs)
                        info = "Werkdag afgerond. ${prefs.getInt(KEY_BUSINESS_TRIP_COUNT, 0)} zakelijke ritten staan in Ritten."
                    })
            } else if (selectedTab == 5) {
                InsightsTab(history)
            } else if (selectedTab == 6) {
                ChargeSessionsTab(prefs, selectedVehicle)
            } else if (selectedTab == 7) {
                StationsTab(context, prefs)
            } else if (selectedTab == 8) {
                MaintenanceTab(prefs, prefs.getFloat(KEY_LAST_ODO, prefs.getFloat(KEY_END_ODO, 0f)), profiles, selectedVehicle.id,
                    onVehicleSelect = { profile ->
                        selectedVehicleId = profile.id
                        prefs.edit().putString(KEY_SELECTED_VEHICLE, profile.id).apply()
                        if (profile.electric) { electricConsumption = profile.consumption.formatInput(); electricPrice = profile.price.formatPrice() }
                        else { consumption = profile.consumption.formatInput(); price = profile.price.formatPrice() }
                    },
                    onProfileUpdated = { profile ->
                        profiles = profiles.map { if (it.id == profile.id) profile else it }
                        writeVehicleProfiles(prefs, profiles)
                        if (selectedVehicleId == profile.id) {
                            if (profile.electric) {
                                electricConsumption = profile.consumption.formatInput(); electricPrice = profile.price.formatPrice()
                                prefs.edit().putFloat(KEY_ELECTRIC_CONSUMPTION, profile.consumption).putFloat(KEY_ELECTRIC_PRICE, profile.price).apply()
                            } else {
                                consumption = profile.consumption.formatInput(); price = profile.price.formatPrice()
                                prefs.edit().putFloat(KEY_CONSUMPTION, profile.consumption).putFloat(KEY_PRICE, profile.price).apply()
                            }
                        }
                    })
            } else if (selectedTab == 9) {
                VehicleManagerTab(profiles, selectedVehicle.id, enabled = !running && !workdayActive,
                    onSelect = { selectVehicle(it, navigateToTripTab = false) },
                    onSave = ::saveVehicleProfile,
                    onDelete = ::deleteVehicleProfile)
            } else if (selectedTab == 10) {
                AppInfoTab(context, latestRelease, availableRelease, checkingUpdates) { coroutineScope.launch { checkForAppUpdate() } }
            } else {
            val electricTab = selectedTab == 1
            val activeElectric = prefs.getBoolean(KEY_TRIP_ELECTRIC, false)
            val displayedConsumption = if (electricTab) electricConsumption else consumption
            val displayedPrice = if (electricTab) electricPrice else price
            if (workdayActive) {
                Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Zakelijke werkdag actief", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                        Text(if (businessTripActive) "VLOTRIT registreert nu automatisch een zakelijke rit." else "VLOTRIT wacht tot je gaat rijden en bewaart daarna automatisch elke rit.", color = Muted, fontSize = 13.sp)
                        Button(onClick = { selectedTab = 4 }) { Text("Bekijk werkdag") }
                    }
                }
            } else if (running && activeElectric != electricTab) {
                Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (activeElectric) "Je EV-rit loopt" else "Je brandstofrit loopt", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                        Text("Stop deze rit op het tabblad ${if (activeElectric) "EV" else "Brandstof"}.", color = Muted, fontSize = 13.sp)
                        Button(onClick = { selectedTab = if (activeElectric) 1 else 0 }) { Text("Ga naar actieve rit") }
                    }
                }
            } else {
            Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(if (electricTab) "EV-rit" else "Brandstofrit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericField("Beginstand", startOdo, { startOdo = it }, "km", Modifier.weight(1f), enabled = !running)
                        NumericField("Eindstand", if (running) "—" else if (endOdo > 0f) endOdo.formatInput() else "—", {}, "km", Modifier.weight(1f), enabled = false)
                    }
                    NumericField("Gereden afstand · GPS of handmatig", distance, { distance = it; if (running) prefs.edit().putFloat(KEY_TRACKED_KM, it.toFloatOrNull() ?: 0f).apply() }, "km", Modifier.fillMaxWidth(), enabled = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericField(if (electricTab) "Gemiddeld stroomverbruik" else "Gemiddeld verbruik", displayedConsumption,
                            { value ->
                                if (electricTab) electricConsumption = value else consumption = value
                                value.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }?.let { number ->
                                    updateSelectedVehicle(selectedVehicle.copy(consumption = number))
                                    prefs.edit().putFloat(if (electricTab) KEY_ELECTRIC_CONSUMPTION else KEY_CONSUMPTION, number).apply()
                                }
                            },
                            if (electricTab) "kWh/100 km" else "L/100 km", Modifier.weight(1f), enabled = !running)
                        NumericField(if (electricTab) "Stroomprijs" else "${fuelTypeLabel(selectedVehicle.fuelType)} prijs", displayedPrice,
                            { value ->
                                if (electricTab) electricPrice = value else price = value
                                value.replace(',', '.').toFloatOrNull()?.takeIf { it >= 0f }?.let { number ->
                                    updateSelectedVehicle(selectedVehicle.copy(price = number))
                                    prefs.edit().putFloat(if (electricTab) KEY_ELECTRIC_PRICE else KEY_PRICE, number).apply()
                                    if (!electricTab && selectedVehicle.fuelType == "E10" && number == 0f) showFreeFuelDialog = true
                                }
                            },
                            if (electricTab) "€/kWh" else "€/L", Modifier.weight(1f), enabled = !running)
                    }
                    Text(if (electricTab) "Vul de prijs per kWh van je laadpas of laadpunt in." else if (selectedVehicle.fuelType == "E10") priceInfo else "Pas de ${fuelTypeLabel(selectedVehicle.fuelType)}-prijs aan op je huidige pompprijs.", color = Muted, fontSize = 11.sp)
                    val useDepreciation = selectedVehicle.depreciationEnabled
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useDepreciation, onCheckedChange = { checked ->
                            if (electricTab) { electricDepreciation = checked; prefs.edit().putBoolean(KEY_ELECTRIC_DEPRECIATION, checked).apply() }
                            else { fuelDepreciation = checked; prefs.edit().putBoolean(KEY_FUEL_DEPRECIATION, checked).apply() }
                            updateSelectedVehicle(selectedVehicle.copy(depreciationEnabled = checked))
                        })
                        Text("Afschrijving meenemen in de bon", color = Ink, fontSize = 13.sp)
                    }
                    if (useDepreciation) NumericField("Afschrijving", depreciationRate, {
                        depreciationRate = it
                        it.replace(',', '.').toFloatOrNull()?.takeIf { n -> n >= 0f }?.let { value ->
                            prefs.edit().putFloat(KEY_DEPRECIATION_RATE, value).apply()
                            updateSelectedVehicle(selectedVehicle.copy(depreciationPerKm = value))
                        }
                    }, "€/km", Modifier.fillMaxWidth(), enabled = !running)
                    OutlinedTextField(value = tripName, onValueChange = { tripName = it }, enabled = !running, label = { Text("Naam / omschrijving (optioneel)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
                    if (running) Text("GPS meet ${distance.toFloatOrNull()?.formatNl() ?: "0"} km${if (gpsError.isNotBlank()) " · $gpsError" else ""}.", color = Muted, fontSize = 12.sp)
                    if (info.isNotBlank()) Text(info, color = Forest, fontSize = 12.sp)
                    if (!running && endOdo > 0f) TextButton(onClick = {
                        prefs.edit().putBoolean(KEY_RUNNING, false).putFloat(KEY_TRACKED_KM, 0f).putFloat(KEY_START_ODO, endOdo).putFloat(KEY_END_ODO, 0f).putString(KEY_ROUTE, "").putLong(KEY_STOPPED_AT, 0L).apply()
                        startOdo = endOdo.formatInput(); distance = "0"; route = ""; info = "Nieuwe rit klaar. Beginstand overgenomen."
                    }) { Text("Nieuwe rit", color = Forest) }
                }
            }
            if (!running && prefs.getLong(KEY_STOPPED_AT, 0L) > 0L) {
                val savedReceipt = history.firstOrNull { it.id == prefs.getLong(KEY_STOPPED_AT, 0L) }
                if (savedReceipt == null || savedReceipt.electric == electricTab) ReceiptCard(context, prefs, savedReceipt?.route ?: route, endOdo, distance.toFloatOrNull() ?: 0f,
                    savedReceipt?.consumption ?: consumption.toFloatOrNull() ?: 11f,
                    savedReceipt?.price ?: price.toFloatOrNull() ?: 2.092f,
                    savedReceipt?.priceUpdatedAt ?: prefs.getLong(KEY_PRICE_UPDATED_AT, 0L), savedReceipt?.name ?: tripName, electricTab,
                    savedReceipt?.startLocation.orEmpty(), savedReceipt?.endLocation.orEmpty(), savedReceipt?.depreciationEnabled ?: false, savedReceipt?.depreciationPerKm ?: 0.15f,
                    savedReceipt?.vehicleId ?: selectedVehicle.id, savedReceipt?.vehicleName ?: selectedVehicle.name, savedReceipt?.fuelType ?: selectedVehicle.fuelType, savedReceipt?.actualEnergy)
            }
            Text("De laatste kilometerstand wordt op dit toestel bewaard. GPS-afstand is een schatting op basis van locatiepunten.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
            }
            }
        }
      }
    }
}

@Composable
private fun HeroBanner(selectedTab: Int, running: Boolean, gpsError: String, workdayActive: Boolean, onTripAction: () -> Unit) {
    if (selectedTab in 0..1 && !workdayActive) {
        TripActionBanner(running, gpsError, onTripAction)
        return
    }
    val section = when (selectedTab) {
        1 -> "EV RIJDEN"
        2 -> "JOUW RITTEN"
        3 -> "ROUTE PLANNER"
        4 -> "ZAKELIJK RIJDEN"
        5 -> "RITSTATISTIEKEN"
        6 -> "EV LADEN"
        7 -> "TANKEN & LADEN"
        8 -> "ONDERHOUD & HERINNERINGEN"
        9 -> "JOUW VOERTUIGEN"
        10 -> "OVER VLOTRIT"
        else -> "BRANDSTOF & KILOMETERS"
    }
    val title = when (selectedTab) {
        1 -> "Grip op elke\nlaadrit."
        2 -> "Al je ritten.\nHelder bewaard."
        3 -> "Plan je route.\nKen de kosten."
        4 -> "Werkdag gestart.\nRitten geregeld."
        5 -> "Je kilometers.\nJe kosten."
        6 -> "Elke laadbeurt\noverzichtelijk."
        7 -> "Vind een plek\nin de buurt."
        8 -> "Onderhoud op\ntijd geregeld."
        9 -> "Al je auto's.\nOp één plek."
        10 -> "Alle ritinfo.\nOp één plek."
        else -> "Elke rit.\nHelder geregeld."
    }
    val subtitle = when (selectedTab) {
        1 -> "Bekijk je stroomverbruik en kosten per rit."
        2 -> "Zoek terug, bekijk bonnen en houd totalen bij."
        3 -> "Bereken afstand en kosten voor je vertrekt."
        4 -> "Je ritten worden automatisch bijgehouden."
        5 -> "Bekijk je ontwikkeling per week, maand of jaar."
        6 -> "Bewaar kWh, laadprijs en locatie per sessie."
        7 -> "Bekijk nabije tankstations en laadpunten op de kaart."
        8 -> "Bewaar APK-, onderhouds- en bandenwisseldata."
        9 -> "Beheer je voertuigprofielen en kentekens."
        10 -> "Bekijk appversie, gegevens en updates."
        else -> "Afstand, verbruik en kosten overzichtelijk bij elkaar."
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF258DD1), Color(0xFF155B9B), Color(0xFF124577))), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp, vertical = 21.dp)
    ) {
        Canvas(Modifier.align(Alignment.TopEnd).size(width = 128.dp, height = 110.dp)) {
            val routeLine = Path().apply {
                moveTo(size.width * 0.05f, size.height * 0.82f)
                cubicTo(size.width * 0.32f, size.height * 0.20f, size.width * 0.48f, size.height * 0.96f, size.width * 0.91f, size.height * 0.23f)
            }
            drawPath(routeLine, Color.White.copy(alpha = 0.32f), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.86f), radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.82f))
            drawCircle(Color.White.copy(alpha = 0.86f), radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.91f, size.height * 0.23f))
        }
        Column(Modifier.fillMaxWidth(0.83f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(section, color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Text(title, color = Color.White, fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TripActionBanner(running: Boolean, gpsError: String, onTripAction: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF1688D2), Color(0xFF07569A), Color(0xFF123B70))), RoundedCornerShape(27.dp))
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Canvas(Modifier.align(Alignment.CenterEnd).size(width = 175.dp, height = 155.dp)) {
            val mesh = Path().apply {
                moveTo(size.width * .05f, size.height * .91f)
                cubicTo(size.width * .25f, size.height * .75f, size.width * .36f, size.height * .45f, size.width * .55f, size.height * .57f)
                cubicTo(size.width * .72f, size.height * .68f, size.width * .78f, size.height * .22f, size.width * .99f, size.height * .12f)
            }
            drawPath(mesh, Color.White.copy(alpha = .52f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            for (row in 0..5) for (column in 0..7) {
                val x = size.width * (column / 7f)
                val y = size.height * (.48f + row * .085f)
                drawCircle(Color.White.copy(alpha = .12f), radius = 1.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
            drawCircle(Color.White, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .05f, size.height * .91f))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .99f, size.height * .12f))
        }
        Column(Modifier.fillMaxWidth(.88f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(if (running) "RIT LOOPT" else "KLAAR VOOR EEN NIEUWE RIT?", color = Color.White.copy(alpha = .82f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(if (running) "Je route wordt\nbijgehouden." else "Ritregistratie\nstaat klaar.", color = Color.White, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                when {
                    running && gpsError.isNotBlank() -> gpsError
                    running -> "Live GPS en ritregistratie actief."
                    else -> "GPS start zodra je een rit begint."
                },
                color = Color.White.copy(alpha = .9f), fontSize = 13.sp, lineHeight = 18.sp
            )
            Button(
                onClick = onTripAction,
                modifier = Modifier.fillMaxWidth(.85f).height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF148449)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(if (running) "Stop rit & maak bon" else "Start rit", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Surface(color = Color(0xFFE4F8EA), shape = RoundedCornerShape(50)) {
                        Text(if (running) "● LIVE" else "GPS", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color(0xFF138447), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericField(label: String, value: String, onValue: (String) -> Unit, suffix: String, modifier: Modifier, enabled: Boolean) {
    OutlinedTextField(value = value, onValueChange = onValue, enabled = enabled, label = { Text(label, fontSize = 12.sp) }, trailingIcon = { Text(suffix, color = Muted, fontSize = 10.sp) }, modifier = modifier,
        shape = RoundedCornerShape(16.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun VehicleManagerTab(
    profiles: List<VehicleProfile>, selectedId: String, enabled: Boolean,
    onSelect: (VehicleProfile) -> Unit, onSave: (VehicleProfile) -> Unit, onDelete: (VehicleProfile) -> Unit
) {
    var editor by remember { mutableStateOf<VehicleProfile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<VehicleProfile?>(null) }
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Voertuigen beheren", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${profiles.size} voertuigprofiel${if (profiles.size == 1) "" else "len"}", color = Muted, fontSize = 12.sp)
                }
                Button(onClick = { creating = true; editor = null }, enabled = enabled) { Text("+ Toevoegen") }
            }
            Text("Kies een voertuig of bewerk de naam, het kenteken, brandstof, verbruik en afschrijving.", color = Muted, fontSize = 12.sp)
            profiles.forEach { profile ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (profile.id == selectedId) BlueTint else Paper)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (profile.id == selectedId) Text("Actief", color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text("${profile.licensePlate.ifBlank { "Geen kenteken" }} · ${fuelTypeLabel(profile.fuelType)}", color = Muted, fontSize = 12.sp)
                        Text("${profile.consumption.formatNl()} ${if (profile.electric) "kWh/100 km" else "L/100 km"} · ${profile.depreciationPerKm.euro()}/km afschrijving", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(onClick = { onSelect(profile) }, enabled = enabled && profile.id != selectedId) { Text("Kiezen") }
                            OutlinedButton(onClick = { editor = profile; creating = false }, enabled = enabled) { Text("Bewerken") }
                            TextButton(onClick = { deleteTarget = profile }, enabled = enabled && profiles.size > 1) { Text("Verwijderen", color = Color(0xFF9C422D)) }
                        }
                    }
                }
            }
        }
    }
    if (creating || editor != null) VehicleProfileEditorDialog(editor, onDismiss = { creating = false; editor = null }, onSave = {
        onSave(it); creating = false; editor = null
    })
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Voertuig verwijderen?") },
            text = { Text("${target.name} wordt verwijderd. Bestaande ritten blijven bewaard.") },
            confirmButton = { TextButton(onClick = { onDelete(target); deleteTarget = null }) { Text("Verwijderen", color = Color(0xFF9C422D)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Annuleren") } })
    }
}

@Composable
private fun VehicleProfilesCard(
    profiles: List<VehicleProfile>, selectedId: String, enabled: Boolean, onSelect: (VehicleProfile) -> Unit,
    onPlateEasterEgg: () -> Unit, onHornEasterEgg: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var headlightFlash by remember { mutableStateOf(false) }
    var headlightTaps by remember { mutableIntStateOf(0) }
    var lastHeadlightTap by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val active = profiles.firstOrNull { it.id == selectedId } ?: profiles.first()
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Box {
            Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { menuOpen = true }.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.width(104.dp).height(60.dp)) {
                    VehicleSilhouette(Modifier.fillMaxSize(), headlightFlash)
                    Box(Modifier.fillMaxSize().pointerInput(headlightFlash) {
                        detectTapGestures { tap ->
                            val atHeadlight = tap.x > size.width * .84f && tap.y in (size.height * .43f)..(size.height * .70f)
                            if (atHeadlight) {
                                val now = System.currentTimeMillis()
                                headlightTaps = if (now - lastHeadlightTap < 1400L) headlightTaps + 1 else 1
                                lastHeadlightTap = now
                                if (headlightTaps >= 3) {
                                    headlightTaps = 0
                                    headlightFlash = true
                                    onHornEasterEgg()
                                    scope.launch { delay(450); headlightFlash = false }
                                }
                            }
                        }
                    })
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(active.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                    Text("${active.consumption.formatNl()} ${if (active.electric) "kWh/100 km" else "L/100 km"}", color = Muted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = BlueTint, shape = RoundedCornerShape(7.dp), modifier = Modifier.pointerInput(active.licensePlate) {
                            if (active.licensePlate.isNotBlank()) awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val completed = withTimeoutOrNull<Boolean>(3000L) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                        if (!change.pressed) return@withTimeoutOrNull false
                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y
                                        if (dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop) return@withTimeoutOrNull false
                                    }
                                    false
                                }
                                if (completed == null) onPlateEasterEgg()
                            }
                        }) {
                            Text(active.licensePlate.ifBlank { "Kenteken toevoegen" }, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = if (active.electric) Color(0xFFE5F4FB) else Color(0xFFE2F7E8), shape = RoundedCornerShape(7.dp)) {
                            Text(fuelTypeLabel(active.fuelType), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (active.electric) Color(0xFF1674A7) else Color(0xFF168044), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text("⌄", color = Muted, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                profiles.forEach { profile -> DropdownMenuItem(
                    text = { Column { Text(profile.name, fontWeight = FontWeight.SemiBold); Text("${profile.licensePlate.ifBlank { fuelTypeLabel(profile.fuelType) }} · ${profile.consumption.formatNl()} ${if (profile.electric) "kWh/100 km" else "L/100 km"}", color = Muted, fontSize = 11.sp) } },
                    onClick = { menuOpen = false; onSelect(profile) }, enabled = enabled
                ) }
            }
        }
    }
}

@Composable
private fun VehicleSilhouette(modifier: Modifier = Modifier, headlightFlash: Boolean = false) {
    Canvas(modifier) {
        val car = Path().apply {
            moveTo(size.width * .06f, size.height * .69f)
            cubicTo(size.width * .08f, size.height * .56f, size.width * .17f, size.height * .50f, size.width * .32f, size.height * .45f)
            lineTo(size.width * .42f, size.height * .22f)
            cubicTo(size.width * .45f, size.height * .16f, size.width * .52f, size.height * .15f, size.width * .60f, size.height * .18f)
            lineTo(size.width * .74f, size.height * .24f)
            cubicTo(size.width * .79f, size.height * .27f, size.width * .82f, size.height * .35f, size.width * .85f, size.height * .42f)
            lineTo(size.width * .95f, size.height * .51f)
            cubicTo(size.width * .99f, size.height * .56f, size.width, size.height * .65f, size.width * .97f, size.height * .74f)
            lineTo(size.width * .94f, size.height * .81f)
            lineTo(size.width * .08f, size.height * .81f)
            close()
        }
        drawPath(car, Ink)
        val windows = Path().apply {
            moveTo(size.width * .38f, size.height * .42f)
            lineTo(size.width * .46f, size.height * .25f)
            cubicTo(size.width * .49f, size.height * .21f, size.width * .55f, size.height * .22f, size.width * .60f, size.height * .24f)
            lineTo(size.width * .70f, size.height * .29f)
            lineTo(size.width * .76f, size.height * .43f)
            close()
        }
        drawPath(windows, BlueTint)
        drawCircle(if (headlightFlash) Color(0xFFFFF176) else Color(0xFFB8DFFF), size.height * .055f, androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .59f))
        listOf(.28f, .76f).forEach { x ->
            drawCircle(Ink, size.height * .19f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * .76f))
            drawCircle(Color(0xFFEAF0F7), size.height * .09f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * .76f))
            drawCircle(Color(0xFF8494A5), size.height * .035f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * .76f))
        }
    }
}

@Composable
private fun VehicleProfileEditorDialog(initial: VehicleProfile?, onDismiss: () -> Unit, onSave: (VehicleProfile) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var fuelType by remember(initial?.id) { mutableStateOf(fuelTypeLabel(initial?.fuelType ?: "E10")) }
    var consumption by remember(initial?.id) { mutableStateOf((initial?.consumption ?: 11f).formatInput()) }
    var price by remember(initial?.id) { mutableStateOf((initial?.price ?: 2.092f).formatPrice()) }
    var depreciation by remember(initial?.id) { mutableStateOf((initial?.depreciationPerKm ?: 0.15f).formatPrice()) }
    var licensePlate by remember(initial?.id) { mutableStateOf(initial?.licensePlate.orEmpty()) }
    var includeDepreciation by remember(initial?.id) { mutableStateOf(initial?.depreciationEnabled ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Voertuig toevoegen" else "Voertuig bewerken") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Naam voertuig") }, singleLine = true)
                OutlinedTextField(licensePlate, { licensePlate = it.uppercase(Locale.ROOT) }, label = { Text("Kenteken (NL)") }, supportingText = { Text("Bij opslaan vult VLOTRIT bekende RDW-verbruiksgegevens automatisch in.") }, singleLine = true)
                Text("Brandstofsoort", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("E10", "E5/98", "EV").forEach { type ->
                        FilterChip(selected = fuelType == type, onClick = { fuelType = type }, label = { Text(type) })
                    }
                }
                NumericField("Standaardverbruik", consumption, { consumption = it }, if (fuelType == "EV") "kWh/100 km" else "L/100 km", Modifier.fillMaxWidth(), true)
                NumericField("Prijs", price, { price = it }, if (fuelType == "EV") "€/kWh" else "€/L", Modifier.fillMaxWidth(), true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(includeDepreciation, { includeDepreciation = it })
                    Text("Afschrijving rekenen", color = Ink, fontSize = 13.sp)
                }
                NumericField("Afschrijving per km", depreciation, { depreciation = it }, "€/km", Modifier.fillMaxWidth(), includeDepreciation)
            }
        },
        confirmButton = { TextButton(onClick = {
            val parsed = consumption.replace(',', '.').toFloatOrNull()
            val parsedPrice = price.replace(',', '.').toFloatOrNull()
            if (name.isNotBlank() && parsed != null && parsed > 0 && parsedPrice != null && parsedPrice >= 0) onSave(VehicleProfile(
                initial?.id ?: "vehicle-${System.currentTimeMillis()}", name.trim(), fuelType, parsed, parsedPrice,
                depreciation.replace(',', '.').toFloatOrNull() ?: 0.15f, includeDepreciation, licensePlate.trim().uppercase(Locale.ROOT)
            ))
        }) { Text("Opslaan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}

@Composable
private fun BusinessWorkdayCard(
    active: Boolean, tripActive: Boolean, completedTrips: Int, currentKm: Float, vehicle: VehicleProfile,
    label: String, startOdometer: String, statusText: String, onLabelChange: (String) -> Unit,
    onStartOdometerChange: (String) -> Unit, onStart: () -> Unit, onStop: () -> Unit
) {
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Zakelijke werkdag", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (statusText.isNotBlank()) Text(statusText, color = Forest, fontSize = 12.sp)
            if (active) {
                Text("Werkdag loopt · ${vehicle.name} · $completedTrips ritten afgerond", color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (tripActive) Text("Rit wordt automatisch geregistreerd: ${currentKm.formatNl()} km tot nu toe.", color = Muted, fontSize = 13.sp)
                else Text("VLOTRIT wacht op autorijden. Een rit wordt na ongeveer 5 minuten stilstaan afgerond en als bon bewaard.", color = Muted, fontSize = 13.sp)
                Text("Laat de VLOTRIT-melding actief voor GPS-registratie op de achtergrond.", color = Muted, fontSize = 11.sp)
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Stop werkdag en bewaar laatste rit") }
            } else {
                Text("VLOTRIT start en stopt tijdens je werkdag automatisch een rit. Elke afgeronde rit krijgt een eigen bon in Ritten.", color = Muted, fontSize = 13.sp)
                OutlinedTextField(label, onLabelChange, modifier = Modifier.fillMaxWidth(), label = { Text("Klant, project of omschrijving (optioneel)") }, singleLine = true, shape = RoundedCornerShape(13.dp))
                Text("Voertuig: ${vehicle.name} · ${vehicle.fuelType} · ${vehicle.consumption.formatNl()} ${if (vehicle.electric) "kWh/100 km" else "L/100 km"}", color = Muted, fontSize = 12.sp)
                NumericField("Kilometerstand bij start werkdag", startOdometer, onStartOdometerChange, "km", Modifier.fillMaxWidth(), true)
                Text("GPS en een doorlopende VLOTRIT-melding zijn nodig om ritten automatisch te herkennen.", color = Muted, fontSize = 11.sp)
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Start werkdag", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private data class AppRelease(val versionName: String, val apkUrl: String, val notes: String)

@Composable
private fun UpdateCard(context: Context, prefs: android.content.SharedPreferences, latestRelease: AppRelease?, availableRelease: AppRelease?, checking: Boolean, onCheck: () -> Unit) {
    var downloading by remember { mutableStateOf(false) }
    var installStatus by remember { mutableStateOf("") }
    var pendingUpdatePath by remember { mutableStateOf(prefs.getString("pending_update_file", "").orEmpty()) }
    val scope = rememberCoroutineScope()

    Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("App updaten", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (checking) "Controleren op nieuwe versies..." else availableRelease?.let { "Versie ${it.versionName} is beschikbaar." } ?: "Je gebruikt de nieuwste versie.",
                        color = if (availableRelease != null) Forest else Muted, fontSize = 11.sp)
                }
                TextButton(enabled = !checking, onClick = onCheck) {
                    Text(if (checking) "Controleren..." else "Controleer", color = Forest)
                }
            }
            Text("Sinds je laatste update", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(latestRelease?.notes?.takeIf { it.isNotBlank() } ?: "Geen wijzigingsnotities beschikbaar.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 5)
            Text("Je downloadt updates via GitHub. Android kan eenmalig toestemming vragen om updates vanuit VLOTRIT te installeren. Play Protect kan daarna een scan aanbieden; laat die beveiliging aan.",
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            availableRelease?.let { available ->
                Button(enabled = !downloading, onClick = {
                    scope.launch {
                        downloading = true
                        try {
                            val rememberedFile = pendingUpdatePath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isFile }
                            val file = rememberedFile ?: withContext(Dispatchers.IO) { downloadUpdate(context, available) }
                            if (!context.packageManager.canRequestPackageInstalls()) {
                                pendingUpdatePath = file.absolutePath
                                prefs.edit().putString("pending_update_file", pendingUpdatePath).apply()
                                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                                installStatus = "Sta installatie vanuit VLOTRIT toe en keer daarna hier terug. De APK is bewaard; je hoeft hem niet opnieuw te downloaden."
                            } else {
                                pendingUpdatePath = ""
                                prefs.edit().remove("pending_update_file").apply()
                                withContext(Dispatchers.IO) { installUpdate(context, file) }
                                installStatus = "Bevestig de update in het Android-installatievenster."
                            }
                        } catch (e: Exception) {
                            installStatus = e.message ?: "Downloaden of installeren is mislukt."
                        } finally { downloading = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (downloading) "Update downloaden..." else if (pendingUpdatePath.isNotBlank()) "Doorgaan met installeren" else "Download en installeer ${available.versionName}")
                }
            }
            if (installStatus.isNotBlank()) Text(installStatus, color = Muted, fontSize = 11.sp)
        }
    }
}

private fun fetchLatestRelease(repository: String, sinceVersion: String? = null): AppRelease {
    require(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(repository)) { "Updatekanaal is ongeldig." }
    val connection = (URL("https://api.github.com/repos/$repository/releases/latest").openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000; readTimeout = 12000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        setRequestProperty("User-Agent", "VLOTRIT Android updater")
    }
    return try {
        if (connection.responseCode !in 200..299) error("Geen openbare VLOTRIT-release gevonden in $repository.")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val version = json.optString("tag_name").trim().removePrefix("v").removePrefix("V")
        require(Regex("^\\d+(\\.\\d+)*$").matches(version)) { "Gebruik releasetags zoals v1.12." }
        val assets = json.optJSONArray("assets") ?: error("De nieuwste GitHub-release bevat geen APK.")
        val apk = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("name") == "Vlotrit.apk" }
            ?: error("Voeg Vlotrit.apk toe aan de GitHub-release.")
        val releaseBody = json.optString("body").trim()
        val isOnlyChangelogLink = releaseBody.contains("Full Changelog", ignoreCase = true)
        val notes = if (releaseBody.isBlank() || isOnlyChangelogLink) {
            fetchReleaseChanges(repository, version, sinceVersion) ?: "Bekijk de wijzigingen van versie $version op GitHub."
        } else releaseBody
        AppRelease(version, apk.getString("browser_download_url"), notes)
    } finally { connection.disconnect() }
}

private fun fetchReleaseChanges(repository: String, version: String, sinceVersion: String?): String? {
    val base = sinceVersion?.takeIf { compareVersionNames(it, version) < 0 } ?: run {
        val parts = version.split('.').mapNotNull { it.toIntOrNull() }.toMutableList()
        if (parts.isEmpty() || parts.last() <= 0) return null
        parts[parts.lastIndex] = parts.last() - 1
        parts.joinToString(".")
    }
    val connection = (URL("https://api.github.com/repos/$repository/compare/v$base...v$version").openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000; readTimeout = 10000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "VLOTRIT Android updater")
    }
    return try {
        if (connection.responseCode !in 200..299) return null
        val commits = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("commits") ?: return null
        (0 until commits.length()).mapNotNull { index ->
            commits.optJSONObject(index)?.optJSONObject("commit")?.optString("message")
                ?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        }.distinct().joinToString("\n").take(500).ifBlank { null }
    } catch (_: Exception) { null } finally { connection.disconnect() }
}

private fun compareVersionNames(left: String, right: String): Int {
    val a = Regex("\\d+").findAll(left).map { it.value.toInt() }.toList()
    val b = Regex("\\d+").findAll(right).map { it.value.toInt() }.toList()
    for (index in 0 until maxOf(a.size, b.size)) {
        val diff = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

private fun downloadUpdate(context: Context, release: AppRelease): File {
    val url = URL(release.apkUrl)
    require(url.protocol == "https") { "De update-download is niet beveiligd." }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
        setRequestProperty("User-Agent", "VLOTRIT Android updater")
        setRequestProperty("Accept", "application/octet-stream")
    }
    val folder = File(context.cacheDir, "updates").apply { mkdirs() }
    val apk = File(folder, "Vlotrit-${release.versionName}.apk")
    return try {
        if (connection.responseCode !in 200..299) error("APK downloaden is mislukt (${connection.responseCode}).")
        connection.inputStream.use { input -> apk.outputStream().buffered().use { output -> input.copyTo(output) } }
        val archived = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("Het gedownloade bestand is geen geldige APK.")
        require(archived.packageName == context.packageName) { "Deze APK is niet de VLOTRIT-app." }
        require(archived.versionName == release.versionName) { "De APK-versie komt niet overeen met de GitHub-release." }
        val current = context.packageManager.getPackageInfo(context.packageName, 0)
        require(archived.versionCode > current.versionCode) { "De APK is niet nieuwer dan de geïnstalleerde versie." }
        apk
    } catch (e: Exception) {
        apk.delete()
        throw e
    } finally { connection.disconnect() }
}

private fun installUpdate(context: Context, apk: File) {
    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val sessionId = installer.createSession(params)
    try {
        installer.openSession(sessionId).use { session ->
            val output = session.openWrite("Vlotrit.apk", 0, apk.length())
            apk.inputStream().buffered().use { it.copyTo(output) }
            session.fsync(output)
            output.close()
            val callback = Intent(context, UpdateInstallReceiver::class.java).setAction(UPDATE_INSTALL_STATUS_ACTION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, 1212, callback, flags)
            session.commit(pendingIntent.intentSender)
        }
    } catch (e: Exception) {
        installer.abandonSession(sessionId)
        throw e
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
            }
            PackageInstaller.STATUS_SUCCESS -> context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("update_install_result", "Update geïnstalleerd").apply()
        }
    }
}

private fun startTrip(context: Context, prefs: android.content.SharedPreferences, startText: String, consumption: String, price: String, name: String, electric: Boolean, vehicle: VehicleProfile) {
    val start = startText.toFloatOrNull() ?: prefs.getFloat(KEY_LAST_ODO, 0f)
    val tripConsumption = consumption.replace(',', '.').toFloatOrNull() ?: vehicle.consumption
    val tripPrice = price.replace(',', '.').toFloatOrNull() ?: vehicle.price
    prefs.edit().putBoolean(KEY_RUNNING, true).putFloat(KEY_START_ODO, start).putFloat(KEY_TRACKED_KM, 0f)
        .putFloat(KEY_LAST_LAT, Float.NaN).putFloat(KEY_LAST_LON, Float.NaN).putString(KEY_ROUTE, "").remove(KEY_GPS_ERROR)
        .putBoolean(KEY_TRIP_ELECTRIC, electric)
        .putFloat(if (electric) KEY_ELECTRIC_CONSUMPTION else KEY_CONSUMPTION, tripConsumption)
        .putFloat(if (electric) KEY_ELECTRIC_PRICE else KEY_PRICE, tripPrice)
        .putString(KEY_ACTIVE_VEHICLE_ID, vehicle.id).putString(KEY_ACTIVE_VEHICLE_NAME, vehicle.name).putString(KEY_ACTIVE_FUEL_TYPE, vehicle.fuelType)
        .putFloat(KEY_ACTIVE_CONSUMPTION, tripConsumption).putFloat(KEY_ACTIVE_PRICE, tripPrice)
        .putBoolean(KEY_ACTIVE_DEPRECIATION, vehicle.depreciationEnabled).putFloat(KEY_ACTIVE_DEPRECIATION_RATE, vehicle.depreciationPerKm)
        .putString(KEY_NAME, name).putLong(KEY_STARTED_AT, System.currentTimeMillis()).putLong(KEY_STOPPED_AT, 0L).apply()
    val intent = Intent(context, RouteTrackingService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

@Composable
private fun ReceiptCard(context: Context, prefs: android.content.SharedPreferences, route: String, endOdo: Float, km: Float, consumption: Float, price: Float, priceUpdatedAt: Long, tripName: String, electric: Boolean, startLocation: String, endLocation: String, depreciationEnabled: Boolean, depreciationPerKm: Float, vehicleId: String, vehicleName: String, fuelType: String, actualEnergy: Float?) {
    val visibleFuelType = fuelTypeLabel(fuelType)
    val estimatedEnergy = km * consumption / 100f
    val energy = actualEnergy ?: estimatedEnergy
    val energyUnit = if (electric) "kWh" else "L"
    val consumptionUnit = if (electric) "kWh/100 km" else "L/100 km"
    val priceLabel = if (electric) "Stroomprijs" else "$visibleFuelType prijs"
    val depreciationCost = if (depreciationEnabled) km * depreciationPerKm else 0f
    val total = energy * price + depreciationCost
    val start = prefs.getFloat(KEY_START_ODO, endOdo - km)
    val started = prefs.getLong(KEY_STARTED_AT, System.currentTimeMillis())
    val stopped = prefs.getLong(KEY_STOPPED_AT, System.currentTimeMillis())
    val receiptLines = mutableListOf(
        "VLOTRIT Bon${if (tripName.isNotBlank()) " · $tripName" else ""}",
        "Voertuig: $vehicleName ($visibleFuelType)",
        "${km.formatNl()} km gereden",
        "Gemiddeld verbruik: ${estimatedEnergy.formatNl()} $energyUnit (${consumption.formatNl()} $consumptionUnit)",
        "$priceLabel: ${price.euro()} per $energyUnit"
    )
    actualEnergy?.let { receiptLines.add("Gemeten verbruik: ${it.formatNl()} $energyUnit totaal (${(it * 100f / km.coerceAtLeast(0.01f)).formatNl()} $consumptionUnit)") }
    receiptLines.add("${if (electric) "Stroomkosten" else "Brandstofkosten"}: ${(energy * price).euro()}")
    if (!electric && fuelType != "E5/98") receiptLines.add("ANWB-prijs opgehaald: ${priceUpdatedAt.dateTimeOrUnknown()}")
    receiptLines.add("Kilometerstand: ${start.formatNl()} → ${endOdo.formatNl()} km")
    if (startLocation.isNotBlank()) receiptLines.add("Vertrek: $startLocation")
    if (endLocation.isNotBlank()) receiptLines.add("Aankomst: $endLocation")
    if (depreciationEnabled) receiptLines.add("Afschrijving (${depreciationPerKm.euro()}/km): ${depreciationCost.euro()}")
    receiptLines.add("------------------------------")
    receiptLines.add("TOTALE KOSTEN: ${total.euro()}")
    val receipt = receiptLines.joinToString("\n")
    val receiptTrip = TripRecord(stopped, tripName, start, endOdo, km, consumption, price, priceUpdatedAt, started, stopped,
        route, electric, startLocation, endLocation, depreciationEnabled, depreciationPerKm, vehicleId, vehicleName, fuelType, actualEnergy)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF14395F)), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VLOTRIT Bon · $vehicleName ($visibleFuelType)", color = Color(0xFFB6CBBF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            HorizontalDivider(color = Color.White.copy(alpha = .2f))
            ReceiptLine("Afstand", "${km.formatNl()} km")
            actualEnergy?.let { ReceiptLine("Gemeten verbruik", "${it.formatNl()} $energyUnit totaal") }
            ReceiptLine("Gemiddeld verbruik", "${energy.formatNl()} $energyUnit · ${consumption.formatNl()} $consumptionUnit")
            ReceiptLine(if (electric) "Stroomkosten" else "Brandstofkosten", (energy * price).euro())
            if (depreciationEnabled) ReceiptLine("Afschrijving (${depreciationPerKm.euro()}/km)", depreciationCost.euro())
            ReceiptLine(priceLabel, "${price.euro()} / $energyUnit")
            if (!electric && fuelType != "E5/98") ReceiptLine("ANWB-prijs opgehaald", priceUpdatedAt.dateTimeOrUnknown())
            ReceiptLine("Beginstand → eindstand", "${start.formatNl()} → ${endOdo.formatNl()} km")
            ReceiptLine("Tijd", "${Date(started).clock()} – ${Date(stopped).clock()}")
            if (route.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text("GEREDEN ROUTE", color = Color(0xFFB6CBBF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                RouteMap(route, Modifier.fillMaxWidth().height(175.dp))
                Text("Kaartgegevens © OpenStreetMap", color = Color(0xFFB6CBBF), fontSize = 10.sp)
            } else Text("Geen GPS-route opgeslagen. De afstand is handmatig ingevoerd of GPS was niet beschikbaar.", color = Color(0xFFB6CBBF), fontSize = 11.sp)
            if (startLocation.isNotBlank()) ReceiptLine("Vertrek", startLocation)
            if (endLocation.isNotBlank()) ReceiptLine("Aankomst", endLocation)
            HorizontalDivider(color = Color.White.copy(alpha = .55f), thickness = 1.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOTALE KOSTEN", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(total.euro(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = { shareTripPdf(context, receiptTrip) }, modifier = Modifier.weight(1f)) { Text("PDF") }
                OutlinedButton(onClick = { shareTripImage(context, receiptTrip) }, modifier = Modifier.weight(1f)) { Text("Afbeelding") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    shareTrip(context, receiptTrip, receipt)
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Forest)) { Text("↗ Delen", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable private fun ReceiptLine(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color(0xFFDCE8DF), fontSize = 12.sp); Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
@Composable private fun HistoryDetailLine(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun InsightsTab(trips: List<TripRecord>) {
    var range by remember { mutableStateOf("Maand") }
    var metric by remember { mutableStateOf("Kosten") }
    val buckets = remember(trips, range) { buildInsightBuckets(trips, range) }
    val totalKm = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCost = trips.sumOf { it.cost.toDouble() }.toFloat()
    val averageKm = if (trips.isEmpty()) 0f else totalKm / trips.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Totaaloverzicht · alle voertuigen", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InsightMetric("GEREDEN", "${totalKm.formatNl()} km", Modifier.weight(1f))
                    InsightMetric("TOTALE KOSTEN", totalCost.euro(), Modifier.weight(1f))
                    InsightMetric("RITTEN", "${trips.size}", Modifier.weight(0.8f))
                }
                Text("Gemiddeld ${averageKm.formatNl()} km per rit", color = Muted, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Week", "Maand", "Jaar").forEach { option ->
                FilterChip(selected = range == option, onClick = { range = option }, label = { Text(option) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Kosten", "Kilometers").forEach { option ->
                FilterChip(selected = metric == option, onClick = { metric = option }, label = { Text(option) })
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (metric == "Kosten") "Kosten per ${range.lowercase(Locale("nl", "NL"))}" else "Kilometers per ${range.lowercase(Locale("nl", "NL"))}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val values = buckets.map { if (metric == "Kosten") it.cost else it.km }
                val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
                Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                    val rows = 4
                    for (i in 0..rows) {
                        val y = size.height * i / rows
                        drawLine(Color(0xFFEAF0F6), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    val spacing = size.width * 0.025f
                    val barWidth = ((size.width - spacing * (buckets.size + 1)) / buckets.size).coerceAtLeast(2f)
                    buckets.forEachIndexed { index, bucket ->
                        val value = if (metric == "Kosten") bucket.cost else bucket.km
                        val height = (value / maxValue * (size.height - 9.dp.toPx())).coerceAtLeast(if (value > 0) 5.dp.toPx() else 0f)
                        val x = spacing + index * (barWidth + spacing)
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF58B7E2), Forest)),
                            topLeft = androidx.compose.ui.geometry.Offset(x, size.height - height),
                            size = androidx.compose.ui.geometry.Size(barWidth, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    buckets.forEach { bucket ->
                        Text(bucket.label, Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                    }
                }
                val periodTotal = values.sum()
                Text(if (metric == "Kosten") "Totaal in deze periode: ${periodTotal.euro()}" else "Totaal in deze periode: ${periodTotal.formatNl()} km", color = Forest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("De grafiek gebruikt je opgeslagen ritten en houdt rekening met brandstof-, stroom- en afschrijvingskosten.", color = Muted, fontSize = 11.sp)
            }
        }
        if (trips.isEmpty()) Text("Je grafiek wordt gevuld zodra je ritten opslaat.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun InsightMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(BlueTint, RoundedCornerShape(15.dp)).padding(horizontal = 11.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, maxLines = 1)
        Text(value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun ChargeSessionsTab(prefs: android.content.SharedPreferences, vehicle: VehicleProfile) {
    var sessions by remember { mutableStateOf(readChargeSessions(prefs)) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedSessionIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var kwh by remember { mutableStateOf("") }
    var price by remember(vehicle.id) { mutableStateOf((if (vehicle.electric) vehicle.price else 0.30f).formatPrice()) }
    var location by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val totalKwh = sessions.filter { it.vehicleId == vehicle.id }.sumOf { it.kwh.toDouble() }.toFloat()
    val totalCost = sessions.filter { it.vehicleId == vehicle.id }.sumOf { it.cost.toDouble() }.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nieuwe laadbeurt", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Profiel: ${vehicle.name}${if (vehicle.electric) "" else " · kies bovenaan een elektrisch voertuig"}", color = if (vehicle.electric) Muted else Color(0xFF9A5B16), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericField("Geladen energie", kwh, { kwh = it }, "kWh", Modifier.weight(1f), enabled = true)
                    NumericField("Prijs per kWh", price, { price = it }, "€/kWh", Modifier.weight(1f), enabled = true)
                }
                OutlinedTextField(location, { location = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Laadlocatie (optioneel)") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                val amount = kwh.replace(',', '.').toFloatOrNull() ?: 0f
                val unitPrice = price.replace(',', '.').toFloatOrNull() ?: 0f
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Totaal", color = Muted, fontSize = 13.sp)
                    Text((amount * unitPrice).euro(), color = Forest, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Button(onClick = {
                    val energy = kwh.replace(',', '.').toFloatOrNull()
                    val rate = price.replace(',', '.').toFloatOrNull()
                    if (!vehicle.electric) message = "Selecteer eerst een elektrisch voertuig bij Voertuigprofiel."
                    else if (energy == null || energy <= 0f || rate == null || rate < 0f) message = "Vul geldige kWh en een prijs per kWh in."
                    else {
                        val session = ChargeSession(System.currentTimeMillis(), vehicle.id, vehicle.name, energy, rate, location.trim(), System.currentTimeMillis())
                        sessions = listOf(session) + sessions
                        writeChargeSessions(prefs, sessions)
                        kwh = ""; location = ""
                        message = "Laadbeurt opgeslagen."
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), enabled = vehicle.electric) { Text("Laadbeurt opslaan", fontWeight = FontWeight.Bold) }
                if (message.isNotBlank()) Text(message, color = Forest, fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Laadgeschiedenis · ${vehicle.name}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selectionMode = !selectionMode; selectedSessionIds = emptySet() }) {
                        Text(if (selectionMode) "Annuleren" else "Selecteer", color = Forest)
                    }
                }
                Text("${totalKwh.formatNl()} kWh geladen  ·  ${totalCost.euro()} totaal", color = Forest, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                val visible = sessions.filter { it.vehicleId == vehicle.id }
                if (selectionMode) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${selectedSessionIds.size} geselecteerd", color = Muted, fontSize = 12.sp)
                    Button(onClick = { confirmDelete = true }, enabled = selectedSessionIds.isNotEmpty()) { Text("Verwijder selectie") }
                }
                if (visible.isEmpty()) Text("Je opgeslagen laadbeurten verschijnen hier.", color = Muted, fontSize = 12.sp)
                visible.forEach { session ->
                    HorizontalDivider(color = Color(0xFFEAF0F6))
                    Row(Modifier.fillMaxWidth().clickable(enabled = selectionMode) {
                        selectedSessionIds = if (session.id in selectedSessionIds) selectedSessionIds - session.id else selectedSessionIds + session.id
                    }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (selectionMode) Checkbox(checked = session.id in selectedSessionIds, onCheckedChange = { checked ->
                            selectedSessionIds = if (checked) selectedSessionIds + session.id else selectedSessionIds - session.id
                        })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${session.kwh.formatNl()} kWh  ·  ${session.cost.euro()}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${Date(session.timestamp).dateTime()}${if (session.location.isNotBlank()) "  ·  ${session.location}" else ""}", color = Muted, fontSize = 10.sp)
                            Text("${session.pricePerKwh.euro()} per kWh", color = Muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            sessions = sessions.filterNot { it.id == session.id }
                            selectedSessionIds = selectedSessionIds - session.id
                            writeChargeSessions(prefs, sessions)
                        }) { Text("Verwijder", color = Color(0xFFB14F45), fontSize = 11.sp) }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Laadbeurten verwijderen?") },
        text = { Text("${selectedSessionIds.size} geselecteerde laadbeurt(en) worden definitief verwijderd.") },
        confirmButton = { TextButton(onClick = {
            sessions = sessions.filterNot { it.id in selectedSessionIds }
            writeChargeSessions(prefs, sessions)
            selectedSessionIds = emptySet(); selectionMode = false; confirmDelete = false
        }) { Text("Verwijderen", color = Color(0xFFB14F45)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MaintenanceTab(
    prefs: android.content.SharedPreferences, currentOdometer: Float, profiles: List<VehicleProfile>, selectedId: String,
    onVehicleSelect: (VehicleProfile) -> Unit, onProfileUpdated: (VehicleProfile) -> Unit
) {
    val context = LocalContext.current
    val vehicle = profiles.firstOrNull { it.id == selectedId } ?: profiles.first()
    val reminders = remember { mutableStateListOf<JSONObject>().apply {
        try { val array = JSONArray(prefs.getString(KEY_MAINTENANCE_REMINDERS, "[]")); for (i in 0 until array.length()) add(array.getJSONObject(i)) } catch (_: Exception) { }
    } }
    var kind by remember { mutableStateOf("APK") }
    var dueDate by remember { mutableStateOf("") }
    var dueOdo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var lookupStatus by remember { mutableStateOf("") }
    fun persist() = prefs.edit().putString(KEY_MAINTENANCE_REMINDERS, JSONArray(reminders).toString()).apply()

    LaunchedEffect(vehicle.id, vehicle.licensePlate) {
        if (vehicle.licensePlate.isNotBlank()) {
            lookupStatus = "RDW-gegevens voor ${vehicle.licensePlate} ophalen…"
            try {
                val rdw = withContext(Dispatchers.IO) { lookupRdwVehicle(vehicle.licensePlate) }
                if (rdw.apkDate.isNotBlank()) {
                    val updatedReminder = JSONObject().put("id", "apk-${vehicle.id}").put("kind", "APK")
                        .put("date", rdw.apkDate).put("odometer", 0).put("note", "Automatisch opgehaald bij de RDW")
                        .put("vehicleId", vehicle.id).put("vehicleName", vehicle.name).put("licensePlate", vehicle.licensePlate)
                    val existing = reminders.indexOfFirst { it.optString("kind") == "APK" && it.optString("vehicleId") == vehicle.id }
                    if (existing >= 0) reminders[existing] = updatedReminder else reminders.add(updatedReminder)
                    persist()
                }
                rdw.consumption?.let { value -> if (value > 0f && (abs(vehicle.consumption - value) > 0.01f || (rdw.electric && !vehicle.electric))) {
                    onProfileUpdated(vehicle.copy(fuelType = if (rdw.electric) "EV" else vehicle.fuelType,
                        price = if (rdw.electric && !vehicle.electric) 0.30f else vehicle.price, consumption = value))
                } }
                lookupStatus = when {
                    rdw.apkDate.isNotBlank() && rdw.consumption != null -> "RDW: APK ${rdw.apkDate} · testverbruik ${rdw.consumption.formatNl()} ${if (rdw.electric) "kWh/100 km" else "L/100 km"}."
                    rdw.apkDate.isNotBlank() -> "RDW: APK verloopt op ${rdw.apkDate}. Geen testverbruik voor dit kenteken gevonden."
                    else -> "Geen actuele APK-vervaldatum beschikbaar bij de RDW. Controleer het kenteken of voeg de datum handmatig toe."
                }
            } catch (e: Exception) { lookupStatus = e.message ?: "RDW-gegevens niet bereikbaar. Je kunt de APK-datum handmatig invullen." }
        } else lookupStatus = "Vul bij dit voertuig eerst het kenteken in om de APK via RDW op te zoeken."
    }
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Voertuig herinneringen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Plan APK, onderhoud en bandenwissels op datum of kilometerstand.", color = Muted, fontSize = 13.sp)
            var vehicleMenuOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { vehicleMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Voertuig: ${vehicle.name} · ${vehicle.licensePlate.ifBlank { "geen kenteken" }} ▾")
                }
                DropdownMenu(expanded = vehicleMenuOpen, onDismissRequest = { vehicleMenuOpen = false }) {
                    profiles.forEach { profile -> DropdownMenuItem(
                        text = { Text("${profile.name} · ${profile.licensePlate.ifBlank { fuelTypeLabel(profile.fuelType) }}") },
                        onClick = { vehicleMenuOpen = false; onVehicleSelect(profile) })
                    }
                }
            }
            Text(lookupStatus, color = if (lookupStatus.startsWith("Geen") || lookupStatus.startsWith("Vul")) Muted else Forest, fontSize = 11.sp)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(kind, {}, readOnly = true, label = { Text("Type herinnering") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("APK", "Onderhoud", "Bandenwissel").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { kind = item; expanded = false }) }
                }
            }
            OutlinedButton(onClick = {
                val cal = java.util.Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day -> dueDate = "%04d-%02d-%02d".format(year, month + 1, day) }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }, modifier = Modifier.fillMaxWidth()) { Text(if (dueDate.isBlank()) "Kies datum" else "Datum: $dueDate") }
            NumericField("Bij kilometerstand (optioneel)", dueOdo, { dueOdo = it }, "km", Modifier.fillMaxWidth(), true)
            OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notitie (optioneel)") }, singleLine = true)
            Button(onClick = {
                val odo = dueOdo.replace(',', '.').toFloatOrNull() ?: 0f
                if (dueDate.isNotBlank() || odo > 0f) {
                    reminders.add(JSONObject().put("id", System.currentTimeMillis()).put("kind", kind).put("date", dueDate).put("odometer", odo).put("note", note.trim())
                        .put("vehicleId", vehicle.id).put("vehicleName", vehicle.name).put("licensePlate", vehicle.licensePlate))
                    persist(); dueDate = ""; dueOdo = ""; note = ""
                }
            }, enabled = dueDate.isNotBlank() || (dueOdo.replace(',', '.').toFloatOrNull() ?: 0f) > 0f, modifier = Modifier.fillMaxWidth()) { Text("Herinnering opslaan") }
            HorizontalDivider()
            Text("Opgeslagen herinneringen", color = Ink, fontWeight = FontWeight.Bold)
            if (reminders.isEmpty()) Text("Nog geen herinneringen. Stel bijvoorbeeld de volgende APK-datum in.", color = Muted, fontSize = 12.sp)
            reminders.sortedBy { it.optString("date", "9999") }.forEach { reminder ->
                val date = reminder.optString("date", "")
                val odo = reminder.optDouble("odometer", 0.0).toFloat()
                val overdueDate = date.isNotBlank() && date < LocalDate.now().toString()
                val overdueOdo = odo > 0f && currentOdometer >= odo
                Card(colors = CardDefaults.cardColors(containerColor = if (overdueDate || overdueOdo) Color(0xFFFFF0E9) else BlueTint), shape = RoundedCornerShape(15.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(reminder.optString("kind"), fontWeight = FontWeight.Bold, color = Ink)
                            Text("${reminder.optString("vehicleName", "Voertuig")} · ${reminder.optString("licensePlate")}", color = Muted, fontSize = 11.sp)
                            Text(listOfNotNull(date.takeIf { it.isNotBlank() }, (if (odo > 0f) "${odo.formatNl()} km" else null)).joinToString(" · "), color = Muted, fontSize = 12.sp)
                            if (reminder.optString("note").isNotBlank()) Text(reminder.optString("note"), color = Muted, fontSize = 12.sp)
                            if (overdueDate || overdueOdo) Text("Aandacht nodig", color = Color(0xFFAA4328), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        TextButton(onClick = { reminders.remove(reminder); persist() }) { Text("Verwijder") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInfoTab(context: Context, latestRelease: AppRelease?, availableRelease: AppRelease?, checkingUpdates: Boolean, onCheckUpdates: () -> Unit) {
    val packageInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: "Onbekend"
    @Suppress("DEPRECATION")
    val versionCode = packageInfo?.let {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()).toString()
    } ?: "Onbekend"

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        UpdateCard(context, context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), latestRelease, availableRelease, checkingUpdates, onCheckUpdates)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(painter = painterResource(R.drawable.vlotrit_logo), contentDescription = "VLOTRIT-logo",
                    modifier = Modifier.size(62.dp), contentScale = ContentScale.Crop)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("VLOTRIT", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("Ritten, kosten en routes overzichtelijk bij elkaar.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appgegevens", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                HistoryDetailLine("Versie", versionName)
                HistoryDetailLine("Buildnummer", versionCode)
                HistoryDetailLine("App-id", context.packageName)
                Text("Nieuwe updates vind je bovenaan dit Info-tabblad.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                TextButton(onClick = { openExternal(context, "https://github.com/Redglowmk3/vlotrit/releases/latest") }) {
                    Text("Bekijk releases op GitHub ↗", color = Forest, fontWeight = FontWeight.Bold)
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Kaart en prijzen", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Kaart via OpenFreeMap met OpenStreetMap-gegevens. Pompprijzen zijn indicatief en kunnen afwijken van de prijs aan de pomp.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    TextButton(onClick = { openExternal(context, "https://openfreemap.org/") }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 0.dp)) { Text("OpenFreeMap", color = Forest) }
                    TextButton(onClick = { openExternal(context, "https://www.openstreetmap.org/copyright") }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 0.dp)) { Text("OpenStreetMap", color = Forest) }
                    TextButton(onClick = { openExternal(context, "https://hoeveelbenzine.nl/methodologie") }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Prijsbron: Hoeveelbenzine.nl", color = Forest) }
                }
            }
        }
    }
}

@Composable
private fun StationsTab(context: Context, prefs: android.content.SharedPreferences) {
    var filter by remember { mutableStateOf("Alles") }
    var stations by remember { mutableStateOf(emptyList<NearbyStation>()) }
    // Always show a useful map, even before permission/location lookup succeeds.
    val netherlandsCenter = 52.1326 to 5.2913
    var center by remember { mutableStateOf<Pair<Double, Double>?>(netherlandsCenter) }
    var hasGpsCenter by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var sortBy by remember { mutableStateOf("Afstand") }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var expandedStation by remember { mutableStateOf<String?>(null) }
    var e10Price by remember { mutableFloatStateOf(prefs.getFloat(KEY_PRICE, 2.092f)) }
    var e10Updated by remember { mutableLongStateOf(prefs.getLong(KEY_PRICE_UPDATED_AT, 0L)) }
    val scope = rememberCoroutineScope()
    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) permissionRefresh++
        else status = "Locatietoegang is nodig om stations in de buurt te vinden."
    }

    LaunchedEffect(permissionRefresh) {
        if (!hasLocation) {
                status = "Geef locatietoegang om stations in jouw buurt te zoeken."
        } else {
            loading = true; status = "Stations zoeken in de buurt…"
            try {
                val location = withContext(Dispatchers.IO) { currentLocation(context) }
                    ?: error("Geen actuele locatie gevonden. Zet locatie aan en probeer opnieuw.")
                center = location.latitude to location.longitude
                hasGpsCenter = true
                val nearby = withContext(Dispatchers.IO) { findNearbyStations(location.latitude, location.longitude) }
                val city = nearby.firstOrNull { it.city.isNotBlank() }?.city
                    ?: runCatching { withContext(Dispatchers.IO) { reverseGeocodeCity(location.latitude, location.longitude) } }.getOrNull().orEmpty()
                val broaderPrices = if (city.isNotBlank()) runCatching {
                    withContext(Dispatchers.IO) { fetchCommunityStationPrices(city, nearby) }
                }.getOrDefault(emptyMap()) else emptyMap()
                stations = nearby.map { station ->
                    val wider = broaderPrices[stationAddressKey(station.address)]
                    station.copy(priceE10 = wider?.e10, priceE5 = wider?.e5,
                        priceDiesel = wider?.diesel, priceCheckedAt = wider?.checkedAt ?: 0L,
                        priceSource = if (wider != null) "ANWB / DirectLease via HoeveelBenzine" else "")
                }
                status = if (stations.isEmpty()) "Geen stations binnen 5 km gevonden. Probeer de zoekstraal opnieuw." else "${stations.size} locaties gevonden binnen 5 km."
            } catch (e: Exception) { status = e.message ?: "Stations konden niet worden geladen." }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) { fetchE10Price().first }
            e10Price = result
            e10Updated = System.currentTimeMillis()
            prefs.edit().putFloat(KEY_PRICE, result).putLong(KEY_PRICE_UPDATED_AT, e10Updated).apply()
        } catch (_: Exception) { }
    }

    val visibleStations = remember(stations, filter) {
        when (filter) {
            "Tankstations" -> stations.filter { it.kind == "Tankstation" }
            "Laadpunten" -> stations.filter { it.kind == "Laadpunt" }
            else -> stations
        }
    }
    val sortedStations = remember(visibleStations, sortBy) {
        when (sortBy) {
            "Prijs laag-hoog" -> visibleStations.sortedWith(compareBy<NearbyStation> { it.priceE10 == null }.thenBy { it.priceE10 ?: Float.MAX_VALUE }.thenBy { it.distanceKm })
            "Prijs hoog-laag" -> visibleStations.sortedWith(compareBy<NearbyStation> { it.priceE10 == null }.thenByDescending { it.priceE10 ?: 0f }.thenBy { it.distanceKm })
            "Naam" -> visibleStations.sortedBy { it.name.lowercase(Locale.ROOT) }
            else -> visibleStations.sortedBy { it.distanceKm }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ANWB-gemiddelde E10", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${e10Price.euro()} per liter", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (e10Updated > 0L) Date(e10Updated).dateTime() else "Datum onbekend", color = Muted, fontSize = 10.sp)
                }
                Text("Pompprijzen zijn indicatief en komen uit ANWB/DirectLease; de prijsfeed wordt ongeveer drie keer per dag bijgewerkt. Niet ieder station heeft een prijs.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                TextButton(onClick = { openExternal(context, "https://hoeveelbenzine.nl/goedkoop-tanken") }) { Text("Bekijk prijsbron ↗", color = Forest, fontWeight = FontWeight.Bold) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Alles", "Tankstations", "Laadpunten").forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option, fontSize = 11.sp) })
            }
        }
        if (center != null) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StationMap(sortedStations, center!!, hasGpsCenter, Modifier.fillMaxWidth().height(245.dp))
                    Text("© OpenStreetMap contributors · tik op een markering voor details", color = Muted, fontSize = 10.sp)
                }
            }
        }
        Text(status, color = if (status.startsWith("Geen") || status.startsWith("Locatie")) Color(0xFF9A5B16) else Muted, fontSize = 11.sp)
        Button(onClick = {
            if (hasLocation) permissionRefresh++ else {
                val requested = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                locationLauncher.launch(requested.toTypedArray())
            }
        }, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (loading) "Ophalen..." else if (hasLocation) "Ververs stations en prijzen" else "Locatietoegang geven")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${sortedStations.size} locaties", color = Muted, fontSize = 12.sp)
            Box {
                OutlinedButton(onClick = { sortMenuOpen = true }) { Text("Sorteer: ${sortBy}") }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    listOf("Afstand", "Prijs laag-hoog", "Prijs hoog-laag", "Naam").forEach { choice ->
                        DropdownMenuItem(text = { Text(choice) }, onClick = { sortBy = choice; sortMenuOpen = false })
                    }
                }
            }
        }
        sortedStations.forEach { station ->
            val key = "${station.latitude},${station.longitude}"
            val expanded = expandedStation == key
            Card(Modifier.fillMaxWidth().clickable { expandedStation = if (expanded) null else key },
                shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(if (station.kind == "Tankstation") Color(0xFFFFF2E7) else BlueTint, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Text(if (station.kind == "Tankstation") "T" else "⚡", color = if (station.kind == "Tankstation") Color(0xFFB76A2B) else Forest, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(station.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(station.address.ifBlank { station.kind }, color = Muted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (station.priceE10 != null && station.kind == "Tankstation") station.priceE10.euro() else "—",
                                color = if (station.priceE10 != null) Forest else Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            if (station.kind == "Tankstation") Text("E10 / liter", color = Muted, fontSize = 9.sp)
                            Text("${String.format(Locale("nl", "NL"), "%.1f", station.distanceKm)} km", color = Color(0xFF2563A6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (expanded) {
                        HorizontalDivider(color = Color(0xFFE5EBE5))
                        val fuelPrices = listOfNotNull(
                            station.priceE10?.let { "E10 ${it.euro()}/L" },
                            station.priceE5?.let { "E5/98 ${it.euro()}/L" },
                            station.priceDiesel?.let { "Diesel ${it.euro()}/L" }
                        ).joinToString(" · ")
                        Text(if (station.kind == "Tankstation") fuelPrices.ifBlank { "Geen prijs voor dit station in de prijsfeed." }
                            else station.details.ifBlank { "Laadpuntgegevens niet beschikbaar." }, color = Ink, fontSize = 12.sp)
                        if (fuelPrices.isNotBlank()) Text("Indicatieve prijzen · ANWB/DirectLease · gegevens opgehaald ${Date(station.priceCheckedAt).dateTime()}", color = Muted, fontSize = 10.sp)
                        Text("${station.name} · ${station.address.ifBlank { "Adres onbekend" }}", color = Muted, fontSize = 11.sp)
                        if (station.details.isNotBlank()) Text(station.details, color = Muted, fontSize = 11.sp)
                        if (station.openingHours.isNotBlank()) Text("Openingstijden: ${station.openingHours}", color = Muted, fontSize = 11.sp)
                        if (station.phone.isNotBlank()) Text("Telefoon: ${station.phone}", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { openStationNavigation(context, station) }) { Text("Navigeer", color = Forest) }
                            if (station.website.isNotBlank()) TextButton(onClick = { openExternal(context, station.website) }) { Text("Website", color = Forest) }
                            TextButton(onClick = { openAnwbStations(context) }) { Text("ANWB-prijs", color = Forest) }
                        }
                    }
                }
            }
        }
        if (!loading && stations.isEmpty()) Text("Geen locaties gevonden. Controleer locatietoegang en probeer opnieuw.", color = Muted, fontSize = 11.sp)

    }
}

private fun currentLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        manager.getProviders(true).mapNotNull { provider -> manager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
    } catch (_: SecurityException) { null }
}

private fun findNearbyStations(latitude: Double, longitude: Double): List<NearbyStation> {
    val clauses = "node(around:5000,$latitude,$longitude)[amenity=fuel];way(around:5000,$latitude,$longitude)[amenity=fuel];" +
        "node(around:5000,$latitude,$longitude)[amenity=charging_station];way(around:5000,$latitude,$longitude)[amenity=charging_station];"
    val query = "[out:json][timeout:15];($clauses);out center tags;"
    val endpoints = listOf(
        "https://ovp.dresel.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass-api.de/api/interpreter"
    )
    var lastError = "geen antwoord"
    for (endpoint in endpoints) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10000; readTimeout = 18000
            setRequestProperty("User-Agent", "VLOTRIT/1.19 (Android; https://github.com/Redglowmk3/vlotrit)")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        try {
            connection.outputStream.use { it.write(("data=" + java.net.URLEncoder.encode(query, "UTF-8")).toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) error("HTTP $responseCode")
            val elements = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("elements") ?: JSONArray()
            return (0 until elements.length()).mapNotNull { i ->
                val item = elements.optJSONObject(i) ?: return@mapNotNull null
                val tags = item.optJSONObject("tags") ?: JSONObject()
                val kind = if (tags.optString("amenity") == "fuel") "Tankstation" else if (tags.optString("amenity") == "charging_station") "Laadpunt" else return@mapNotNull null
                val center = item.optJSONObject("center")
                val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
                val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
                if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
                val displayName = tags.optString("name").ifBlank { tags.optString("brand").ifBlank { tags.optString("operator").ifBlank { kind } } }
                val address = listOf(tags.optString("addr:street"), tags.optString("addr:housenumber"), tags.optString("addr:city"))
                    .filter { it.isNotBlank() }.joinToString(" ")
                val details = if (kind == "Laadpunt") listOf(tags.optString("socket:type2").takeIf { it.isNotBlank() }?.let { "Type 2 $it" },
                    tags.optString("socket:ccs").takeIf { it.isNotBlank() }?.let { "CCS $it" }, tags.optString("capacity").takeIf { it.isNotBlank() })
                    .filterNotNull().joinToString(" · ") else tags.optString("fuel:e10").takeIf { it.isNotBlank() }?.let { "E10: $it" }.orEmpty()
                val distance = FloatArray(1)
                Location.distanceBetween(latitude, longitude, lat, lon, distance)
                NearbyStation(displayName, kind, address, lat, lon, distance[0] / 1000.0, details,
                    website = tags.optString("website"), phone = tags.optString("phone"), openingHours = tags.optString("opening_hours"), city = tags.optString("addr:city"))
            }.distinctBy { "${it.latitude},${it.longitude}" }.sortedBy { it.distanceKm }.take(40)
        } catch (e: Exception) {
            lastError = e.message ?: "onbekende fout"
        } finally { connection.disconnect() }
    }
    error("Stations zoeken is tijdelijk niet beschikbaar. Laatste fout: $lastError")
}

private fun openAnwbStations(context: Context) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.anwb.nl/auto/brandstof/goedkoopste-tankstation"))) }
    catch (_: Exception) { }
}

private data class AnwbStationPrice(val address: String, val price: Float)
private data class CommunityFuelPrice(val e10: Float? = null, val e5: Float? = null, val diesel: Float? = null, val checkedAt: Long = 0L)

private fun normalizeStationAddress(value: String): String = value.lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")

private fun stationAddressKey(value: String): String {
    val clean = normalizeStationAddress(value)
    val parts = clean.split(' ').filter { it.isNotBlank() }
    if (parts.size < 2) return clean
    val houseIndex = parts.indexOfFirst { it.any(Char::isDigit) }
    return if (houseIndex > 0) parts.take(houseIndex + 1).joinToString(" ") else parts.take(2).joinToString(" ")
}

private fun reverseGeocodeCity(latitude: Double, longitude: Double): String {
    val url = URL("https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=10&addressdetails=1&lat=$latitude&lon=$longitude")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 9000; readTimeout = 10000
        setRequestProperty("User-Agent", "VLOTRIT Android personal vehicle-cost app")
    }
    return try {
        val address = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONObject("address") ?: return ""
        listOf("city", "town", "village", "municipality", "hamlet").firstNotNullOfOrNull { key -> address.optString(key).takeIf { it.isNotBlank() } }.orEmpty()
    } finally { connection.disconnect() }
}

private fun fetchCommunityStationPrices(city: String, stations: List<NearbyStation>): Map<String, CommunityFuelPrice> {
    val slug = normalizeStationAddress(city).replace(' ', '-')
    val url = URL("https://hoeveelbenzine.nl/goedkoop-tanken/$slug")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000; readTimeout = 15000
        setRequestProperty("User-Agent", "VLOTRIT Android personal vehicle-cost app")
    }
    return try {
        val html = connection.inputStream.bufferedReader().use { it.readText() }
        val plain = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().replace('\u00a0', ' ')
        val result = mutableMapOf<String, CommunityFuelPrice>()
        val pattern = Regex("(?i)(Euro\\s*95\\s*(?:\\(E10\\))?|Super\\s*Plus\\s*98|Euro\\s*98|Diesel(?:\\s*\\(B7\\))?)[^€\\n]{0,70}€\\s*([0-9]+[,.][0-9]{2,3})")
        pattern.findAll(plain).forEach { match ->
            val price = match.groupValues[2].replace(',', '.').toFloatOrNull() ?: return@forEach
            val before = plain.substring((match.range.first - 650).coerceAtLeast(0), match.range.first)
            val normalizedBefore = normalizeStationAddress(before)
            val owner = stations.mapNotNull { station ->
                val key = stationAddressKey(station.address)
                val position = if (key.length >= 5) normalizedBefore.lastIndexOf(key) else -1
                if (position < 0) null else Triple(station, key, position)
            }.maxByOrNull { it.third }?.first ?: return@forEach
            val key = stationAddressKey(owner.address)
            val prior = result[key] ?: CommunityFuelPrice()
            val product = match.groupValues[1].lowercase(Locale.ROOT)
            result[key] = when {
                product.contains("diesel") -> prior.copy(diesel = price, checkedAt = System.currentTimeMillis())
                product.contains("98") -> prior.copy(e5 = price, checkedAt = System.currentTimeMillis())
                else -> prior.copy(e10 = price, checkedAt = System.currentTimeMillis())
            }
        }
        result
    } finally { connection.disconnect() }
}

private fun fetchAnwbStationPrices(): List<AnwbStationPrice> {
    val connection = (URL("https://www.anwb.nl/auto/brandstof/goedkoopste-tankstation").openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000; readTimeout = 15000
        setRequestProperty("User-Agent", "VLOTRIT Android app (https://github.com/Redglowmk3/vlotrit)")
    }
    return try {
        val html = connection.inputStream.bufferedReader().use { it.readText() }
        val plain = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
            .replace('\u00a0', ' ').replace(Regex("[ \\t]+"), " ")
        val blocks = Regex("(?is)(?:Zuid-Holland|Noord-Holland|Noord-Brabant|Gelderland|Utrecht|Overijssel|Limburg|Friesland|Groningen|Drenthe|Flevoland|Zeeland) top 3(.*?)(?=(?:Zuid-Holland|Noord-Holland|Noord-Brabant|Gelderland|Utrecht|Overijssel|Limburg|Friesland|Groningen|Drenthe|Flevoland|Zeeland) top 3|Brandstofprijzen)")
        blocks.findAll(plain).flatMap { block ->
            val rowRegex = Regex("(?is)(?:^|\\n)\\s*[1-3]\\s+(.{3,180}?)\\s+Bekijk op de kaart.*?Brandstofprijs\\s*\\(€\\s*/\\s*liter\\)\\s*([0-9]+,[0-9]{2,3})")
            rowRegex.findAll(block.groupValues[1]).mapNotNull { row ->
                val address = row.groupValues[1].trim().replace(Regex("\\s+"), " ")
                val price = row.groupValues[2].replace(',', '.').toFloatOrNull()
                if (address.isBlank() || price == null) null else AnwbStationPrice(address, price)
            }
        }.toList()
    } finally { connection.disconnect() }
}

private fun openStationNavigation(context: Context, station: NearbyStation) {
    val label = Uri.encode(station.name + " " + station.address)
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$label&mode=d"))) }
    catch (_: Exception) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${station.latitude},${station.longitude}?q=$label"))) }
        catch (_: Exception) { }
    }
}

private fun openExternal(context: Context, value: String) {
    val url = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
}

private fun playEasterEggSound(horn: Boolean = false) {
    val sampleRate = 22050
    val durationMs = if (horn) 420 else 560
    val samples = ShortArray(sampleRate * durationMs / 1000)
    var phase = 0.0
    samples.indices.forEach { index ->
        val progress = index.toDouble() / samples.size
        val time = index.toDouble() / sampleRate
        val frequency = if (horn) if (progress < .48) 520.0 else 660.0 else 82.0 + 155.0 * progress
        phase += 2.0 * Math.PI * frequency / sampleRate
        val envelope = minOf(1.0, progress * 18.0, (1.0 - progress) * 14.0).coerceAtLeast(0.0)
        val pulse = if (horn) 1.0 else .52 + .48 * kotlin.math.abs(kotlin.math.sin(2.0 * Math.PI * 19.0 * time))
        val wave = if (horn) kotlin.math.sin(phase) else kotlin.math.sin(phase) + .34 * kotlin.math.sin(phase * 2.0) + .16 * kotlin.math.sin(phase * 3.0)
        samples[index] = (wave * pulse * envelope * 7500.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    val track = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    }.getOrNull() ?: return
    if (track.write(samples, 0, samples.size) < 0) { track.release(); return }
    track.play()
    Handler(Looper.getMainLooper()).postDelayed({ runCatching { track.stop() }; track.release() }, durationMs.toLong() + 150L)
}

@Composable
private fun StationMap(stations: List<NearbyStation>, center: Pair<Double, Double>, hasGpsCenter: Boolean, modifier: Modifier = Modifier) {
    val markers = remember(stations) {
        JSONArray().apply { stations.forEach { station -> put(JSONObject().put("lat", station.latitude).put("lon", station.longitude)
            .put("name", station.name).put("kind", station.kind).put("address", station.address).put("details", station.details)
            .put("priceE10", station.priceE10 ?: JSONObject.NULL)) } }.toString()
    }
    val script = remember(markers, center, hasGpsCenter) {
        """window.initVlotritMap=function(){try{
            const stations=$markers,me=[${center.first},${center.second}];
            const map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[me[1],me[0]],zoom:10,attributionControl:true});
            map.addControl(new maplibregl.NavigationControl({showCompass:false}),'top-right');
            map.on('error',()=>document.getElementById('map-error').style.display='block');
            const points=[[me[1],me[0]]];
            new maplibregl.Marker({color:'#14395f'}).setLngLat([me[1],me[0]]).setPopup(new maplibregl.Popup().setText('Jouw locatie')).addTo(map);
            stations.forEach(s=>{const point=[s.lon,s.lat];points.push(point);const popup=document.createElement('div');popup.textContent=[s.name,s.kind,s.address,s.priceE10?'E10 EUR '+Number(s.priceE10).toFixed(3)+'/L':'',s.details].filter(Boolean).join(' | ');new maplibregl.Marker({color:s.kind==='Tankstation'?'#ef8a3c':'#1883bd'}).setLngLat(point).setPopup(new maplibregl.Popup({offset:24}).setDOMContent(popup)).addTo(map)});
            map.on('load',()=>{document.getElementById('map-error').style.display='none';if(!${hasGpsCenter}&&stations.length===0)map.setZoom(7);else if(points.length>1){const bounds=points.reduce((b,p)=>b.extend(p),new maplibregl.LngLatBounds(points[0],points[0]));map.fitBounds(bounds,{padding:35,maxZoom:15})}});
            setTimeout(()=>map.resize(),350);setTimeout(()=>{if(!map.isStyleLoaded())document.getElementById('map-error').style.display='block'},15000);
        }catch(e){document.getElementById('map-error').style.display='block'}};"""
    }
    MapWebView(script, modifier)
}

private fun loadMapHtml(context: Context, script: String): String =
    """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><link rel="stylesheet" href="https://unpkg.com/maplibre-gl@5/dist/maplibre-gl.css"><style>html,body,#map{width:100%;height:100%;margin:0;padding:0;background:#eef3f7}body{overflow:hidden}.maplibregl-ctrl-attrib{font:10px sans-serif}#map-error{display:none;position:absolute;z-index:5;left:12px;right:12px;bottom:12px;background:#fff;padding:10px 12px;border-radius:10px;color:#773d2b;font:13px sans-serif;box-shadow:0 2px 8px #0003}</style></head><body><div id="map"></div><div id="map-error">Kaart niet geladen. Controleer je internetverbinding en probeer opnieuw.</div><script>$script</script><script>(function(){const urls=['https://unpkg.com/maplibre-gl@5/dist/maplibre-gl.js','https://cdn.jsdelivr.net/npm/maplibre-gl@5/dist/maplibre-gl.js'];let i=0;function next(){if(i>=urls.length){document.getElementById('map-error').style.display='block';return}const s=document.createElement('script');s.src=urls[i++];s.onload=()=>{if(window.maplibregl)window.initVlotritMap();else next()};s.onerror=next;document.head.appendChild(s)}next()})();</script></body></html>"""

@Composable
private fun MapWebView(script: String, modifier: Modifier) {
    val context = LocalContext.current
    val html = remember(context, script) { loadMapHtml(context, script) }
    AndroidView(modifier = modifier, factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            val webViewAgent = android.webkit.WebSettings.getDefaultUserAgent(ctx)
            settings.userAgentString = "$webViewAgent VLOTRIT/1.27 (+https://github.com/Redglowmk3/vlotrit)"
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.rgb(238, 243, 247))
        }
    }, update = { web ->
        if (web.tag != script) {
            web.tag = script
            web.loadDataWithBaseURL("https://tiles.openfreemap.org/", html, "text/html", "UTF-8", null)
        }
    })
}

private data class PlannedRoute(val km: Double, val origin: String, val destination: String, val geometry: String, val roundTrip: Boolean = false, val stops: List<String> = emptyList())

@Composable
private fun RoutePlanner(context: Context, prefs: android.content.SharedPreferences, vehicle: VehicleProfile) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var viaPoints by remember { mutableStateOf(listOf<String>()) }
    var roundTrip by remember { mutableStateOf(false) }
    var electric by remember(vehicle.id) { mutableStateOf(vehicle.electric) }
    var fuelConsumption by remember(vehicle.id) { mutableStateOf((if (!vehicle.electric) vehicle.consumption else prefs.getFloat(KEY_CONSUMPTION, 11f)).formatInput()) }
    var fuelPrice by remember(vehicle.id) { mutableStateOf((if (!vehicle.electric) vehicle.price else prefs.getFloat(KEY_PRICE, 2.092f)).formatPrice()) }
    var electricConsumption by remember(vehicle.id) { mutableStateOf((if (vehicle.electric) vehicle.consumption else prefs.getFloat(KEY_ELECTRIC_CONSUMPTION, 18f)).formatInput()) }
    var electricPrice by remember(vehicle.id) { mutableStateOf((if (vehicle.electric) vehicle.price else prefs.getFloat(KEY_ELECTRIC_PRICE, 0.30f)).formatPrice()) }
    var includeDepreciation by remember(vehicle.id) { mutableStateOf(vehicle.depreciationEnabled) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PlannedRoute?>(null) }
    val scope = rememberCoroutineScope()
    val consumption = (if (electric) electricConsumption else fuelConsumption).toFloatOrNull() ?: if (electric) 18f else 11f
    val price = (if (electric) electricPrice else fuelPrice).toFloatOrNull() ?: if (electric) 0.30f else 2.092f
    val perKm = consumption * price / 100f

    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Route plannen", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(origin, { origin = it; result = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Beginlocatie") }, singleLine = true, shape = RoundedCornerShape(13.dp))
            viaPoints.forEachIndexed { index, point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(point, { value -> viaPoints = viaPoints.toMutableList().also { it[index] = value }; result = null },
                        modifier = Modifier.weight(1f), label = { Text("Tussenstop ${index + 1}") }, singleLine = true, shape = RoundedCornerShape(13.dp))
                    TextButton(onClick = { viaPoints = viaPoints.filterIndexed { i, _ -> i != index }; result = null }) { Text("Weg") }
                }
            }
            TextButton(onClick = { viaPoints = viaPoints + ""; result = null }, enabled = viaPoints.size < 6) { Text("+ Tussenstop toevoegen", color = Forest) }
            OutlinedTextField(destination, { destination = it; result = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Eindlocatie") }, singleLine = true, shape = RoundedCornerShape(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = roundTrip, onCheckedChange = { roundTrip = it; result = null })
                Text("Retour (terug naar beginlocatie)", color = Ink, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = !electric, onClick = { electric = false; result = null }, label = { Text(if (vehicle.electric) "E10" else vehicle.fuelType) })
                FilterChip(selected = electric, onClick = { electric = true; result = null }, label = { Text("EV") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericField(if (electric) "Stroomverbruik" else "Benzineverbruik", if (electric) electricConsumption else fuelConsumption,
                    { if (electric) electricConsumption = it else fuelConsumption = it; result = null },
                    if (electric) "kWh/100 km" else "L/100 km", Modifier.weight(1f), enabled = true)
                NumericField("Prijs", if (electric) electricPrice else fuelPrice,
                    { if (electric) electricPrice = it else fuelPrice = it; result = null },
                    if (electric) "€/kWh" else "€/L", Modifier.weight(1f), enabled = true)
            }
            Text("Deze plannerwaarden zijn apart van je ritinstellingen.", color = Muted, fontSize = 11.sp)
            Text("Rekent met ${consumption.formatNl()} ${if (electric) "kWh/100 km" else "L/100 km"} en ${price.euro()} per ${if (electric) "kWh" else "L"}.", color = Muted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeDepreciation, onCheckedChange = { includeDepreciation = it })
                Text("Afschrijving meenemen (${vehicle.depreciationPerKm.euro()}/km)", color = Ink, fontSize = 13.sp)
            }
            Button(onClick = {
                loading = true; error = ""; result = null
                scope.launch {
                    try { result = withContext(Dispatchers.IO) { fetchPlannedRoute(origin, viaPoints.filter { it.isNotBlank() }, destination, roundTrip) } }
                    catch (e: Exception) { error = e.message ?: "Route kon niet worden berekend." }
                    finally { loading = false }
                }
            }, enabled = !loading && origin.isNotBlank() && destination.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (loading) "Route ophalen…" else "Bereken kilometers en kosten")
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFF9C422D), fontSize = 12.sp)
            result?.let { planned ->
                val energy = planned.km * perKm
                val depreciation = if (includeDepreciation) planned.km * vehicle.depreciationPerKm else 0.0
                HorizontalDivider(color = Color(0xFFE5EBE5))
                Text("${String.format(Locale("nl", "NL"), "%.1f", planned.km)} km", color = Forest, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("${planned.origin}  →  ${planned.destination}${if (planned.roundTrip) "  →  terug" else ""}", color = Muted, fontSize = 12.sp)
                if (planned.stops.isNotEmpty()) Text("Tussenstops: ${planned.stops.joinToString(" · ")}", color = Muted, fontSize = 11.sp)
                HistoryDetailLine(if (electric) "Geschatte stroomkosten" else "Geschatte brandstofkosten", energy.toFloat().euro())
                if (includeDepreciation) HistoryDetailLine("Afschrijving", depreciation.toFloat().euro())
                HistoryDetailLine("Totaal", (energy + depreciation).toFloat().euro())
                Text("Voorgestelde route", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                RouteMap(planned.geometry, Modifier.fillMaxWidth().height(190.dp))
                Text("Kaartgegevens © OpenStreetMap contributors", color = Muted, fontSize = 10.sp)
                Text("Indicatie voor eigen gebruik. Verkeersdrukte en gekozen route kunnen de afstand en kosten veranderen.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

private fun fetchPlannedRoute(origin: String, via: List<String>, destination: String, roundTrip: Boolean): PlannedRoute {
    val searches = listOf(origin) + via + destination
    val locations = mutableListOf<Triple<Double, Double, String>>()
    searches.forEachIndexed { index, query ->
        if (index > 0) Thread.sleep(1100)
        val previous = locations.lastOrNull()?.let { it.second to it.first }
        locations += geocodePlace(query, previous)
    }
    if (roundTrip) locations += locations.first()
    val coordinates = locations.joinToString(";") { "${it.first},${it.second}" }
    val url = URL("https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=geojson")
    val response = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000; readTimeout = 12000; setRequestProperty("User-Agent", "Vlotrit Android personal route planner")
    }
    return try {
        val json = JSONObject(response.inputStream.bufferedReader().use { it.readText() })
        val route = json.optJSONArray("routes")?.optJSONObject(0) ?: error("Geen autoroute gevonden tussen deze locaties.")
        val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: error("Routekaart kon niet worden geladen.")
        val geometry = (0 until coords.length()).mapNotNull { i ->
            val pair = coords.optJSONArray(i) ?: return@mapNotNull null
            val lon = pair.optDouble(0, Double.NaN); val lat = pair.optDouble(1, Double.NaN)
            if (lat.isNaN() || lon.isNaN()) null else "$lat,$lon"
        }.joinToString(";")
        val destinationIndex = if (roundTrip) locations.lastIndex - 1 else locations.lastIndex
        PlannedRoute(route.getDouble("distance") / 1000.0, locations.first().third,
            locations[destinationIndex].third, geometry, roundTrip, locations.subList(1, destinationIndex).map { it.third })
    } finally { response.disconnect() }
}

private fun geocodePlace(query: String, near: Pair<Double, Double>? = null): Triple<Double, Double, String> {
    val explicitCountry = Regex("(?i)(Nederland|Netherlands|Duitsland|Germany|Belgi[eë]|Belgium|Frankrijk|France|Luxemburg|Luxembourg|\\bDE\\b|\\bBE\\b|\\bFR\\b|\\bLU\\b)").containsMatchIn(query)
    val localQuery = if (explicitCountry) query else "$query, Nederland"
    val encoded = java.net.URLEncoder.encode(localQuery, "UTF-8")
    val countryFilter = if (explicitCountry && !Regex("(?i)(Nederland|Netherlands)").containsMatchIn(query)) "" else "&countrycodes=nl"
    val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=20&addressdetails=1&accept-language=nl$countryFilter&q=$encoded")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000; readTimeout = 12000
        setRequestProperty("User-Agent", "VLOTRIT Android/1.27 (https://github.com/Redglowmk3/vlotrit)")
        setRequestProperty("Accept", "application/json")
    }
    return try {
        val results = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        val candidates = (0 until results.length()).mapNotNull { index ->
            val candidate = results.optJSONObject(index) ?: return@mapNotNull null
            val lon = candidate.optDouble("lon", Double.NaN); val lat = candidate.optDouble("lat", Double.NaN)
            if (!lon.isFinite() || !lat.isFinite()) null else Triple(lon, lat, candidate.optString("display_name", query))
        }
        val chosen = if (near == null) candidates.firstOrNull() else candidates.minByOrNull { candidate ->
            val result = FloatArray(1)
            Location.distanceBetween(near.first, near.second, candidate.second, candidate.first, result)
            result[0]
        }
        chosen ?: error("Geen Nederlandse plaats gevonden voor ‘$query’. Voeg eventueel een provincie of land toe.")
    } finally { connection.disconnect() }
}

@Composable
private fun TripHistory(context: Context, prefs: android.content.SharedPreferences, trips: List<TripRecord>, selectedId: Long?, activeProfile: VehicleProfile,
    onSelect: (Long?) -> Unit, onTripsChanged: (List<TripRecord>) -> Unit, onApplyAverage: (Float) -> Unit) {
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf("Alle") }
    var categoryFilter by remember { mutableStateOf("Alle") }
    var monthFilter by remember { mutableStateOf("") }
    var yearFilter by remember { mutableStateOf(SimpleDateFormat("yyyy", Locale.US).format(Date())) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredTrips = trips.filter { trip ->
        val typeOk = typeFilter == "Alle" || (typeFilter == "EV") == trip.electric
        val date = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(trip.stoppedAt))
        val monthOk = monthFilter.isBlank() || date == monthFilter.trim()
        val yearOk = yearFilter.isBlank() || date.startsWith(yearFilter.trim())
        val textOk = searchQuery.isBlank() || trip.name.contains(searchQuery.trim(), true) || trip.vehicleName.contains(searchQuery.trim(), true)
        typeOk && monthOk && yearOk && textOk && (categoryFilter == "Alle" || trip.category == categoryFilter)
    }
    val totalKm = filteredTrips.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCost = filteredTrips.sumOf { it.cost.toDouble() }.toFloat()
    val measuredTrips = trips.filter { it.vehicleId == activeProfile.id && fuelTypeLabel(it.fuelType) == fuelTypeLabel(activeProfile.fuelType) && it.actualEnergy != null && it.distanceKm > 0f }
    val measuredAverage = measuredTrips.takeIf { it.size >= 3 }?.let { group ->
        group.sumOf { it.actualEnergy!!.toDouble() } * 100.0 / group.sumOf { it.distanceKm.toDouble() }
    }?.toFloat()
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ritgeschiedenis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("${trips.size} ritten opgeslagen", color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = { selectionMode = !selectionMode; selectedIds = emptySet() }) {
                    Text(if (selectionMode) "Annuleren" else "Selecteer", color = Forest)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Alle", "Brandstof", "EV").forEach { option ->
                    FilterChip(selected = typeFilter == option, onClick = { typeFilter = option }, label = { Text(option) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Alle", "Privé", "Zakelijk").forEach { option ->
                    FilterChip(selected = categoryFilter == option, onClick = { categoryFilter = option }, label = { Text(option) })
                }
            }
            OutlinedTextField(searchQuery, { searchQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Zoek op label of notitie") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(monthFilter, { monthFilter = it }, modifier = Modifier.weight(1f), label = { Text("Maand (YYYY-MM)") }, singleLine = true)
                OutlinedTextField(yearFilter, { yearFilter = it }, modifier = Modifier.weight(0.7f), label = { Text("Jaar") }, singleLine = true)
            }
            Text("${filteredTrips.size} ritten · ${totalKm.formatNl()} km · totaal ${totalCost.euro()}", color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (filteredTrips.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { shareCsvOverview(context, filteredTrips) }, modifier = Modifier.weight(1f)) { Text("Deel CSV") }
                OutlinedButton(onClick = { shareTripsPdf(context, filteredTrips, "VLOTRIT overzicht") }, modifier = Modifier.weight(1f)) { Text("Deel PDF") }
            }
            measuredAverage?.let { average ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BlueTint), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Gemeten gemiddelde over ${measuredTrips.size} ritten: ${average.formatNl()} ${if (activeProfile.electric) "kWh/100 km" else "L/100 km"}.", color = Ink, fontSize = 12.sp)
                        Text("Dit gebruikt alleen ritten waarvoor je het werkelijk verbruik hebt ingevuld.", color = Muted, fontSize = 11.sp)
                        TextButton(onClick = { onApplyAverage(average) }) { Text("Gebruik als standaard voor ${activeProfile.name}", color = Forest) }
                    }
                }
            } ?: Text("Vul bij ritten het werkelijk verbruikte aantal liters of kWh in. Na 3 gemeten ritten kan VLOTRIT je gemiddelde voorstellen.", color = Muted, fontSize = 11.sp)
            if (selectionMode) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${selectedIds.size} geselecteerd", color = Muted, fontSize = 12.sp)
                    Button(onClick = { confirmDelete = true }, enabled = selectedIds.isNotEmpty()) { Text("Verwijder selectie") }
                }
            }
            if (trips.isEmpty()) Text("Je afgeronde ritten verschijnen hier.", color = Muted, fontSize = 13.sp)
            else if (filteredTrips.isEmpty()) Text("Geen ritten gevonden voor deze filters.", color = Muted, fontSize = 13.sp)
            filteredTrips.forEach { trip ->
                Column {
                    Row(Modifier.fillMaxWidth().clickable { onSelect(trip.id) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${trip.category} · ${fuelTypeLabel(trip.fuelType.ifBlank { if (trip.electric) "EV" else "E10" })} · ${trip.name.ifBlank { trip.vehicleName.ifBlank { "Rit" } }}", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${Date(trip.stoppedAt).clock()}  ·  ${trip.distanceKm.formatNl()} km", color = Muted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(trip.cost.euro(), color = Forest, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${trip.energyAmount.formatNl()} ${trip.energyUnit}", color = Muted, fontSize = 11.sp)
                        }
                        if (selectionMode) Checkbox(checked = trip.id in selectedIds, onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + trip.id else selectedIds - trip.id
                        })
                    }
                    if (selectedId == trip.id) {
                        if (trip.vehicleName.isNotBlank()) HistoryDetailLine("Voertuig", "${trip.vehicleName} · ${trip.fuelType}")
                        var newCategory by remember(trip.id, trip.category) { mutableStateOf(trip.category) }
                        var passengers by remember(trip.id, trip.passengers) { mutableStateOf(trip.passengers.joinToString(", ")) }
                        HorizontalDivider(color = Color(0xFFE5EBE5))
                        Column(Modifier.padding(top = 10.dp, bottom = 7.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf("Privé", "Zakelijk").forEach { category -> FilterChip(selected = newCategory == category, onClick = { newCategory = category }, label = { Text(category) }) }
                                TextButton(onClick = {
                                    val updated = trips.map { if (it.id == trip.id) it.copy(category = newCategory) else it }
                                    writeHistory(prefs, updated); onTripsChanged(updated)
                                }) { Text("Bewaar categorie") }
                            }
                            OutlinedTextField(passengers, { passengers = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Samenrit · passagiers, gescheiden met komma") }, supportingText = { Text("Kosten worden gelijk verdeeld, inclusief jouw aandeel als bestuurder.") })
                            Button(onClick = {
                                val names = passengers.split(",", ";", "\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                val updated = trips.map { if (it.id == trip.id) it.copy(passengers = names) else it }
                                writeHistory(prefs, updated); onTripsChanged(updated)
                            }, enabled = passengers.isNotBlank()) { Text("Bewaar samenrit") }
                            if (trip.passengers.isNotEmpty()) {
                                val share = trip.cost / (trip.passengers.size + 1)
                                HistoryDetailLine("Jouw aandeel (${trip.passengers.size + 1} personen)", share.euro())
                                trip.passengers.forEach { HistoryDetailLine("$it betaalt", share.euro()) }
                            }
                            HistoryDetailLine("Beginstand → eindstand", "${trip.startKm.formatNl()} → ${trip.endKm.formatNl()} km")
                            if (trip.startLocation.isNotBlank()) HistoryDetailLine("Vertrek", trip.startLocation)
                            if (trip.endLocation.isNotBlank()) HistoryDetailLine("Aankomst", trip.endLocation)
                            if (trip.depreciationEnabled) HistoryDetailLine("Afschrijving (${trip.depreciationPerKm.euro()}/km)", trip.depreciationCost.euro())
                            HistoryDetailLine(if (trip.electric) "Gemiddeld stroomverbruik" else "Gemiddeld verbruik", "${trip.consumption.formatNl()} ${trip.consumptionUnit}")
                            HistoryDetailLine(if (trip.electric) "Stroomprijs" else "${fuelTypeLabel(trip.fuelType.ifBlank { "E10" })} prijs", "${trip.price.euro()} ${trip.priceUnit}")
                            if (!trip.electric && trip.fuelType != "E5/98") HistoryDetailLine("ANWB-prijs opgehaald", trip.priceUpdatedAt.dateTimeOrUnknown())
                            ActualEnergyEditor(trip) { measured ->
                                val updated = trips.map { if (it.id == trip.id) it.copy(actualEnergy = measured) else it }
                                writeHistory(prefs, updated)
                                onTripsChanged(updated)
                            }
                            if (trip.route.isNotBlank()) {
                                RouteMap(trip.route, Modifier.fillMaxWidth().height(170.dp))
                                Text("© OpenStreetMap contributors", color = Muted, fontSize = 10.sp)
                            } else Text("Geen GPS-route bij deze rit opgeslagen.", color = Muted, fontSize = 11.sp)
                            OutlinedButton(onClick = { shareTrip(context, trip) }, modifier = Modifier.fillMaxWidth()) { Text("Deel bon + GPS-routebestand") }
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedButton(onClick = { shareTripPdf(context, trip) }, modifier = Modifier.weight(1f)) { Text("Bon als PDF") }
                                OutlinedButton(onClick = { shareTripImage(context, trip) }, modifier = Modifier.weight(1f)) { Text("Bon als afbeelding") }
                            }
                            TextButton(onClick = { selectedIds = setOf(trip.id); confirmDelete = true }) { Text("Verwijder deze rit", color = Color(0xFF9C422D)) }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE5EBE5))
                }
            }
        }
    }
    if (confirmDelete) {
        val removing = if (selectedIds.isNotEmpty()) selectedIds else setOfNotNull(selectedId)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (removing.size == 1) "Rit verwijderen?" else "Ritten verwijderen?") },
            text = { Text(if (removing.size == 1) "Deze rit wordt uit je geschiedenis verwijderd." else "${removing.size} ritten worden uit je geschiedenis verwijderd.") },
            confirmButton = { TextButton(onClick = {
                val remaining = trips.filterNot { it.id in removing }
                writeHistory(prefs, remaining)
                onTripsChanged(remaining)
                if (prefs.getLong(KEY_STOPPED_AT, 0L) in removing) {
                    prefs.edit().putLong(KEY_STOPPED_AT, 0L).putString(KEY_ROUTE, "").apply()
                }
                if (selectedId in removing) onSelect(null)
                selectedIds = emptySet(); selectionMode = false; confirmDelete = false
            }) { Text("Verwijderen", color = Color(0xFF9C422D)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } }
        )
    }
}

@Composable
private fun ActualEnergyEditor(trip: TripRecord, onSave: (Float?) -> Unit) {
    var amount by remember(trip.id, trip.actualEnergy) { mutableStateOf(trip.actualEnergy?.formatInput().orEmpty()) }
    Text("Werkelijk verbruikt over deze rit", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    NumericField("Hoeveelheid totaal", amount, { amount = it }, trip.energyUnit, Modifier.fillMaxWidth(), true)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = {
            amount.replace(',', '.').toFloatOrNull()?.takeIf { it >= 0f }?.let { onSave(it) }
        }, enabled = amount.replace(',', '.').toFloatOrNull()?.let { it >= 0f } == true) { Text("Bewaar meting") }
        if (trip.actualEnergy != null) TextButton(onClick = { amount = ""; onSave(null) }) { Text("Wis meting") }
    }
    trip.actualEnergy?.let {
        Text("Gemeten: ${(it * 100f / trip.distanceKm.coerceAtLeast(0.01f)).formatNl()} ${trip.consumptionUnit}", color = Forest, fontSize = 11.sp)
    }
}

private fun shareTrip(context: Context, trip: TripRecord, body: String? = null) {
    val receiptLines = mutableListOf(
        "VLOTRIT Bon${if (trip.name.isNotBlank()) " · ${trip.name}" else ""}",
        "Categorie: ${trip.category}",
        if (trip.vehicleName.isNotBlank()) "Voertuig: ${trip.vehicleName} (${fuelTypeLabel(trip.fuelType.ifBlank { if (trip.electric) "EV" else "E10" })})" else "Voertuig: ${fuelTypeLabel(trip.fuelType.ifBlank { if (trip.electric) "EV" else "E10" })}",
        "${trip.distanceKm.formatNl()} km gereden",
        "Gemiddeld verbruik: ${trip.estimatedEnergyAmount.formatNl()} ${trip.energyUnit} (${trip.consumption.formatNl()} ${trip.consumptionUnit})",
        "${if (trip.electric) "Stroomkosten" else "Brandstofkosten"}: ${trip.energyCost.euro()}",
        "${if (trip.electric) "Stroomprijs" else "${fuelTypeLabel(trip.fuelType.ifBlank { "E10" })} prijs"}: ${trip.price.euro()} per ${trip.energyUnit}"
    )
    trip.actualEnergy?.let { actual -> receiptLines.add("Gemeten verbruik: ${actual.formatNl()} ${trip.energyUnit} totaal (${(actual * 100f / trip.distanceKm.coerceAtLeast(0.01f)).formatNl()} ${trip.consumptionUnit})") }
    if (!trip.electric && trip.fuelType != "E5/98") receiptLines.add("ANWB-prijs opgehaald: ${trip.priceUpdatedAt.dateTimeOrUnknown()}")
    receiptLines.add("Kilometerstand: ${trip.startKm.formatNl()} → ${trip.endKm.formatNl()} km")
    receiptLines.add("${Date(trip.startedAt).clock()} – ${Date(trip.stoppedAt).clock()}")
    if (trip.startLocation.isNotBlank()) receiptLines.add("Vertrek: ${trip.startLocation}")
    if (trip.endLocation.isNotBlank()) receiptLines.add("Aankomst: ${trip.endLocation}")
    if (trip.depreciationEnabled) receiptLines.add("Afschrijving (${trip.depreciationPerKm.euro()}/km): ${trip.depreciationCost.euro()}")
    if (trip.passengers.isNotEmpty()) {
        val share = trip.cost / (trip.passengers.size + 1)
        receiptLines.add("Samenrit: ${trip.passengers.joinToString(", ")}")
        receiptLines.add("Kosten per persoon incl. bestuurder (${trip.passengers.size + 1}): ${share.euro()}")
    }
    receiptLines.add("------------------------------")
    receiptLines.add("TOTALE KOSTEN: ${trip.cost.euro()}")
    val receipt = body ?: receiptLines.joinToString("\n")
    val subject = "Factuur - ${trip.name.ifBlank { trip.vehicleName.ifBlank { "Rit" } }}"
    if (trip.route.isBlank()) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, receipt)
        context.startActivity(Intent.createChooser(send, "Deel je ritbon")); return
    }
    try {
        val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(shareDir, "Vlotrit-route-${trip.id}.gpx")
        val points = trip.route.split(';').mapNotNull { p ->
            val xy = p.split(','); if (xy.size != 2) null else xy[0].toDoubleOrNull()?.let { lat -> xy[1].toDoubleOrNull()?.let { lon -> "<trkpt lat=\"$lat\" lon=\"$lon\"/>" } }
        }.joinToString("")
        file.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"Vlotrit\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Vlotrit ${trip.name}</name><trkseg>$points</trkseg></trk></gpx>")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType("application/gpx+xml").putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, "$receipt\n\nDe exacte GPS-route staat in het bijgevoegde GPX-bestand.")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Deel je bon en route"))
    } catch (_: Exception) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, "$receipt\nGPS-punten: ${trip.route.split(';').size}")
        context.startActivity(Intent.createChooser(send, "Deel je ritbon"))
    }
}

private fun shareCsvOverview(context: Context, trips: List<TripRecord>) {
    fun cell(value: String) = "\"${value.replace("\"", "\"\"")}\""
    val lines = mutableListOf("Datum;Label;Categorie;Voertuig;Type;Km;Gemeten hoeveelheid;Verbruik per 100 km;Prijs;Afschrijving;Totaal")
    trips.forEach { t ->
        val measuredRate = t.actualEnergy?.let { it * 100f / t.distanceKm.coerceAtLeast(0.01f) }?.formatNl().orEmpty()
        lines.add(listOf(Date(t.stoppedAt).dateTime(), t.name, t.category, t.vehicleName, fuelTypeLabel(t.fuelType.ifBlank { if (t.electric) "EV" else "E10" }),
            t.distanceKm.formatNl(), t.actualEnergy?.formatNl().orEmpty(), measuredRate, t.price.euro(), t.depreciationCost.euro(), t.cost.euro()).joinToString(";") { cell(it) })
    }
    shareTextFile(context, "Vlotrit-ritten.csv", "text/csv", lines.joinToString("\n"), "VLOTRIT rittenoverzicht")
}

private fun shareTripsPdf(context: Context, trips: List<TripRecord>, heading: String) {
    val lines = mutableListOf("$heading · ${trips.size} ritten", "Kilometers: ${trips.sumOf { it.distanceKm.toDouble() }.toFloat().formatNl()} km",
        "Totale kosten: ${trips.sumOf { it.cost.toDouble() }.toFloat().euro()}", "")
    trips.forEach { t -> lines.add("${Date(t.stoppedAt).dateTime()} · ${t.vehicleName.ifBlank { t.fuelType }} · ${t.name.ifBlank { "Rit" }} · ${t.distanceKm.formatNl()} km · ${t.cost.euro()}") }
    sharePdf(context, "Vlotrit-ritten.pdf", heading, lines)
}

private fun shareTripPdf(context: Context, trip: TripRecord) {
    val lines = tripReceiptText(trip).lines()
    sharePdf(context, "Vlotrit-bon-${trip.id}.pdf", "VLOTRIT Bon", lines)
}

private fun shareTripImage(context: Context, trip: TripRecord) {
    val lines = tripReceiptText(trip).lines()
    val rows = lines.flatMap { it.chunked(42).ifEmpty { listOf("") } }
    val bitmap = android.graphics.Bitmap.createBitmap(720, maxOf(520, 100 + rows.size * 38), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(24, 55, 45))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 24f }
    var y = 58f
    rows.forEachIndexed { index, line ->
        if (index == 0) paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        canvas.drawText(line, 36f, y, paint)
        y += if (index == 0) 48f else 36f
        if (index == 0) { paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL); paint.textSize = 21f }
    }
    val file = File(context.cacheDir, "shared/Vlotrit-bon-${trip.id}.png").apply { parentFile?.mkdirs() }
    file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    shareUri(context, file, "image/png", "VLOTRIT Bon")
}

private fun tripReceiptText(trip: TripRecord): String = buildList {
    add("VLOTRIT Bon${if (trip.name.isNotBlank()) " · ${trip.name}" else ""}")
    add("Categorie: ${trip.category}")
    if (trip.vehicleName.isNotBlank()) add("Voertuig: ${trip.vehicleName} (${fuelTypeLabel(trip.fuelType)})")
    add("${trip.distanceKm.formatNl()} km gereden")
    add("Gemiddeld verbruik: ${trip.estimatedEnergyAmount.formatNl()} ${trip.energyUnit} (${trip.consumption.formatNl()} ${trip.consumptionUnit})")
    trip.actualEnergy?.let { add("Gemeten verbruik: ${it.formatNl()} ${trip.energyUnit} totaal (${(it * 100f / trip.distanceKm.coerceAtLeast(0.01f)).formatNl()} ${trip.consumptionUnit})") }
    add("${if (trip.electric) "Stroomkosten" else "Brandstofkosten"}: ${trip.energyCost.euro()}")
    add("${if (trip.electric) "Stroomprijs" else "${fuelTypeLabel(trip.fuelType.ifBlank { "E10" })} prijs"}: ${trip.price.euro()} per ${trip.energyUnit}")
    if (!trip.electric && trip.fuelType != "E5/98") add("ANWB-prijs opgehaald: ${trip.priceUpdatedAt.dateTimeOrUnknown()}")
    add("Kilometerstand: ${trip.startKm.formatNl()} → ${trip.endKm.formatNl()} km")
    add("${Date(trip.startedAt).dateTime()} – ${Date(trip.stoppedAt).dateTime()}")
    if (trip.startLocation.isNotBlank()) add("Vertrek: ${trip.startLocation}")
    if (trip.endLocation.isNotBlank()) add("Aankomst: ${trip.endLocation}")
    if (trip.depreciationEnabled) add("Afschrijving (${trip.depreciationPerKm.euro()}/km): ${trip.depreciationCost.euro()}")
    if (trip.passengers.isNotEmpty()) {
        val share = trip.cost / (trip.passengers.size + 1)
        add("Samenrit: ${trip.passengers.joinToString(", ")}")
        add("Kosten per persoon incl. bestuurder (${trip.passengers.size + 1}): ${share.euro()}")
    }
    add("------------------------------")
    add("TOTALE KOSTEN: ${trip.cost.euro()}")
}.joinToString("\n")

private fun sharePdf(context: Context, filename: String, title: String, lines: List<String>) {
    val document = android.graphics.pdf.PdfDocument()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(23, 43, 37); textSize = 12f
    }
    var pageNumber = 0
    var page: android.graphics.pdf.PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null
    var y = 48f
    fun newPage() {
        page?.let { document.finishPage(it) }
        pageNumber++
        page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        canvas = page!!.canvas
        canvas!!.drawColor(android.graphics.Color.WHITE)
        y = 48f
    }
    newPage()
    lines.flatMap { it.chunked(82).ifEmpty { listOf("") } }.forEachIndexed { index, line ->
        if (y > 790f) newPage()
        if (index == 0) { paint.textSize = 18f; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD) }
        else { paint.textSize = 12f; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL) }
        canvas!!.drawText(line, 40f, y, paint)
        y += if (index == 0) 34f else 21f
    }
    page?.let { document.finishPage(it) }
    val file = File(context.cacheDir, "shared/$filename").apply { parentFile?.mkdirs() }
    file.outputStream().use(document::writeTo)
    document.close()
    shareUri(context, file, "application/pdf", title)
}

private fun shareTextFile(context: Context, filename: String, mime: String, contents: String, subject: String) {
    val file = File(context.cacheDir, "shared/$filename").apply { parentFile?.mkdirs() }
    file.writeText(contents)
    shareUri(context, file, mime, subject)
}

private fun shareUri(context: Context, file: File, mime: String, subject: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Deel via"))
}

@Composable
private fun RouteMap(route: String, modifier: Modifier = Modifier) {
    val points = remember(route) {
        route.split(';').mapNotNull { point ->
            val pair = point.split(',')
            if (pair.size != 2) null else {
                val lat = pair[0].toDoubleOrNull()
                val lon = pair[1].toDoubleOrNull()
                if (lat == null || lon == null) null else lat to lon
            }
        }
    }
    val routeJson = remember(points) { JSONArray().apply { points.forEach { put(JSONArray().put(it.first).put(it.second)) } }.toString() }
    val script = remember(routeJson) {
        """window.initVlotritMap=function(){try{
           const pts=$routeJson,map=new maplibregl.Map({container:'map',style:'https://tiles.openfreemap.org/styles/liberty',center:[5.2913,52.1326],zoom:7,attributionControl:true});
           map.addControl(new maplibregl.NavigationControl({showCompass:false}),'top-right');map.on('error',()=>document.getElementById('map-error').style.display='block');
           map.on('load',()=>{document.getElementById('map-error').style.display='none';if(pts.length){const coords=pts.map(p=>[p[1],p[0]]);map.addSource('route',{type:'geojson',data:{type:'Feature',properties:{},geometry:{type:'LineString',coordinates:coords}}});map.addLayer({id:'route-casing',type:'line',source:'route',layout:{'line-cap':'round','line-join':'round'},paint:{'line-color':'#ffffff','line-width':9}});map.addLayer({id:'route-line',type:'line',source:'route',layout:{'line-cap':'round','line-join':'round'},paint:{'line-color':'#176b4b','line-width':5}});new maplibregl.Marker({color:'#176b4b'}).setLngLat(coords[0]).setPopup(new maplibregl.Popup().setText('Start')).addTo(map);if(coords.length>1)new maplibregl.Marker({color:'#e4774d'}).setLngLat(coords[coords.length-1]).setPopup(new maplibregl.Popup().setText('Einde')).addTo(map);if(coords.length>1){const bounds=coords.reduce((b,p)=>b.extend(p),new maplibregl.LngLatBounds(coords[0],coords[0]));map.fitBounds(bounds,{padding:30,maxZoom:15})}}});setTimeout(()=>map.resize(),350);setTimeout(()=>{if(!map.isStyleLoaded())document.getElementById('map-error').style.display='block'},15000);
           }catch(e){document.getElementById('map-error').style.display='block'}};"""
    }
    MapWebView(script, modifier)
}

private fun routeEndpoints(route: String): Pair<Pair<Double, Double>?, Pair<Double, Double>?> {
    val points = route.split(';').mapNotNull { point ->
        val values = point.split(',')
        if (values.size != 2) null else {
            val lat = values[0].toDoubleOrNull()
            val lon = values[1].toDoubleOrNull()
            if (lat == null || lon == null) null else lat to lon
        }
    }
    return points.firstOrNull() to points.lastOrNull()
}

private fun coordinateLabel(point: Pair<Double, Double>): String =
    String.format(Locale.US, "GPS %.5f, %.5f", point.first, point.second)

@Suppress("DEPRECATION")
private fun reverseGeocode(context: Context, point: Pair<Double, Double>): String = try {
    Geocoder(context, Locale.getDefault()).getFromLocation(point.first, point.second, 1)
        ?.firstOrNull()?.getAddressLine(0).orEmpty()
} catch (_: Exception) { "" }

private fun fetchE10Price(): Pair<Float, String> {
    val connection = (URL("https://www.anwb.nl/auto/brandstof/benzineprijs").openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000; setRequestProperty("User-Agent", "Vlotrit Android app"); requestMethod = "GET" }
    return try {
        val html = connection.inputStream.bufferedReader().use { it.readText() }
        val row = Regex("(?is)Euro\\s*95\\s*\\(E10\\)(.{0,300})").find(html)?.groupValues?.get(1) ?: error("E10 prijs niet gevonden")
        val amount = Regex("([0-9],[0-9]{2,3})").find(row)?.groupValues?.get(1)?.replace(',', '.')?.toFloat() ?: error("E10 prijs niet gevonden")
        amount to "ANWB"
    } finally { connection.disconnect() }
}

private fun Float.formatInput() = if (this == 0f) "0" else String.format(Locale.US, "%.1f", this).trimEnd('0').trimEnd('.')
private fun Float.formatPrice() = String.format(Locale.US, "%.3f", this)
private fun Float.formatNl() = String.format(Locale("nl", "NL"), "%.1f", this)
private fun Float.euro() = NumberFormat.getCurrencyInstance(Locale("nl", "NL")).format(this)
private fun Date.clock() = SimpleDateFormat("dd-MM HH:mm", Locale("nl", "NL")).format(this)
private fun Date.dateTime() = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL")).format(this)
private fun Long.dateTimeOrUnknown() = if (this > 0L) Date(this).dateTime() else "Niet beschikbaar"


