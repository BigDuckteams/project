package ru.localdialog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.localdialog.data.AppSettings
import ru.localdialog.data.ModelRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, repository: ModelRepository, revision: Int, onSettings: (AppSettings) -> Unit, onBack: () -> Unit) {
    var languages by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader("Настройки", onBack = onBack)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Тёмная тема", fontWeight = FontWeight.Medium)
                        Text("Удобно для длинных расшифровок", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(settings.darkTheme, { onSettings(settings.copy(darkTheme = it)) })
                }
                ExposedDropdownMenuBox(expanded = languages, onExpandedChange = { languages = it }) {
                    OutlinedTextField(
                        value = settings.whisperLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Язык многоязычного Whisper") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(languages) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(languages, { languages = false }) {
                        listOf("ru" to "Русский", "en" to "English", "de" to "Deutsch", "fr" to "Français", "es" to "Español", "uk" to "Українська", "auto" to "Автоопределение").forEach { (code, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { onSettings(settings.copy(whisperLanguage = code)); languages = false })
                        }
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(18.dp)) {
                Text("Приватность по умолчанию", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Аудио и расшифровки хранятся во внутреннем хранилище приложения. Сетевые запросы выполняются только при скачивании выбранной модели. Приложение не содержит аналитики и рекламы.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text("Хранилище моделей", fontWeight = FontWeight.SemiBold)
                if (revision >= 0) Text("${repository.storageBytes() / 1_048_576} МБ · ${repository.root.absolutePath}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text("О приложении", fontWeight = FontWeight.SemiBold)
                Text("Тихо 1.0 · sherpa-onnx 2.0.1\nGigaAM, Whisper, SenseVoice, Dolphin\nPyannote + 3D-Speaker", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Подсказка: точность диаризации зависит от качества записи. Одновременная речь нескольких людей может быть отнесена к одному участнику.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
