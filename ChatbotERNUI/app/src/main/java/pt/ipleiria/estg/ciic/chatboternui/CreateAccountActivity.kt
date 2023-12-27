package pt.ipleiria.estg.ciic.chatboternui
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme

class CreateAccountActivity : AccountBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatbotERNUITheme {
                super.onCreateActivity("Criar conta", "Já possui uma conta? Efetue o login",  true, { submitAccountRequest() }, this)
            }
        }
    }
    override fun submitAccountRequest() {
        val bodyCreateAccount = JSONObject()
        bodyCreateAccount.put("email",email.value)
        bodyCreateAccount.put("password",password.value)
        bodyCreateAccount.put("role","client")

        scope.launch {
            try{
            val responseRequest = httpRequests.request("POST", "/clients", bodyCreateAccount.toString())
                handleRequestStatusCode(responseRequest["status_code"].toString())
            }catch (ex : JSONException){
                Log.i("ERROR: ",ex.message.toString())
            }
        }
    }
}