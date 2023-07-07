package pt.ipleiria.estg.ciic.chatboternui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests
import pt.ipleiria.estg.ciic.chatboternui.utils.Others

class LoginActivity : ComponentActivity() {
    private var _email: MutableState<String> = mutableStateOf("")
    private var _password: MutableState<String> = mutableStateOf("")
    private var _response: MutableState<String> = mutableStateOf("")
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private var _showSuccessDialog: MutableState<Boolean> = mutableStateOf(false)
    private var _showConnectivityError: MutableState<Boolean> = mutableStateOf(false)
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
                   MainSection()
               }
            }

        }
    }
    private fun togglePasswordVisibility(){
        _passwordHidden.value = !_passwordHidden.value
    }

    private fun submitLogin(){
        if(_email.value.isEmpty() || _password.value.isEmpty()){
            _response.value = "Por favor indique tanto o seu email como uma palavra-passe"
            return
        }
        if(!utils.isEmailValid(_email.value)){
            _response.value = "O seu endereço de email é inválido"
            return
        }
        _response.value = ""

        val bodyLogin = JSONObject()
        bodyLogin.put("email",_email.value)
        bodyLogin.put("password",_password.value)
        scope.launch {
            val response = httpRequests.request("POST", "/auth/login", bodyLogin.toString())
            val data = JSONObject(response["data"].toString())
            try{
                utils.addStringToStore(sharedPreferences,"email", _email.value)
                utils.addStringToStore(sharedPreferences,"password", _password.value)
                utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
                utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
                utils.startDetailActivity(applicationContext,ConversationActivity::class.java)
            }catch (ex : JSONException){
                _response.value = "Email ou password inválidos."
            }
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun MainSection(){
        Box (modifier = Modifier.fillMaxSize()) {
            Image(painter = painterResource (id = R.drawable.background),
                contentDescription = "Imagem de fundo",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp),
                contentScale = ContentScale.Crop
            )
            Box (modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(
                    RoundedCornerShape(15.dp)
                )
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            ){
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp, 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CommonComposables.AccountHeader("Login")
                    LoginScreen()
                    CommonComposables.AccountFooter("Login",
                        "Não possui uma conta?", {
                            submitLogin()
                        }, {
                            utils.startDetailActivity(applicationContext, CreateAccountActivity::class.java)
                        })
                }
                CommonComposables.DialogConnectivity(_showConnectivityError.value,
                    onClick = {
                        _showConnectivityError.value = false
                        finish()
                    },onDismissRequest = {
                        _showConnectivityError.value = false
                    }
                )
                CommonComposables.SuccessModal(
                    "Login efetuado com sucesso.",
                    _showSuccessDialog.value,
                    onClick = {
                        _showSuccessDialog.value = false
                        utils.startDetailActivity(applicationContext, ConversationActivity::class.java)
                    },onDismissRequest = {
                        _showSuccessDialog.value = false
                    }
                )
            }
        }
    }

    @Composable
    fun LoginScreen() {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween){
            CommonComposables.TextFieldAccount(_email,"Email",
                "Insira o seu email",
                R.drawable.email,
                "Email",
                _passwordHidden.value,
                togglePasswordVisibility = { togglePasswordVisibility() }
            )
            Spacer(modifier = Modifier.height(15.dp))
            CommonComposables.TextFieldAccount(_password,
                "Palavra-passe",
                "Insira a palavra-passe",
                R.drawable.password,
                "Palavra-passe",
                _passwordHidden.value,
                togglePasswordVisibility = { togglePasswordVisibility() }
            )
            // Error message
            if(_response.value.isNotEmpty()){
                Text(
                    text = _response.value,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

}