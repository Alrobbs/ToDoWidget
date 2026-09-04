package com.alexdo.todo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// ---------- Model ----------

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    /** ISO date, e.g. 2026-09-04 */
    val date: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE date = :date AND completed = 0 ORDER BY createdAt ASC")
    suspend fun openFor(date: String): List<Todo>

    @Query("SELECT * FROM todos WHERE date = :date ORDER BY completed ASC, createdAt ASC")
    fun watch(date: String): Flow<List<Todo>>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Query("UPDATE todos SET completed = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("UPDATE todos SET text = :text WHERE id = :id")
    suspend fun setText(id: Long, text: String)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT DISTINCT date FROM todos WHERE completed = 0")
    fun datesWithOpenItems(): Flow<List<String>>
}

@Database(entities = [Todo::class], version = 1, exportSchema = false)
abstract class TodoDb : RoomDatabase() {
    abstract fun dao(): TodoDao

    companion object {
        @Volatile private var instance: TodoDb? = null

        fun get(context: Context): TodoDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TodoDb::class.java,
                "todos.db"
            ).build().also { instance = it }
        }
    }
}

// ---------- Settings ----------

enum class Align { START, CENTER, END }

data class WidgetSettings(
    val fontSizeSp: Int = 16,
    val align: Align = Align.START,
    val darkText: Boolean = false,
    /** 0..70 percent black scrim behind the widget, for legibility on light wallpapers. */
    val backgroundDim: Int = 0,
    val confirmOnComplete: Boolean = true,
    val showDivider: Boolean = false
)

private val Context.settingsStore by preferencesDataStore("widget_settings")

object Settings {
    private val FONT = intPreferencesKey("font_size_v2")
    private val ALIGN = stringPreferencesKey("align")
    private val DARK = booleanPreferencesKey("dark_text")
    private val DIM = intPreferencesKey("bg_dim")
    private val CONFIRM = booleanPreferencesKey("confirm")
    private val DIVIDER = booleanPreferencesKey("divider_v2")

    private fun from(p: Preferences) = WidgetSettings(
        fontSizeSp = p[FONT] ?: 16,
        align = runCatching { Align.valueOf(p[ALIGN] ?: "START") }.getOrDefault(Align.START),
        darkText = p[DARK] ?: false,
        backgroundDim = p[DIM] ?: 0,
        confirmOnComplete = p[CONFIRM] ?: true,
        showDivider = p[DIVIDER] ?: false
    )

    fun flow(context: Context): Flow<WidgetSettings> =
        context.settingsStore.data.map { from(it) }

    suspend fun read(context: Context): WidgetSettings =
        from(context.settingsStore.data.first())

    suspend fun save(context: Context, s: WidgetSettings) {
        context.settingsStore.edit { p ->
            p[FONT] = s.fontSizeSp
            p[ALIGN] = s.align.name
            p[DARK] = s.darkText
            p[DIM] = s.backgroundDim
            p[CONFIRM] = s.confirmOnComplete
            p[DIVIDER] = s.showDivider
        }
    }
}

// ---------- Shared helpers ----------

object Fmt {
    fun iso(date: LocalDate): String = date.toString()

    /** "Thu 4 Sep" for other days, "Today" / "Tomorrow" / "Yesterday" for the near ones. */
    fun label(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> {
            val dow = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            val mon = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            "$dow ${date.dayOfMonth} $mon"
        }
    }
}
