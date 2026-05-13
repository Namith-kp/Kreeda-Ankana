package com.kreedaankana.ui.booking

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kreedaankana.data.model.Booking
import com.kreedaankana.data.repository.BookingRepository
import com.kreedaankana.databinding.ActivityBookSlotBinding
import com.kreedaankana.utils.Extensions.SPORTS
import com.kreedaankana.utils.Extensions.TIME_SLOTS
import com.kreedaankana.utils.Extensions.getTodayDate
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class BookSlotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookSlotBinding
    private val repository = BookingRepository()
    private var selectedDate = getTodayDate()

    private data class WeatherInfo(
        val temp: Double,
        val tempMax: Double,
        val tempMin: Double,
        val rainChance: Int,
        val weatherCode: Int,
        val description: String,
        val isSimulated: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookSlotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        prefillTeamName()
        setupWeatherWatchers()
        setupBookButton()
        
        // Initial trigger
        updateWeather()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Book a Slot"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupDropdowns() {
        val sportsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SPORTS)
        binding.autoSport.setAdapter(sportsAdapter)

        val startAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TIME_SLOTS)
        binding.autoStartTime.setAdapter(startAdapter)

        val endSlots = TIME_SLOTS.drop(1) + listOf("22:00")
        val endAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, endSlots)
        binding.autoEndTime.setAdapter(endAdapter)

        binding.tvSelectedDate.text = selectedDate
    }

    private fun setupDatePicker() {
        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                binding.tvSelectedDate.text = selectedDate
                updateWeather()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun prefillTeamName() {
        val team = SessionManager.getTeam(this)
        if (team != null) {
            binding.etTeamName.setText(team.teamName)
            binding.autoSport.setText(team.sport, false)
        }
        val selectedSport = intent.getStringExtra("SELECTED_SPORT")
        if (!selectedSport.isNullOrEmpty()) {
            binding.autoSport.setText(selectedSport, false)
        }
    }

    private fun setupWeatherWatchers() {
        binding.etVenuePlace.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateWeather()
            }
        })

        binding.autoSport.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateWeather()
            }
        })
    }

    private fun updateWeather() {
        val venuePlace = binding.etVenuePlace.text.toString().trim()
        val sport = binding.autoSport.text.toString().trim()

        if (venuePlace.isEmpty()) {
            binding.cardWeather.gone()
            return
        }

        binding.cardWeather.visible()

        // Show elegant loading state first
        binding.tvWeatherTemp.text = "..."
        binding.tvWeatherDesc.text = "Fetching live forecast..."
        binding.tvWeatherRainChance.text = ""
        binding.tvWeatherRange.text = ""
        binding.tvWeatherBadge.text = "SYNCING"
        binding.tvWeatherBadge.setTextColor(Color.parseColor("#757575"))
        binding.tvWeatherBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEEEEE"))
        binding.tvWeatherAdvice.text = "Retrieving local meteorological predictions..."
        binding.ivWeatherIcon.setImageResource(com.kreedaankana.R.drawable.ic_weather_unknown)

        val coords = getCoordinatesForVenue(venuePlace)
        val targetDate = selectedDate

        lifecycleScope.launch {
            val weatherInfo = fetchWeatherData(coords.first, coords.second, targetDate)
            bindWeatherData(weatherInfo, sport)
        }
    }

    private fun getCoordinatesForVenue(venueName: String): Pair<Double, Double> {
        val lower = venueName.lowercase()
        return when {
            lower.contains("mumbai") || lower.contains("bombay") -> Pair(19.0760, 72.8777)
            lower.contains("delhi") || lower.contains("ncr") || lower.contains("noida") || lower.contains("gurgaon") -> Pair(28.7041, 77.1025)
            lower.contains("chennai") || lower.contains("madras") -> Pair(13.0827, 80.2707)
            lower.contains("kolkata") || lower.contains("calcutta") -> Pair(22.5726, 88.3639)
            lower.contains("hyderabad") -> Pair(17.3850, 78.4867)
            lower.contains("pune") -> Pair(18.5204, 73.8567)
            lower.contains("kochi") || lower.contains("cochin") -> Pair(9.9312, 76.2673)
            lower.contains("mysore") || lower.contains("mysuru") -> Pair(12.2958, 76.6394)
            lower.contains("bengaluru") || lower.contains("bangalore") || lower.contains("karnataka") -> Pair(12.9716, 77.5946)
            else -> Pair(12.9716, 77.5946) // Default to Bengaluru
        }
    }

    private suspend fun fetchWeatherData(lat: Double, lon: Double, dateStr: String): WeatherInfo {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

                if (conn.responseCode == 200) {
                    val stream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    stream.close()

                    val json = JSONObject(response.toString())
                    val daily = json.getJSONObject("daily")
                    val times = daily.getJSONArray("time")
                    var foundIndex = -1
                    for (i in 0 until times.length()) {
                        if (times.getString(i) == dateStr) {
                            foundIndex = i
                            break
                        }
                    }

                    if (foundIndex != -1) {
                        val code = daily.getJSONArray("weathercode").getInt(foundIndex)
                        val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(foundIndex)
                        val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(foundIndex)
                        val rain = daily.getJSONArray("precipitation_probability_max").getInt(foundIndex)
                        val avgTemp = (maxTemp + minTemp) / 2.0

                        val desc = getWeatherDescription(code)
                        return@withContext WeatherInfo(
                            temp = avgTemp,
                            tempMax = maxTemp,
                            tempMin = minTemp,
                            rainChance = rain,
                            weatherCode = code,
                            description = desc,
                            isSimulated = false
                        )
                    }
                }

                return@withContext simulateWeather(dateStr)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext simulateWeather(dateStr)
            }
        }
    }

    private fun simulateWeather(dateStr: String): WeatherInfo {
        val month = try {
            val parts = dateStr.split("-")
            if (parts.size >= 2) parts[1].toInt() else 5
        } catch (e: Exception) {
            5
        }

        val maxTemp: Double
        val minTemp: Double
        val rainChance: Int
        val code: Int

        when (month) {
            3, 4, 5 -> { // Summer
                maxTemp = 34.0 + (Math.random() * 4.0)
                minTemp = 23.0 + (Math.random() * 3.0)
                rainChance = (Math.random() * 15).toInt()
                code = if (rainChance > 10) 1 else 0
            }
            6, 7, 8, 9 -> { // Monsoon
                maxTemp = 27.0 + (Math.random() * 3.0)
                minTemp = 20.0 + (Math.random() * 2.0)
                rainChance = 60 + (Math.random() * 35).toInt()
                code = if (rainChance > 80) 63 else 61
            }
            10, 11 -> { // Autumn
                maxTemp = 29.0 + (Math.random() * 3.0)
                minTemp = 18.0 + (Math.random() * 3.0)
                rainChance = 10 + (Math.random() * 25).toInt()
                code = if (rainChance > 25) 2 else 1
            }
            else -> { // Winter
                maxTemp = 26.0 + (Math.random() * 4.0)
                minTemp = 14.0 + (Math.random() * 4.0)
                rainChance = (Math.random() * 10).toInt()
                code = 0
            }
        }

        val avgTemp = (maxTemp + minTemp) / 2.0
        val desc = getWeatherDescription(code) + " (Seasonal Forecast)"

        return WeatherInfo(
            temp = avgTemp,
            tempMax = maxTemp,
            tempMin = minTemp,
            rainChance = rainChance,
            weatherCode = code,
            description = desc,
            isSimulated = true
        )
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Light Drizzle"
            61, 63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            80, 81, 82 -> "Passing Showers"
            95, 96, 99 -> "Thunderstorm Warning"
            else -> "Partly Cloudy"
        }
    }

    private fun bindWeatherData(info: WeatherInfo, sport: String) {
        binding.tvWeatherTemp.text = "%.1f°C".format(info.temp)
        binding.tvWeatherDesc.text = info.description
        binding.tvWeatherRainChance.text = "Rain: ${info.rainChance}%"
        binding.tvWeatherRange.text = "%.1f°C - %.1f°C".format(info.tempMin, info.tempMax)

        val iconRes = when (info.weatherCode) {
            0, 1 -> com.kreedaankana.R.drawable.ic_weather_sunny
            2, 3, 45, 48 -> com.kreedaankana.R.drawable.ic_weather_cloudy
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> com.kreedaankana.R.drawable.ic_weather_rainy
            95, 96, 99 -> com.kreedaankana.R.drawable.ic_weather_stormy
            else -> com.kreedaankana.R.drawable.ic_weather_unknown
        }
        binding.ivWeatherIcon.setImageResource(iconRes)

        val lowerSport = sport.lowercase()
        val isOutdoor = lowerSport.contains("foot") || lowerSport.contains("soccer") ||
                        lowerSport.contains("crick") || lowerSport.contains("athlet") ||
                        lowerSport.contains("tenn") || lowerSport.contains("golf")

        val badgeText: String
        val badgeBgTint: Int
        val badgeTextColor: Int
        val adviceText: String

        when (info.weatherCode) {
            0, 1 -> {
                badgeText = "IDEAL"
                badgeBgTint = Color.parseColor("#E8F5E9") // Light Green
                badgeTextColor = Color.parseColor("#2E7D32") // Dark Green
                adviceText = if (isOutdoor) {
                    "⚽ Clear skies! Excellent playability for high-energy outdoor matches. Keep hydrated!"
                } else {
                    "🏸 Beautiful sunny weather. Indoor court humidity is optimal, play with full force!"
                }
            }
            2, 3, 45, 48 -> {
                badgeText = "GOOD"
                badgeBgTint = Color.parseColor("#E3F2FD") // Light Blue
                badgeTextColor = Color.parseColor("#1565C0") // Dark Blue
                adviceText = if (isOutdoor) {
                    "⛅ Overcast and cool. Perfect breeze for open-field outdoor tournaments!"
                } else {
                    "🏸 Cozy shaded day. Great visibility. Play indoor or outdoor with equal comfort!"
                }
            }
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> {
                badgeText = "CAUTION"
                badgeBgTint = Color.parseColor("#FFF3E0") // Orange 50
                badgeTextColor = Color.parseColor("#E65100") // Orange 900
                adviceText = if (isOutdoor) {
                    "⚠️ Damp field & wet turf. High slippage hazard. Consider heavy-grip studs or booking an indoor hall."
                } else {
                    "🌧️ Rainy outdoors. Excellent opportunity to book an indoor court instead and stay warm!"
                }
            }
            else -> {
                badgeText = "SEVERE"
                badgeBgTint = Color.parseColor("#FFEBEE") // Red 50
                badgeTextColor = Color.parseColor("#C62828") // Red 800
                adviceText = if (isOutdoor) {
                    "⛈️ Lightning & Thunderstorm hazard. STRONGLY UNSAFE to play outdoors. Postpone or shift indoors immediately!"
                } else {
                    "⛈️ Storm alert outside. Stay dry and play on fully covered indoor facilities only."
                }
            }
        }

        binding.tvWeatherBadge.text = badgeText
        binding.tvWeatherBadge.backgroundTintList = ColorStateList.valueOf(badgeBgTint)
        binding.tvWeatherBadge.setTextColor(badgeTextColor)
        binding.tvWeatherAdvice.text = adviceText
    }

    private fun setupBookButton() {
        binding.btnBook.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            val venuePlace = binding.etVenuePlace.text.toString().trim()
            val sport = binding.autoSport.text.toString().trim()
            val startTime = binding.autoStartTime.text.toString().trim()
            val endTime = binding.autoEndTime.text.toString().trim()

            if (teamName.isEmpty()) { binding.tilTeamName.error = "Required"; return@setOnClickListener }
            if (venuePlace.isEmpty()) { binding.tilVenuePlace.error = "Required"; return@setOnClickListener }
            if (sport.isEmpty()) { showToast("Select a sport"); return@setOnClickListener }
            if (startTime.isEmpty()) { showToast("Select start time"); return@setOnClickListener }
            if (endTime.isEmpty()) { showToast("Select end time"); return@setOnClickListener }
            if (startTime >= endTime) { showToast("End time must be after start time"); return@setOnClickListener }

            binding.tilTeamName.error = null
            binding.tilVenuePlace.error = null

            val booking = Booking(
                teamName = teamName,
                sport = sport,
                venuePlace = venuePlace,
                date = selectedDate,
                startTime = startTime,
                endTime = endTime,
                teamId = SessionManager.getTeam(this)?.id ?: ""
            )

            binding.progressBar.visible()
            binding.btnBook.isEnabled = false

            lifecycleScope.launch {
                val result = repository.bookSlot(booking)
                binding.progressBar.gone()
                binding.btnBook.isEnabled = true

                result.onSuccess {
                    showToast("✅ Slot Booked Successfully!")
                    finish()
                }.onFailure { error ->
                    showToast("⚠️ ${error.message}")
                }
            }
        }
    }
}
