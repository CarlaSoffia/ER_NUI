package pt.ipleiria.estg.ciic.chatboternui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.ipleiria.estg.ciic.chatboternui.R
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography

object CommonComposables {

    @Composable
    fun DialogConnectivity(onClick: () -> Unit, onDismissRequest: () -> Unit , title: String , message : String){
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.error),
                    contentDescription = "Erro",
                    modifier = Modifier.size(80.dp))
            },
            onDismissRequest = { onDismissRequest() },
            title = { Text(title,
                color = colorScheme.onPrimary,
                fontSize = Typography.titleSmall.fontSize,
                fontWeight = Typography.titleSmall.fontWeight) },
            text = { Text(message,
                color = colorScheme.onPrimary,
                fontSize = Typography.bodyMedium.fontSize,
                fontWeight = Typography.bodyMedium.fontWeight,
                textAlign = TextAlign.Justify) },
            confirmButton = {
                Button(
                    onClick = {
                        onClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary
                    ),
                    ) {
                    Text("Ok")
                }
            },
            dismissButton = null
        )
    }
    @Composable
    fun StartForm(title:String, description: String, onClick: () -> Unit, onDismissRequest: () -> Unit){
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.questionnaire),
                    contentDescription = "Questionnaire",
                    modifier = Modifier.size(80.dp)
                )
            },
            onDismissRequest = { onDismissRequest() },
            title = {
                Text(
                    title,
                    color = colorScheme.onPrimary,
                    fontSize = Typography.titleSmall.fontSize,
                    fontWeight = Typography.titleSmall.fontWeight,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    description,
                    color = colorScheme.onPrimary,
                    fontSize = Typography.bodyMedium.fontSize,
                    fontWeight = Typography.bodyMedium.fontWeight,
                    textAlign = TextAlign.Justify
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary
                    ),
                    ) {
                    Text(
                        "Sim",
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primaryContainer
                    ),

                    ) {
                    Text(
                        "Mais tarde",
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight
                    )
                }
            }
        )

    }
    @Composable
    fun AccountHeader(text:String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(0.dp, 30.dp, 0.dp, 0.dp)){
            Text(
                text = "MIMO",
                color = colorScheme.onPrimary,
                fontSize = Typography.headlineLarge.fontSize,
                fontWeight = Typography.headlineLarge.fontWeight,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.menu),
                modifier = Modifier.height(90.dp),
                contentDescription = "MIMO Logótipo")
            Spacer(modifier = Modifier.height(30.dp))
            Text (text = text,
                color = colorScheme.onPrimary,
                fontSize = Typography.titleLarge.fontSize,
                fontWeight = Typography.titleLarge.fontWeight)
        }
    }

    @Composable
    fun AccountFooter(text:String, onClick: () -> Unit){
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 10.dp)){
            // Submit button
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary
                ),
                onClick = { onClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = text,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = colorScheme.onSurface)
            }
        }
    }
}