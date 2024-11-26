package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ScaffoldState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.ActivateAccountAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.SignOutAlert

class SignInActivity : IBaseActivity, BaseActivity(){
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private var email: MutableState<String> = mutableStateOf("")
    private var password: MutableState<String> = mutableStateOf("")
    private var errorMsg: MutableState<String> = mutableStateOf("")
    private var showActivateAccountModal: MutableState<Boolean> = mutableStateOf(false)
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        showActivateAccountModal.value = sharedPreferences.getBoolean("showActivateAccountModal", false)
        super.onCreateBaseActivity(this)
        if(showActivateAccountModal.value){
            // Initialize SignOutAlert alert dialog
            val activateAccountAlert = ActivateAccountAlert()
            activateAccountAlert.confirmButton.onClick = {
                activateAccount()
            }
            activateAccountAlert.dismissButton.onClick = {
                finish()
            }
            alerts[ActivateAccountAlert::class.simpleName.toString()] = activateAccountAlert
        }
    }
    private fun activateAccount(){
        val activateAccount = JSONObject()
        val email = sharedPreferences.getString( "email", "")
        val password = sharedPreferences.getString( "password", "")
        activateAccount.put("email", email)
        activateAccount.put("password", password)
        scope.launch {
            val response = httpRequests.request(sharedPreferences, "PATCH", "/auth/activateClient", activateAccount.toString())
            if(handleConnectivityError(response["status_code"].toString())) {
                mediaPlayer = utils.playSound(R.raw.error, applicationContext)
                return@launch
            }
            mediaPlayer = utils.playSound(R.raw.success, applicationContext)
            utils.removeFromStore(sharedPreferences, "email")
            utils.removeFromStore(sharedPreferences, "password")
            showActivateAccountModal.value = false
            utils.addBooleanToStore(sharedPreferences, "showActivateAccountModal", false)
        }


    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    override fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
        if(showActivateAccountModal.value){
            CommonComposables.ShowAlertDialog(alert = alerts[ActivateAccountAlert::class.simpleName.toString()]!!)
        }
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                CommonComposables.HeaderWithTitleAndImage("Bem vindo de volta ao Mimo!", R.drawable.session_background)
                TextFieldAccount(email,"Email",
                    "Insira o seu email",
                    R.drawable.email
                )
                TextFieldAccount(password,
                    "Palavra-passe",
                    "Insira a palavra-passe",
                    R.drawable.password
                )
                // Error message
                if(errorMsg.value.isNotEmpty()){
                    Text(
                        text = errorMsg.value,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CommonComposables.ActionButton("Iniciar sessão",R.drawable.login,{ accountRequest() }, true)
                CommonComposables.LinkText("Não tem uma conta?") {
                    utils.startActivity(
                        applicationContext,
                        SignUpActivity::class.java,
                        activity
                    )
                }
            }
    }
    
    @Composable
    fun TextFieldAccount(value: MutableState<String>, label: String, description: String, icon: Int){
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
                        Image(
                            painter = painterResource(id = if(_passwordHidden.value) R.drawable.show else R.drawable.hide),
                            contentDescription = "Esconder/mostrar a palavra-passe")
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
        errorMsg.value = ""
        scope.launch {
            apiRequest()
        }
    }

    private fun togglePasswordVisibility(){
        _passwordHidden.value = !_passwordHidden.value
    }

    private fun areFieldsValid(): Boolean {
        if(!utils.isEmailValid(email.value)){
            errorMsg.value = "O seu endereço de email é inválido"
            return false
        }
        if(password.value.isEmpty() || password.value.isBlank()){
            errorMsg.value = "Insira a sua palavra-passe"
            return false
        }
        return true
    }

    private suspend fun apiRequest() {
        val bodyLogin = JSONObject()
        bodyLogin.put("email",email.value)
        bodyLogin.put("password",password.value)
        bodyLogin.put("type","MobileApp")
        scope.launch {
            val response = httpRequests.request(sharedPreferences, "POST", "/auth/login", bodyLogin.toString())
            if(handleConnectivityError(response["status_code"].toString())) {
                mediaPlayer = utils.playSound(R.raw.error, applicationContext)
                return@launch
            }
            mediaPlayer = utils.playSound(R.raw.success, applicationContext)
            val data = JSONObject(response["data"].toString())
            utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
            utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
            utils.addIntToStore(sharedPreferences,"expires_in", data["expires_in"] as Int)
            utils.startActivity(applicationContext, MainActivity::class.java, activity)
        }

    }
    override val activity: Activity
        get() = this

    override fun onDestroy() {
        super.onDestroy()
        // Safely handle the mediaPlayer
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (e: IllegalStateException) {
                // Log or handle the error if the media player is in an invalid state
                e.printStackTrace()
            } finally {
                it.release()
            }
        }
        mediaPlayer = null
    }
}