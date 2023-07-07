package pt.ipleiria.estg.ciic.chatboternui.utils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val URL_API = "http://34.18.41.153/api"
class HTTPRequests {
    suspend fun requestFormData(apiURL:String, jsonBody:JSONObject, token:String): JSONObject{
        return withContext(Dispatchers.IO) {
            val okHttpClient = OkHttpClient()
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            // Add form data parts based on the properties of the JSON object
            jsonBody.keys().forEach { key ->
                val value = jsonBody[key].toString()
                builder.addFormDataPart(key, value)
            }
            val body = builder.build()
            val request = Request.Builder()
                .url(URL_API+apiURL)
                .post(body)
                .addHeader("Authorization", "Bearer $token")
                .build()

            // Use the OkHttp client to make an asynchronous request
            val response = okHttpClient.newCall(request).execute()
            val result = JSONObject()
            result.put("status_code", response.code)
            var data = JSONObject(response.body?.string()!!)
            try{
                data = JSONObject(data.get("data").toString())
            }catch(ex: JSONException){
                Log.i("Debug", "Error: ${ex.message}")
            }
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
                try {
                    data.put("list", JSONArray(data.get("data").toString()))
                } catch (ex1: JSONException) {
                    Log.i("Debug", "Error: ${ex1.message}")
                }
            }
            result.put("data", data)
        }
    }
}

