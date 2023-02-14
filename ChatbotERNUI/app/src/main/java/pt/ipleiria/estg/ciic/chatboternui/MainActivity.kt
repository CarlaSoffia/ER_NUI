package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt


private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

class MainActivity : ComponentActivity(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var _finishedLoading: MutableState<Boolean> = mutableStateOf(false)
    private var _messages = mutableStateListOf<Message>()
    private var _message : MutableState<String> = mutableStateOf("")
    private var _microActive : MutableState<Boolean> = mutableStateOf(true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        setContent {
            ChatbotERNUITheme {
                if(!_finishedLoading.value){
                    LoadScreen()
                }else{
                    MainScreen()
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
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
        }
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
        val medianAccuracy = calculateRootMeanSquare(confs)

        var transcription = result.getString("text")
        _messages.add(Message(id = _messages.size.toLong()+1, text = transcription, client_id ="1", accuracy = medianAccuracy, isChatbot = false))
        chatbotResponse(accuracy = medianAccuracy)
    }
    private fun calculateRootMeanSquare(values: Array<Double>): Double {

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
            _messages.add(Message(id = _messages.size.toLong()+1, text = "Não entendi quase nada, repita por favor", isChatbot = true))
        }
    }
    private fun checkTime(time: LocalDateTime): DateTimeFormatter {
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
    @Composable
    private fun LoadScreen(){
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.chatbot),
                contentDescription = "Chatbot"
            )
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 26.dp),
                color = LightColorScheme.tertiary
            )
            Text(text = "A Carregar...", fontSize = Typography.titleLarge.fontSize, color = LightColorScheme.surface)

        }
    }
    @Composable
    fun ChatSection(modifier: Modifier = Modifier) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
                reverseLayout = true
        ) {
        items(_messages.reversed()) { chat ->
                val simpleDateFormat = checkTime(chat.time)
                MessageItem(
                    messageText = chat.text,
                    time = simpleDateFormat.format(chat.time),
                    isChatbot = chat.isChatbot,
                )
                Spacer(modifier = Modifier.height(8.dp))
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
            horizontalAlignment = if (!isChatbot) Alignment.End else Alignment.Start
        )
        {
            if (!messageText.isNullOrEmpty()) {

                Text(
                    text = if(!isChatbot) "Você" else "Chatbot",
                    fontSize = Typography.bodyLarge.fontSize,
                    modifier = Modifier.padding(start = 8.dp),
                    color = LightColorScheme.surface
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (!isChatbot) LightColorScheme.primary else LightColorScheme.secondary,
                            shape = if (!isChatbot) authorChatBubbleShape else botChatBubbleShape
                        )
                        .border(
                            1.dp,
                            color = if (!isChatbot) LightColorScheme.onPrimary else LightColorScheme.onSecondary,
                            shape = if (!isChatbot) authorChatBubbleShape else botChatBubbleShape
                        )
                        .padding(
                            top = 8.dp,
                            bottom = 8.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    Text(
                        text = messageText,
                        color = LightColorScheme.surface,
                        fontSize = Typography.bodyLarge.fontSize
                    )
                }
                Text(
                    text = time,
                    fontSize = Typography.bodyLarge.fontSize,
                    modifier = Modifier.padding(start = 8.dp),
                    color = LightColorScheme.surface
                )
            }
        }
    }
    @Composable
    fun MessageSection() {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .height(60.dp))
             {
            TextField(
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = LightColorScheme.primary,
                    unfocusedBorderColor = LightColorScheme.primary
                ),
                textStyle = TextStyle.Default.copy(fontSize = Typography.bodyLarge.fontSize),
                placeholder = {
                    Text("Escreva aqui", fontSize = Typography.bodyLarge.fontSize, color = LightColorScheme.surface)
                },
                value = _message.value,
                onValueChange = {
                    _message.value = it
                },
                modifier = Modifier
                    .fillMaxWidth(0.88F)
                    .background(
                        color = LightColorScheme.primary,
                        shape = RoundedCornerShape(40.dp, 40.dp, 40.dp, 40.dp)
                    )
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.send),
                contentDescription = "Botão enviar",
                tint = LightColorScheme.tertiary,
                modifier = Modifier.clickable {
                    _messages.add(
                        Message(
                            id = _messages.size.toLong() + 1,
                            text = _message.value,
                            client_id = "1",
                            isChatbot = false
                        )
                    )
                    _message.value = ""
                }
                .align(Alignment.CenterVertically)
                .size(35.dp)
            )
            Spacer(Modifier.weight(1f))
        }
    }
    @Composable
    fun TopBar(title: String){
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp)
            .height(60.dp)
            .background(LightColorScheme.secondary)) {
            Icon(
                painter = painterResource(id = R.drawable.back),
                contentDescription = "Botão voltar atrás",
                tint = LightColorScheme.tertiary,
                modifier = Modifier.clickable {
                    // change view
                }
                .align(Alignment.CenterVertically)
                .size(40.dp)
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.3f))
            Text(title, fontSize = Typography.titleLarge.fontSize, color = LightColorScheme.surface, modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.fillMaxWidth(0.75f))
            Icon(
                painter = painterResource(id = if (_microActive.value) R.drawable.write else R.drawable.speak),
                contentDescription = "Botão alternar entre voz e texto",
                tint = LightColorScheme.tertiary,
                modifier = Modifier.clickable {
                    _microActive.value = !_microActive.value
                }
                    .align(Alignment.CenterVertically)
                    .size(30.dp)
            )
        }
    }
    @Composable
    fun MainScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
          //  verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar("Chatbot")
            ChatSection(Modifier.weight(1f))
            if(!_microActive.value){
                MessageSection()
            }else{
                recognizeMicrophone()
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview(){
        ChatbotERNUITheme {
            LoadScreen()
        }
    }
}

