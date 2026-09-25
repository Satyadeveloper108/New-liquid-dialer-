package com.example.data

import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.VoicemailItem

object SampleDataProvider {

  val sampleContacts = listOf(
    Contact("1", "Alexander Wright", "+1 (555) 234-5678", "mobile", 0, isFavorite = true),
    Contact("2", "Alice Morgan", "+1 (555) 345-6789", "iPhone", 1, isFavorite = true),
    Contact("3", "Benjamin Hayes", "+1 (555) 456-7890", "home", 2, isFavorite = false),
    Contact("4", "Chloe Bennett", "+1 (555) 567-8901", "mobile", 3, isFavorite = true),
    Contact("5", "Daniel Cooper", "+1 (555) 678-9012", "work", 4, isFavorite = false),
    Contact("6", "David Miller", "+1 (555) 789-0123", "mobile", 5, isFavorite = false),
    Contact("7", "Emma Watson", "+1 (555) 890-1234", "mobile", 6, isFavorite = true),
    Contact("8", "Ethan Hunt", "+1 (555) 901-2345", "iPhone", 7, isFavorite = false),
    Contact("9", "Grace Hopper", "+1 (555) 012-3456", "work", 0, isFavorite = false),
    Contact("10", "Henry Cavill", "+1 (555) 123-4567", "mobile", 1, isFavorite = false),
    Contact("11", "Isabella Ross", "+1 (555) 234-8765", "mobile", 2, isFavorite = false),
    Contact("12", "James Wilson", "+1 (555) 345-9876", "home", 3, isFavorite = false),
    Contact("13", "Liam Gallagher", "+1 (555) 456-0987", "iPhone", 4, isFavorite = true),
    Contact("14", "Maya Lin", "+1 (555) 567-1098", "mobile", 5, isFavorite = false),
    Contact("15", "Noah Centineo", "+1 (555) 678-2109", "mobile", 6, isFavorite = false),
    Contact("16", "Olivia Wilde", "+1 (555) 789-3210", "mobile", 7, isFavorite = true),
    Contact("17", "Pupa Village", "+1 (555) 890-4321", "mobile", 0, isFavorite = true),
    Contact("18", "Sophia Loren", "+1 (555) 901-5432", "home", 1, isFavorite = false),
    Contact("19", "William Turner", "+1 (555) 012-6543", "work", 2, isFavorite = false),
    Contact("20", "Zoe Saldana", "+1 (555) 123-7654", "mobile", 3, isFavorite = false),
  )

  val sampleRecents = listOf(
    CallRecord("r1", "Pupa Village", "+1 (555) 890-4321", CallType.MISSED, "10:42 AM", "Today", repeatCount = 2),
    CallRecord("r2", "Alexander Wright", "+1 (555) 234-5678", CallType.OUTGOING, "9:15 AM", "Today"),
    CallRecord("r3", "Chloe Bennett", "+1 (555) 567-8901", CallType.INCOMING, "Yesterday", "Yesterday"),
    CallRecord("r4", "Alice Morgan", "+1 (555) 345-6789", CallType.MISSED, "Yesterday", "Yesterday"),
    CallRecord("r5", "+1 (800) 555-0199", "+1 (800) 555-0199", CallType.MISSED, "Wednesday", "Sep 23", phoneType = "toll-free"),
    CallRecord("r6", "Emma Watson", "+1 (555) 890-1234", CallType.OUTGOING, "Tuesday", "Sep 22"),
    CallRecord("r7", "Liam Gallagher", "+1 (555) 456-0987", CallType.INCOMING, "Monday", "Sep 21"),
    CallRecord("r8", "Olivia Wilde", "+1 (555) 789-3210", CallType.OUTGOING, "Sep 18", "Sep 18"),
  )

  val sampleVoicemails = listOf(
    VoicemailItem(
      id = "v1",
      callerName = "Pupa Village",
      phoneNumber = "+1 (555) 890-4321",
      dateFormatted = "10:43 AM",
      durationFormatted = "0:34",
      transcription = "Hi there, just wanted to check in about the project timeline and see if you are free for lunch this afternoon. Give me a call back whenever you can!",
      isRead = false,
    ),
    VoicemailItem(
      id = "v2",
      callerName = "Alice Morgan",
      phoneNumber = "+1 (555) 345-6789",
      dateFormatted = "Yesterday",
      durationFormatted = "1:05",
      transcription = "Hey! Left some documents at the front desk for you to sign. Let me know once you grab them.",
      isRead = false,
    ),
    VoicemailItem(
      id = "v3",
      callerName = "Alexander Wright",
      phoneNumber = "+1 (555) 234-5678",
      dateFormatted = "Sep 22",
      durationFormatted = "0:22",
      transcription = "Thanks for the callback earlier, everything is confirmed for Thursday. Talk soon.",
      isRead = true,
    ),
  )
}
