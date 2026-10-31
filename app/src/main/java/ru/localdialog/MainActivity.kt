package ru.localdialog

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.localdialog.ui.DialogApp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) viewModel.startRecording()
            }
            val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result[Manifest.permission.RECORD_AUDIO] == true) viewModel.startRecording()
            }
            DialogApp(viewModel = viewModel, darkTheme = settings.darkTheme) {
                if (Build.VERSION.SDK_INT >= 33) {
                    audioPermission.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
                } else {
                    recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        handleShare(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: android.content.Intent) {
        val uri = if (intent.action == android.content.Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
        } else if (intent.action == android.content.Intent.ACTION_VIEW) intent.data else null
        if (uri != null) {
            viewModel.selectAudio(uri)
            intent.action = android.content.Intent.ACTION_MAIN
            intent.data = null
        }
    }
}
