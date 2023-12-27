package pt.ipleiria.estg.ciic.chatboternui

import android.app.Activity
import android.content.Context
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests

open class AccountBaseActivity : BaseActivity() {
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private var _confirmPassword: MutableState<String> = mutableStateOf("")
    private var isCreateAccountActivity: Boolean = false
    protected var email: MutableState<String> = mutableStateOf("")
    protected var password: MutableState<String> = mutableStateOf("")
    protected var response: MutableState<String> = mutableStateOf("")
    protected val httpRequests = HTTPRequests()
    protected val scope = CoroutineScope(Dispatchers.Main)

    @Override
    fun onCreateActivity(title: String, text: String, createAccount: Boolean, footerBehaviour: () -> Unit, oldActivity: Activity) {
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        ThemeState.isDarkThemeEnabled = sharedPreferences.getBoolean("theme_mode_is_dark", false)
        isCreateAccountActivity = createAccount
        setContent {
            ChatbotERNUITheme {
                MainSection(title, text, footerBehaviour, oldActivity)
            }
        }
    }

    @Composable
    fun MainSection(title: String, text: String, footerBehaviour:  () -> Unit, oldActivity: Activity){
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
                    CommonComposables.AccountHeader(title)
                    AccountScreen()
                    CommonComposables.AccountFooter(title,
                        text, {
                            footerBehaviour.invoke()
                        }, {

                            if(isCreateAccountActivity) {
                                utils.startDetailActivity(applicationContext, LoginActivity::class.java, oldActivity)
                            }else{
                                utils.startDetailActivity(applicationContext, CreateAccountActivity::class.java, oldActivity)
                            }
                        })
                }
                HandleDialogs()
            }
        }
    }

    @Composable
    fun AccountScreen() {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween){
            CommonComposables.TextFieldAccount(email,"Email",
                "Insira o seu email",
                R.drawable.email,
                "Email",
                _passwordHidden.value,
                togglePasswordVisibility = { togglePasswordVisibility() }
            )
            Spacer(modifier = Modifier.height(15.dp))
            CommonComposables.TextFieldAccount(password,
                "Palavra-passe",
                "Insira a palavra-passe",
                R.drawable.password,
                "Palavra-passe",
                _passwordHidden.value,
                togglePasswordVisibility = { togglePasswordVisibility() }
            )
            if(isCreateAccountActivity){
                Spacer(modifier = Modifier.height(15.dp))
                CommonComposables.TextFieldAccount(_confirmPassword,
                    "Confirme a palavra-passe",
                    "Repita a palavra-passe",
                    R.drawable.password,
                    "Palavra-passe",
                    _passwordHidden.value,
                    togglePasswordVisibility = { togglePasswordVisibility() }
                )
            }
            // Error message
            if(response.value.isNotEmpty()){
                Text(
                    text = response.value,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Override
    open fun submitAccountRequest(){
        if (!areFieldsValid()) return
        response.value = ""
    }
    private fun togglePasswordVisibility(){
        _passwordHidden.value = !_passwordHidden.value
    }

    private fun areFieldsValid(): Boolean {
        if(email.value.isEmpty() || password.value.isEmpty() || _confirmPassword.value.isEmpty()){
            response.value = "Por favor indique tanto o seu email como uma palavra-passe"
            return false
        }
        if(!utils.isEmailValid(email.value)){
            response.value = "O seu endereço de email é inválido"
            return false
        }
        if(isCreateAccountActivity && password.value != _confirmPassword.value){
            response.value = "As palavras-passe não correspondem"
            return false
        }
        return true
    }
    public fun handleRequestStatusCode(statusCode: String){
        when(statusCode){
            "409" -> {
                _showConnectivityError.value = true
                response.value = "Este email já está associado a uma conta"
            }
            "ECONNREFUSED" -> {
                _showConnectivityError.value = true
                response.value = "Oops! Conecte-se à internet por favor."
            }
        }
    }
}