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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.ipleiria.estg.ciic.chatboternui.R
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography

object CommonComposables {

    @Composable
    fun DialogConnectivity(showConnectivityError:Boolean, onClick: () -> Unit, onDismissRequest: () -> Unit ){
        if (showConnectivityError) {
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
                title = { Text("Sem conexão à internet",
                    color = colorScheme.onPrimary,
                    fontSize = Typography.titleSmall.fontSize,
                    fontWeight = Typography.titleSmall.fontWeight) },
                text = { Text("Parece que está atualmente offline.\n\nVerifique as suas definições de Wi-Fi ou de dados móveis e certifique-se de que tem uma ligação estável à Internet.\n\nQuando estiver ligado, reinicie a aplicação para continuar a desfrutar das suas funcionalidades.",
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
    }

    @Composable
    fun SuccessModal(text:String, showSuccessDialog:Boolean, onClick: () -> Unit, onDismissRequest: () -> Unit){
        if (showSuccessDialog) {
            AlertDialog(
                containerColor = colorScheme.background,
                tonalElevation = 0.dp,
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = "Sucesso",
                        modifier = Modifier.size(80.dp))
                },
                onDismissRequest = { onDismissRequest() },
                title = { Text("Sucesso",
                    fontSize = Typography.titleSmall.fontSize,
                    fontWeight = Typography.titleSmall.fontWeight) },
                text = { Text(text,
                    fontSize = Typography.titleSmall.fontSize,
                    fontWeight = Typography.titleSmall.fontWeight) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondary
                        )
                    ) {
                        Text("Ok",
                            fontSize = Typography.titleSmall.fontSize,
                            fontWeight = Typography.titleSmall.fontWeight)
                    }
                },
                dismissButton = null
            )
        }
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
                text = "EmoCare",
                color = colorScheme.onPrimary,
                fontSize = Typography.headlineLarge.fontSize,
                fontWeight = Typography.headlineLarge.fontWeight,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.chatbot),
                modifier = Modifier.height(90.dp),
                contentDescription = "EmoCare Logótipo")
            Spacer(modifier = Modifier.height(30.dp))
            Text (text = text,
                color = colorScheme.onPrimary,
                fontSize = Typography.titleLarge.fontSize,
                fontWeight = Typography.titleLarge.fontWeight)
        }
    }

    @Composable
    fun AccountFooter(text:String, textbutton: String, onClick: () -> Unit, textButtonOnClick: () -> Unit){
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
            Spacer(modifier = Modifier.height(15.dp))
            TextButton(onClick = {
                textButtonOnClick()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(textbutton,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = colorScheme.onPrimary,
                    textAlign = TextAlign.Center)
            }
        }
    }

    @Composable
    fun TextFieldAccount(value: MutableState<String>, label: String, description: String, icon: Int, iconDescription: String, passwordHidden:Boolean, togglePasswordVisibility:  () -> Unit){
        val isPassword = label=="Palavra-passe" || label=="Confirme a palavra-passe"
        TextField(
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = colorScheme.secondary,
                unfocusedBorderColor = colorScheme.secondary,
                textColor = colorScheme.onPrimary
            ),
            visualTransformation = if(isPassword && passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
            value = value.value,
            onValueChange = { value.value = it },
            label = {
                Text(
                    text = label,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = colorScheme.onPrimary
                )
            },
            placeholder = {
                Text(description,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = colorScheme.onPrimary
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = iconDescription,
                    tint = colorScheme.onPrimary)
            },
            trailingIcon = {if (isPassword)
                IconButton(onClick = { togglePasswordVisibility() }) {
                    Icon(
                        painter = painterResource(id = if(passwordHidden) R.drawable.show else R.drawable.hide),
                        contentDescription = "Esconder/mostrar a palavra-pass",
                        tint = colorScheme.onPrimary)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
    }

}