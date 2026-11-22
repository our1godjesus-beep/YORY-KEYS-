package app.yory.keys.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private data class RegistrationInviteCopy(
    val title: String,
    val message: String,
    val privacy: String,
    val primary: String,
    val secondary: String,
    val isRtl: Boolean = false
)

private val InviteMint = Color(0xFF70D8C8)
private val InviteViolet = Color(0xFFA79AFF)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun YoryRegistrationInvitationHost(
    canOfferRegistration: Boolean,
    onRegister: () -> Unit
) {
    val context = LocalContext.current
    val launchPrefs = remember { context.getSharedPreferences("yory_launch_state", Context.MODE_PRIVATE) }
    var hasValidatedInternet by remember { mutableStateOf(isValidatedInternetAvailable(context)) }
    var isVisible by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post { hasValidatedInternet = isValidatedInternetAvailable(context) }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                mainHandler.post { hasValidatedInternet = validated }
            }

            override fun onLost(network: Network) {
                mainHandler.post { hasValidatedInternet = isValidatedInternetAvailable(context) }
            }
        }
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
        onDispose {
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
    }

    LaunchedEffect(canOfferRegistration, hasValidatedInternet) {
        if (canOfferRegistration && hasValidatedInternet && !isVisible) {
            kotlinx.coroutines.delay(1_500L)
            if (!canOfferRegistration || !hasValidatedInternet) return@LaunchedEffect
            val now = System.currentTimeMillis()
            val nextAllowed = launchPrefs.getLong("registration_invite_next_allowed", 0L)
            if (now >= nextAllowed) {
                // Record the impression before presenting so force-closing the app cannot
                // cause the invitation to reappear on every launch.
                launchPrefs.edit()
                    .putLong("registration_invite_next_allowed", now + 30L * 24L * 60L * 60L * 1000L)
                    .apply()
                isVisible = true
            }
        }
    }

    if (isVisible) {
        val copy = remember { registrationInviteCopy(context) }
        CompositionLocalProvider(
            LocalLayoutDirection provides if (copy.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            ModalBottomSheet(
                onDismissRequest = { isVisible = false },
                containerColor = Color(0xFF111722),
                contentColor = Color(0xFFF4F3F0),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(
                        modifier = Modifier.size(62.dp).clip(CircleShape)
                            .background(Color(0xFF1B3438))
                            .border(1.dp, InviteMint.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☁", color = InviteMint, fontSize = 29.sp)
                    }
                    Text(
                        "YORY STUDIO",
                        color = InviteViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.0.sp
                    )
                    Text(
                        copy.title,
                        color = Color(0xFFF4F3F0),
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        copy.message,
                        color = Color(0xFFB7C0CD),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            isVisible = false
                            onRegister()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InviteMint,
                            contentColor = Color(0xFF09201F)
                        )
                    ) {
                        Text(copy.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(copy.secondary, color = Color(0xFFE0E4EB), fontSize = 14.sp)
                    }
                    Text(
                        copy.privacy,
                        color = Color(0xFF8793A4),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun isValidatedInternetAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun registrationInviteCopy(context: Context): RegistrationInviteCopy {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val language = locale.language.lowercase(Locale.ROOT)
    val resolved = languageForCountry(locale.country)
        ?: language.takeIf { it in setOf("ar", "de", "es", "fr", "it", "pt", "en") }
        ?: "en"
    return when (resolved) {
        "ar" -> RegistrationInviteCopy(
            "تجربتك مع YORY لسه قدامها المزيد",
            "أنشئ حساب YORY لتكون مستعدًا للخدمات السحابية القادمة عند إطلاقها.",
            "ستظل لوحة المفاتيح متاحة دون إنترنت. كتاباتك وقاموسك المحلي يظلان على جهازك.",
            "إنشاء حساب",
            "ليس الآن",
            true
        )
        "de" -> RegistrationInviteCopy(
            "Mit YORY geht noch mehr",
            "Erstelle ein YORY-Konto, um für kommende Cloud-Funktionen bereit zu sein.",
            "Deine Tastatur bleibt offline verfügbar. Deine Texte und dein lokales Wörterbuch bleiben auf deinem Gerät.",
            "Konto erstellen",
            "Nicht jetzt"
        )
        "es" -> RegistrationInviteCopy(
            "Tu experiencia con YORY puede crecer",
            "Crea una cuenta YORY para estar preparado cuando lleguen las próximas funciones en la nube.",
            "El teclado seguirá funcionando sin conexión. Tus textos y tu diccionario local permanecen en tu dispositivo.",
            "Crear cuenta",
            "Ahora no"
        )
        "fr" -> RegistrationInviteCopy(
            "Votre expérience YORY peut aller plus loin",
            "Créez un compte YORY pour être prêt lorsque les prochaines fonctions cloud seront disponibles.",
            "Votre clavier reste disponible hors ligne. Vos textes et votre dictionnaire personnel restent sur votre appareil.",
            "Créer un compte",
            "Pas maintenant"
        )
        "it" -> RegistrationInviteCopy(
            "Con YORY c'è ancora molto da scoprire",
            "Crea un account YORY per essere pronto quando arriveranno le prossime funzioni cloud.",
            "La tastiera continuerà a funzionare offline. I tuoi testi e il dizionario personale restano sul dispositivo.",
            "Crea account",
            "Non ora"
        )
        "pt" -> RegistrationInviteCopy(
            "Sua experiência com YORY pode ir além",
            "Crie uma conta YORY para estar pronto quando os próximos recursos na nuvem chegarem.",
            "Seu teclado continuará funcionando offline. Seus textos e seu dicionário pessoal ficam no dispositivo.",
            "Criar conta",
            "Agora não"
        )
        else -> RegistrationInviteCopy(
            "There's more room to grow with YORY",
            "Create a YORY account to be ready for upcoming cloud features when they arrive.",
            "Your keyboard stays available offline. Your typing and local dictionary remain on this device.",
            "Create account",
            "Not now"
        )
    }
}

private fun languageForCountry(country: String): String? = when (country.uppercase(Locale.ROOT)) {
    "EG", "SA", "AE", "JO", "KW", "QA", "BH", "OM", "IQ", "MA", "DZ", "TN", "LY", "YE" -> "ar"
    "DE", "AT" -> "de"
    "ES", "MX", "AR", "CO", "CL", "PE", "VE", "UY", "EC" -> "es"
    "FR", "BE", "MC" -> "fr"
    "IT" -> "it"
    "PT", "BR", "AO", "MZ" -> "pt"
    "US", "GB", "IE", "AU", "NZ", "IN" -> "en"
    else -> null
}
