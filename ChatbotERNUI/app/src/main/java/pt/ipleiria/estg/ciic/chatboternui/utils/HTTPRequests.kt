package pt.ipleiria.estg.ciic.chatboternui.utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val URL_API = "http://34.18.41.153/api"
private const val WEBHOOK_RASA = "http://34.18.41.153/webhooks/rest/webhook"
class HTTPRequests {
    suspend fun requestRasa(body:String): JSONObject{
        return withContext(Dispatchers.IO) {
            if (body.isEmpty()) {
                throw IllegalArgumentException("[Error] - POST Request must have a body");
            }
            val timeoutMillis = 10000L
            val requestBody = body.toRequestBody()
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .method("POST", requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .url(WEBHOOK_RASA)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val result = JSONObject()

            result.put("status_code", response.code)
            val data = JSONArray(response.body?.string()!!)

            result.put("data", data)
        }
    }
    suspend fun request(requestMethod:String,apiURL:String,body:String="",token:String=""): JSONObject{
        return withContext(Dispatchers.IO) {
            if (requestMethod != "GET" && body.isEmpty()) {
                throw IllegalArgumentException("[Error] - $requestMethod Request must have a body");
            }
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
            // Use the OkHttp client to make an asynchronous request
            val response = okHttpClient.newCall(request).execute()
            val result = JSONObject()
            result.put("status_code", response.code)
            var data = JSONObject(response.body?.string()!!)
            try{
                data = JSONObject(data.get("data").toString())
            }catch(ex: JSONException){
                // Do nothing, we are in the Auth Login situation
                println(ex.message)
            }
            result.put("data", data)
        }
    }
}

