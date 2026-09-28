package com.example.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(private val dao: CallRecordingDao) {

  fun getAllRecordings(): Flow<List<CallRecording>> = dao.getAllRecordings()

  fun getRecordingsForNumber(phoneNumber: String): Flow<List<CallRecording>> {
    val clean = phoneNumber.filter { it.isDigit() }
    val lookupKey = if (clean.length >= 7) clean.takeLast(7) else clean
    return dao.getRecordingsForNumber(phoneNumber, lookupKey)
  }

  suspend fun insertRecording(recording: CallRecording): Long {
    return withContext(Dispatchers.IO) {
      dao.insertRecording(recording)
    }
  }

  suspend fun deleteRecording(recording: CallRecording) {
    withContext(Dispatchers.IO) {
      dao.deleteRecordingById(recording.id)
      try {
        val file = File(recording.filePath)
        if (file.exists()) {
          file.delete()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
