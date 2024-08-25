package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class SignUpActivity : IBaseActivity, BaseActivity() {
    private var passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    private var name: MutableState<String> = mutableStateOf("")
    private var birthday: MutableState<String> = mutableStateOf("")
    private var email: MutableState<String> = mutableStateOf("")
    private var password: MutableState<String> = mutableStateOf("")
    private var errorMsg: MutableState<String> = mutableStateOf("")
    private var showingPartOneModal: MutableState<Boolean> = mutableStateOf(true)
    private var formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    private var today : LocalDate = LocalDate.now()
    private lateinit var mediaPlayer: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        super.onCreateBaseActivity(this)
    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    override fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?){
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            CommonComposables.HeaderWithTitleAndImage("Bem-vindo ao Mimo,\n" +
                    "vamos criar a sua conta!", R.drawable.session_background)

            if(showingPartOneModal.value){
                TextFieldAccount(name,"Nome",
                    "Insira o seu nome",
                    R.drawable.name
                )
                TextFieldAccount(birthday,"Aniversário",
                    "aaaa/mm/dd",
                    R.drawable.birthdate
                )
            }else{
                TextFieldAccount(email,"Email",
                    "Insira o seu email",
                    R.drawable.email
                )
                TextFieldAccount(password,
                    "Palavra-passe",
                    "Insira a palavra-passe",
                    R.drawable.password
                )
            }
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
            CommonComposables.ActionButton(
                                            if(showingPartOneModal.value ) "Continuar"
                                            else "Criar conta",
                                            if(showingPartOneModal.value ) R.drawable.next
                                            else R.drawable.login,
                                            {
                                                if(showingPartOneModal.value ) areFieldsValid()
                                                else accountRequest()
                                            }, true)
            CommonComposables.LinkText("Já tem uma conta?") {
                utils.startActivity(
                    applicationContext,
                    SignInActivity::class.java,
                    activity
                )
            }
        }
    }
    private fun accountRequest(){
        if (!areFieldsValid()) return
        errorMsg.value = ""
        val bodySignIn = JSONObject()
        bodySignIn.put("email", email.value)
        bodySignIn.put("password", password.value)
        bodySignIn.put("birthdate", birthday.value)
        bodySignIn.put("name", name.value)
       scope.launch {
            val response = httpRequests.request(sharedPreferences, "POST", "/clients", bodySignIn.toString())
            if(handleConnectivityError(response["status_code"].toString()))  {
                mediaPlayer = utils.playSound(R.raw.error, applicationContext)
                return@launch
            }
            mediaPlayer = utils.playSound(R.raw.success, applicationContext)
            utils.addStringToStore(sharedPreferences, "email", email.value)
            utils.addStringToStore(sharedPreferences, "password", password.value)
            utils.addBooleanToStore(sharedPreferences, "showActivateAccountModal", true)
            utils.startActivity(applicationContext, SignInActivity::class.java, activity)
        }
    }
    @Composable
    fun TextFieldAccount(value: MutableState<String>, label: String, description: String, icon: Int){
        val isBirthDate = icon == R.drawable.birthdate
        val isPassword = icon == R.drawable.password
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val locale = Locale("pt", "PT")
        Locale.setDefault(locale)

        // Ensure the configuration is updated for the current context
        val config = LocalContext.current.resources.configuration
        config.setLocale(locale)
        LocalContext.current.resources.updateConfiguration(config, LocalContext.current.resources.displayMetrics)

        val datePickerDialog = DatePickerDialog(
            LocalContext.current,
            R.style.DatePickerTheme,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                birthday.value = "$selectedYear/${selectedMonth + 1}/$selectedDayOfMonth"
            }, year, month, day
        )
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
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    textColor = MaterialTheme.colorScheme.onBackground
                ),
                visualTransformation = if(isPassword && passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
                value = value.value,
                onValueChange = { value.value = it },
                placeholder = {
                    Text(description,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    if(isBirthDate){
                       IconButton(onClick = {
                           datePickerDialog.show()
                        }) {
                           Image(
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = "Escolher uma data")
                        }
                    }
                    if (isPassword){
                    IconButton(onClick = { togglePasswordVisibility() }) {
                        Image(
                            painter = painterResource(id = if(passwordHidden.value) R.drawable.show else R.drawable.hide),
                            contentDescription = "Esconder/mostrar a palavra-passe")
                    }
                }
                },
                readOnly = isBirthDate,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(25.dp)
            )
        }
    }
    private fun areFieldsValid(): Boolean {
        if(showingPartOneModal.value){
            if(name.value.isEmpty() || name.value.isBlank()){
                errorMsg.value = "Insira o seu nome"
                return false
            }
            if(birthday.value.isEmpty() || birthday.value.isBlank()){
                errorMsg.value = "Indique o seu aniversário"
                return false
            }

            val date = LocalDate.parse(birthday.value, formatter)
            if(!date.isBefore(today)){
                errorMsg.value = "O seu aniversário não pode ser hoje nem no futuro"
                return false
            }
            mediaPlayer = utils.playSound(R.raw.success, applicationContext)
            showingPartOneModal.value = false
        }else{
            if(!utils.isEmailValid(email.value)){
                errorMsg.value = "O seu endereço de email é inválido"
                return false
            }
            if(password.value.isEmpty() || password.value.isBlank()){
                errorMsg.value = "Insira a sua palavra-passe"
                return false
            }
            if(password.value.length < 6){
                errorMsg.value = "A palavra-passe é muito curta"
                return false
            }
        }
        return true
    }
    private fun togglePasswordVisibility(){
        passwordHidden.value = !passwordHidden.value
    }
    override val activity: Activity
        get() = this
    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources when activity is destroyed
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}