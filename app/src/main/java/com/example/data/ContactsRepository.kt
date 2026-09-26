package com.example.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactsRepository {

  fun hasContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun hasCallLogPermission(context: Context): Boolean {
    val permGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CALL_LOG,
    ) == PackageManager.PERMISSION_GRANTED
    if (permGranted) return true

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)) {
        return true
      }
    }
    return false
  }

  suspend fun getDeviceContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
    if (!hasContactsPermission(context)) {
      return@withContext emptyList()
    }

    val contactsMap = mutableMapOf<String, Contact>()
    val contentResolver: ContentResolver = context.contentResolver

    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.TYPE,
      ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
      ContactsContract.CommonDataKinds.Phone.STARRED,
    )

    val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

    try {
      contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
        val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
        val starredCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

        var index = 0
        while (cursor.moveToNext()) {
          val contactId = if (idCol >= 0) cursor.getString(idCol) ?: "c_$index" else "c_$index"
          val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
          val rawNumber = if (numCol >= 0) cursor.getString(numCol) ?: "" else ""
          val typeInt = if (typeCol >= 0) cursor.getInt(typeCol) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
          val photoUri = if (photoCol >= 0) cursor.getString(photoCol) else null
          val isStarred = if (starredCol >= 0) cursor.getInt(starredCol) == 1 else false

          val typeStr = when (typeInt) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "home"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "work"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "main"
            else -> "mobile"
          }

          // Use normalized contact identity to group multiple numbers if needed
          if (!contactsMap.containsKey(contactId) && rawNumber.isNotBlank()) {
            val colorIndex = Math.abs(name.hashCode()) % 8
            contactsMap[contactId] = Contact(
              id = contactId,
              name = name.trim(),
              phoneNumber = rawNumber.trim(),
              type = typeStr,
              avatarColorIndex = colorIndex,
              isFavorite = isStarred,
              photoUri = photoUri,
              isSystemContact = true,
            )
          }
          index++
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return@withContext contactsMap.values.toList().sortedBy { it.name.lowercase() }
  }

  suspend fun getDeviceCallLogs(context: Context): List<CallRecord> = withContext(Dispatchers.IO) {
    if (!hasCallLogPermission(context)) {
      return@withContext emptyList()
    }

    val callLogs = mutableListOf<CallRecord>()
    val contentResolver: ContentResolver = context.contentResolver

    val projection = arrayOf(
      CallLog.Calls._ID,
      CallLog.Calls.CACHED_NAME,
      CallLog.Calls.NUMBER,
      CallLog.Calls.TYPE,
      CallLog.Calls.DATE,
      CallLog.Calls.DURATION,
    )

    val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 50"

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val now = Date()
    val todayString = todayFormat.format(now)

    try {
      contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
        val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val numCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
        val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)
        val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)

        while (cursor.moveToNext()) {
          val id = if (idCol >= 0) cursor.getString(idCol) ?: "" else ""
          val name = if (nameCol >= 0) cursor.getString(nameCol) else null
          val number = if (numCol >= 0) cursor.getString(numCol) ?: "Unknown" else "Unknown"
          val type = if (typeCol >= 0) cursor.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
          val dateMillis = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

          val callDate = Date(dateMillis)
          val dateStr = if (todayFormat.format(callDate) == todayString) {
            "Today"
          } else {
            dateFormat.format(callDate)
          }
          val timeStr = timeFormat.format(callDate)

          val callType = when (type) {
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
            else -> CallType.INCOMING
          }

          callLogs.add(
            CallRecord(
              id = id.ifBlank { dateMillis.toString() },
              contactName = if (!name.isNullOrBlank()) name else number,
              phoneNumber = number,
              callType = callType,
              timeFormatted = timeStr,
              dateFormatted = dateStr,
              repeatCount = 1,
              phoneType = "mobile",
            ),
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return@withContext callLogs
  }

  suspend fun addDeviceContact(
    context: Context,
    displayName: String,
    phoneNumber: String,
    phoneType: String = "mobile",
  ): Boolean = withContext(Dispatchers.IO) {
    if (displayName.isBlank() || phoneNumber.isBlank()) return@withContext false

    val ops = ArrayList<ContentProviderOperation>()

    val rawContactInsertIndex = ops.size
    ops.add(
      ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        .build(),
    )

    // Name
    ops.add(
      ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName.trim())
        .build(),
    )

    // Phone Number
    val typeInt = when (phoneType.lowercase()) {
      "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
      "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
      else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
    }

    ops.add(
      ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.trim())
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, typeInt)
        .build(),
    )

    try {
      context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  suspend fun toggleFavoriteInDevice(
    context: Context,
    contactId: String,
    makeFavorite: Boolean,
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val values = ContentValues().apply {
        put(ContactsContract.Contacts.STARRED, if (makeFavorite) 1 else 0)
      }
      val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
      val rows = context.contentResolver.update(uri, values, null, null)
      rows > 0
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  suspend fun seedDemoContactsToDevice(context: Context): Int = withContext(Dispatchers.IO) {
    val sampleList = listOf(
      Triple("Alexander Wright", "+1 (555) 234-5678", "mobile"),
      Triple("Alice Morgan", "+1 (555) 345-6789", "mobile"),
      Triple("Benjamin Hayes", "+1 (555) 456-7890", "home"),
      Triple("Chloe Bennett", "+1 (555) 567-8901", "mobile"),
      Triple("Daniel Cooper", "+1 (555) 678-9012", "work"),
      Triple("Emma Watson", "+1 (555) 890-1234", "mobile"),
      Triple("Liam Gallagher", "+1 (555) 456-0987", "mobile"),
      Triple("Olivia Wilde", "+1 (555) 789-3210", "mobile"),
      Triple("Pupa Village", "+1 (555) 890-4321", "mobile"),
    )

    var count = 0
    for ((name, phone, type) in sampleList) {
      if (addDeviceContact(context, name, phone, type)) {
        count++
      }
    }
    count
  }
}
