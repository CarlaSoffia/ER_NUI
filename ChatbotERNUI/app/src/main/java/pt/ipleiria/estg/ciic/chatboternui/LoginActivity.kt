package pt.ipleiria.estg.ciic.chatboternui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.utils.IAccountActivity

class LoginActivity : IAccountActivity, AccountBaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatbotERNUITheme {
                super.onCreateActivity("Login", this)
            }
        }
    }
    override suspend fun accountRequestSubmit(): String {
        val bodyLogin = JSONObject()
        bodyLogin.put("email",email.value)
        bodyLogin.put("password",password.value)
        val responseDeferred = CompletableDeferred<JSONObject>()
        try {
            val job = scope.launch {
                try {
                    val response = httpRequests.request("POST", "/auth/login", bodyLogin.toString())
                    val data = JSONObject(response["data"].toString())
                    utils.addStringToStore(sharedPreferences,"email", email.value)
                    utils.addStringToStore(sharedPreferences,"password", password.value)
                    utils.addStringToStore(sharedPreferences,"access_token", data["access_token"].toString())
                    utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
                    responseDeferred.complete(response)
                } catch (ex: JSONException) {
                    responseDeferred.completeExceptionally(ex)
                }
            }
            job.join()
            val response = responseDeferred.await()
            return response["status_code"].toString()
        } catch (e: CancellationException) {
            // Handle cancellation exception if needed
            return "Cancellation Exception: ${e.message}"
        }
    }
    override val activity: Activity
        get() = this
}