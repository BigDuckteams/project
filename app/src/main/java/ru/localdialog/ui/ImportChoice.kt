package ru.localdialog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportChoice(onImport: (android.net.Uri) -> Unit, onRecord: () -> Unit, onBack: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BackHeader("Добавить аудио", onBack = onBack)
        Spacer(Modifier.weight(1f))
        SonicOrb(Modifier.size(170.dp))
        Spacer(Modifier.height(22.dp))
        Text("Что расшифруем?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Или продиктуйте разговор прямо сейчас", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = { launcher.launch(arrayOf("audio/*", "video/*")) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.AudioFile, null)
            Spacer(Modifier.size(8.dp))
            Text("Выбрать файл")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRecord, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Mic, null)
            Spacer(Modifier.size(8.dp))
            Text("Записать с микрофона")
        }
        Spacer(Modifier.weight(1f))
    }
}
