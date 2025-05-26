package pl.example.planer.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ExamStudent(
    var uid: String = "",
    val name: String = "",
    present: Boolean = false,
    val points: Map<String, Long> = emptyMap()
) {
    var present by mutableStateOf(present)
}
