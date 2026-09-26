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

  // Track if default dialer prompt was already presented this launch session
  private var hasPromptedRoleThisLaunch = false

  // Step 1: Default Dialer Role Request Launcher
  private val roleRequestLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { _ ->
    // After user accepts or declines, refresh permissions and proceed to Contacts flow
    dialerViewModel.refreshPermissions(this)
    requestContactsPermissionFlow()
  }

  // Step 2: Contacts Permission Launcher
  private val contactsPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    dialerViewModel.onContactsPermissionResult(isGranted)
    // After contacts permission is accepted or declined, proceed to Call Log flow
    requestCallLogPermissionFlow()
  }

  // Step 3: Call Log & Phone Permissions Launcher
  private val callLogPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG]
      ?: dialerViewModel.checkCallLogPermission(this)
    dialerViewModel.onCallLogPermissionResult(callLogGranted)
    dialerViewModel.refreshPermissions(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Run first-launch permission and setup flow in strict sequential order
    startFirstLaunchSetupFlow()

    setContent {
      PhoneTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          DialerApp(
            viewModel = dialerViewModel,
            onRequestDefaultDialer = { requestDefaultDialerRole(force = true) },
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    dialerViewModel.refreshPermissions(this)
  }

  /**
   * Orchestrates the startup permission & default dialer sequence:
   * 1. Check if app already holds RoleManager.ROLE_DIALER.
   * 2. If not, immediately launch the official Android Default Phone/Dialer role request.
   * 3. After accept/decline (or if already held), continue to required Contacts permission flow.
   * 4. Then request READ_CALL_LOG when Android allows it.
   */
  private fun startFirstLaunchSetupFlow() {
    if (!isDefaultDialer() && !hasPromptedRoleThisLaunch) {
      hasPromptedRoleThisLaunch = true
      requestDefaultDialerRole(force = false)
    } else {
      // Already default dialer or already prompted this launch -> proceed directly to permissions
      requestContactsPermissionFlow()
    }
  }

  private fun isDefaultDialer(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
      val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
      telecomManager?.defaultDialerPackage == packageName
    }
  }

  private fun requestDefaultDialerRole(force: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
        if (force || !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
          val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
          roleRequestLauncher.launch(intent)
          return
        }
      }
    } else {
      val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
      if (telecomManager != null) {
        if (force || telecomManager.defaultDialerPackage != packageName) {
          val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
          roleRequestLauncher.launch(intent)
          return
        }
      }
    }

    // If role request couldn't be launched or is already held, continue flow
    requestContactsPermissionFlow()
  }

  private fun requestContactsPermissionFlow() {
    val hasContacts = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasContacts) {
      contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    } else {
      dialerViewModel.onContactsPermissionResult(true)
      requestCallLogPermissionFlow()
    }
  }

  private fun requestCallLogPermissionFlow() {
    val needsCallLog = !dialerViewModel.checkCallLogPermission(this)
    val needsCallPhone = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.CALL_PHONE,
    ) != PackageManager.PERMISSION_GRANTED
    val needsBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT,
      ) != PackageManager.PERMISSION_GRANTED
    } else {
      false
    }

    val permsToRequest = mutableListOf<String>()
    if (needsCallLog) {
      permsToRequest.add(Manifest.permission.READ_CALL_LOG)
    }
    if (needsCallPhone) {
      permsToRequest.add(Manifest.permission.CALL_PHONE)
    }
    if (needsBluetooth && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (permsToRequest.isNotEmpty()) {
      callLogPermissionLauncher.launch(permsToRequest.toTypedArray())
    } else {
      dialerViewModel.refreshPermissions(this)
    }
  }
}
