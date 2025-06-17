package pl.example.planer.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


data class ExamStudent(
    var uid: String = "",
    val name: String = "",
    var present: Boolean = false,
    val points: Map<String, Long> = emptyMap()
)

@Composable
fun AttendanceScreen(
    examId: String,
    navController: NavController
) {
    val db = Firebase.firestore
    val students = remember { mutableStateListOf<ExamStudent>() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(examId) {
        db.collection("exams")
            .document(examId)
            .collection("students")
            .get()
            .addOnSuccessListener { snap ->
                students.clear()
                snap.documents.forEach { doc ->
                    val student = doc.toObject(ExamStudent::class.java)
                    if (student != null) {
                        student.uid = doc.id
                        students.add(student)
                    }
                }
                loading = false
            }
            .addOnFailureListener { e ->
                Log.e("AttendanceScreen", "Błąd pobierania studentów: ${e.message}")
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Sprawdź obecność",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        } else if (students.isEmpty()) {
            Text("Brak zapisanych uczniów.", style = MaterialTheme.typography.bodyMedium)

        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(students) { student ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = student.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = student.present,
                            onCheckedChange = { isChecked ->
                                student.present = isChecked
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val batch = db.batch()
                    students.forEach { st ->
                        val ref = db.collection("exams")
                            .document(examId)
                            .collection("students")
                            .document(st.uid)
                        batch.update(ref, "present", st.present)
                    }
                    batch.commit()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz obecność")
            }
        }
    }
}
