package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.material.ScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.IRequestActivity

open class AccountBaseActivity : IBaseActivity, BaseActivity() {
    private var _passwordHidden: MutableState<Boolean> = mutableStateOf(true)
    protected var email: MutableState<String> = mutableStateOf("")
    protected var password: MutableState<String> = mutableStateOf("")
    private lateinit var currentAccountActivity : IRequestActivity
    @Override
    fun onCreateActivity(title: String, accountActivity: IRequestActivity, baseActivity: IBaseActivity) {
        super.instantiateInitialData()
        super.onCreateBaseActivity(title, baseActivity)
        currentAccountActivity = accountActivity
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    override fun MainScreen(title: String?, scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
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
                    CommonComposables.AccountHeader(title!!)
                    AccountScreen()
                    CommonComposables.AccountFooter(title,{ accountRequest() })
                }
            }
        }
    }

    @Composable
    fun AccountScreen() {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween){
            TextFieldAccount(email,"Email",
                "Insira o seu email",
                R.drawable.email,
                "Email"
            )
            Spacer(modifier = Modifier.height(15.dp))
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
        }
    }
    @Composable
    fun TextFieldAccount(value: MutableState<String>, label: String, description: String, icon: Int, iconDescription: String){
        val isPassword = label=="Palavra-passe" || label=="Confirme a palavra-passe"
        TextField(
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onPrimary
            ),
            visualTransformation = if(isPassword && _passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
            value = value.value,
            onValueChange = { value.value = it },
            label = {
                Text(
                    text = label,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            placeholder = {
                Text(description,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = iconDescription,
                    tint = MaterialTheme.colorScheme.onPrimary)
            },
            trailingIcon = {if (isPassword)
                IconButton(onClick = { togglePasswordVisibility() }) {
                    Icon(
                        painter = painterResource(id = if(_passwordHidden.value) R.drawable.show else R.drawable.hide),
                        contentDescription = "Esconder/mostrar a palavra-pass",
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
    }
    private fun accountRequest(){
        if (!areFieldsValid()) return
        alertMessage.value = ""
        scope.launch {
            currentAccountActivity.apiRequest()
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
    override val activity: Activity
        get() = this
}