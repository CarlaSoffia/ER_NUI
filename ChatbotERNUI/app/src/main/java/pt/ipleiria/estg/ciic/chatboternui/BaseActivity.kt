package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.models.MenuItem
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests
import pt.ipleiria.estg.ciic.chatboternui.utils.IAccountActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.Others
open class BaseActivity: ComponentActivity() {
    private lateinit var currentActivity: IBaseActivity
    protected var token: String = ""
    protected val utils = Others()
    private var _modeDark : MutableState<Boolean> = mutableStateOf(false)
    private var showConnectivityError: MutableState<Boolean> = mutableStateOf(false)
    protected lateinit var sharedPreferences : SharedPreferences
    protected var alertMessage: MutableState<String> = mutableStateOf("")
    protected val httpRequests = HTTPRequests()
    protected val scope = CoroutineScope(Dispatchers.Main)

    private fun onCreate(activity: Activity){
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        ThemeState.isDarkThemeEnabled = sharedPreferences.getBoolean("theme_mode_is_dark", false)
        if (sharedPreferences.getString("macAddress", "") == "") {
            utils.addStringToStore(
                sharedPreferences, "macAddress", utils.getAndroidMacAddress(activity)
            )
        }
        token = sharedPreferences.getString("access_token", "").toString()
    }

    @Override
    fun onCreateBaseActivity(oldActivity: IAccountActivity) {
        onCreate(oldActivity.activity)
        setContent {
            ChatbotERNUITheme {
                HandleDialogs()
            }
        }
    }
    @Override
    fun onCreateBaseActivityWithMenu(oldActivity: IBaseActivity) {
        onCreate(oldActivity.activity)
        currentActivity = oldActivity
        setContent {
            ChatbotERNUITheme {
                HandleDialogs()
                AppScreen()
            }
        }
    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun AppScreen()
    {
        val scaffoldState = rememberScaffoldState()
        val scopeState = rememberCoroutineScope()
        androidx.compose.material.Scaffold(
            scaffoldState = scaffoldState,
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            drawerContent = {
                Drawer()
            },
            //customizing the drawer. Will also share this shape below.
            drawerElevation = 90.dp
        ) {//Here goes the whole content of our Screen
            currentActivity.MainScreen(scaffoldState, scopeState)
        }
    }

    @Composable
    fun HandleDialogs(){
        if(showConnectivityError.value){
            CommonComposables.DialogConnectivity(
                onClick = {
                    showConnectivityError.value = false
                    finish()
                },onDismissRequest = {
                    showConnectivityError.value = false
                }
            )
        }
    }
    fun handleRequestStatusCode(statusCode: String, activity: Activity? = null){
        when(statusCode){
            "409" -> {
                showConnectivityError.value = true
                alertMessage.value = "Este email já está associado a uma conta"
            }
            "ECONNREFUSED" -> {
                showConnectivityError.value = true
            }
            // TODO - handle navigation for good codes
            else -> {
                showConnectivityError.value = false
                if(activity != null) utils.startDetailActivity(applicationContext,MainActivity::class.java, activity)
            }
        }
    }
    @Composable
    fun Drawer() {
        val menuItems = listOf(
            MenuItem(
                id = "profile",
                title = "Perfil",
                icon = R.drawable.profile,
                onClick = {},
                addDivider = false
            ),
            MenuItem(
                id = "mino",
                title = "Conversa com o Mimo",
                icon = R.drawable.email,
                onClick = {
                    utils.startDetailActivity(applicationContext, MainActivity::class.java, this)
                },
                addDivider = true
            ),
            MenuItem(
                id = "questionnaires",
                title = "Questionários",
                icon = R.drawable.questionnaires,
                onClick = {
                    utils.startDetailActivity(applicationContext, QuestionnairesActivity::class.java, this)
                },
                addDivider = true
            ),
            MenuItem(
                id = "mode",
                title = if (!_modeDark.value) "Modo escuro" else "Modo claro",
                icon = if (!_modeDark.value) R.drawable.dark else R.drawable.light,
                onClick = {
                    _modeDark.value = utils.changeMode(sharedPreferences)
                },
                addDivider = false
            ),
            MenuItem(
                id = "information",
                title = "Tutorial",
                icon = R.drawable.information,
                onClick = {},
                addDivider = true
            ),
            MenuItem(
                id = "logout",
                title = "Terminar sessão",
                icon = R.drawable.logout,
                onClick = {
                    utils.clearSharedPreferences(sharedPreferences)
                    ThemeState.isDarkThemeEnabled = false
                    utils.startDetailActivity(applicationContext,LoginActivity::class.java, this)
                },
                addDivider = false
            )
        )
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(0.dp, 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically){
                Image(
                    painter = painterResource(R.drawable.chatbot),
                    contentDescription = "Chatbot",
                    modifier = Modifier.size(75.dp)
                )
                Spacer(modifier = Modifier.width(25.dp))
                Text(
                    text = "Menu",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = Typography.titleLarge.fontSize
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(35.dp)
            ) {
                items(menuItems)
                { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                item.onClick()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = item.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = Typography.bodyLarge.fontSize,
                            fontWeight = Typography.bodyLarge.fontWeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if(item.addDivider){
                        Spacer(modifier = Modifier.height(15.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(15.dp))
                    }
                }
            }
        }
    }
}