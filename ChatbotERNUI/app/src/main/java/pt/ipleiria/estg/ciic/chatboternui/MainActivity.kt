package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import pt.ipleiria.estg.ciic.chatboternui.models.Message
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.SpeechService


private const val STATE_BEGIN = 0
private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

class MainActivity : IBaseActivity, BaseActivity(), RecognitionListener ,TextToSpeech.OnInitListener {
    private var _messages = mutableStateListOf<Message>()
    private var _messageWritten : MutableState<String> = mutableStateOf("")
    private var _microActive : MutableState<Boolean> = mutableStateOf(true)
    private var _finishedLoading: MutableState<Int> = mutableStateOf(STATE_BEGIN)
    private var middleGeriatricQuestionnaire: Boolean = false
    private var middleOxfordHappinessQuestionnaire: Boolean = false
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var _showAlertGeriatricQuestionnaire : MutableState<Boolean> = mutableStateOf(false)
    private var _showAlertOxfordQuestionnaire : MutableState<Boolean> = mutableStateOf(false)
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        super.onCreateBaseActivityWithMenu(this)
        if(sharedPreferences.getString("access_token", "").toString() == ""){
            utils.startDetailActivity(applicationContext,LoginActivity::class.java, this)
            return
        }
        tts = TextToSpeech(this, this)
        checkPermission()
        getAllMessages()
        handleQuestionnaires()
   }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            for (locale in tts!!.availableLanguages) {
                if(locale.displayCountry.equals("Portugal")) {
                    tts!!.language = locale
                    break
                }
            }
        }

        tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // TTS started speaking
                // Ensures the STT service stops listening when TTS start to speak
                if(_microActive.value){
                    speechService!!.setPause(true)
                }
            }

            override fun onDone(utteranceId: String?) {
                // TTS finished speaking
                // Ensures the STT service resumes listening when TTS is done speaking
                if(_microActive.value){
                    speechService!!.setPause(false)
                }
            }

            override fun onError(utteranceId: String?) {
                // Handle TTS error
                if(_microActive.value){
                    speechService!!.setPause(false)
                }
            }
        })
    }
    private fun handleQuestionnaires(){
        middleGeriatricQuestionnaire = sharedPreferences.getBoolean("middleGeriatricQuestionnaire", false)
        middleOxfordHappinessQuestionnaire = sharedPreferences.getBoolean("middleOxfordHappinessQuestionnaire", false)
        if(!middleGeriatricQuestionnaire && !middleOxfordHappinessQuestionnaire){
            val geriatricQuestionnaireCompletedDate = sharedPreferences.getString("geriatricQuestionnaireCompletedDate","")
            val oxfordHappinessQuestionnaireCompletedDate = sharedPreferences.getString("oxfordHappinessQuestionnaireCompletedDate","")
            if(geriatricQuestionnaireCompletedDate != ""){
                _showAlertGeriatricQuestionnaire.value = utils.has24HoursPassed(geriatricQuestionnaireCompletedDate!!)
            }else{
                _showAlertGeriatricQuestionnaire.value = true
            }
            if(oxfordHappinessQuestionnaireCompletedDate != ""){
                _showAlertOxfordQuestionnaire.value = utils.has24HoursPassed(oxfordHappinessQuestionnaireCompletedDate!!)
            }else{
                _showAlertOxfordQuestionnaire.value = true
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
        _microActive.value = sharedPreferences.getBoolean("microActive", false)
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
        if(medianAccuracy >= 0.60){
            chatbotResponse(transcription = transcription)
        }
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

    private fun getAllMessages() {
        _messages = mutableStateListOf()
        scope.launch {
            val response = httpRequests.request(sharedPreferences, "GET", "/messages?order=asc&limiy=100")
            if(handleConnectivityError(response["status_code"].toString(), activity)) return@launch
            val messages = JSONArray(response["data"].toString())
            for (i in 0 until messages.length()) {
                val message = JSONObject(messages[i].toString())
                if(message["body"].toString().contains("{")){
                    if(message["body"].toString().contains("points")){
                        val message = Message(id = message["id"].toString().toLong(),
                            text = JSONObject(message["body"].toString())["message"] as String?,
                            isChatbot = message["isChatbot"].toString() == "1",
                            time = utils.convertStringLocalDateTime(message["created_at"].toString()))
                        _messages.add(message)
                    }
                    continue
                }
                if (message["body"].toString() == "start_geriatric_form" || message["body"].toString() == "start_oxford_happiness_form") {
                    continue
                }
                _messages.add(Message(id = message["id"].toString().toLong(),
                    text = message["body"] as String?,
                    isChatbot = message["isChatbot"].toString() == "1",
                    time = utils.convertStringLocalDateTime(message["created_at"].toString())))
            }
        }
    }
    private fun chatbotResponse(transcription: String){
        sendMessage(transcription)
    }

    private fun sendMessage(transcription: String) {
        val messageSend = JSONObject()
        messageSend.put("isChatbot", false)
        messageSend.put("body", transcription)
        var response: JSONObject
        scope.launch {
            response = httpRequests.request(sharedPreferences, "POST", "/messages", messageSend.toString())
            if(handleConnectivityError(response["status_code"].toString(), activity)) return@launch
            val messages = JSONArray(response["data"].toString())
            for (i in 0 until messages.length()) {
                val message = JSONObject(messages[i].toString())
                val body = message["body"].toString()
                if(body == "start_geriatric_form" || body == "start_oxford_happiness_form"){
                    continue
                }
                val isChatbot = message["isChatbot"].toString() == "true"
                val id = message["id"].toString()
                _messages.add(Message(id = id.toLong(),
                    text = body,
                    isChatbot = isChatbot,
                    time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                if(isChatbot) tts!!.speak(body, TextToSpeech.QUEUE_ADD, null, id)
            }
        }
    }

    @Composable
    private fun LoadScreen(){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.chatbot),
                contentDescription = "Chatbot"
            )
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 26.dp),
                color = colorScheme.onPrimary
            )
            Text(text = "A Carregar...",
                fontSize = Typography.titleLarge.fontSize,
                fontWeight = Typography.titleLarge.fontWeight,
                color = colorScheme.onPrimary)
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
                MessageItem(
                    messageText = chat.text,
                    time = utils.formatDatePortugueseLocale(chat.time).toString(),
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
        val botChatBubbleShape = RoundedCornerShape(0.dp, 15.dp, 15.dp, 15.dp)
        val authorChatBubbleShape = RoundedCornerShape(15.dp, 0.dp, 15.dp, 15.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = if (!isChatbot) Alignment.End else Alignment.Start
        )
        {
            if (!messageText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(
                            if (!isChatbot) colorScheme.primaryContainer else colorScheme.secondary,
                            if (!isChatbot) authorChatBubbleShape else botChatBubbleShape
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
                        color = colorScheme.onSurface,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight
                    )
                }
                Text(
                    text = time,
                    fontSize = Typography.bodySmall.fontSize,
                    fontWeight = Typography.bodySmall.fontWeight,
                    modifier = Modifier.padding(start = 8.dp),
                    color = colorScheme.onSurface
                )
            }
        }
    }
    @Composable
    fun MessageSection() {
        val focusRequester = remember { FocusRequester() }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            shadowElevation = 15.dp,
            shape = RoundedCornerShape(25.dp, 25.dp, 0.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .background(
                        color = colorScheme.background,
                        shape = RoundedCornerShape(25.dp, 25.dp, 0.dp, 0.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = _messageWritten.value,
                        onValueChange = { _messageWritten.value = it },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = {
                            Text("Escreva uma mensagem",
                                fontSize = Typography.bodyLarge.fontSize,
                                fontWeight = Typography.bodyLarge.fontWeight,
                                color = colorScheme.onBackground)
                        },
                        textStyle = TextStyle.Default.copy(fontSize = Typography.bodyLarge.fontSize,
                            fontWeight = Typography.bodyLarge.fontWeight,
                            color = colorScheme.onBackground
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send",
                    tint = colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(5.dp, 0.dp)
                        .clickable {
                            chatbotResponse(transcription = _messageWritten.value)
                            _messageWritten.value = ""
                        }
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    @Composable
    fun TopBar(title: String, scaffoldState: ScaffoldState, scopeState: CoroutineScope){
        val focusManager = LocalFocusManager.current
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colorScheme.tertiary)
            .padding(10.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End) {
            Icon(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = "Botão Menu",
                tint = colorScheme.onPrimary,
                modifier = Modifier
                    .clickable {
                        focusManager.clearFocus()
                        scopeState.launch { scaffoldState.drawerState.open() }
                    }
                    .size(30.dp)
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.3f))
            Text(title,
                fontSize = Typography.titleLarge.fontSize,
                color = colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.fillMaxWidth(0.75f))
            Icon(
                painter = painterResource(id = if (_microActive.value) R.drawable.write else R.drawable.speak),
                contentDescription = "Botão alternar entre voz e texto",
                tint = colorScheme.onPrimary,
                modifier = Modifier
                    .clickable {
                        _microActive.value = !_microActive.value
                        utils.addBooleanToStore(
                            sharedPreferences,
                            "microActive",
                            _microActive.value
                        )
                    }
                    .align(Alignment.CenterVertically)
                    .size(30.dp)
            )
        }
    }
    @Composable
    override fun MainScreen(title: String?, scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
        if(!_microActive.value){
            _finishedLoading.value = STATE_DONE
        }
        if(_finishedLoading.value != STATE_DONE){
            if(_finishedLoading.value != STATE_READY){
                LoadScreen()
            }
            else if(_microActive.value && !tts!!.isSpeaking){
                recognizeMicrophone()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar("MIMO", scaffoldState!!, scope!!)
            ChatSection(Modifier.weight(1f))
            if(_showAlertGeriatricQuestionnaire.value){
                CommonComposables.StartForm("Gostaria de responder a um questionário para avaliar sintomas de depressão?",
                    "Este questionário que demora sensívelmente 6 minutos e contém 15 perguntas, às quais poderá responder com:\n"+
                            "- Sim\n" +
                            "- Não\n\n" +
                            "Além disso, será solicitado que justifique a sua resposta anterior para cada uma dessas perguntas.",
                    onClick = {
                        _showAlertGeriatricQuestionnaire.value = false
                        _showAlertOxfordQuestionnaire.value = false
                        utils.addBooleanToStore(sharedPreferences, "middleGeriatricQuestionnaire", true)
                        sendMessage("start_geriatric_form")
                    },onDismissRequest = {
                        _showAlertGeriatricQuestionnaire.value = false
                    })
            }else if(_showAlertOxfordQuestionnaire.value){
                    CommonComposables.StartForm("Gostaria de responder a um questionário para avaliar o seu nível de felicidade?",
                        "Este questionário é composto por um total de 29 perguntas, onde poderá responder com:\n\n"+
                                "- Discordo fortemente\n" +
                                "- Discordo moderadamente\n" +
                                "- Discordo levemente\n" +
                                "- Concordo levemente\n" +
                                "- Concordo moderadamente\n" +
                                "- Concordo fortemente \n\n" +
                                "Depois de responder a cada pergunta será lhe pedido para justificar a sua resposta.",
                        onClick = {
                            _showAlertOxfordQuestionnaire.value = false
                            _showAlertGeriatricQuestionnaire.value = false
                            utils.addBooleanToStore(sharedPreferences, "middleOxfordHappinessQuestionnaire", true)
                            sendMessage("start_oxford_happiness_form")
                        },onDismissRequest = {
                            _showAlertOxfordQuestionnaire.value = false
                        })
            }
            if(!_microActive.value){
                onDestroy()
                MessageSection()
            }
        }
    }
    override val activity: Activity
        get() = this
}

