package pt.ipleiria.estg.ciic.chatboternui.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import java.io.IOException
private const val PERMISSIONS_DENIED = 0
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val ERROR_MICROPHONE = 3


class SpeechListener(private var sharedPreferences: SharedPreferences) {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private val utils = Others()

    fun checkPermission(activity: Activity, applicationContext: Context):Int{
        // Check if user has given permission to record audio, init the model after permission is granted
        val permissionCheck = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return PERMISSIONS_DENIED
        } else {
            return initModel(applicationContext)
        }
    }

    private fun initModel(applicationContext: Context):Int{
        StorageService.unpack(applicationContext, "model-pt", "model",
            { model: Model? ->
                this.model = model
            }
        ) {
                exception: IOException -> utils.setErrorState("Failed to unpack the model" + exception.message)
        }
        return STATE_READY
    }

   fun stopSpeechService(){
       if (speechService != null) {
           speechService!!.stop()
           speechService!!.shutdown()
           speechService = null
       }
   }

    fun recognizeMicrophone(listener: RecognitionListener):Int {
        if (speechService == null) {
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService =
                    SpeechService(
                        rec,
                        16000.0f
                    )
                speechService!!.startListening(listener)
            } catch (e: IOException) {
                utils.setErrorState(e.message)
                return ERROR_MICROPHONE
            }
            return STATE_DONE
        }
        return ERROR_MICROPHONE
    }
}