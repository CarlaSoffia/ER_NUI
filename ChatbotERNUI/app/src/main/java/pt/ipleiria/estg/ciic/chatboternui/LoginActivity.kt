package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ScaffoldState
import androidx.compose.material.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity

class LoginActivity : IBaseActivity, BaseActivity(){
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private var email: MutableState<String> = mutableStateOf("")
    private var password: MutableState<String> = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreateBaseActivity(this)
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    override fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
        Box (modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)){
            Column(
                Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                CommonComposables.HeaderWithTitleAndImage("Bem vindo de volta ao Mimo!", R.drawable.session_background)
                TextFieldAccount(email,"Email",
                    "Insira o seu email",
                    R.drawable.email,
                    "Email"
                )
                TextFieldAccount(password,
                    "Palavra-passe",
                    "Insira a palavra-passe",
                    R.drawable.password,
                    "Palavra-passe"
                )
                // Error message
                if(alertMessage.value.isNotEmpty()){
                    Text(
                        text = alertMessage.value,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CommonComposables.ActionButton("Iniciar sessão",R.drawable.login,{ accountRequest() }, true)
                CommonComposables.LinkText("Não tem uma conta?", {})
            }
        }
    }
    
    @Composable
    fun TextFieldAccount(value: MutableState<String>, label: String, description: String, icon: Int, iconDescription: String){
        val isPassword = label=="Palavra-passe" || label=="Confirme a palavra-passe"
        Row(horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 0.dp)
        ){
            Image(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.scale(1.25F))
            Spacer(modifier = Modifier.width(15.dp))
            Text(text = label,
                fontSize = Typography.bodyLarge.fontSize,
                fontWeight = Typography.bodyLarge.fontWeight,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 0.dp)
        ){
            OutlinedTextField(
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor =MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    textColor = MaterialTheme.colorScheme.onBackground
                ),
                visualTransformation = if(isPassword && _passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
                value = value.value,
                onValueChange = { value.value = it },
                placeholder = {
                    Text(description,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {if (isPassword)
                    IconButton(onClick = { togglePasswordVisibility() }) {
                        Icon(
                            painter = painterResource(id = if(_passwordHidden.value) R.drawable.show else R.drawable.hide),
                            contentDescription = "Esconder/mostrar a palavra-pass",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(25.dp)
            )
        }
    }
    private fun accountRequest(){
        if (!areFieldsValid()) return
        alertMessage.value = ""
        scope.launch {
            apiRequest()
        }
    }

    private fun togglePasswordVisibility(){
        _passwordHidden.value = !_passwordHidden.value
    }

    private fun areFieldsValid(): Boolean {
        if(!utils.isEmailValid(email.value)){
            alertMessage.value = "O seu endereço de email é inválido"
            return false
        }
        return true
    }

    suspend fun apiRequest() {
        val bodyLogin = JSONObject()
        bodyLogin.put("email",email.value)
        bodyLogin.put("password",password.value)
        bodyLogin.put("type","MobileApp")
        scope.launch {
            val response = httpRequests.request(sharedPreferences, "POST", "/auth/login", bodyLogin.toString())
            if(handleConnectivityError(response["status_code"].toString())) return@launch
            val data = JSONObject(response["data"].toString())
            utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
            utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
            utils.addIntToStore(sharedPreferences,"expires_in", data["expires_in"] as Int)
            // Reset the values
            email.value = ""
            password.value = ""
            utils.startDetailActivity(applicationContext, MainActivity::class.java, activity)
        }

    }
    override val activity: Activity
        get() = this
}