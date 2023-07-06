package pt.ipleiria.estg.ciic.chatboternui.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt
import android.provider.Settings.Secure
import java.time.OffsetDateTime

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

    fun convertStringLocalDateTime(date: String): LocalDateTime {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val offsetDateTime = OffsetDateTime.parse(date, formatter)
        return offsetDateTime.toLocalDateTime()
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
    fun setErrorState(message: String?) {
        println(message)
    }

    fun startDetailActivity(context: Context, activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    fun getAndroidMacAddress(context: Context): String {
        val identifier = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        // Convert hex string to bytes
        val hexBytes = identifier.chunked(2) // Split into pairs of characters
            .map { it.toInt(16).toByte() } // Convert each pair to a byte

        // Set MAC address format with hex bytes
        val macBytes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        for (i in 0 until minOf(hexBytes.size, 6)) {
            macBytes[i] = hexBytes[i]
        }

        // Format MAC address as string
        return macBytes.joinToString(":") { "%02X".format(it) }
    }

}