package pt.ipleiria.estg.ciic.chatboternui

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.utils.IRequestActivity

class LoginActivity : IRequestActivity, AccountBaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreateActivity("Login", this, this)
    }
    override suspend fun apiRequest() {
        val bodyLogin = JSONObject()
        bodyLogin.put("email",email.value)
        bodyLogin.put("password",password.value)
        scope.launch {
            val response = httpRequests.request("POST", "/auth/login", bodyLogin.toString())
            if(handleConnectivityError(response["status_code"].toString(), activity)) return@launch
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