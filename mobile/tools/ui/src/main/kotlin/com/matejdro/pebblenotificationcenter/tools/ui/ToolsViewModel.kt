package com.matejdro.pebblenotificationcenter.tools.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.matejdro.pebble.common.logging.FileLoggingController
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.keys.ToolsScreenKey
import com.matejdro.pebblenotificationcenter.rules.GLOBAL_PREFERENCES_DATASTORE_NAME
import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.rules.RulesRepository
import com.matejdro.pebblenotificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.set
import com.matejdro.pebblenotificationcenter.tools.ui.settings.ProcessKiller
import com.matejdro.pebblenotificationcenter.tools.ui.settings.RULES_JSON_NAME
import com.matejdro.pebblenotificationcenter.tools.ui.settings.Entry
import com.matejdro.pebblenotificationcenter.tools.ui.settings.ExportedRuleJson
import com.matejdro.pebblenotificationcenter.tools.ui.settings.SettingsCodec
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.firstSuccessOrThrow
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Stable
@Inject
@ContributesScopedService
class ToolsViewModel(
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
   private val context: Context,
   private val fileLoggingController: FileLoggingController,
   private val preferenceStore: DataStore<Preferences>,
   private val rulesRepository: RulesRepository,
   private val processKiller: ProcessKiller,
) : SingleScreenViewModel<ToolsScreenKey>(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<ToolsState>>(Outcome.Progress())
   val appVersion: StateFlow<Outcome<ToolsState>>
      get() = _uiState

   private val _logSave = MutableStateFlow<Outcome<Uri?>>(Outcome.Success(null))
   val logSave: StateFlow<Outcome<Uri?>> = _logSave

   private val _settingsExport = MutableStateFlow<Outcome<Uri?>>(Outcome.Success(null))
   val settingsExport: StateFlow<Outcome<Uri?>> = _settingsExport

   private val _settingsImport = MutableStateFlow<Outcome<Unit>>(Outcome.Success(Unit))
   val settingsImport: StateFlow<Outcome<Unit>> = _settingsImport

   override fun onServiceRegistered() {
      actionLogger.logAction { "ToolsViewModel.onServiceRegistered()" }

      val pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
      val versionName = pInfo.versionName.orEmpty()

      resources.launchResourceControlTask(_uiState) {
         emitAll(
            preferenceStore.data.map { preferences ->
               Outcome.Success(
                  ToolsState(
                     versionName,
                     preferences
                  )
               )
            }
         )
      }
   }

   @Suppress("MissingUseCall") // Stream wrapping is fine
   fun getLogs() = resources.launchResourceControlTask(_logSave) {
      actionLogger.logAction { "ToolsViewModel.getLogs()" }

      val zipUri = withDefault {
         fileLoggingController.flush()

         val logFolder = fileLoggingController.getLogFolder()
         File(logFolder, "device.txt").writeText(fileLoggingController.getDeviceInfo())

         val logsZipFile = File(logFolder, "logs.zip")
         ZipOutputStream(FileOutputStream(logsZipFile).buffered()).use { zipOutputStream ->
            zipOutputStream.addAllLogsToZip(logFolder, logsZipFile)
         }

         FileProvider.getUriForFile(context, "com.matejdro.pebblenotificationcenter2.logs", logsZipFile)
      }

      emit(Outcome.Success(zipUri))
   }

   private fun ZipOutputStream.addAllLogsToZip(logFolder: File, logsZipFile: File) {
      val buffer = ByteArray(ZIP_BUFFER_SIZE)

      for (logFile in logFolder.listFiles().orEmpty()) {
         if (logFile == logsZipFile) {
            continue
         }

         val zipEntry = ZipEntry(logFile.name)
         putNextEntry(zipEntry)
         FileInputStream(logFile).use { logFileInputStream ->
            var readBytes: Int
            while ((logFileInputStream.read(buffer).also { readBytes = it }) > 0) {
               write(buffer, 0, readBytes)
            }
         }
         closeEntry()
      }
   }

   private fun ZipOutputStream.addEntryBytes(name: String, bytes: ByteArray) {
      putNextEntry(ZipEntry(name))
      write(bytes)
      closeEntry()
   }

   private fun ZipOutputStream.addEntryFile(file: File) {
      val buffer = ByteArray(ZIP_BUFFER_SIZE)

      putNextEntry(ZipEntry(file.name))
      FileInputStream(file).use { fileInputStream ->
         var readBytes: Int
         while ((fileInputStream.read(buffer).also { readBytes = it }) > 0) {
            write(buffer, 0, readBytes)
         }
      }
      closeEntry()
   }

   fun resetLog() {
      actionLogger.logAction { "ToolsViewModel.resetLog()" }
      _logSave.value = Outcome.Success(null)
   }

   fun exportSettings() = resources.launchResourceControlTask(_settingsExport) {
      actionLogger.logAction { "ToolsViewModel.exportSettings()" }

      val zipUri = withDefault {
         val rules = rulesRepository.getAll().firstSuccessOrThrow()

         val exportFolder = File(context.cacheDir, "settings_export").apply { mkdirs() }
         val settingsZipFile = File(exportFolder, "settings_export.zip")

         // DataStore files are written atomically, so copying one mid-write may snapshot the previous state
         val globalPreferencesFile = context.preferencesDataStoreFile(GLOBAL_PREFERENCES_DATASTORE_NAME)
         val rulePreferencesFiles = rules
            .map { context.preferencesDataStoreFile(it.id.toString()) }
            .filter { it.exists() }

         FileOutputStream(settingsZipFile).buffered().use { outputStream ->
            ZipOutputStream(outputStream).use { zipOutputStream ->
               zipOutputStream.addEntryBytes(RULES_JSON_NAME, SettingsCodec.encodeRules(rules).toByteArray())

               if (globalPreferencesFile.exists()) {
                  zipOutputStream.addEntryFile(globalPreferencesFile)
               }

               for (rulePreferencesFile in rulePreferencesFiles) {
                  zipOutputStream.addEntryFile(rulePreferencesFile)
               }
            }
         }

         FileProvider.getUriForFile(context, "com.matejdro.pebblenotificationcenter2.logs", settingsZipFile)
      }

      emit(Outcome.Success(zipUri))
   }

   fun resetSettingsExport() {
      actionLogger.logAction { "ToolsViewModel.resetSettingsExport()" }
      _settingsExport.value = Outcome.Success(null)
   }

   fun importSettings(uri: Uri) = resources.launchResourceControlTask(_settingsImport) {
      actionLogger.logAction { "ToolsViewModel.importSettings()" }

      withDefault {
         val exported = readSettingsExport(uri)
         rulesRepository.importRules(exported.rules.map { rule -> RuleMetadata(rule.id, rule.name) })

         val globalPreferencesFile = context.preferencesDataStoreFile(GLOBAL_PREFERENCES_DATASTORE_NAME)
         exported.globalFileBytes?.let { bytes -> writeDataStoreFile(bytes, globalPreferencesFile) }

         for ((ruleId, bytes) in exported.ruleFileBytes) {
            writeDataStoreFile(bytes, context.preferencesDataStoreFile(ruleId.toString()))
         }

         deleteStaleRuleDataStoreFiles(exported.ruleFileBytes.keys)
      }

      emit(Outcome.Success(Unit))
      processKiller.kill()
   }

   private data class SettingsExportData(
      val rules: List<ExportedRuleJson>,
      val globalFileBytes: ByteArray?,
      val ruleFileBytes: Map<Int, ByteArray>,
   )

   private fun readSettingsExport(uri: Uri): SettingsExportData {
      var rulesJsonText: String? = null
      var globalFileBytes: ByteArray? = null
      val ruleFileBytes = HashMap<Int, ByteArray>()

      val zipInputStream = context.contentResolver.openInputStream(uri)?.buffered()
         ?: throw IllegalArgumentException("Cannot open settings export: $uri")
      ZipInputStream(zipInputStream).use { archive ->
         var entry = archive.nextEntry
         while (entry != null) {
            when (val classified = SettingsCodec.classifyEntry(entry.name)) {
               Entry.RulesJson -> rulesJsonText = archive.readBytes().decodeToString()
               Entry.GlobalDataStore -> globalFileBytes = archive.readBytes()
               is Entry.RuleDataStore -> ruleFileBytes[classified.ruleId] = archive.readBytes()
               Entry.Ignored -> Unit
            }

            archive.closeEntry()
            entry = archive.nextEntry
         }
      }

      val rulesJson = rulesJsonText
         ?: throw IllegalArgumentException("Missing rules.json in settings export")

      return SettingsExportData(
         rules = SettingsCodec.decodeRules(rulesJson),
         globalFileBytes = globalFileBytes,
         ruleFileBytes = ruleFileBytes,
      )
   }

   private fun writeDataStoreFile(bytes: ByteArray, file: File) {
      val tempFile = File(file.parentFile, file.name + ".tmp")
      tempFile.writeBytes(bytes)

      if (!tempFile.renameTo(file)) {
         tempFile.delete()
         file.writeBytes(bytes)
      }
   }

   private fun deleteStaleRuleDataStoreFiles(keptRuleIds: Set<Int>) {
      val datastoreFolder = File(context.filesDir, "datastore")

      datastoreFolder.listFiles().orEmpty()
         .filter { file -> file.name.endsWith(".preferences_pb") || file.name.endsWith(".preferences_pb.tmp") }
         .forEach { file ->
            val ruleId = file.name
               .removeSuffix(".tmp")
               .removeSuffix(".preferences_pb")
               .toIntOrNull()
            if (ruleId != null && ruleId !in keptRuleIds) {
               file.delete()
            }
         }
   }

   fun <T> updatePreference(key: PreferenceKeyWithDefault<T>, value: T) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "ToolsViewModel.updatePreference($key)" }
      preferenceStore.edit {
         it[key] = value
      }
   }
}

data class ToolsState(
   val versionName: String,
   val preferences: Preferences,
)

private const val ZIP_BUFFER_SIZE = 1024
