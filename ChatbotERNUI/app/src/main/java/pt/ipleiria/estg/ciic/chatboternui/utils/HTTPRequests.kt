package pt.ipleiria.estg.ciic.chatboternui.utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject


class HTTPRequests {
    private val utils = Others()

    suspend fun request(requestMethod:String,apiURL:String,body:String="",token:String=""): JSONObject{
        return withContext(Dispatchers.IO) {
            if (requestMethod != "GET" && body.isEmpty()) {
                throw IllegalArgumentException("[Error] - $requestMethod Request must have a body");
            }
            val isRasa = apiURL.contains("webhooks")
            val isAuth = apiURL.contains("login")
            if (!isRasa && !isAuth && token.isEmpty()) {
                throw IllegalArgumentException("[Error] - $requestMethod Request must have a token");
            }
            val okHttpClient = OkHttpClient()
            val request: Request = if (isRasa) {
                val requestBody = body.toRequestBody()
                Request.Builder()
                    .method(requestMethod, requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .url(apiURL)
                    .build()
            } else if (requestMethod != "GET" && !isAuth) {
                val requestBody = body.toRequestBody()
                Request.Builder()
                    .method(requestMethod, requestBody)
                    .url(apiURL)
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
                    .url(apiURL)
                    .build()
            } else {
                Request.Builder()
                    .url(apiURL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
            }
            // Use the OkHttp client to make an asynchronous request
            okHttpClient.newCall(request)
            val response = okHttpClient.newCall(request).execute()
            val result = JSONObject()
            result.put("status_code", response.code)
            var data = JSONObject(response.body?.string()!!)
            try{
                data = JSONObject(data.get("data").toString())
            }catch(ex: JSONException){
                // Do nothing, we are in the Auth Login situation
            }
            result.put("data", data)
        }
    }
}

