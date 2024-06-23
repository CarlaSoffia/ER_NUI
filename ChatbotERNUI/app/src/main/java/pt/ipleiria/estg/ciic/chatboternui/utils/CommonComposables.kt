package pt.ipleiria.estg.ciic.chatboternui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    fun LinkText(text:String, onClick: () -> Unit){
        TextButton(
            onClick = { onClick() },
        ) {
            Text(
                text = text,
                fontSize = Typography.bodyLarge.fontSize,
                fontWeight = Typography.bodyLarge.fontWeight,
                textDecoration = TextDecoration.Underline
            )
        }
    }
    @Composable
    fun HeaderWithTitleAndImage(text:String, image: Int) {
        Text(
            text = text,
            color = colorScheme.onBackground,
            fontSize = Typography.titleLarge.fontSize,
            fontWeight = Typography.titleLarge.fontWeight,
            lineHeight = Typography.titleLarge.lineHeight,
            textAlign = TextAlign.Center
        )
       Image(
            painter = painterResource(id = image),
            contentDescription = text,
            modifier = Modifier.size(250.dp))
    }

    @Composable
    fun ActionButton(text:String, icon: Int, onClick: () -> Unit, isActionStarted: Boolean){
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = if(isActionStarted) colorScheme.primary else colorScheme.secondary
            ),
            modifier = Modifier.fillMaxWidth()
                .padding(70.dp, 0.dp),
            onClick = { onClick() },
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Text(text = text,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = if(isActionStarted) colorScheme.onPrimary else colorScheme.onSecondary)
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    modifier = Modifier.scale(1.25F))
            }
        }
    }
}