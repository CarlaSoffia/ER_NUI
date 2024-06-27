package pt.ipleiria.estg.ciic.chatboternui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.compose.material.ScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity

class SignUpActivity : IBaseActivity, BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.instantiateInitialData()
        super.onCreateBaseActivity(this)
    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    override fun MainScreen(scaffoldState: ScaffoldState?, scope: CoroutineScope?){
        Text(text = "dummy shit",
            fontSize = Typography.bodyLarge.fontSize,
            fontWeight = Typography.bodyLarge.fontWeight,
            color = MaterialTheme.colorScheme.onBackground)
    }


    override val activity: Activity
        get() = this
}