package pl.example.planer.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pl.example.planer.model.ExamStudent

@Composable
fun DashboardTeacherScreen() {
    val db = Firebase.firestore
    val examId = "maj_mat_pod_2025"

    val students = remember { mutableStateListOf<ExamStudent>() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        db.collection("exams").document(examId).collection("students")
            .get()
            .addOnSuccessListener { result ->
                students.clear()
                result.documents.forEach { doc ->
                    val raw = doc.toObject(ExamStudent::class.java)
                    if (raw != null) {
                        raw.uid = doc.id
                        students.add(raw)
                    }
                }
                loading = false
            }

            .addOnFailureListener {
                Log.e("Firestore", "Błąd pobierania studentów: ${it.message}")
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Egzamin: Matematyka podstawowa", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(students) { student ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(student.name, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = student.present,
                            onCheckedChange = { isChecked ->
                                student.present = isChecked
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                val batch = db.batch()
                students.forEach { student ->
                    if (student.uid.isNotBlank()) {
                        val ref = db.collection("exams")
                            .document(examId)
                            .collection("students")
                            .document(student.uid)

                        batch.update(ref, "present", student.present)
                    } else {
                        Log.e("Firestore", "Student UID is blank — cannot update")
                    }
                }
                batch.commit()
            }) {
                Text("Zapisz obecność")
            }

        }
    }
}
