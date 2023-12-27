package pt.ipleiria.estg.ciic.chatboternui

import android.os.Bundle
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme

class LoginActivity : AccountBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatbotERNUITheme {
                super.onCreateActivity("Login", "Não possui uma conta?", false, { submitAccountRequest() }, this)
            }
        }
    }
    override fun submitAccountRequest(){
        val bodyLogin = JSONObject()
        bodyLogin.put("email",email.value)
        bodyLogin.put("password",password.value)
        scope.launch {
            try{
                val responseRequest = httpRequests.request("POST", "/auth/login", bodyLogin.toString())
                val data = JSONObject(responseRequest["data"].toString())
                utils.addStringToStore(sharedPreferences,"email", email.value)
                utils.addStringToStore(sharedPreferences,"password", password.value)
                utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
                utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
                utils.startDetailActivity(applicationContext,MainActivity::class.java, this@LoginActivity)
            }catch (ex : JSONException){
                response.value = "Email ou password inválidos."
            }
        }
    }
}