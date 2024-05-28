package pt.ipleiria.estg.ciic.chatboternui.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings.Secure
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class Others {
    private var portugal: Locale? = Locale("pt")

    fun calculateRootMeanSquare(values: Array<Double>): Double {
        var square : Double = 0.0
        val root : Double
        // Calculate square.
        for (value in values) {
            square += value.pow(2.0)
        }
        // Calculate Mean.
        val mean : Double = square / values.size.toFloat()
        // Calculate Root.
        root = sqrt(mean)
        return root
    }

    fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun convertStringLocalDateTime(date: String): LocalDateTime {
        val instant = Instant.ofEpochSecond(date.toLong())
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    }

    fun getTimeNow() :String{
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", portugal)
        val currentTime = Calendar.getInstance().time
        return dateFormat.format(currentTime)
    }

    fun has24HoursPassed(previousTime: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", portugal)
        val currentTime = Calendar.getInstance().time
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(previousTime)

        // Add 24 hours to the previous time
        calendar.add(Calendar.HOUR_OF_DAY, 24)

        // Check if the current time is after the updated previous time
        return currentTime.after(calendar.time)
    }

    fun formatDatePortugueseLocale(time: LocalDateTime): String? {
        val now = LocalDateTime.now()
        val duration = Duration.between(time, now)
        var pattern = ""
        if(duration.toDays() <= 1){
            pattern = "EEEE, H:mm"
        }
        else if(duration.toDays() in 2..7){
            pattern = "dd MMMM, H:mm"
        }else{
            pattern = "yyyy/MM/dd, H:mm"
        }

        val newDateFormatter = DateTimeFormatter.ofPattern(pattern, portugal)
        return newDateFormatter.format(time)
    }

    fun storeTokenExpiry(expiresIn: Int): Long {
        val currentTime = Instant.now().epochSecond
        return currentTime + expiresIn
    }

    fun isTokenExpired(expiryTime: Long): Boolean {
        val currentTime = Instant.now().epochSecond
        return currentTime >= expiryTime
    }
    fun addIntToStore(sharedPreferences:SharedPreferences, key: String, value: Int){
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }
    fun addStringToStore(sharedPreferences:SharedPreferences, key: String, value: String){
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }
    fun addBooleanToStore(sharedPreferences: SharedPreferences, key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun clearSharedPreferences(sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun changeMode(sharedPreferences: SharedPreferences) : Boolean {
        ThemeState.isDarkThemeEnabled = !ThemeState.isDarkThemeEnabled
        addBooleanToStore(sharedPreferences, "theme_mode_is_dark", ThemeState.isDarkThemeEnabled)
        return ThemeState.isDarkThemeEnabled
    }

    fun setErrorState(message: String?) {
        println(message)
    }

    fun startDetailActivity(context: Context, activityClass: Class<*>, oldActivity: Activity) {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        oldActivity.finish()
    }
}