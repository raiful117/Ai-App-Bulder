package com.example.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.AiProviderSettings
import com.example.model.AiProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "bcf_builder_settings")

class StudioPreferences(private val context: Context) {
    private object Keys {
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val BUILD_WORKER_URL = stringPreferencesKey("build_worker_url")
        val BUILD_WORKER_TOKEN = stringPreferencesKey("build_worker_token")
        val AUTOSAVE = booleanPreferencesKey("autosave_enabled")
        val EDITOR_FONT_SP = intPreferencesKey("editor_font_sp")
    }

    val settingsFlow: Flow<AiProviderSettings> = context.studioDataStore.data.map { prefs ->
        val providerName = prefs[Keys.AI_PROVIDER] ?: AiProviderType.GEMINI_FIREBASE.name
        val provider = AiProviderType.entries.firstOrNull { it.name == providerName }
            ?: AiProviderType.GEMINI_FIREBASE
        AiProviderSettings(
            provider = provider,
            modelName = prefs[Keys.MODEL_NAME]?.ifBlank { provider.defaultModel } ?: provider.defaultModel,
            customEndpointUrl = prefs[Keys.CUSTOM_ENDPOINT].orEmpty(),
            customApiKey = prefs[Keys.CUSTOM_API_KEY].orEmpty(),
            buildWorkerUrl = prefs[Keys.BUILD_WORKER_URL].orEmpty(),
            buildWorkerToken = prefs[Keys.BUILD_WORKER_TOKEN].orEmpty(),
            autosaveEnabled = prefs[Keys.AUTOSAVE] ?: true,
            editorFontSizeSp = prefs[Keys.EDITOR_FONT_SP] ?: 12
        )
    }

    suspend fun saveSettings(settings: AiProviderSettings) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = settings.provider.name
            prefs[Keys.MODEL_NAME] = settings.modelName.trim()
            prefs[Keys.CUSTOM_ENDPOINT] = settings.customEndpointUrl.trim()
            prefs[Keys.CUSTOM_API_KEY] = settings.customApiKey.trim()
            prefs[Keys.BUILD_WORKER_URL] = settings.buildWorkerUrl.trim()
            prefs[Keys.BUILD_WORKER_TOKEN] = settings.buildWorkerToken.trim()
            prefs[Keys.AUTOSAVE] = settings.autosaveEnabled
            prefs[Keys.EDITOR_FONT_SP] = settings.editorFontSizeSp.coerceIn(10, 18)
        }
    }
}

@Entity(tableName = "local_projects")
data class LocalProjectEntity(
    @PrimaryKey val projectId: String,
    val userId: String,
    val name: String,
    val packageName: String,
    val jsonPayload: String,
    val updatedAtMs: Long,
    val dirtyOffline: Boolean
)

@Entity(tableName = "project_snapshots")
data class ProjectSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val projectId: String,
    val userId: String,
    val label: String,
    val jsonPayload: String,
    val createdAtMs: Long
)

@Dao
interface LocalProjectDao {
    @Query("SELECT * FROM local_projects WHERE userId = :userId ORDER BY updatedAtMs DESC")
    fun observeLocalProjects(userId: String): Flow<List<LocalProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: LocalProjectEntity)

    @Query("DELETE FROM local_projects WHERE projectId = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM project_snapshots WHERE projectId = :projectId ORDER BY createdAtMs DESC LIMIT 20")
    fun observeSnapshots(projectId: String): Flow<List<ProjectSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ProjectSnapshotEntity)

    @Query("DELETE FROM project_snapshots WHERE snapshotId = :snapshotId")
    suspend fun deleteSnapshot(snapshotId: String)
}

@Database(
    entities = [LocalProjectEntity::class, ProjectSnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalStudioDatabase : RoomDatabase() {
    abstract fun projectDao(): LocalProjectDao

    companion object {
        @Volatile
        private var INSTANCE: LocalStudioDatabase? = null

        fun getInstance(context: Context): LocalStudioDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalStudioDatabase::class.java,
                    "bcf_builder_local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
