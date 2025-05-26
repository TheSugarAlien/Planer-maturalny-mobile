package pl.example.planer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudentExamInfo(
    val subject: String = "",
    val room: String = "",
    val date: Timestamp? = null,
    val points: Map<String, Long> = emptyMap()
)

@Composable
fun DashboardStudentScreen() {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid

    var exams by remember { mutableStateOf<List<StudentExamInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("exams")
                .get()
                .addOnSuccessListener { result ->
                    val matchingExams = mutableListOf<StudentExamInfo>()

                    val examsList = result.documents

                    examsList.forEach { examDoc ->
                        val examId = examDoc.id
                        val examData = examDoc.data

                        db.collection("exams").document(examId)
                            .collection("students").document(uid)
                            .get()
                            .addOnSuccessListener { studentDoc ->
                                if (studentDoc.exists()) {
                                    val points = studentDoc.get("points") as? Map<String, Long> ?: emptyMap()
                                    val subject = examData?.get("subject") as? String ?: "Brak przedmiotu"
                                    val room = (examData?.get("room") ?: "").toString()
                                    val date = examData?.get("date") as? Timestamp

                                    matchingExams.add(
                                        StudentExamInfo(
                                            subject = subject,
                                            room = room,
                                            date = date,
                                            points = points
                                        )
                                    )
                                    exams = matchingExams.sortedBy { it.date?.toDate() }
                                }
                                loading = false
                            }
                    }

                    if (examsList.isEmpty()) {
                        loading = false
                    }
                }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("📚 Twoje egzaminy", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (exams.isEmpty()) {
            Text("Nie znaleziono przypisanych egzaminów.")
        } else {
            LazyColumn {
                items(exams) { exam ->
                    ExamCard(exam)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ExamCard(exam: StudentExamInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Przedmiot: ${exam.subject}", style = MaterialTheme.typography.titleMedium)
            Text("Sala: ${exam.room}")
            Text("Data: ${exam.date?.toDate()?.formatToUi()}")

            Spacer(modifier = Modifier.height(8.dp))
            Text("Wynik punktowy:")
            exam.points.forEach { (zad, pkt) ->
                Text("• $zad: $pkt pkt")
            }
        }
    }
}

fun Date.formatToUi(): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl"))
    return formatter.format(this)
}
