package pl.example.planer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import pl.example.planer.R
import kotlin.math.roundToInt

@Composable
fun SplashScreen() {
    val config         = LocalConfiguration.current
    val screenHeightDp = config.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current){ screenHeightDp.toPx() }

    val offsetY  = remember { Animatable(screenHeightPx) }
    val rotation = remember { Animatable(0f) }
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        delay(100)
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(800, easing = LinearEasing)
        )
        showText = true
        delay(700)
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (showText) 1f else 0f,
        animationSpec = tween(700)
    )

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .graphicsLayer { rotationZ = rotation.value }
                .align(Alignment.Center)
        )
        if (showText) {
            Text(
                text = "APLIKACJA",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = textAlpha }
                    .padding(top = 160.dp)
            )
        }
    }
}
