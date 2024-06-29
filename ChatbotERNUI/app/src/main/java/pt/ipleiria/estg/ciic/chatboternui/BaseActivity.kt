package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import pt.ipleiria.estg.ciic.chatboternui.models.MenuItem
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.ChatbotERNUITheme
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.CommonComposables
import pt.ipleiria.estg.ciic.chatboternui.utils.HTTPRequests
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity
import pt.ipleiria.estg.ciic.chatboternui.utils.Others
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.ChatbotUnavailableAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.DepressionQuestionnaireAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.HappinessQuestionnaireAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.IAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.IncorrectCredentialsAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.ServiceUnavailableAlert
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.SignOutAlert

open class BaseActivity: ComponentActivity() {
    private lateinit var currentActivity: IBaseActivity
    protected val utils = Others()
    private var _modeDark : MutableState<Boolean> = mutableStateOf(false)
    protected var showAlertDialog : MutableState<Boolean> = mutableStateOf(false)
    protected lateinit var sharedPreferences : SharedPreferences
    protected val httpRequests = HTTPRequests()
    protected val scope = CoroutineScope(Dispatchers.Main)
    protected var alerts : MutableMap<String, IAlert> = mutableMapOf()
    protected lateinit var alert : IAlert
    private val menuItems = listOf(
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
            icon = R.drawable.help,
            onClick = {},
            addDivider = true
        ),
        MenuItem(
            id = "logout",
            title = "Terminar sessão",
            icon = R.drawable.logout,
            onClick = {
                alert = alerts[SignOutAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            },
            addDivider = false
        )
    )
    fun instantiateInitialData(){
        sharedPreferences = getSharedPreferences("ERNUI", Context.MODE_PRIVATE)
        ThemeState.isDarkThemeEnabled = sharedPreferences.getBoolean("theme_mode_is_dark", false)
        initAlerts()
    }

    private fun initAlerts() {
        // Initialize IncorrectCredentialsAlert alert dialog
        val incorrectCredentialsAlert = IncorrectCredentialsAlert()
        incorrectCredentialsAlert.confirmButton.onClick = {
            showAlertDialog.value = false
        }
        alerts[IncorrectCredentialsAlert::class.simpleName.toString()] = incorrectCredentialsAlert

        // Initialize ChatbotUnavailableAlert alert dialog
        val chatbotUnavailableAlert = ChatbotUnavailableAlert()
        chatbotUnavailableAlert.confirmButton.onClick = {
            showAlertDialog.value = false
            finish()
        }
        alerts[ChatbotUnavailableAlert::class.simpleName.toString()] = chatbotUnavailableAlert

        // Initialize ServiceUnavailableAlert alert dialog
        val serviceUnavailableAlert = ServiceUnavailableAlert()
        serviceUnavailableAlert.confirmButton.onClick = {
            showAlertDialog.value = false
            finish()
        }
        alerts[ServiceUnavailableAlert::class.simpleName.toString()] = serviceUnavailableAlert

        // Initialize SignOutAlert alert dialog
        val signOutAlert = SignOutAlert()
        signOutAlert.confirmButton.onClick = {
            showAlertDialog.value = false
            utils.clearSharedPreferences(sharedPreferences)
            ThemeState.isDarkThemeEnabled = false
            utils.startActivity(applicationContext,SignInActivity::class.java, this)
        }
        signOutAlert.dismissButton.onClick = {
            showAlertDialog.value = false
        }
        alerts[SignOutAlert::class.simpleName.toString()] = signOutAlert

        // Initialize DepressionQuestionnaireAlert alert dialog
        alerts[DepressionQuestionnaireAlert::class.simpleName.toString()] = DepressionQuestionnaireAlert()

        // Initialize HappinessQuestionnaireAlert alert dialog
        alerts[HappinessQuestionnaireAlert::class.simpleName.toString()] = HappinessQuestionnaireAlert()
    }
    fun onCreateBaseActivity(activity : IBaseActivity) {
        currentActivity = activity
        setContent {
            ChatbotERNUITheme {
                HandleAlertDialog()
                currentActivity.MainScreen(null, null)
            }
        }
    }

    fun onCreateBaseActivityWithMenu(oldActivity: IBaseActivity) {
        currentActivity = oldActivity
        setContent {
            ChatbotERNUITheme {
                HandleAlertDialog()
                MainScreenWithMenu()
            }
        }
    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    private fun MainScreenWithMenu()
    {
        val scaffoldState = rememberScaffoldState()
        val scopeState = rememberCoroutineScope()
        androidx.compose.material.Scaffold(
            scaffoldState = scaffoldState,
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            drawerContent = {
                Menu(scaffoldState, scopeState)
            },
            //customizing the drawer. Will also share this shape below.
            drawerElevation = 90.dp
        ) {//Here goes the whole content of our Screen
            currentActivity.MainScreen( scaffoldState, scopeState)
        }
    }

    @Composable
    private fun HandleAlertDialog(){
        if(showAlertDialog.value){
            CommonComposables.ShowAlertDialog(alert)
        }
    }

    fun handleConnectivityError(statusCode: String): Boolean{
        when(statusCode){
            "403" -> {
                alert = alerts[IncorrectCredentialsAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            }
            "MIMO_ERROR" -> {
                alert = alerts[ChatbotUnavailableAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            }
            "UNKNOWN_HOST" ->{
                alert = alerts[ServiceUnavailableAlert::class.simpleName.toString()]!!
                showAlertDialog.value = true
            }
            else -> {
                showAlertDialog.value = false
            }
        }
        return showAlertDialog.value
    }
    @Composable
    private fun Menu(scaffoldState: ScaffoldState, scopeState: CoroutineScope) {

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Row(verticalAlignment = Alignment.CenterVertically){
                    Image(
                        painter = painterResource(id = R.drawable.menu_dark),
                        contentDescription = "Menu",
                        modifier = Modifier.scale(1.25F)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = "Menu",
                        fontSize = Typography.titleLarge.fontSize,
                        fontWeight = Typography.titleLarge.fontWeight,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Fechar",
                    modifier = Modifier.scale(1.25F)
                        .clickable { scopeState.launch { scaffoldState.drawerState.close() } }
                )
            }
            Image(
                painter = painterResource(id = R.drawable.menu_background),
                contentDescription = "Imagem do menu",
                modifier = Modifier.size(250.dp))
            for (item in menuItems){
                CommonComposables.MenuButton(item.title, item.icon, item.onClick, isActionStarter = true)
                if(item.addDivider){
                    Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 2.dp)
                }
            }
        }
    }
}