package com.matejdro.pebblenotificationcenter.tools.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import com.matejdro.pebblenotificationcenter.tools.ui.errors.InvalidImportFileException
import com.matejdro.pebblenotificationcenter.tools.ui.settings.RULES_JSON_NAME
import com.matejdro.pebblenotificationcenter.tools.ui.settings.Entry
import com.matejdro.pebblenotificationcenter.tools.ui.settings.ExportedRuleJson
import com.matejdro.pebblenotificationcenter.tools.ui.settings.SettingsCodec
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.firstSuccessOrThrow
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
         val dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
         val settingsZipFile = File(exportFolder, "pnc2_export_$dateStamp.zip")

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
         rulesRepository.importRules(
            exported.rules.map { rule -> RuleMetadata(rule.id, rule.name) },
            exported.rulePreferences
         )

         exported.globalPreferences?.let { imported -> preferenceStore.updateData { imported } }
      }

      emit(Outcome.Success(Unit))
   }

   private data class SettingsExportData(
      val rules: List<ExportedRuleJson>,
      val globalPreferences: Preferences?,
      val rulePreferences: Map<Int, Preferences>,
   )

   @Suppress("ThrowsCount")
   private suspend fun readSettingsExport(uri: Uri): SettingsExportData {
      var rulesJsonText: String? = null
      var globalFile: Pair<String, ByteArray>? = null
      val ruleFiles = HashMap<Int, Pair<String, ByteArray>>()

      val zipInputStream = context.contentResolver.openInputStream(uri)?.buffered()
         ?: throw InvalidImportFileException("Cannot read the selected file")
      try {
         ZipInputStream(zipInputStream).use { archive ->
            var entry = archive.nextEntry
            while (entry != null) {
               when (val classified = SettingsCodec.classifyEntry(entry.name)) {
                  Entry.RulesJson -> rulesJsonText = archive.readBytes().decodeToString()
                  Entry.GlobalDataStore -> globalFile = entry.name to archive.readBytes()
                  is Entry.RuleDataStore -> ruleFiles[classified.ruleId] = entry.name to archive.readBytes()
                  Entry.Ignored -> Unit
               }

               archive.closeEntry()
               entry = archive.nextEntry
            }
         }
      } catch (e: IOException) {
         throw InvalidImportFileException("Cannot open settings export: not a valid zip archive", e)
      } catch (e: IllegalArgumentException) {
         throw InvalidImportFileException("Invalid settings export entry name", e)
      }

      val rulesJson = rulesJsonText
         ?: throw InvalidImportFileException("Missing rules.json in settings export")

      val rules = try {
         SettingsCodec.decodeRules(rulesJson)
      } catch (e: SerializationException) {
         throw InvalidImportFileException("Cannot decode rules.json", e)
      } catch (e: IllegalArgumentException) {
         throw InvalidImportFileException("Cannot decode rules.json", e)
      }

      val tempFolder = File(context.cacheDir, "settings_import").apply { mkdirs() }
      val tempScope = CoroutineScope(coroutineContext + SupervisorJob())
      val corruptionHandler = ReplaceFileCorruptionHandler<Preferences> { throw CorruptImportFileException() }

      try {
         val globalPreferences = globalFile?.let { (name, bytes) ->
            decodeSettingsDataStoreFile(File(tempFolder, name), bytes, tempScope, corruptionHandler)
         }

         val rulePreferences = ruleFiles.mapValues { entry ->
            val (name, bytes) = entry.value
            decodeSettingsDataStoreFile(File(tempFolder, name), bytes, tempScope, corruptionHandler)
         }

         return SettingsExportData(
            rules = rules,
            globalPreferences = globalPreferences,
            rulePreferences = rulePreferences,
         )
      } finally {
         tempScope.cancel()
         tempFolder.deleteRecursively()
      }
   }

   private suspend fun decodeSettingsDataStoreFile(
      file: File,
      bytes: ByteArray,
      scope: CoroutineScope,
      corruptionHandler: ReplaceFileCorruptionHandler<Preferences>,
   ): Preferences {
      file.writeBytes(bytes)

      return try {
         PreferenceDataStoreFactory.create(
            corruptionHandler = corruptionHandler,
            scope = scope,
            produceFile = { file },
         ).data.first()
      } catch (e: CancellationException) {
         throw e
      } catch (e: Exception) {
         throw InvalidImportFileException("Corrupt or invalid settings data file: ${file.name}", e)
      }
   }

   fun <T> updatePreference(key: PreferenceKeyWithDefault<T>, value: T) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "ToolsViewModel.updatePreference($key)" }
      preferenceStore.edit {
         it[key] = value
      }
   }
}

private class CorruptImportFileException : Exception("Corrupt or invalid settings data file")

data class ToolsState(
   val versionName: String,
   val preferences: Preferences,
)

private const val ZIP_BUFFER_SIZE = 1024
