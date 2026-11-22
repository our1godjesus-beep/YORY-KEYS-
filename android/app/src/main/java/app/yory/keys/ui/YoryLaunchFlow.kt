package app.yory.keys.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
    var keyboardSetupSeen by remember { mutableStateOf(launchPrefs.getBoolean("keyboard_setup_seen", false)) }
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
        !keyboardSetupSeen -> YoryKeyboardSetupScreen(
            onContinue = {
                launchPrefs.edit().putBoolean("keyboard_setup_seen", true).apply()
                keyboardSetupSeen = true
            }
        )
        authenticated || offlineMode -> YoryStudioScreen(
            canOfferRegistration = offlineMode && !authenticated,
            onSignOut = { returnToAccountGate() },
            onRegister = {
                offlineMode = false
                launchPrefs.edit().putBoolean("offline_mode", false).apply()
            }
        )
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
private fun YoryKeyboardSetupScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isYoryKeyboardEnabled(context)) }
    var selected by remember { mutableStateOf(isYoryKeyboardSelected(context)) }
    var testText by remember { mutableStateOf("") }

    fun refreshStatus() {
        enabled = isYoryKeyboardEnabled(context)
        selected = isYoryKeyboardSelected(context)
    }

    LaunchBackdrop()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LaunchBrandMark()
        Text("Set up your keyboard", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Two Android system steps are required before YORY KEYS can type in other apps.",
            color = LaunchMuted, fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, LaunchGlassBorder, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text(if (enabled) "✓  1. Keyboard enabled" else "1. Enable YORY KEYS", color = if (enabled) Color(0xFF9DE5B2) else Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                "Android opens its own secure settings page. Find YORY KEYS Keyboard and turn it on.",
                color = LaunchMuted, fontSize = 12.sp, lineHeight = 17.sp
            )
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (enabled) "OPEN KEYBOARD SETTINGS" else "ENABLE YORY KEYS") }

            Divider(color = Color.White.copy(alpha = 0.14f))
            Text(if (selected) "✓  2. YORY KEYS selected" else "2. Choose YORY KEYS", color = if (selected) Color(0xFF9DE5B2) else Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                "After enabling it, open the keyboard picker and select YORY KEYS as your current input method.",
                color = LaunchMuted, fontSize = 12.sp, lineHeight = 17.sp
            )
            OutlinedButton(
                onClick = {
                    refreshStatus()
                    if (enabled) {
                        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        manager.showInputMethodPicker()
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text("CHOOSE YORY KEYS", color = Color.White) }

            TextButton(onClick = { refreshStatus() }, modifier = Modifier.fillMaxWidth()) {
                Text("I finished — check status", color = LaunchAccent)
            }
            Text(
                if (selected) "YORY KEYS is selected. Tap the field below to test typing."
                else if (enabled) "Keyboard is enabled, but it may not be the selected keyboard yet."
                else "YORY KEYS is not enabled yet.",
                color = LaunchMuted, fontSize = 11.sp
            )
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Test your keyboard") },
                placeholder = { Text("Tap here and type…") },
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = LaunchAccent,
                    focusedBorderColor = LaunchAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedLabelColor = LaunchAccent,
                    unfocusedLabelColor = LaunchMuted
                )
            )
        }
        Spacer(Modifier.height(14.dp))
        LaunchGlassButton(label = "CONTINUE", onClick = onContinue)
        TextButton(onClick = onContinue) {
            Text("Set it up later", color = LaunchMuted, fontSize = 12.sp)
        }
        Text(
            "No microphone, contacts or location permission is needed for basic typing. Android manages keyboard activation separately in system settings.",
            color = LaunchMuted.copy(alpha = 0.72f), fontSize = 11.sp, lineHeight = 16.sp
        )
    }
}

private fun isYoryKeyboardEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return manager.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isYoryKeyboardSelected(context: Context): Boolean {
    val selected = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return selected?.startsWith(context.packageName) == true
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
            if (isRegister) "Create an optional account to sync settings and use cloud features." else "Sign in to continue to YORY Studio.",
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
