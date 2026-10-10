package xyz.malkki.neostumbler.ui.composables.shared

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.FileUtils
import android.os.OperationCanceledException
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R

@Composable
fun FilePicker(
    modifier: Modifier = Modifier,
    directory: File,
    defaultFileName: String,
    selectedFile: File?,
    onFileSelected: (File) -> Unit,
    onFileDeleted: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val contentResolver: ContentResolver = context.contentResolver

                coroutineScope.launch(Dispatchers.IO) {
                    val selectedFileName = contentResolver.getFileName(uri) ?: defaultFileName

                    val file = directory.resolve(selectedFileName).apply { parentFile?.mkdirs() }

                    contentResolver.copyUriToLocalStorage(uri, file)
                    onFileSelected(file)
                }
            }
        }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectedFile == null) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.no_file_selected),
            )

            IconButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Icon(
                    painter = painterResource(R.drawable.file_open_24px),
                    contentDescription = stringResource(R.string.select_file),
                )
            }
        } else {
            val fileName = remember(selectedFile) { selectedFile.name }

            Text(
                modifier = Modifier.weight(1f),
                text = fileName,
            )

            IconButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        selectedFile.delete()

                        onFileDeleted()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_forever_24px),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

private suspend fun ContentResolver.getFileName(uri: Uri): String? =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()

            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            try {
                val cursor: Cursor? =
                    query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                        cancellationSignal,
                    )

                val fileName = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) it.getString(nameIndex) else null
                    } else {
                        null
                    }
                }

                if (continuation.isActive) {
                    continuation.resume(fileName)
                }
            } catch (e: OperationCanceledException) {
                if (continuation.isActive) {
                    continuation.cancel(e)
                }
            }
        }
    }

private suspend fun ContentResolver.copyUriToLocalStorage(
    sourceUri: Uri,
    destinationFile: File,
): Unit =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()

            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            try {
                val inputStream: InputStream =
                    openInputStream(sourceUri)
                        ?: throw IOException("Failed to open input stream for Uri: $sourceUri")

                inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        FileUtils.copy(input, output, cancellationSignal, null, null)
                    }
                }

                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            } catch (oce: OperationCanceledException) {
                if (continuation.isActive) {
                    continuation.cancel(oce)
                }
            } catch (e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
