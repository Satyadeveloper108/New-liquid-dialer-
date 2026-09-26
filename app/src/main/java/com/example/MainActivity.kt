package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.DialerApp
import com.example.ui.theme.PhoneTheme
import com.example.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

  private val dialerViewModel: DialerViewModel by viewModels()

  // System role launcher for Default Dialer if required by Android for call log
  private val roleRequestLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { _ ->
    val hasCallLog = dialerViewModel.checkCallLogPermission(this)
    dialerViewModel.onCallLogPermissionResult(hasCallLog)
  }

  // Runtime permissions launcher for startup permissions: READ_CONTACTS & READ_CALL_LOG
  private val startupPermissionsLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val contactsGranted = permissions[Manifest.permission.READ_CONTACTS]
      ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)

    val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG]
      ?: (dialerViewModel.checkCallLogPermission(this))

    dialerViewModel.onStartupPermissionsResult(
      contactsGranted = contactsGranted,
      callLogGranted = callLogGranted,
    )

    // If call log permission was not granted directly, check if system requires default dialer role
    if (!callLogGranted) {
      requestDefaultDialerRoleIfNeeded()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Automatically request READ_CONTACTS and READ_CALL_LOG at startup
    requestStartupPermissions()

    setContent {
      PhoneTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          DialerApp(
            viewModel = dialerViewModel,
            onRequestDefaultDialer = { requestDefaultDialerRoleIfNeeded() },
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    dialerViewModel.refreshPermissions(this)
  }

  private fun requestStartupPermissions() {
    val needsContacts = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.READ_CONTACTS,
    ) != PackageManager.PERMISSION_GRANTED

    val needsCallLog = !dialerViewModel.checkCallLogPermission(this)

    if (needsContacts || needsCallLog) {
      val permsToRequest = mutableListOf<String>()
      if (needsContacts) permsToRequest.add(Manifest.permission.READ_CONTACTS)
      if (needsCallLog) permsToRequest.add(Manifest.permission.READ_CALL_LOG)
      startupPermissionsLauncher.launch(permsToRequest.toTypedArray())
    } else {
      dialerViewModel.refreshPermissions(this)
    }
  }

  fun requestDefaultDialerRoleIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        roleRequestLauncher.launch(intent)
        return
      }
    } else {
      val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
      if (telecomManager != null && telecomManager.defaultDialerPackage != packageName) {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
          .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        roleRequestLauncher.launch(intent)
        return
      }
    }
  }
}
