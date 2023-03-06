package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.internal.wait
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import pt.ipleiria.estg.ciic.chatboternui.models.Message
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.*
import pt.ipleiria.estg.ciic.chatboternui.utils.*

private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

private const val URL_LARAVEL = "https://aalemotion.dei.estg.ipleiria.pt/api/"

class ConversationActivity : ComponentActivity(), RecognitionListener {
    private var _messages = mutableStateListOf<Message>()
    private var _message : MutableState<String> = mutableStateOf("")
    private var _microActive : MutableState<Boolean> = mutableStateOf(true)
    private val utils = Others()
    private val httpRequests = HTTPRequests()
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var sharedPreferences : SharedPreferences
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var _finishedLoading: MutableState<Int> = mutableStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        setContent {
            ChatbotERNUITheme {
                if(_finishedLoading.value != STATE_DONE){
                    if(_finishedLoading.value != STATE_READY){
                        LoadScreen()
                    }else if(_microActive.value){
                        recognizeMicrophone()
                    }
                }else{
                    MainScreen()
                }
            }
        }
   }
    private fun recognizeMicrophone() {
        if (speechService == null) {
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
                _finishedLoading.value = STATE_DONE
            } catch (e: IOException) {
                utils.setErrorState(e.message)
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
    private fun initModel() {
        StorageService.unpack(applicationContext, "model-pt", "model",
            { model: Model? ->
                this.model = model
                _finishedLoading.value = STATE_READY
            }
        ) { exception: IOException -> utils.setErrorState("Failed to unpack the model" + exception.message) }
    }
    public override fun onDestroy() {
        super.onDestroy()
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
            speechService = null
        }
    }

    override fun onPartialResult(hypothesis: String) {
        // Nothing
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
        val medianAccuracy = utils.calculateRootMeanSquare(confs)
        val transcription = result.getString("text")
        _messages.add(Message(id = _messages.size.toLong()+1, text = transcription, isChatbot = false))
        chatbotResponse(transcription = transcription, accuracy = medianAccuracy)
    }

    override fun onFinalResult(hypothesis: String?) {
        // Nothing
    }

    override fun onError(exception: Exception?) {
        if (exception != null) {
            utils.setErrorState(exception.message)
        }
    }

    override fun onTimeout() {
        // got quiet after a long while
        _finishedLoading.value = STATE_DONE
    }
    private fun chatbotResponse(transcription: String, accuracy: Double=-1.0){
        if (accuracy != -1.0 && accuracy < 80){
            _messages.add(Message(id = _messages.size.toLong()+1, text = "Por favor, repita o que disse.", isChatbot = true))
            return
        }
        val response = sendRasaServer(transcription)
        val messages :JSONArray = response["data"] as JSONArray
        for (i in 0 until messages.length()) {
            val message = JSONObject(messages[i].toString())
            _messages.add(Message(id = _messages.size.toLong()+1, text = message["text"] as String?, isChatbot = true))
        }
    }
    private fun refreshAccessToken(){
        val body = JSONObject()
        body.put("email",sharedPreferences.getString("username", ""))
        body.put("password",sharedPreferences.getString("password", ""))
        scope.launch {
            val response = httpRequests.request("POST", URL_LARAVEL, body.toString())
            val data = JSONObject(response["data"].toString())
            utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
        }
    }
    private fun sendRasaServer(transcription: String): JSONObject {
        val messageRasa = JSONObject()
        messageRasa.put("sender", sharedPreferences.getString("username", ""))
        messageRasa.put("message", transcription)
        messageRasa.put("metadata", JSONObject().put("token",sharedPreferences.getString("access_token", "")))
        var response: JSONObject = JSONObject()
        runBlocking {
                response = httpRequests.requestRasa(messageRasa.toString())
                val code = response["status_code"]
                if(code == 403 || code == 401){
                    refreshAccessToken()
                    response = sendRasaServer(transcription)
                }
            }
        return response
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
                val simpleDateFormat = utils.checkTime(chat.time)
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
    @OptIn(ExperimentalMaterial3Api::class)
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
                            isChatbot = false
                        )
                    )
                    chatbotResponse(transcription = _message.value)
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
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar("Chatbot")
            ChatSection(Modifier.weight(1f))
            if(!_microActive.value){
                onDestroy()
                MessageSection()
            }else {
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

