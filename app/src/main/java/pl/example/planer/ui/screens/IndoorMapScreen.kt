package pl.example.planer.ui.screens

import android.content.Context
import android.hardware.*
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pl.example.planer.R
import kotlin.math.*

private enum class CalibrationState {
    Idle, SelectingStart, Walking, SelectingEnd, Tracking
}

@Composable
fun IndoorMapScreen() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val stepDetector    = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    val stepCounter     = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val accel           = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val rotationVector  = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    var calState       by remember { mutableStateOf(CalibrationState.Idle) }
    var stepsTaken     by remember { mutableStateOf(0) }
    var initialCounter by remember { mutableStateOf<Float?>(null) }
    var lastMag        by remember { mutableStateOf(0f) }

    var startPoint   by remember { mutableStateOf(Offset.Zero) }
    var endPoint     by remember { mutableStateOf(Offset.Zero) }
    var currentPoint by remember { mutableStateOf(Offset.Zero) }

    var stepLength   by remember { mutableStateOf(0f) }
    var azimuth      by remember { mutableStateOf(0f) }

    fun handleStep() {
        when (calState) {
            CalibrationState.Tracking -> {
                val dx = stepLength * sin(azimuth)
                val dy = -stepLength * cos(azimuth)
                currentPoint = Offset(currentPoint.x + dx, currentPoint.y + dy)
            }
            CalibrationState.Walking -> {
                stepsTaken++
                if (stepsTaken >= 5) {
                    calState = CalibrationState.SelectingEnd
                }
            }
            else -> {}
        }
    }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {

                    Sensor.TYPE_STEP_DETECTOR -> handleStep()

                    Sensor.TYPE_STEP_COUNTER -> {
                        val total = event.values[0]
                        if (initialCounter == null) {
                            initialCounter = total
                        } else {
                            val delta = (total - initialCounter!!).toInt()
                            if (delta > stepsTaken) {
                                repeat(delta - stepsTaken) { handleStep() }
                            }
                        }
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val mag = sqrt(x * x + y * y + z * z)
                        val delta = mag - SensorManager.GRAVITY_EARTH
                        val threshold = 1.2f
                        if (delta > threshold && lastMag <= threshold) {
                            handleStep()
                        }
                        lastMag = delta
                    }

                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMat = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotMat, event.values)

                        val remappedRotMat = FloatArray(9)
                        SensorManager.remapCoordinateSystem(
                            rotMat,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_Z,
                            remappedRotMat
                        )

                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(remappedRotMat, orientation)
                        azimuth = orientation[0]
                    }
                }
            }
        }
    }

    DisposableEffect(calState) {
        if (calState == CalibrationState.Walking || calState == CalibrationState.Tracking) {
            listOfNotNull(stepDetector, stepCounter, accel, rotationVector).forEach {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        onDispose {
            sensorManager.unregisterListener(sensorListener)
            initialCounter = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(calState) {
                    detectTapGestures { tap ->
                        when (calState) {
                            CalibrationState.SelectingStart -> {
                                startPoint = tap
                                currentPoint = tap
                                stepsTaken = 0
                                calState = CalibrationState.Walking
                            }

                            CalibrationState.SelectingEnd -> {
                                endPoint = tap
                                val dx = endPoint.x - startPoint.x
                                val dy = endPoint.y - startPoint.y
                                stepLength = sqrt(dx * dx + dy * dy) / stepsTaken.toFloat()
                                currentPoint = endPoint
                                calState = CalibrationState.Tracking
                            }

                            else -> {}
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.school_map),
                contentDescription = "Mapa szkoły",
                modifier = Modifier.fillMaxSize()
            )

            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = Color.Red, radius = 12f, center = currentPoint)

                val arrowLength = 50f
                val end = Offset(
                    currentPoint.x + arrowLength * sin(azimuth),
                    currentPoint.y - arrowLength * cos(azimuth)
                )
                drawLine(color = Color.Blue, start = currentPoint, end = end, strokeWidth = 4f)
            }
        }

        Spacer(Modifier.height(12.dp))

        when (calState) {
            CalibrationState.Idle -> {
                Button(
                    onClick = { calState = CalibrationState.SelectingStart },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Kalibruj start")
                }
            }

            CalibrationState.SelectingStart -> {
                Text(
                    "Dotknij mapy, gdzie aktualnie stoisz",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            CalibrationState.Walking -> {
                Column(Modifier.padding(16.dp)) {
                    Text("Przejdź 5 kroków", style = MaterialTheme.typography.bodyMedium)
                    Text("Licznik: $stepsTaken / 5", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { handleStep() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔃 Symuluj krok")
                    }
                }
            }

            CalibrationState.SelectingEnd -> {
                Text(
                    "Dotknij mapy, gdzie skończyłeś po 5 krokach",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            CalibrationState.Tracking -> {
                Column(Modifier.padding(16.dp)) {
                    Text("Nawigacja aktywna", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { handleStep() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔃 Symuluj krok")
                    }
                }
            }
        }
    }
}
