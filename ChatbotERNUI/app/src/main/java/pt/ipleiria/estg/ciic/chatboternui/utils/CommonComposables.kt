package pt.ipleiria.estg.ciic.chatboternui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pt.ipleiria.estg.ciic.chatboternui.R
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography

object CommonComposables {

    @Composable
    fun ShowAlertDialog(onClick: () -> Unit, onDismissRequest: () -> Unit , title: String , message : String){
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.error),
                    modifier = Modifier.scale(2.0F),
                    contentDescription = "Erro: $title"
                )
            },
            onDismissRequest = { onDismissRequest() },
            title = { Text(title,
                color = colorScheme.onBackground,
                fontSize = Typography.titleMedium.fontSize,
                fontWeight = Typography.titleMedium.fontWeight,
                lineHeight = Typography.bodyLarge.lineHeight) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ){
                    Text(message,
                        color = colorScheme.onBackground,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        lineHeight = Typography.bodyLarge.lineHeight,
                        textAlign = TextAlign.Justify)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("carla.c.mendes@ipleiria.pt",
                        color = colorScheme.onBackground,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        lineHeight = Typography.bodyLarge.lineHeight,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline)
                }
                },
            confirmButton = {
                // To override confirmButton's original position: aligned to right side
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center)
                {
                    ActionButton("Sair", R.drawable.logout, { onClick() }, true)
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
            onClick = { onClick() }
        ) {
            Row(horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(5.dp)
            ) {
                Text(text = text,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = if(isActionStarted) colorScheme.onPrimary else colorScheme.onSecondary)
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    modifier = Modifier.scale(1.25F))
            }
        }
    }
}