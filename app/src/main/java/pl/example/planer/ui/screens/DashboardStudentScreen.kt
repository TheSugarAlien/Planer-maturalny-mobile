package pl.example.planer.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.Alignment
import pl.example.planer.navigation.Screen

data class StudentExamInfo(
    val subject: String = "",
    val room: String = "",
    val date: Timestamp? = null,
    val points: Map<String, Long> = emptyMap()
)

@Composable
fun DashboardStudentScreen(navController: NavController) {
    val context = LocalContext.current
    val auth    = Firebase.auth
    val db      = Firebase.firestore
    val uid     = auth.currentUser?.uid ?: return

    var exams   by remember { mutableStateOf<List<StudentExamInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        val list = mutableListOf<StudentExamInfo>()
        db.collection("exams")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    loading = false
                    return@addOnSuccessListener
                }
                snap.documents.forEachIndexed { index, doc ->
                    doc.reference.collection("students").document(uid)
                        .get()
                        .addOnSuccessListener { stud ->
                            if (stud.exists()) {
                                list += StudentExamInfo(
                                    subject = doc.getString("subject").orEmpty(),
                                    room    = doc.getString("room").orEmpty(),
                                    date    = doc.getTimestamp("date"),
                                    points  = stud.get("points") as? Map<String, Long> ?: emptyMap()
                                )
                            }
                        }
                        .addOnCompleteListener {
                            if (index == snap.size() - 1) {
                                exams = list.sortedBy { it.date?.toDate() }
                                loading = false
                            }
                        }
                }
            }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val readOk  = perms[Manifest.permission.READ_CALENDAR]  == true
        val writeOk = perms[Manifest.permission.WRITE_CALENDAR] == true
        if (readOk && writeOk) {
            addAllToCalendar(context, exams)
        } else {
            Toast.makeText(context, "Potrzebne oba uprawnienia do kalendarza", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Twoje egzaminy", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            permLauncher.launch(arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            ))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("➕ Dodaj do kalendarza")
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (exams.isEmpty()) {
            Text("Brak egzaminów.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(exams) { exam ->
                    ExamCard(exam)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.IndoorMap.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Otwórz mapę szkoły")
            }
        }
    }
}

@Composable
fun ExamCard(exam: StudentExamInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Przedmiot: ${exam.subject}", style = MaterialTheme.typography.titleMedium)
            Text("Sala: ${exam.room}")
            Text("Data: ${exam.date?.toDate()?.let {
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl")).format(it)
            }.orEmpty()}")
            Spacer(Modifier.height(8.dp))
            Text("Wynik punktowy:")
            exam.points.forEach { (zad, pkt) ->
                Text("• $zad: $pkt pkt")
            }
        }
    }
}

fun addAllToCalendar(context: Context, exams: List<StudentExamInfo>) {
    val resolver = context.contentResolver
    val calCursor = resolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        "${CalendarContract.Calendars.VISIBLE} = 1",
        null, null
    )
    val calId = calCursor?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }
    if (calId == null) {
        Toast.makeText(context, "Nie znaleziono kalendarza", Toast.LENGTH_SHORT).show()
        return
    }
    exams.forEach { exam ->
        val startMillis = exam.date?.toDate()?.time ?: return@forEach
        val endMillis   = startMillis + 60 * 60 * 1000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.TITLE, "Egzamin: ${exam.subject}")
            put(CalendarContract.Events.EVENT_LOCATION, "Sala ${exam.room}")
            put(CalendarContract.Events.DESCRIPTION, "Matura: ${exam.subject}")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        resolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }
    Toast.makeText(context, "Dodano ${exams.size} egzaminów do kalendarza", Toast.LENGTH_SHORT).show()
}
