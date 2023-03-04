package pt.ipleiria.estg.ciic.chatboternui.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt

class Others {

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

    fun checkTime(time: LocalDateTime): DateTimeFormatter {
        val now = LocalDateTime.now()
        val duration = Duration.between(time, now)
        if(duration.toDays() <= 1){
            return DateTimeFormatter.ofPattern("H:mm")
        }
        if(duration.toDays() in 2..7){
            return DateTimeFormatter.ofPattern("E H:mm")
        }
        return DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm")
    }

    fun addStringToStore(sharedPreferences:SharedPreferences, key: String, value: String){
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }
    fun addIntToStore(sharedPreferences:SharedPreferences, key: String, value: Int){
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }
    fun setErrorState(message: String?) {
        println(message)
    }
}