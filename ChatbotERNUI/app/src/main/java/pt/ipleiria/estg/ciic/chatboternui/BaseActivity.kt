package pt.ipleiria.estg.ciic.chatboternui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.Others

open class BaseActivity : ComponentActivity() {
    protected val utils = Others()
    protected lateinit var sharedPreferences : SharedPreferences
    protected var _showConnectivityError: MutableState<Boolean> = mutableStateOf(false)
    @Override
     fun onCreateBaseActivity() {
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        ThemeState.isDarkThemeEnabled = sharedPreferences.getBoolean("theme_mode_is_dark", false)
        if (sharedPreferences.getString("macAddress", "") == "") {
            utils.addStringToStore(
                sharedPreferences, "macAddress", utils.getAndroidMacAddress(this)
            )
        }
        if(sharedPreferences.getString("access_token", "").toString() == ""){
            utils.startDetailActivity(applicationContext,LoginActivity::class.java, this)
            return
        }
    }

    @Composable
    fun HandleDialogs(){
        if(_showConnectivityError.value){
            CommonComposables.DialogConnectivity(
                onClick = {
                    _showConnectivityError.value = false
                    finish()
                },onDismissRequest = {
                    _showConnectivityError.value = false
                }
            )
        }
    }
}