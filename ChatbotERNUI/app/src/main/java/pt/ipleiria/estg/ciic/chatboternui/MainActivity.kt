package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
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
import kotlinx.coroutines.*
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import pt.ipleiria.estg.ciic.chatboternui.models.MenuItem
import pt.ipleiria.estg.ciic.chatboternui.models.Message
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.*
import pt.ipleiria.estg.ciic.chatboternui.utils.*
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
private const val STATE_BEGIN = 0
private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

class MainActivity : ComponentActivity(), RecognitionListener {
    private var _messages = mutableStateListOf<Message>()
    private var _messageWritten : MutableState<String> = mutableStateOf("")
    private var _microActive : MutableState<Boolean> = mutableStateOf(true)
    private var _modeLight : MutableState<Boolean> = mutableStateOf(true)
    private var _finishedLoading: MutableState<Int> = mutableStateOf(STATE_BEGIN)
    private var _showConnectivityError: MutableState<Boolean> = mutableStateOf(false)
    private var iterations: MutableList<Pair<String,JSONObject>> = mutableListOf()
    private var groupsEmotions: List<Pair<String,List<String>>> = emptyList()
    private var middleGeriatricQuestionnaire: Boolean = false
    private var middleOxfordHappinessQuestionnaire: Boolean = false
    private var idGeriatricQuestionnaire: Int = -1
    private var idOxfordHappinessQuestionnaire: Int = -1
    private val utils = Others()
    private val httpRequests = HTTPRequests()
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var token: String
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var _showAlertGeriatricQuestionnaire : MutableState<Boolean> = mutableStateOf(true)
    private var _showAlertOxfordQuestionnaire : MutableState<Boolean> = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("access_token", "").toString()
        _modeLight.value = sharedPreferences.getBoolean("modeLight", true)

        if(token == ""){
            utils.startDetailActivity(applicationContext,LoginActivity::class.java)
            return
        }

        checkPermission()
        getGroupsEmotions()
        getAllMessages()
        _microActive.value = sharedPreferences.getBoolean("microActive", false)
        val geriatricQuestionnaireCompletedDate = sharedPreferences.getString("geriatricQuestionnaireCompletedDate","")
        val oxfordHappinessQuestionnaireCompletedDate = sharedPreferences.getString("oxfordHappinessQuestionnaireCompletedDate","")
        if(geriatricQuestionnaireCompletedDate != ""){
            _showAlertGeriatricQuestionnaire.value = utils.has24HoursPassed(geriatricQuestionnaireCompletedDate!!)
        }
        if(oxfordHappinessQuestionnaireCompletedDate != ""){
            _showAlertOxfordQuestionnaire.value = utils.has24HoursPassed(oxfordHappinessQuestionnaireCompletedDate!!)
        }

        setContent {
            ChatbotERNUITheme {
                val scaffoldState = rememberScaffoldState()
                val scopeState = rememberCoroutineScope()
                if(!_microActive.value){
                    _finishedLoading.value = STATE_DONE
                }
                if(_finishedLoading.value != STATE_DONE){
                    if(_finishedLoading.value != STATE_READY){
                        LoadScreen()
                    }
                    else if(_microActive.value){
                        recognizeMicrophone()
                    }
                }
                else{
                   AppScreen(scaffoldState, scopeState)
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
    private fun getGroupsEmotions(){
        scope.launch {
            var response = httpRequests.request("GET", "/emotions/groups", token = token)
            try {
                var data = JSONObject(response["data"].toString())
                val groups = JSONArray(data["list"].toString())
                for (i in 0 until groups.length()) {
                    val group = JSONObject(groups[i].toString())
                    val name = group["name"].toString()
                    response = httpRequests.request("GET", "/emotions/groups/${name}", token = token)
                    data = JSONObject(response["data"].toString())
                    val emotions = JSONArray(data["list"].toString())
                    val emotionsList : MutableList<String> = mutableListOf()
                    for (j in 0 until emotions.length()) {
                        val emotion = JSONObject(emotions[j].toString())
                        emotionsList.add(emotion["name"].toString())
                    }
                    groupsEmotions = groupsEmotions.plus(name to emotionsList)
                }
            } catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
        }
    }

    private fun handleSpeech(text:String, sentiment: JSONObject, question:Int=-1, isWhy: Boolean=false): Int{
        val group = groupsEmotions.find { it.second.contains(sentiment["emotion"].toString()) }?.first
        val iterationGroup = iterations.find { it.first == group }
        val accuracy = sentiment["predictions"].toString().split(";")
            .map { it.split("#") }
            .find { it.first() == sentiment["emotion"].toString() }
            ?.get(1)
        val speechBody = JSONObject()
        speechBody.put("iteration_id", iterationGroup?.second?.getString("iteration_id"))
        speechBody.put("iteration_usage_id", iterationGroup?.second?.getString("iteration_usage_id"))
        speechBody.put("datesSpeeches[0]", utils.getTimeNow())
        speechBody.put("accuraciesSpeeches[0]", accuracy.toString())
        speechBody.put("textsSpeeches[0]", text)
        speechBody.put("preditionsSpeeches[0]", sentiment["predictions"].toString())
        var id = -1
        var response: JSONObject
        runBlocking {
            response =
                httpRequests.requestFormData("/speeches", speechBody, token = token)
            try{
                val data = JSONObject(response["data"].toString())
                id = data["id"].toString().toInt()
            }catch (ex: JSONException){
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                if(response["status_code"].toString() == "200"){
                    createIteration(group.toString())
                    handleSpeech(text, sentiment, question, isWhy)
                }
            }
        }
        return id
    }
    private fun handleIteration(sentiment: JSONObject){
        val group = groupsEmotions.find { it.second.contains(sentiment["emotion"].toString()) }?.first
        val iterationGroup = iterations.find { it.first == group.toString() }
        if(iterations.isNotEmpty() && iterationGroup != null){
            return
        }
        createIteration(group.toString())

    }
    private fun createIteration(group: String){
        val iterationBody = JSONObject()
        iterationBody.put("macAddress", "00:00:00:00:00:00")
        iterationBody.put("emotion", group)
        iterationBody.put("type", "best")
        var response: JSONObject
        runBlocking {
            response =
                httpRequests.request("POST", "/iterations", iterationBody.toString(), token = token)
            val data = JSONObject(response["data"].toString())
            try{
                val iteration = JSONObject()
                    .put("iteration_id", data["id"].toString())
                    .put("iteration_usage_id", data["usage_id"].toString())
                val index = iterations.indexOfFirst { it.first == group }
                if(iterations.isNotEmpty() && index != -1){
                    iterations.removeAt(index)
                }
                iterations.add(group to iteration)
            }
            catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
        }
    }
    private fun getAllMessages() {
        _messages = mutableStateListOf()
        scope.launch {
            val response = httpRequests.request("GET", "/messages", token = token)
            try {
                val data = JSONObject(response["data"].toString())
                val messages = JSONArray(data["list"].toString())

                for (i in 0 until messages.length()) {
                    val message = JSONObject(messages[i].toString())
                    if(message["body"].toString().contains("{")){
                        if(message["body"].toString().contains("points")){
                            _messages.add(Message(id = message["id"].toString().toLong(),
                                text = JSONObject(message["body"].toString())["message"] as String?,
                                isChatbot = message["isChatbot"].toString() == "1",
                                time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                        }
                        continue
                    }
                    _messages.add(Message(id = message["id"].toString().toLong(),
                        text = message["body"] as String?,
                        isChatbot = message["isChatbot"].toString() == "1",
                        time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                }
            } catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
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
                response = httpRequests.request("POST", "/messages", messageSend.toString(), token = token)
                try{
                    val data = JSONObject(response["data"].toString())
                    val messages = JSONArray(data["list"].toString())
                    for (i in 0 until messages.length()) {
                        var speechId = -1
                        val message = JSONObject(messages[i].toString())
                        if(message["body"].toString().contains("{")){
                            if(message["body"].toString().contains("accuracy")){
                                val sentiment = JSONObject(message["body"].toString())
                                // Creates iteration if doesn't exists
                                handleIteration(sentiment)
                                speechId = handleSpeech(transcription,sentiment)
                            }
                            // Creates speech and or not the response if we are in the questionnaire
                            if(message["body"].toString().contains("is_why")){
                                val responseQuestion = JSONObject(message["body"].toString())
                                handleResponseQuestionnaire(responseQuestion["question"].toString().toInt(), transcription, responseQuestion["is_why"].toString().toBoolean(), speechId)
                            }
                            if(message["body"].toString().contains("points")){
                                val pointsResponse = JSONObject(message["body"].toString())
                                handlePointsQuestionnaire(pointsResponse["points"].toString().toDouble())
                                _messages.add(Message(id = message["id"].toString().toLong(),
                                    text = pointsResponse["message"] as String?,
                                    isChatbot = message["isChatbot"].toString() == "true",
                                    time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                            }
                        }else if (message["body"] != "start_geriatric_form" && message["body"] != "start_oxford_happiness_form"){
                            _messages.add(Message(id = message["id"].toString().toLong(),
                                text = message["body"] as String?,
                                isChatbot = message["isChatbot"].toString() == "true",
                                time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                        }
                    }
                }
            catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
            }

    }

    private fun handlePointsQuestionnaire(points: Double){
        val apiURL = if(middleGeriatricQuestionnaire) "/geriatricQuestionnaires/${idGeriatricQuestionnaire}/points" else if (middleOxfordHappinessQuestionnaire) "/oxfordHappinessQuestionnaires/${idOxfordHappinessQuestionnaire}/points" else return
        val pointsQuestionnaire = JSONObject()
        pointsQuestionnaire.put("points", points)

        scope.launch {
            val response = httpRequests.request("PUT", apiURL, pointsQuestionnaire.toString(), token = token)
            try{
                val data = JSONObject(response["data"].toString())
                if(middleGeriatricQuestionnaire){
                    middleGeriatricQuestionnaire = false
                    utils.addStringToStore(sharedPreferences,"geriatricQuestionnaireCompletedDate",utils.getTimeNow())
                }
                if(middleOxfordHappinessQuestionnaire){
                    middleOxfordHappinessQuestionnaire = false
                    utils.addStringToStore(sharedPreferences,"oxfordHappinessQuestionnaireCompletedDate",utils.getTimeNow())
                }
            }
            catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
        }
    }
    private fun handleResponseQuestionnaire(question: Int, response: String, isWhy: Boolean, speechId: Int){
        val apiURL = if(middleGeriatricQuestionnaire) "/geriatricQuestionnaires/${idGeriatricQuestionnaire}/responses" else if (middleOxfordHappinessQuestionnaire) "/oxfordHappinessQuestionnaires/${idOxfordHappinessQuestionnaire}/responses" else return

        val responseQuestionnaire = JSONObject()
        responseQuestionnaire.put("question", question)
        responseQuestionnaire.put("response", response)
        responseQuestionnaire.put("is_why", isWhy)
        responseQuestionnaire.put("speech_id", speechId)

        scope.launch {
            val response = httpRequests.request("PUT", apiURL, responseQuestionnaire.toString(), token = token)
            try{
                JSONObject(response["data"].toString())
            }
            catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
        }
    }
    private fun createQuestionnaire(){
        val apiURL = if(middleGeriatricQuestionnaire) "/geriatricQuestionnaires" else if (middleOxfordHappinessQuestionnaire) "/oxfordHappinessQuestionnaires" else return
        var response: JSONObject
        scope.launch {
            response = httpRequests.request("POST", apiURL, token = token)
            try{
                val data = JSONObject(response["data"].toString())
                if(middleGeriatricQuestionnaire){
                    idGeriatricQuestionnaire = data["id"].toString().toInt()
                }else{
                   idOxfordHappinessQuestionnaire = data["id"].toString().toInt()
                }
            }
            catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
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
    fun MainScreen(scaffoldState: ScaffoldState,
                   scopeState: CoroutineScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar("EmoCare", scaffoldState, scopeState)
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
                        middleGeriatricQuestionnaire = true
                        createQuestionnaire()
                        sendMessage("start_geriatric_form")
                    },onDismissRequest = {
                        _showAlertGeriatricQuestionnaire.value = false
                    })
            }else{
                if(_showAlertOxfordQuestionnaire.value){
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
                            middleOxfordHappinessQuestionnaire = true
                            createQuestionnaire()
                            sendMessage("start_oxford_happiness_form")
                        },onDismissRequest = {
                            _showAlertOxfordQuestionnaire.value = false
                        })
                }
            }
            if(!_microActive.value){
                onDestroy()
                MessageSection()
            }else {
                recognizeMicrophone()
            }
            CommonComposables.DialogConnectivity(_showConnectivityError.value,
                onClick = {
                    _showConnectivityError.value = false
                    finish()
                },onDismissRequest = {
                    _showConnectivityError.value = false
                }
            )

        }
    }
    @Composable
    fun Drawer() {
        val menuItems = listOf(
            MenuItem(
                id = "profile",
                title = "Perfil",
                icon = R.drawable.profile,
                onClick = {},
                addDivider = false
            ),
            MenuItem(
                id = "questionnaires",
                title = "Questionários",
                icon = R.drawable.questionnaires,
                onClick = {},
                addDivider = true
            ),
            MenuItem(
                id = "mode",
                title = if (_modeLight.value) "Modo escuro" else "Modo claro",
                icon = if (_modeLight.value) R.drawable.dark else R.drawable.light,
                onClick = {},
                addDivider = false
            ),
            MenuItem(
                id = "information",
                title = "Tutorial",
                icon = R.drawable.information,
                onClick = {},
                addDivider = true
            ),
            MenuItem(
                id = "logout",
                title = "Terminar sessão",
                icon = R.drawable.logout,
                onClick = {},
                addDivider = false
            )
        )
        Column(
            Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(0.dp, 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically){
                Image(
                    painter = painterResource(R.drawable.chatbot),
                    contentDescription = "Chatbot",
                    modifier = Modifier.size(75.dp)
                )
                Spacer(modifier = Modifier.width(25.dp))
                Text(
                    text = "Menu principal",
                    color = colorScheme.onPrimary,
                    fontSize = Typography.titleLarge.fontSize
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(35.dp)
            ) {
                items(menuItems)
                { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                item.onClick()
                            }
                    ) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = item.title,
                            color = colorScheme.onSurface,
                            fontSize = Typography.bodyLarge.fontSize,
                            fontWeight = Typography.bodyLarge.fontWeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if(item.addDivider){
                        Divider(color = colorScheme.onSurface, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun AppScreen( scaffoldState: ScaffoldState,
                   scope: CoroutineScope)
    {
        androidx.compose.material.Scaffold(
            scaffoldState = scaffoldState,
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            drawerContent = {
                Drawer()
            },
            //customizing the drawer. Will also share this shape below.
            drawerElevation = 90.dp
        ) {//Here goes the whole content of our Screen
            MainScreen(scaffoldState, scope)
        }
    }
}

