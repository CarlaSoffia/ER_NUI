package pt.ipleiria.estg.ciic.chatboternui.utils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

private const val URL_API = "https://1f58-2001-8a0-f24e-5f00-1ae9-d2ec-237b-5682.ngrok-free.app/api"
class HTTPRequests {
    suspend fun request(requestMethod:String,apiURL:String,body:String="",token:String=""): JSONObject{
        return withContext(Dispatchers.IO) {
            val isAuth = apiURL.contains("login")
            if (!isAuth && token.isEmpty()) {
                throw IllegalArgumentException("[Error] - $requestMethod Request must have a token");
            }
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
            } else if (requestMethod == "POST") {
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
            try{
                // Use the OkHttp client to make an asynchronous request
                val response = okHttpClient.newCall(request).execute()
                val data = JSONObject(response.body?.string()!!)
                result.put("status_code", response.code)
                if(isAuth){
                    result.put("data", data.toString())
                }else{
                    result.put("data", data.get("data").toString())
                }
            }catch (ex: Exception) {
                result.put("status_code", ex.message)
            }
        }
    }
}

