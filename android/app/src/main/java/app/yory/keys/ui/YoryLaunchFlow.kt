package app.yory.keys.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yory.keys.auth.YoryAuthException
import app.yory.keys.auth.YoryAuthRepository
import app.yory.keys.auth.YoryAuthResult
import kotlinx.coroutines.launch

private val LaunchAccent = Color(0xFFA7C7FF)
private val LaunchMuted = Color(0xFFB4BECE)
private val LaunchGlassBorder = Color.White.copy(alpha = 0.22f)

@Composable
fun YoryAppFlow() {
    val context = LocalContext.current
    val launchPrefs = remember { context.getSharedPreferences("yory_launch_state", 0) }
    var introSeen by remember { mutableStateOf(launchPrefs.getBoolean("intro_seen", false)) }
    var offlineMode by remember { mutableStateOf(launchPrefs.getBoolean("offline_mode", false)) }
    var authReady by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        YoryAuthRepository.initialize(context)
        authenticated = runCatching { YoryAuthRepository.restoreSession() }.getOrDefault(false)
        if (authenticated) {
            offlineMode = false
            launchPrefs.edit().remove("offline_mode").apply()
        }
        authReady = true
    }

    fun returnToAccountGate() {
        scope.launch {
            YoryAuthRepository.signOut()
            authenticated = false
            offlineMode = false
            launchPrefs.edit().putBoolean("offline_mode", false).apply()
        }
    }

    when {
        !authReady -> {
            LaunchBackdrop()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LaunchAccent)
            }
        }
        !introSeen -> YoryGlassIntroScreen {
            launchPrefs.edit().putBoolean("intro_seen", true).apply()
            introSeen = true
        }
        authenticated || offlineMode -> YoryStudioScreen(onSignOut = { returnToAccountGate() })
        else -> YoryAuthGate(
            onAuthenticated = {
                authenticated = true
                offlineMode = false
                launchPrefs.edit().remove("offline_mode").apply()
            },
            onContinueOffline = {
                offlineMode = true
                launchPrefs.edit().putBoolean("offline_mode", true).apply()
            }
        )
    }
}

@Composable
private fun YoryGlassIntroScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val logo = remember {
        runCatching {
            context.assets.open("LOGO7.jpeg").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
    val transition = rememberInfiniteTransition(label = "yory-intro")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logo-pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(4800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logo-tilt"
    )
    LaunchBackdrop()
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.size(184.dp).graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    rotationZ = rotation
                }.clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.045f))))
                    .border(1.1.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.padding(12.dp).fillMaxSize().clip(CircleShape)
                        .background(Brush.radialGradient(listOf(LaunchAccent.copy(alpha = 0.26f), Color.Transparent)))
                        .border(0.7.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (logo != null) {
                        Image(bitmap = logo, contentDescription = "YORY KEYS logo", modifier = Modifier.fillMaxSize().padding(20.dp))
                    } else {
                        Text("Y", color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
            Text("YORY KEYS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("YOUR WORDS. YOUR FLOW.", color = LaunchMuted, fontSize = 11.sp, letterSpacing = 2.3.sp)
            Spacer(Modifier.height(18.dp))
            LaunchGlassButton(label = "GET STARTED", onClick = onContinue)
            Text("A keyboard shaped around you", color = LaunchMuted.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun YoryAuthGate(onAuthenticated: () -> Unit, onContinueOffline: () -> Unit) {
    var isRegister by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchBackdrop()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LaunchBrandMark()
        Text(if (isRegister) "Create your YORY account" else "Welcome back", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (isRegister) "Register first to personalize and sync your settings." else "Sign in to continue to YORY Studio.",
            color = LaunchMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.045f))))
                .border(0.9.dp, LaunchGlassBorder, RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRegister) LaunchTextField(displayName, { displayName = it }, "Display name", KeyboardType.Text)
            LaunchTextField(email, { email = it }, "Email address", KeyboardType.Email)
            LaunchTextField(password, { password = it }, "Password", KeyboardType.Password, password = true)
            if (message.isNotBlank()) Text(message, color = if (isInfo) LaunchAccent else Color(0xFFFFA7B3), fontSize = 12.sp)
            LaunchGlassButton(
                label = if (busy) "PLEASE WAIT…" else if (isRegister) "CREATE ACCOUNT" else "SIGN IN",
                enabled = !busy && email.contains("@") && password.length >= 8 && (!isRegister || displayName.isNotBlank())
            ) {
                scope.launch {
                    busy = true
                    message = ""
                    try {
                        val result = if (isRegister) {
                            YoryAuthRepository.signUp(displayName, email, password)
                        } else {
                            YoryAuthRepository.signIn(email, password)
                        }
                        when (result) {
                            is YoryAuthResult.SignedIn -> onAuthenticated()
                            is YoryAuthResult.NeedsEmailVerification -> {
                                isInfo = true
                                message = "Check your email to verify your account, then return here and sign in."
                                isRegister = false
                                password = ""
                            }
                        }
                    } catch (error: YoryAuthException) {
                        isInfo = false
                        message = error.message ?: "Could not complete the account request."
                    } catch (error: Exception) {
                        isInfo = false
                        message = error.message ?: "Something went wrong. Please try again."
                    } finally {
                        busy = false
                    }
                }
            }
            TextButton(onClick = {
                isRegister = !isRegister
                message = ""
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isRegister) "Already have an account? Sign in" else "New to YORY? Create an account",
                    color = LaunchAccent,
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = onContinueOffline, modifier = Modifier.fillMaxWidth()) {
                Text("CONTINUE OFFLINE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(
                "Typing, layouts and personalization work on this device. Internet is only needed for account and cloud features.",
                color = LaunchMuted.copy(alpha = 0.78f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(13.dp))
        Text(
            "Keyboard access is separate: once enabled in Android settings, YORY KEYS can type offline without opening this app or signing in again.",
            color = LaunchMuted.copy(alpha = 0.72f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun LaunchBrandMark() {
    val context = LocalContext.current
    val logo = remember {
        runCatching {
            context.assets.open("LOGO7.jpeg").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
    Box(
        Modifier.size(76.dp).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.045f))))
            .border(0.9.dp, LaunchGlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(bitmap = logo, contentDescription = "YORY KEYS logo", modifier = Modifier.fillMaxSize().padding(8.dp))
        } else {
            Text("Y", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("YORY KEYS", color = LaunchAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun LaunchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = LaunchAccent,
            focusedBorderColor = LaunchAccent.copy(alpha = 0.8f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
            focusedLabelColor = LaunchAccent,
            unfocusedLabelColor = LaunchMuted
        )
    )
}

@Composable
private fun LaunchGlassButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LaunchAccent.copy(alpha = 0.92f),
            contentColor = Color(0xFF101522),
            disabledContainerColor = Color.White.copy(alpha = 0.10f),
            disabledContentColor = LaunchMuted.copy(alpha = 0.55f)
        )
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun LaunchBackdrop() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF242D42), Color(0xFF080B13), Color(0xFF161A2A)))
        )
    ) {
        Box(Modifier.size(260.dp).offset(x = 140.dp, y = (-80).dp).blur(60.dp).background(LaunchAccent.copy(alpha = 0.20f), CircleShape))
        Box(
            Modifier.size(220.dp).align(Alignment.BottomStart).offset(x = (-85).dp, y = 65.dp)
                .blur(65.dp).background(Color(0xFF7A70C8).copy(alpha = 0.18f), CircleShape)
        )
    }
}
