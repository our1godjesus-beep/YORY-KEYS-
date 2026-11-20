package app.yory.keys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("YORY KEYS", style = MaterialTheme.typography.headlineLarge)
                        Text("Your keyboard. Your universe.")
                        Text("First milestone: enable the real YORY keyboard, then select it.")
                        Button(onClick = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }) {
                            Text("Open keyboard settings")
                        }
                        Button(onClick = {
                            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            manager.showInputMethodPicker()
                        }) {
                            Text("Choose YORY KEYS")
                        }
                    }
                }
            }
        }
    }
}
