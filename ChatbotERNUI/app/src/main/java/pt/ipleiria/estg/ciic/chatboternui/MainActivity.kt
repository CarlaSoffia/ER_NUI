package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import pt.ipleiria.estg.ciic.chatboternui.models.Message
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.*
import vosk.SpeechService
import java.io.IOException
import java.util.*


private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

class MainActivity : ComponentActivity(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var _finishedLoading: MutableState<Boolean> = mutableStateOf(false)
    private val _messages: MutableState<Array<Message>> = mutableStateOf(emptyArray())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        setContent {
            ChatbotERNUITheme {
                if(!_finishedLoading.value){
                    CircularProgressBar()
                }else{
                    ChatSection()
                    recognizeMicrophone()
                }
            }
        }
   }
    private fun checkPermission(){
        // Check if user has given permission to record audio, init the model after permission is granted
        val permissionCheck = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else {
            initModel()
        }
    }
    private fun recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE)
            speechService!!.stop()
            speechService = null
        } else {
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
            } catch (e: IOException) {
                setErrorState(e.message)
            }
        }
    }
    private fun initModel() {
        StorageService.unpack(applicationContext, "model-pt", "model",
            { model: Model? ->
                this.model = model
                setUiState(STATE_READY)
            }
        ) { exception: IOException -> setErrorState("Failed to unpack the model" + exception.message) }
    }
    private fun setErrorState(message: String?) {
        print(message)
    }
    private fun setUiState(state: Int) {
        when (state) {
            STATE_READY -> {
                _finishedLoading.value = true
            }
            STATE_DONE -> {
                // user stopped talking
            }
            else -> throw IllegalStateException("Unexpected value: $state")
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
       /* if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
        }
        if (speechStreamService != null) {
            speechStreamService!!.stop()
        }*/
    }
    override fun onResult(hypothesis: String) {
        val result = JSONObject(hypothesis)
        var confs = emptyArray<Double>()
        val wordsConfs: JSONArray
        try {
            wordsConfs = result.getJSONArray("result")
        }catch (e: JSONException){
            return
        }
        for (i in 0 until wordsConfs.length()) {
            val word = wordsConfs.getJSONObject(i)
            confs = confs.plus(word.get("conf").toString().toDouble())
        }
        val medianAccuracy = calculate(confs)
        var transcription = result.getString("text")
        _messages.value.plus(Message(text = transcription, client_id ="1", accuracy = medianAccuracy, isChatbot = false))
        chatbotResponse(accuracy = medianAccuracy)
    }
    private fun calculate(values: Array<Double>): Double {
        Arrays.sort(values)
        val size = values.size
        return (values[size/2] + values[(size-1)/2]) / 2
    }
    override fun onFinalResult(hypothesis: String) {
        // nothing
    }
    override fun onPartialResult(hypothesis: String) {
        // nothing
    }
    override fun onError(e: Exception) {
        setErrorState(e.message)
    }
    override fun onTimeout() {
        setUiState(STATE_DONE)
    }
    private fun chatbotResponse(accuracy: Double){
        if(accuracy < 0.80){
            _messages.value.plus(Message(text = "Não entendi quase nada, repita por favor", isChatbot = true))
        }
    }

    @Composable
    private fun CircularProgressBar(){
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 26.dp),
                color = blueDM
            )
            Text(text = "A Carregar...")

        }
    }
    @Composable
    fun ChatSection() {
        val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(_messages.value.size) {
                _messages.value.forEach { chat ->
                    MessageItem(
                        messageText = chat.text,
                        time = simpleDateFormat.format(chat.time),
                        isChatbot = chat.isChatbot,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    fun MessageItem(
        messageText: String?,
        time: String,
        isChatbot: Boolean
        ) {
        val botChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)
        val authorChatBubbleShape = RoundedCornerShape(8.dp, 0.dp, 8.dp, 8.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = if (isChatbot) Alignment.End else Alignment.Start
        )
        {
            if (!messageText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isChatbot) reseneWhite else reseneLightBlue,
                            shape = if (isChatbot) authorChatBubbleShape else botChatBubbleShape
                        )
                        .border(1 .dp, color = if (isChatbot) reseneDarkWhite else reseneDarkBlue, shape = if (isChatbot) authorChatBubbleShape else botChatBubbleShape)
                        .padding(
                            top = 8.dp,
                            bottom = 8.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    Text(
                        text = messageText,
                        color = black,
                        fontSize = 18.sp
                    )
                }
            Text(
                text = time,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            }
        }
    }
    @Preview(showBackground = true)
    @Composable
    fun AppPreview(){
        ChatbotERNUITheme {
            ChatSection()
        }
    }
}

