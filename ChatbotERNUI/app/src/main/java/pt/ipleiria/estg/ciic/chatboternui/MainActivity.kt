package pt.ipleiria.estg.ciic.chatboternui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ScaffoldState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.IOException
import org.json.JSONArray
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
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.DepressionQuestionnaireAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.HappinessQuestionnaireAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.RecordAudioAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.RecordedResultAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.ToggleInputModeAlert


private const val STATE_BEGIN = 0
private const val STATE_READY = 1
private const val STATE_DONE = 2
private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

class MainActivity : IBaseActivity, BaseActivity(), RecognitionListener ,TextToSpeech.OnInitListener {
    private var _messages = mutableStateListOf<Message>()
    private var _userMessage : MutableState<String> = mutableStateOf("")
    private var _chatbotWaitingMessage : Message = Message(id = Long.MAX_VALUE, time = null, text = "...", isChatbot = true, animate = true)
    private var _microActive : MutableState<Boolean> = mutableStateOf(false)
    private var _showRecordAudioAlert : MutableState<Boolean> = mutableStateOf(false)
    private var _showRecordedResultAlert : MutableState<Boolean> = mutableStateOf(false)
    private var _showInputModeAlert : MutableState<Boolean> = mutableStateOf(false)
    private var _finishedLoading: MutableState<Int> = mutableStateOf(STATE_BEGIN)
    private var middleGeriatricQuestionnaire: Boolean = false
    private var middleOxfordHappinessQuestionnaire: Boolean = false
    private var _showAlertGeriatricQuestionnaire : MutableState<Boolean> = mutableStateOf(false)
    private var _showAlertOxfordQuestionnaire : MutableState<Boolean> = mutableStateOf(false)
    private var sttModel: Model? = null
    private var sttService: SpeechService? = null
    private var ttsService: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        super.onCreateBaseActivityWithMenu(this)
        if(sharedPreferences.getString("access_token", "").toString() == ""){
            utils.startActivity(applicationContext,SignInActivity::class.java, this)
            return
        }
        defineQuestionnaireAlertOnClick(DepressionQuestionnaireAlert::class.simpleName.toString())
        defineQuestionnaireAlertOnClick(HappinessQuestionnaireAlert::class.simpleName.toString())
        defineToggleInputModeAlert()
        defineAudioRecordAlerts()
        ttsService = TextToSpeech(this, this)
        initSttModel()
        //getAllMessages()
        handleQuestionnaires()
   }
    private fun defineAudioRecordAlerts(){
        alerts[RecordAudioAlert::class.simpleName.toString()]!!.confirmButton.onClick = {
            sttService!!.setPause(true)
            _showRecordAudioAlert.value = false
            _showRecordedResultAlert.value = true
        }
        alerts[RecordAudioAlert::class.simpleName.toString()]!!.dismissButton!!.onClick = {
            sttService!!.setPause(true)
            _showRecordAudioAlert.value = false
            _userMessage.value = ""
        }

        alerts[RecordedResultAlert::class.simpleName.toString()]!!.confirmButton.onClick = {
            sendMessage(_userMessage.value)
            _userMessage.value = ""
            _showRecordedResultAlert.value = false
        }
        alerts[RecordedResultAlert::class.simpleName.toString()]!!.dismissButton!!.onClick = {
            _showRecordedResultAlert.value = false
            _userMessage.value = ""
        }
    }
    private fun defineToggleInputModeAlert(){
        _microActive.value = sharedPreferences.getBoolean("microActive", false)
        alerts[ToggleInputModeAlert::class.simpleName.toString()]!!.confirmButton.onClick = {
            _microActive.value = !_microActive.value
            utils.addBooleanToStore(sharedPreferences, "microActive", _microActive.value)
            _showInputModeAlert.value = false
        }
        alerts[ToggleInputModeAlert::class.simpleName.toString()]!!.dismissButton!!.onClick = {
            _showInputModeAlert.value = false
        }
    }
    private fun defineQuestionnaireAlertOnClick(alertClassName: String){
        val isDepressionQuestionnaireAlert = alertClassName == DepressionQuestionnaireAlert::class.simpleName.toString()
        alerts[alertClassName]!!.confirmButton.onClick = {
            showAlertDialog.value = false
            if(isDepressionQuestionnaireAlert){
                utils.addBooleanToStore(sharedPreferences, "middleGeriatricQuestionnaire", true)
                sendMessage("start_geriatric_form")
                _showAlertGeriatricQuestionnaire.value = false
            }else{
                utils.addBooleanToStore(sharedPreferences, "middleOxfordHappinessQuestionnaire", true)
                sendMessage("start_oxford_happiness_form")
                _showAlertOxfordQuestionnaire.value = false
            }
        }
        alerts[alertClassName]!!.dismissButton!!.onClick =  {
            showAlertDialog.value = false
            if(isDepressionQuestionnaireAlert){
                _showAlertGeriatricQuestionnaire.value = false
            }else{
                _showAlertOxfordQuestionnaire.value = false
            }
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            for (locale in ttsService!!.availableLanguages) {
                if(locale.displayCountry.equals("Portugal")) {
                    ttsService!!.language = locale
                    break
                }
            }
        }

        ttsService!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // TTS started speaking
                // Ensures the STT service stops listening when TTS start to speak
                if(_microActive.value){
                    sttService!!.setPause(true)
                }
            }

            override fun onDone(utteranceId: String?) {
                // TTS finished speaking
                // Ensures the STT service resumes listening when TTS is done speaking
                if(_microActive.value){
                    sttService!!.setPause(false)
                }
            }

            override fun onError(utteranceId: String?) {
                // Handle TTS error
                if(_microActive.value){
                    sttService!!.setPause(false)
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

            _showAlertGeriatricQuestionnaire.value = if(geriatricQuestionnaireCompletedDate != ""){
                utils.has24HoursPassed(geriatricQuestionnaireCompletedDate!!)
            }else{
                true
            }

            _showAlertOxfordQuestionnaire.value  = if(oxfordHappinessQuestionnaireCompletedDate != ""){
                utils.has24HoursPassed(oxfordHappinessQuestionnaireCompletedDate!!)
            }else{
                true
            }

        }
    }

    private fun listen(){
        sttService!!.startListening(this)
    }
    private fun initSttModel(){
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
            StorageService.unpack(applicationContext, "model-pt", "model",
                { model: Model? ->
                    this.sttModel = model
                    _finishedLoading.value = STATE_READY
                }
            ) { exception: IOException -> utils.setErrorState("Failed to unpack the model" + exception.message) }
        }
    }
    private fun initSttService(){
        if (sttService == null && sttModel != null) {
            try {
                val rec = Recognizer(sttModel, 16000.0f)
                sttService = SpeechService(rec, 16000.0f)
                _finishedLoading.value = STATE_DONE
            } catch (e: IOException) {
                utils.setErrorState(e.message!!)
            }
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
        if (sttService != null) {
            sttService!!.stop()
            sttService!!.shutdown()
            sttService = null
        }
    }

    override fun onPartialResult(hypothesis: String) {
        // Nothing
    }

    override fun onResult(hypothesis: String) {
        val result = JSONObject(hypothesis)
        _userMessage.value = result.getString("text")
    }

    override fun onFinalResult(hypothesis: String?) {
        // Nothing
    }

    override fun onError(exception: Exception?) {
        if (exception != null) {
            utils.setErrorState(exception.message!!)
        }
    }

    override fun onTimeout() {
        // got quiet after a long while
        _finishedLoading.value = STATE_DONE
    }

    private fun getAllMessages() {
        _messages = mutableStateListOf()
        scope.launch {
            val response = httpRequests.request(sharedPreferences, "GET", "/messages?order=asc&limit=100")
            if(handleConnectivityError(response["status_code"].toString())) return@launch
            val messages = JSONArray(response["data"].toString())
            for (i in 0 until messages.length()) {
                val message = JSONObject(messages[i].toString())
                _messages.add(Message(id = message["id"].toString().toLong(),
                    text = message["body"] as String?,
                    isChatbot = message["isChatbot"].toString() == "1",
                    time = utils.convertStringLocalDateTime(message["created_at"].toString())))
            }
        }
    }

    private fun sendMessage(userInput: String) {
        _messages.add(Message(id = _messages.size.toLong(),
            text = userInput,
            isChatbot = false))
        _messages.add(_chatbotWaitingMessage)

        val messageSend = JSONObject()
        messageSend.put("isChatbot", false)
        messageSend.put("body", userInput)
        var response: JSONObject
        scope.launch {
            response = httpRequests.request(sharedPreferences, "POST", "/messages", messageSend.toString())
            if(handleConnectivityError(response["status_code"].toString())) return@launch
            val messages = JSONArray(response["data"].toString())
            for (i in 0 until messages.length()) {
                val message = JSONObject(messages[i].toString())
                val body = message["body"].toString()
                if(body == "start_geriatric_form" || body == "start_oxford_happiness_form"){
                    continue
                }
                if(message["isChatbot"].toString() != "true"){
                    continue
                }
                val id = message["id"].toString()
                _messages.remove(_chatbotWaitingMessage)
                _messages.add(Message(id = id.toLong(),
                    text = body,
                    isChatbot = false,
                    time = utils.convertStringLocalDateTime(message["created_at"].toString())))
                ttsService!!.speak(body, TextToSpeech.QUEUE_ADD, null, id)
                if(body == "#IS_SHORT_QUESTION#"){
                    // show the modal
                }
            }
        }
    }

    @Composable
    private fun LoadScreen(){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp)
                .background(colorScheme.background),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "A carregar os seus dados...",
                fontSize = Typography.titleLarge.fontSize,
                lineHeight = Typography.titleLarge.lineHeight,
                textAlign = TextAlign.Center,
                color = colorScheme.onBackground
            )
            Image(
                painter = painterResource(id = R.drawable.loading),
                contentDescription = "Imagem de espera",
                modifier = Modifier.size(250.dp))
            Text(text = "Por favor aguarde...",
                fontSize = Typography.titleMedium.fontSize,
                lineHeight = Typography.titleMedium.lineHeight,
                textAlign = TextAlign.Center,
                color = colorScheme.onBackground
            )
            ProgressIndicator()
        }
    }
    @Composable
    fun ChatSection(modifier: Modifier = Modifier) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
                reverseLayout = true
        ) {
        items(_messages.reversed()) { chat ->
                MessageItem(
                    messageText = chat.text,
                    time = if (chat.time == null) null else utils.formatDatePortugueseLocale(chat.time!!).toString(),
                    isChatbot = chat.isChatbot,
                    animate = chat.animate
                )
            }
        }
    }
    @Composable
    fun MessageItem(
        messageText: String?,
        time: String?,
        isChatbot: Boolean,
        animate: Boolean = false
    ) {
        val botChatBubbleShape = RoundedCornerShape(0.dp, 15.dp, 15.dp, 15.dp)
        val authorChatBubbleShape = RoundedCornerShape(15.dp, 0.dp, 15.dp, 15.dp)

        // Define the two colors for the animation
        val primaryContainer = colorScheme.primaryContainer
        val secondaryContainer = colorScheme.secondaryContainer

        // Create an infinite transition if animation is enabled
        val infiniteTransition = rememberInfiniteTransition()
        val animatedColor by infiniteTransition.animateColor(
            initialValue = primaryContainer,
            targetValue = secondaryContainer,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        // Determine the background color based on the animation state
        val backgroundColor = if (animate) animatedColor else if (!isChatbot) primaryContainer else secondaryContainer

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalAlignment = if (!isChatbot) Alignment.End else Alignment.Start
        ) {
            if (!messageText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(
                            color = backgroundColor,
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
                        color = if (!isChatbot) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight
                    )
                }
                if (time != null) {
                    Text(
                        text = time,
                        fontSize = Typography.bodySmall.fontSize,
                        fontWeight = Typography.bodySmall.fontWeight,
                        modifier = Modifier.padding(start = 8.dp),
                        color = colorScheme.onBackground
                    )
                }
            }
        }
    }

    @Composable
    fun InputSection() {
        val focusRequester = remember { FocusRequester() }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            shadowElevation = 25.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colorScheme.background,
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                if(_microActive.value){
                    CommonComposables.CircleButton( R.drawable.keyboard, "Alterar modo de entrada") {
                        _showInputModeAlert.value = true
                    }
                    CommonComposables.ActionButton(
                        text = "Gravar áudio",
                        icon = R.drawable.microphone,
                        onClick = {
                            initSttService()
                            listen()
                            _showRecordAudioAlert.value = true
                        },
                        isActionStarter = true
                    )
                }else{
                    CommonComposables.CircleButton( R.drawable.microphone, "Alterar modo de entrada") {
                        _showInputModeAlert.value = true
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            value = _userMessage.value,
                            onValueChange = { _userMessage.value = it },
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
                    Image(
                        painter = painterResource(id = R.drawable.send_dark),
                        contentDescription = "Send",
                        modifier = Modifier
                            .clickable {
                                sendMessage(_userMessage.value)
                                _userMessage.value = ""
                            }
                            .scale(1.5F)
                    )
                }
            }
        }
    }

    @Composable
    fun TopBar(scaffoldState: ScaffoldState, scopeState: CoroutineScope){
        val focusManager = LocalFocusManager.current
        Row(modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.primary),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween){
            CommonComposables.ActionTransparentButton("Menu", R.drawable.menu, onClick = {
                focusManager.clearFocus()
                scopeState.launch { scaffoldState.drawerState.open() }
            }, true)
            CommonComposables.ActionTransparentButton("Ajuda", R.drawable.help, onClick = {/*something*/}, false)
        }
    }

    @Composable
    fun ToggleInputContent(){
        Text(text = "A trocar entre:",
            fontSize = Typography.bodyLarge.fontSize,
            fontWeight = Typography.bodyLarge.fontWeight,
            color = colorScheme.onBackground)
        CommonComposables.InputMode(
            if (_microActive.value) R.drawable.microphone else R.drawable.keyboard,
            if (_microActive.value) "Áudio" else "Escrita"
        )
        Image(painter = painterResource(R.drawable.resource_switch), contentDescription = "Alterar modo de entrada")
        CommonComposables.InputMode(
            if (_microActive.value) R.drawable.keyboard else R.drawable.microphone,
            if (_microActive.value) "Escrita" else "Áudio"
        )
        Text(alert.text,
            color = colorScheme.onBackground,
            fontSize = Typography.bodyLarge.fontSize,
            fontWeight = Typography.bodyLarge.fontWeight,
            lineHeight = Typography.bodyLarge.lineHeight,
            textAlign = TextAlign.Justify)
    }

    @Composable
    fun ProgressIndicator(){
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 25.dp),
            color = colorScheme.onSecondaryContainer,
            backgroundColor = colorScheme.secondaryContainer,
        )
    }
    @Composable
    fun RecordedAudioContent(){
        if(_userMessage.value.isEmpty()){
            Text(
                text = "Não foi capturada nenhuma mensagem, por favor tente novamente...",
                color = colorScheme.onBackground,
                fontSize = Typography.bodyLarge.fontSize,
                fontWeight = Typography.bodyLarge.fontWeight,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
        }else{
            Box(modifier = Modifier
                .background(color = colorScheme.background)
                .border(1.dp, colorScheme.onBackground, shape = RoundedCornerShape(25.dp))
                .fillMaxWidth()
                .height(200.dp),
            ){
                Text(
                    text = _userMessage.value,
                    color = colorScheme.onBackground,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                )
            }
        }
    }
    @Composable
    override fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
        if(!_microActive.value){
            _finishedLoading.value = STATE_DONE
            onDestroy()
        }else if(_finishedLoading.value != STATE_DONE){
            if(_finishedLoading.value != STATE_READY){
                LoadScreen()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(scaffoldState!!, scope!!)
            ChatSection(Modifier.weight(1f))
            InputSection()
            if(_showInputModeAlert.value){
                alert = alerts[ToggleInputModeAlert::class.simpleName.toString()]!!
                //CommonComposables.ShowAlertDialog(alert)
                CommonComposables.ShowAlertDialogWithContent(alert){
                    ToggleInputContent()
                }
            }
            if(_showAlertGeriatricQuestionnaire.value){
                alert = alerts[DepressionQuestionnaireAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            }else if(_showAlertOxfordQuestionnaire.value){
                alert = alerts[HappinessQuestionnaireAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            }
            if(_showRecordAudioAlert.value){
                alert = alerts[RecordAudioAlert::class.simpleName.toString()]!!
                CommonComposables.ShowAlertDialogWithContent(alert){
                    ProgressIndicator()
                }
            }
            if(_showRecordedResultAlert.value){
                alert = alerts[RecordedResultAlert::class.simpleName.toString()]!!
                CommonComposables.ShowAlertDialogWithContent(alert, _userMessage.value.isNotEmpty()){
                    RecordedAudioContent()
                }
            }
        }
    }
    override val activity: Activity
        get() = this
}

