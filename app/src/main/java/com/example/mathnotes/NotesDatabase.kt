package com.example.mathnotes
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName = "notes")
data class Note(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String = "", val body: String = "", val updatedAt: Long = System.currentTimeMillis())
@Dao
interface NotesDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun observeAll(): Flow<List<Note>>
    @Insert suspend fun insert(note: Note): Long
    @Update suspend fun update(note: Note)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: Long)
}
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() { abstract fun notesDao(): NotesDao }
