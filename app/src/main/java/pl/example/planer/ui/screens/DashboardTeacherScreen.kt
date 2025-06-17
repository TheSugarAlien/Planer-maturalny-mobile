package pl.example.planer.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pl.example.planer.model.TeacherExamInfo
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardTeacherScreen(navController: NavController) {
    val auth    = Firebase.auth
    val db      = Firebase.firestore
    val uid     = auth.currentUser?.uid ?: return
    val exams   = remember { mutableStateListOf<TeacherExamInfo>() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        db.collection("exams")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    loading = false
                    return@addOnSuccessListener
                }
                snap.documents.forEachIndexed { index, doc ->
                    doc.reference
                        .collection("teachers")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { teacherDoc ->
                            if (teacherDoc.exists()) {
                                val subject = doc.getString("subject").orEmpty()
                                val room    = doc.getString("room").orEmpty()
                                val date    = doc.getTimestamp("date")
                                exams.add(
                                    TeacherExamInfo(
                                        id      = doc.id,
                                        subject = subject,
                                        room    = room,
                                        date    = date
                                    )
                                )
                            }
                        }
                        .addOnCompleteListener {
                            if (index == snap.size() - 1) {
                                loading = false
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardTeacher", "Błąd pobierania egzaminów: ${e.message}")
                loading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Egzaminy", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (exams.isEmpty()) {
            Text("Brak przypisanych egzaminów.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(exams) { _, exam ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(exam.subject, style = MaterialTheme.typography.titleMedium)
                            Text("Sala: ${exam.room}")
                            Text("Data: ${
                                exam.date?.toDate()?.let {
                                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl"))
                                        .format(it)
                                }.orEmpty()
                            }")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                navController.navigate("attendance/${exam.id}")
                            }) {
                                Text("Sprawdź obecność")
                            }
                        }
                    }
                }
            }
        }
    }
}
