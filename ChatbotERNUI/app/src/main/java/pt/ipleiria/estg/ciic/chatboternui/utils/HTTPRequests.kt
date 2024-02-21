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

private const val URL_API = "https://70e9-2001-8a0-f266-9800-4b95-f207-aacf-49e2.ngrok-free.app/api"
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
            val result = JSONObject()
            try{
                // Use the OkHttp client to make an asynchronous request
                val response = okHttpClient.newCall(request).execute()
                result.put("status_code", response.code)
                var data = JSONObject(response.body?.string()!!)
                try{
                    data = JSONObject(data.get("data").toString())
                }catch(ex: JSONException){
                    Log.i("Debug", "Error: ${ex.message}")
                }
                result.put("data", data)
            }catch (ex: IOException) {
                result.put("status_code", "503")
                result.put("data", "Oops! No Internet Connection")
            }
        }
    }
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
                result.put("data", data.get("data").toString())
            }catch (ex: Exception) {
                result.put("status_code", "ECONNREFUSED")
            }
        }
    }
}

