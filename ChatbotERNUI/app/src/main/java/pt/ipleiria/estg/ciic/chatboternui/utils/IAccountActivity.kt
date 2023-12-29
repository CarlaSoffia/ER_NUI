package pt.ipleiria.estg.ciic.chatboternui.utils

import android.app.Activity

interface IAccountActivity {
    suspend fun accountRequestSubmit(): String
    val activity: Activity
}