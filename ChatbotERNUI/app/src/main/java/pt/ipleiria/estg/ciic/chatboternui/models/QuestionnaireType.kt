package pt.ipleiria.estg.ciic.chatboternui.models

data class QuestionnaireType (
    var name: String,
    var displayName: String,
    var pointsMin: Int,
    var pointsMax: Int,
    // add the results mappings short names and values, number of questions
)