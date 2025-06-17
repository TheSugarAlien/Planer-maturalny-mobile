package pl.example.planer.model

import com.google.firebase.Timestamp

data class TeacherExamInfo(
    val id: String = "",
    val subject: String = "",
    val room: String = "",
    val date: Timestamp? = null
)
