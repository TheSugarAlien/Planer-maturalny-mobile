package pl.example.planer.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import pl.example.planer.navigation.Screen
import com.google.firebase.firestore.ktx.firestore

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = Firebase.auth
    val db = Firebase.firestore


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Logowanie", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Hasło") })

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            scope.launch {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        val role = document.getString("role")
                                        if (role == "teacher") {
                                            navController.navigate(Screen.DashboardTeacher.route)
                                        } else {
                                            navController.navigate(Screen.DashboardStudent.route)
                                        }
                                    }
                                    .addOnFailureListener {
                                        errorMessage = "Błąd przy pobieraniu roli: ${it.message}"
                                    }
                            } else {
                                errorMessage = "Nie znaleziono UID"
                            }
                        } else {
                            errorMessage = task.exception?.message
                        }
                    }
            }
        }) {
            Text("Zaloguj się")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

