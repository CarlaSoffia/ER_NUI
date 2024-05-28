package pt.ipleiria.estg.ciic.chatboternui.utils

interface IRequestActivity : IBaseActivity {
    suspend fun apiRequest()
}