package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {

  @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
  fun getAllRecordings(): Flow<List<CallRecording>>

  @Query(
    """
    SELECT * FROM call_recordings 
    WHERE phoneNumber = :phoneNumber 
       OR phoneNumber = :cleanNumber 
       OR phoneNumber LIKE '%' || :cleanNumber 
       OR :cleanNumber LIKE '%' || phoneNumber
    ORDER BY timestamp DESC
    """
  )
  fun getRecordingsForNumber(phoneNumber: String, cleanNumber: String): Flow<List<CallRecording>>

  @Query("SELECT * FROM call_recordings WHERE id = :id LIMIT 1")
  suspend fun getRecordingById(id: Long): CallRecording?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecording(recording: CallRecording): Long

  @Query("DELETE FROM call_recordings WHERE id = :id")
  suspend fun deleteRecordingById(id: Long)

  @Query("DELETE FROM call_recordings WHERE filePath = :filePath")
  suspend fun deleteRecordingByPath(filePath: String)
}
