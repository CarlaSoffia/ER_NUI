package pt.ipleiria.estg.ciic.chatboternui
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
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
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests
import pt.ipleiria.estg.ciic.chatboternui.utils.Others
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables

class CreateAccountActivity : ComponentActivity() {
    private var _email: MutableState<String> = mutableStateOf("")
    private var _password: MutableState<String> = mutableStateOf("")
    private var _confirmPassword: MutableState<String> = mutableStateOf("")
    private var _response: MutableState<String> = mutableStateOf("")
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private lateinit var sharedPreferences : SharedPreferences
    private val httpRequests = HTTPRequests()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val utils = Others()
    private var _showSuccessDialog: MutableState<Boolean> = mutableStateOf(false)
    private var _showConnectivityError: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        ThemeState.isDarkThemeEnabled = sharedPreferences.getBoolean("theme_mode_is_dark", false)
        setContent {
            ChatbotERNUITheme {
                MainSection()
            }
        }
    }

    private fun togglePasswordVisibility(){
        _passwordHidden.value = !_passwordHidden.value
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
                .background(colorScheme.background.copy(alpha = 0.8f))
            ){
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp, 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CommonComposables.AccountHeader("Criar conta")
                    CreateAccountScreen()
                    CommonComposables.AccountFooter("Criar conta",
                        "Já possui uma conta? Efetue o login", {
                        submitCreateAccount()
                        }, {
                            utils.startDetailActivity(applicationContext, LoginActivity::class.java, this@CreateAccountActivity)
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
                    "Conta criada com sucesso.",
                    _showSuccessDialog.value,
                    onClick = {
                        _showSuccessDialog.value = false
                        utils.startDetailActivity(applicationContext, LoginActivity::class.java, this@CreateAccountActivity)
                    },onDismissRequest = {
                        _showSuccessDialog.value = false
                    }
                )
            }
        }
    }

    @Composable
    fun CreateAccountScreen() {
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
            Spacer(modifier = Modifier.height(15.dp))
            CommonComposables.TextFieldAccount(_confirmPassword,
                "Confirme a palavra-passe",
                "Repita a palavra-passe",
                R.drawable.password,
                "Palavra-passe",
                _passwordHidden.value,
                togglePasswordVisibility = { togglePasswordVisibility() }
            )
            // Error message
            if(_response.value.isNotEmpty()){
                Text(
                    text = _response.value,
                    color = colorScheme.onError,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun submitCreateAccount() {
        if(_email.value.isEmpty() || _password.value.isEmpty() || _confirmPassword.value.isEmpty()){
            _response.value = "Por favor indique tanto o seu email como uma palavra-passe"
            return
        }
        if(!utils.isEmailValid(_email.value)){
            _response.value = "O seu endereço de email é inválido"
            return
        }
        if(_password.value != _confirmPassword.value){
            _response.value = "As palavras-passe não correspondem"
            return
        }

        _response.value = ""

        val bodyCreateAccount = JSONObject()
        bodyCreateAccount.put("email",_email.value)
        bodyCreateAccount.put("password",_password.value)
        bodyCreateAccount.put("role","client")

        scope.launch {
            val response = httpRequests.request("POST", "/clients", bodyCreateAccount.toString())
            val testStatusCode = response["status_code"].toString()
            try{
                if (testStatusCode == "201") {
                    _showSuccessDialog.value = true
                }
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                else { //409 Username already taken
                    _response.value = "Couldn't create account. Try again later."
                }
            }catch (ex : JSONException){
                Log.i("ERROR: ",ex.message.toString())
            }
        }
    }
}