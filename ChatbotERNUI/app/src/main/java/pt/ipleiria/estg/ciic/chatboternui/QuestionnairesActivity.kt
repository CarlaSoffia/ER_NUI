package pt.ipleiria.estg.ciic.chatboternui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import pt.ipleiria.estg.ciic.chatboternui.models.QuestionnaireType
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity

class QuestionnairesActivity : IBaseActivity, BaseActivity() {

    private var _questionnaireSelected : MutableState<String> = mutableStateOf("")
    private var _questionnaireTypes = mutableStateListOf<QuestionnaireType>()
    private var _showConnectivityError: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        getQuestionnairesTypes()
        super.onCreateBaseActivityWithMenu(this)
    }



    @Composable
    fun TopBar(title: String, scaffoldState: ScaffoldState, scopeState: CoroutineScope){
        val focusManager = LocalFocusManager.current
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(10.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround) {
            Icon(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = "Botão Menu",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clickable {
                        focusManager.clearFocus()
                        scopeState.launch { scaffoldState.drawerState.open() }
                    }
                    .size(30.dp)
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.3f))
            Text(title,
                fontSize = Typography.titleLarge.fontSize,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
    }

    @Composable
    fun QuestionnaireTypeItem(questionnaireType: QuestionnaireType,
                              modifier: Modifier = Modifier
    ) {
        val bubbleShape = RoundedCornerShape(10.dp)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(10.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    bubbleShape
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(
                text = questionnaireType.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = Typography.titleMedium.fontSize,
                fontWeight = Typography.titleMedium.fontWeight
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.5f))
            Text(
                text = "Pontuação mínima: ${questionnaireType.pointsMin}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = Typography.bodyMedium.fontSize,
                fontWeight = Typography.bodyMedium.fontWeight
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.5f))
            Text(
                text = "Pontuação máxima: ${questionnaireType.pointsMax}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = Typography.bodyMedium.fontSize,
                fontWeight = Typography.bodyMedium.fontWeight
            )
        }
    }

    @Composable
    fun DropDownMenu(modifier: Modifier = Modifier) {
        val expanded = remember { mutableStateOf(false) }
        Box(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(15.dp, 0.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Selecione um questionário",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = Typography.titleMedium.fontSize
                )
                Spacer(modifier = Modifier.fillMaxWidth(0.3f))
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Detalhes"
                    )
                } // fix the layout of this
            }
        }
        Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(0.dp, 10.dp)
                ) {
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                _questionnaireTypes.forEach {
                    DropdownMenuItem(
                        content = { Text(it.displayName) },
                        onClick = { _questionnaireSelected.value = it.name }
                    )
                }
            }
        }
    }
    @Composable
    override fun MainScreen(title: String?, scaffoldState: ScaffoldState?, scope: CoroutineScope?) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            TopBar("EmoCare", scaffoldState!!, scope!!)
            DropDownMenu(Modifier.weight(1f))
            if(_questionnaireSelected.value != ""){
                val questionnaire = _questionnaireTypes.first { q -> q.name == _questionnaireSelected.value }
                QuestionnaireTypeItem(questionnaire, Modifier.weight(1f));
            }
        }
    }

    private fun getQuestionnairesTypes() {
        _questionnaireTypes = mutableStateListOf()
        scope.launch {
            val response = httpRequests.request("GET", "/questionnairesTypes", token = token)
            try {
                val data = JSONObject(response["data"].toString())
                val questionnaireTypes = JSONArray(data["list"].toString())

                for (i in 0 until questionnaireTypes.length()) {
                    val questionnaireType = JSONObject(questionnaireTypes[i].toString())
                    _questionnaireTypes.add(
                        QuestionnaireType(name = questionnaireType["name"].toString(),
                            displayName = questionnaireType["display_name"].toString(),
                            pointsMin = questionnaireType["points_min"].toString().toInt(),
                            pointsMax = questionnaireType["points_max"].toString().toInt())
                    )
                }
            } catch (ex: JSONException) {
                if(response["status_code"].toString() == "503"){
                    _showConnectivityError.value = true
                }
                Log.i("Debug", "Error: ${ex.message}")
            }
        }
    }
    override val activity: Activity
        get() = this
}