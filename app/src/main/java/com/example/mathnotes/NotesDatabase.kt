package com.example.mathnotes

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val title: String = "",
 val body: String = "",
 val updatedAt: Long = System.currentTimeMillis(),
 @ColumnInfo(defaultValue = "0") val pinned: Boolean = false
)

@Dao
interface NotesDao {
 @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
 fun observeAll(): Flow<List<Note>>
 @Insert suspend fun insert(note: Note): Long
 @Update suspend fun update(note: Note)
 @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Note::class], version = 2, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
 abstract fun notesDao(): NotesDao
 companion object {
  val MIGRATION_1_2 = object : androidx.room.migration.Migration(1,2) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
   }
  }
 }
}
