package pt.ipleiria.estg.ciic.chatboternui.utils

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

interface IBaseActivity {
    val activity: Activity
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?)
}