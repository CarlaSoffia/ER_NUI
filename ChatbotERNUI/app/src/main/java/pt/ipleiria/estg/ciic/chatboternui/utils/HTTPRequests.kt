package pt.ipleiria.estg.ciic.chatboternui.utils
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.net.UnknownHostException

private const val URL_API = "https://974f-2001-8a0-f24e-5f00-1ae9-d2ec-237b-5682.ngrok-free.app/api"
open class HTTPRequests {

    protected val utils = Others()
    private suspend fun refreshAccessToken(sharedPreferences: SharedPreferences) : String {
        val expiresIn = sharedPreferences.getInt("expires_in", 0)
        if(!utils.isTokenExpired(utils.storeTokenExpiry(expiresIn))){
            return ""
        }
        val refreshToken = sharedPreferences.getString("refresh_token", "").toString()
        val refreshBody = JSONObject()
        refreshBody.put("refresh_token", refreshToken)
        refreshBody.put("type","MobileApp")
        // auth/refresh request with access_token and refresh_token
        val response = request(sharedPreferences,"POST", "/auth/refresh", refreshBody.toString())
        val data = JSONObject(response["data"].toString())
        val token = data["access_token"].toString()
        // Update shared preferences
        utils.addStringToStore(sharedPreferences,"access_token", token)
        utils.addStringToStore(sharedPreferences,"refresh_token", data["refresh_token"].toString())
        utils.addIntToStore(sharedPreferences,"expires_in", data["expires_in"] as Int)
        return token
    }
    suspend fun request(sharedPreferences: SharedPreferences, requestMethod:String,apiURL:String,body:String=""): JSONObject{
        return withContext(Dispatchers.IO) {
            val isAuth = apiURL.contains("login") || apiURL.contains("activateClient") || apiURL.contains("clients")
            val isRefresh = apiURL.contains("refresh")
            val token = sharedPreferences.getString("access_token", "").toString()

            val okHttpClient = OkHttpClient()
            val request: Request = if (requestMethod != "GET" && !isAuth) {
                val requestBody = body.toRequestBody()
                Request.Builder()
                    .method(requestMethod, requestBody)
                    .url(URL_API+apiURL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
            } else if (requestMethod == "POST" || requestMethod == "PATCH") {
                val requestBody = body.toRequestBody()
                Request.Builder()
                    .method(requestMethod, requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .url(URL_API+apiURL)
                    .build()
            } else {
                Request.Builder()
                    .url(URL_API+apiURL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
            }
            val result = JSONObject()
            var response : Response
            var attempts = 3
            var data = JSONObject()
            try{
               do {
                   // Use the OkHttp client to make an asynchronous request
                   response = okHttpClient.newCall(request).execute()
                   if(response.isSuccessful){
                       break
                   }
                   attempts -= 1
                   if(!isAuth && !isRefresh){
                       refreshAccessToken(sharedPreferences)
                   }
                } while (attempts > 0)
                data = JSONObject(response.body?.string()!!)
                result.put("status_code", response.code)
                if(isAuth || isRefresh){
                    result.put("data", data.toString())
                }else{
                    result.put("data", data.get("data").toString())
                }
            }catch (ex: Exception) {
                if(data.has("message")){
                    result.put("status_code", "MIMO_ERROR")
                }else{
                    result.put("status_code", "UNKNOWN_HOST") // Unavailable service
                }
            }
        }
    }
}

