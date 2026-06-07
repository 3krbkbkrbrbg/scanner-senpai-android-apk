package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScannerScreen
import com.example.ui.ScannerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var pendingVpnAction: (() -> Unit)? = null

  private val vpnPermissionLauncher = registerForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      pendingVpnAction?.invoke()
    }
    pendingVpnAction = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Register VPN permission helper handle
    com.example.data.VpnPermissionHelper.onPrepareRequired = { intent, onGranted ->
      pendingVpnAction = onGranted
      vpnPermissionLauncher.launch(intent)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ScannerViewModel = viewModel()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          ScannerScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onDestroy() {
    com.example.data.VpnPermissionHelper.onPrepareRequired = null
    super.onDestroy()
  }
}
