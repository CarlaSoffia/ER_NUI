package pt.ipleiria.estg.ciic.chatboternui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonColors
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.DeepBlue
import pt.ipleiria.estg.ciic.chatboternui.ui.theme.Typography
import pt.ipleiria.estg.ciic.chatboternui.utils.alerts.IAlert

object CommonComposables {

    @Composable
    fun InputMode(icon: Int, text: String){
        Row(horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(50.dp, 15.dp)
                .fillMaxWidth()) {
            CircleBox(icon, text)
            Text(text = text,
                fontSize = Typography.bodyLarge.fontSize,
                fontWeight = Typography.bodyLarge.fontWeight,
                color = colorScheme.onBackground,
                modifier = Modifier.width(70.dp))
        }
    }

    @Composable
    fun ShowAlertDialogWithContent(alert: IAlert, showActionButton: Boolean = true, content: @Composable () -> Unit){
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            onDismissRequest = {},
            icon = {
                // Nothing to do here
            },
            title = {
                Text(alert.title,
                    color = colorScheme.onBackground,
                    fontSize = Typography.titleMedium.fontSize,
                    fontWeight = Typography.titleMedium.fontWeight,
                    lineHeight = Typography.titleMedium.lineHeight,
                    textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ){
                    content()
                }
            },
            confirmButton = {
                if(showActionButton){
                    ActionButton(alert.confirmButton.title, alert.confirmButton.icon, { alert.confirmButton.onClick?.let { it() } }, true)
                }
            },
            dismissButton = {
                ActionButton(alert.dismissButton!!.title, alert.dismissButton!!.icon, { alert.dismissButton!!.onClick?.let { it() } }, false)
            }
        )
    }
    @Composable
    fun ShowAlertDialog(alert: IAlert){
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            icon = {
                if(alert.icon != null){
                    Image(
                        painter = painterResource(id = alert.icon!!),
                        modifier = Modifier.scale(2.0F),
                        contentDescription = "Erro: ${alert.title}"
                    )
                }
            },
            onDismissRequest = {},
            title = { Text(alert.title,
                color = colorScheme.onBackground,
                fontSize = Typography.titleMedium.fontSize,
                fontWeight = Typography.titleMedium.fontWeight,
                lineHeight = Typography.titleMedium.lineHeight,
                textAlign = TextAlign.Center)
                    },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ){
                    if(alert.image != null){
                        Image(
                            painter = painterResource(id = alert.image!!),
                            contentDescription = alert.title,
                            modifier = Modifier.size(250.dp))
                    }
                    Text(alert.text,
                        color = colorScheme.onBackground,
                        fontSize = Typography.bodyLarge.fontSize,
                        fontWeight = Typography.bodyLarge.fontWeight,
                        lineHeight = Typography.bodyLarge.lineHeight,
                        textAlign = TextAlign.Justify)
                    if(alert.showSupportEmail){
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Tente novamente mais tarde ou contacte o suporte através do email:",
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
                }
                },
            confirmButton = {
                if(alert.dismissButton == null){
                    // To override confirmButton's original position: aligned to right side
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center)
                    {
                        ActionButton(alert.confirmButton.title, alert.confirmButton.icon, { alert.confirmButton.onClick?.let { it() } }, true)
                    }
                }else{
                    ActionButton(alert.confirmButton.title, alert.confirmButton.icon, { alert.confirmButton.onClick?.let { it() } }, true)
                }
            },
            dismissButton = {
                if(alert.dismissButton != null){
                    ActionButton(alert.dismissButton!!.title, alert.dismissButton!!.icon, { alert.dismissButton!!.onClick?.let { it() } }, false)
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
    fun ActionTransparentButton(text:String, icon: Int, onClick: () -> Unit, alignStart : Boolean) {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            onClick = { onClick() },
            shape = RectangleShape
        ) {
            Row(
                horizontalArrangement = if(alignStart) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(150.dp)
                    .padding(5.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text
                )
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = text,
                    fontSize = Typography.titleMedium.fontSize,
                    color = colorScheme.onPrimary
                )
            }
        }
    }
    @Composable
    fun CircleBox(icon: Int, contentDescription: String){
        Box(modifier = Modifier
                .size(45.dp)
                .background(color = DeepBlue, shape = CircleShape),
            contentAlignment = Alignment.Center
        ){
            Image(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.scale(1.25F)
            )
        }
    }
    @Composable
    fun CircleButton(icon: Int, contentDescription: String, onClick: () -> Unit){
        Button(
                colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            ),
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape,
            onClick = { onClick() }
        ){
            Image(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.scale(1.25F)
            )
        }
    }
    @Composable
    fun ActionButton(text:String, icon: Int, onClick: () -> Unit, isActionStarter: Boolean) {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActionStarter) colorScheme.primary else colorScheme.secondary
            ),
            onClick = { onClick() }
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(0.dp, 5.dp)
            ) {
                Text(
                    text = text,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = if (isActionStarter) colorScheme.onPrimary else colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text
                )
            }
        }
    }
    @Composable
    fun MenuButton(text:String, icon: Int, onClick: () -> Unit){
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            ),
            onClick = { onClick() }
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    modifier = Modifier.scale(1.25F)
                )
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = text,
                    fontSize = Typography.bodyLarge.fontSize,
                    fontWeight = Typography.bodyLarge.fontWeight,
                    color = colorScheme.onPrimary
                )
            }
        }
    }


    @Composable
    fun MultipleRadioButtons(title: String, items: List<String>, onClick: (String) -> Unit) {
        val selectedValue = remember { mutableStateOf("") }

        val isSelectedItem: (String) -> Boolean = { selectedValue.value == it }
        val onChangeState: (String) -> Unit = {
            selectedValue.value = it
            onClick(selectedValue.value)
        }
        AlertDialog(
            containerColor = colorScheme.background,
            tonalElevation = 0.dp,
            onDismissRequest = {
                // Nothing to do here
            },
            icon = {
                // Nothing to do here
            },
            title = {
                Text(title,
                    color = colorScheme.onBackground,
                    fontSize = Typography.titleMedium.fontSize,
                    fontWeight = Typography.titleMedium.fontWeight,
                    lineHeight = Typography.titleMedium.lineHeight,
                    textAlign = TextAlign.Center)
            },
            text = {
                Column {
                    items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.selectable(
                                selected = isSelectedItem(item),
                                onClick = { onChangeState(item) },
                                role = Role.RadioButton
                            ).padding(5.dp, 10.dp)
                        ) {
                            RadioButton(
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colorScheme.surface
                                ),
                                selected = isSelectedItem(item),
                                onClick = { onChangeState(item) }
                            )
                            Text(
                                text = item,
                                color = colorScheme.onBackground,
                                fontSize = Typography.bodyLarge.fontSize,
                                fontWeight = Typography.bodyLarge.fontWeight,
                                lineHeight = Typography.bodyLarge.lineHeight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                // Nothing to do here
            },
            dismissButton = {
                // Nothing to do here
            }
        )
    }
}