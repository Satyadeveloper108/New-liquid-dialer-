package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.DialerApp
import com.example.ui.theme.PhoneTheme
import com.example.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

  private val dialerViewModel: DialerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PhoneTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          DialerApp(viewModel = dialerViewModel)
        }
      }
    }
  }
}
