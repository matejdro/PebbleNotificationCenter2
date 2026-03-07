package com.matejdro.pebblenotificationcenter.tools.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.matejdro.notificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.set
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.logging.FileLoggingController
import com.matejdro.pebblenotificationcenter.navigation.keys.ToolsScreenKey
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
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
) : SingleScreenViewModel<ToolsScreenKey>(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<ToolsState>>(Outcome.Progress())
   val appVersion: StateFlow<Outcome<ToolsState>>
      get() = _uiState

   private val _logSave = MutableStateFlow<Outcome<Uri?>>(Outcome.Success(null))
   val logSave: StateFlow<Outcome<Uri?>> = _logSave

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

   fun resetLog() {
      actionLogger.logAction { "ToolsViewModel.resetLog()" }
      _logSave.value = Outcome.Success(null)
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
