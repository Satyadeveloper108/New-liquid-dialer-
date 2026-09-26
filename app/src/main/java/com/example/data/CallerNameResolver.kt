package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.model.Contact
import java.util.concurrent.ConcurrentHashMap

object CallerNameResolver {

  private val cache = ConcurrentHashMap<String, String>()

  fun clearCache() {
    cache.clear()
  }

  fun stripFormatting(raw: String): String {
    return raw.replace("[^0-9+]".toRegex(), "")
  }

  fun extractDigits(raw: String): String {
    return raw.filter { it.isDigit() }
  }

  /**
   * Checks if two phone numbers refer to the same subscriber line,
   * taking into account country codes (e.g. +91, 91), national trunk prefixes (0),
   * raw 10-digit formats, and punctuation/formatting differences.
   */
  fun arePhoneNumbersMatching(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false

    val cleanA = stripFormatting(a)
    val cleanB = stripFormatting(b)
    if (cleanA.isNotEmpty() && cleanA == cleanB) return true

    val digitsA = extractDigits(a)
    val digitsB = extractDigits(b)
    if (digitsA.isNotEmpty() && digitsA == digitsB) return true

    // Check 10-digit normalized phone matching (common for mobile numbers worldwide, including India)
    if (digitsA.length >= 10 && digitsB.length >= 10) {
      val last10A = digitsA.takeLast(10)
      val last10B = digitsB.takeLast(10)
      if (last10A == last10B) {
        val prefixA = digitsA.dropLast(10)
        val prefixB = digitsB.dropLast(10)

        // Matching cases:
        // +91XXXXXXXXXX (prefix 91)
        // 91XXXXXXXXXX (prefix 91)
        // 0XXXXXXXXXX (prefix 0)
        // XXXXXXXXXX (prefix "")
        if (prefixA == prefixB) return true
        if ((prefixA.isEmpty() || prefixA == "0" || prefixA == "91" || prefixA == "1") &&
          (prefixB.isEmpty() || prefixB == "0" || prefixB == "91" || prefixB == "1")
        ) {
          return true
        }
      }
    }

    // For shorter numbers (e.g. 7 digits), match if exact last 7 digits match
    if (digitsA.length >= 7 && digitsB.length >= 7) {
      if (digitsA.takeLast(7) == digitsB.takeLast(7)) {
        if (digitsA == digitsB || digitsA.endsWith(digitsB) || digitsB.endsWith(digitsA)) {
          return true
        }
      }
    }

    return false
  }

  /**
   * Generates common search variants for a given phone number to query PhoneLookup.
   */
  fun getLookupVariants(rawNumber: String): List<String> {
    val variants = LinkedHashSet<String>()
    val trimmed = rawNumber.trim()
    if (trimmed.isNotBlank()) {
      variants.add(trimmed)
    }

    val clean = stripFormatting(trimmed)
    if (clean.isNotBlank()) {
      variants.add(clean)
    }

    val digits = extractDigits(trimmed)
    if (digits.isNotBlank()) {
      variants.add(digits)
    }

    if (digits.length >= 10) {
      val last10 = digits.takeLast(10)
      variants.add(last10)
      variants.add("+91$last10")
      variants.add("91$last10")
      variants.add("0$last10")
      variants.add("+1$last10")
    }

    return variants.toList()
  }

  /**
   * Resolves caller name according to strict priority:
   * 1. Device Contacts Provider exact phone-number match.
   * 2. Normalized phone-number match (+91, 91, 0, raw, ignoring formatting).
   * 3. Telecom-provided contact/caller-ID name.
   * 4. Raw phone number as final fallback.
   */
  fun resolveCallerName(
    context: Context?,
    phoneNumber: String,
    telecomCallerName: String? = null,
    inMemoryContacts: List<Contact>? = null,
  ): String {
    val trimmedNumber = phoneNumber.trim()
    if (trimmedNumber.isBlank()) {
      return telecomCallerName?.takeIf { it.isNotBlank() } ?: "Unknown"
    }

    val cacheKey = stripFormatting(trimmedNumber)
    if (cacheKey.isNotEmpty()) {
      val cached = cache[cacheKey]
      if (cached != null) {
        return cached
      }
    }

    // 1. Check inMemoryContacts first if available
    if (!inMemoryContacts.isNullOrEmpty()) {
      // Priority 1: Exact match
      val exactMatch = inMemoryContacts.firstOrNull { contact ->
        stripFormatting(contact.phoneNumber) == cacheKey
      }
      if (exactMatch != null && exactMatch.name.isNotBlank()) {
        cache[cacheKey] = exactMatch.name.trim()
        return exactMatch.name.trim()
      }

      // Priority 2: Normalized match
      val normalizedMatch = inMemoryContacts.firstOrNull { contact ->
        arePhoneNumbersMatching(contact.phoneNumber, trimmedNumber)
      }
      if (normalizedMatch != null && normalizedMatch.name.isNotBlank()) {
        cache[cacheKey] = normalizedMatch.name.trim()
        return normalizedMatch.name.trim()
      }
    }

    // 2. Query Device Contacts Provider if context is available
    if (context != null) {
      val providerName = queryDeviceContacts(context, trimmedNumber)
      if (!providerName.isNullOrBlank()) {
        cache[cacheKey] = providerName
        return providerName
      }
    }

    // Priority 3: Telecom-provided contact/caller-ID name
    if (!telecomCallerName.isNullOrBlank() && telecomCallerName != trimmedNumber && telecomCallerName != "Unknown") {
      return telecomCallerName
    }

    // Priority 4: Raw phone number as final fallback
    return trimmedNumber.ifBlank { "Unknown" }
  }

  private fun queryDeviceContacts(context: Context, rawNumber: String): String? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
      return null
    }

    val contentResolver = context.contentResolver

    // Step A: PhoneLookup with variants
    val variants = getLookupVariants(rawNumber)
    for (variant in variants) {
      try {
        val uri = Uri.withAppendedPath(
          ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
          Uri.encode(variant),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameCol = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameCol >= 0) {
              val name = cursor.getString(nameCol)
              if (!name.isNullOrBlank()) {
                return name.trim()
              }
            }
          }
        }
      } catch (e: Exception) {
        // Continue to next variant
      }
    }

    // Step B: Query CommonDataKinds.Phone for fallback normalized matching
    try {
      val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
      )
      contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        null,
      )?.use { cursor ->
        val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
          val num = if (numCol >= 0) cursor.getString(numCol) else null
          if (!num.isNullOrBlank() && arePhoneNumbersMatching(num, rawNumber)) {
            val name = if (nameCol >= 0) cursor.getString(nameCol) else null
            if (!name.isNullOrBlank()) {
              return name.trim()
            }
          }
        }
      }
    } catch (e: Exception) {
      // Ignore
    }

    return null
  }
}
