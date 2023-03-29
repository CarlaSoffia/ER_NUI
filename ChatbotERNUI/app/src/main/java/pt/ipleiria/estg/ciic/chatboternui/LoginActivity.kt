package pt.ipleiria.estg.ciic.chatboternui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.LightColorScheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests
import pt.ipleiria.estg.ciic.chatboternui.utils.Others

private const val URL_LARAVEL = "https://aalemotion.dei.estg.ipleiria.pt/api/auth/login"
class LoginActivity : ComponentActivity() {
    private var _username: MutableState<String> = mutableStateOf("")
    private var _password: MutableState<String> = mutableStateOf("")
    private var _response: MutableState<String> = mutableStateOf("")
    private val utils = Others()
    private val httpRequests = HTTPRequests()
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        setContent {
            ChatbotERNUITheme {
               if(sharedPreferences.getString("access_token","") != ""){
                   utils.addStringToStore(sharedPreferences,"macAddress", utils.getAndroidMacAddress(this))
                   utils.startDetailActivity(applicationContext,ConversationActivity::class.java)
                }else{
                   LoginScreen()
               }
            }

        }
    }
    private fun submitLogin(){

        if(_username.value.isEmpty() || _password.value.isEmpty()){
            _response.value = "Por favor preencha o Email e a Password"
            return
        }
        if(!utils.isEmailValid(_username.value)){
            _response.value = "O email é inválido, corriga por favor."
            return
        }
        val bodyLogin = JSONObject()
        bodyLogin.put("email",_username.value)
        bodyLogin.put("password",_password.value)
        scope.launch {
            val response = httpRequests.request("POST", URL_LARAVEL, bodyLogin.toString())
            val data = JSONObject(response["data"].toString())
            try{
                utils.addStringToStore(sharedPreferences,"username", _username.value)
                utils.addStringToStore(sharedPreferences,"password", _password.value)
                utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
                utils.startDetailActivity(applicationContext,ConversationActivity::class.java)
            }catch (ex : JSONException){
                _response.value = "Email ou password inválidos."
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chatbot ERNUI",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            TextField(
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = LightColorScheme.primary,
                    unfocusedBorderColor = LightColorScheme.primary
                ),
                value = _username.value,
                textStyle = TextStyle.Default.copy(fontSize = Typography.bodyLarge.fontSize),
                onValueChange = { _username.value = it },
                label = { Text(text = "Email", fontSize = Typography.bodyLarge.fontSize) },
                placeholder = {
                    Text("Insira o seu email", fontSize = Typography.bodyLarge.fontSize, color = LightColorScheme.surface)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            TextField(
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = LightColorScheme.primary,
                    unfocusedBorderColor = LightColorScheme.primary
                ),
                value = _password.value,
                onValueChange = { _password.value = it },
                label = { Text(text = "Password", fontSize = Typography.bodyLarge.fontSize) },
                placeholder = {
                    Text("Insira a sua Password", fontSize = Typography.bodyLarge.fontSize, color = LightColorScheme.surface)
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
            if(_response.value.isNotEmpty()){
                Text(
                    text = _response.value,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            Button(
                onClick = { submitLogin() },
                modifier = Modifier.width(200.dp)
            ) {
                Text(text = "Login",
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}