package com.nanami.koishi.feature.tools.text_apps.hash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.text_apps.common.ImportedTextFile
import com.nanami.koishi.feature.tools.text_apps.common.TEXT_FILE_MIME_TYPES
import com.nanami.koishi.feature.tools.text_apps.common.TextEditorField
import com.nanami.koishi.feature.tools.text_apps.common.TextToolLargeFileCard
import com.nanami.koishi.feature.tools.text_apps.common.TextToolTopBar
import com.nanami.koishi.feature.tools.text_apps.common.copyTextToClipboard
import com.nanami.koishi.feature.tools.text_apps.common.readTextFromClipboard
import com.nanami.koishi.feature.tools.text_apps.common.rememberTextFileImportLauncher
import com.nanami.koishi.feature.tools.text_apps.common.textImportErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun HashRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var importedFile by remember { mutableStateOf<ImportedTextFile?>(null) }
    var digests by remember { mutableStateOf<Map<HashAlgorithm, String>>(emptyMap()) }
    var uppercase by rememberSaveable { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.text_tool_copied)
    val largeFile = importedFile?.takeIf { !it.isInline }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val importLauncher = rememberTextFileImportLauncher(
        onImported = { file ->
            importedFile = file
            input = file.inlineText.orEmpty()
            showMessage(context.getString(R.string.text_tool_imported, file.displayName))
        },
        onError = { error -> showMessage(textImportErrorMessage(context, error)) }
    )

    LaunchedEffect(input, uppercase, largeFile?.uri) {
        isProcessing = true
        val result = if (largeFile != null) {
            HashFileEngine.digestFile(context, largeFile.uri)
        } else {
            delay(150)
            val source = input
            if (source != input) {
                isProcessing = false
                return@LaunchedEffect
            }
            withContext(Dispatchers.Default) { Result.success(HashEngine.digestAll(source)) }
        }
        result.onSuccess { values ->
            digests = values.mapValues { (_, digest) ->
                if (uppercase) digest.uppercase(Locale.ROOT) else digest.lowercase(Locale.ROOT)
            }
        }.onFailure {
            digests = emptyMap()
        }
        isProcessing = false
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_text_hash_name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(TEXT_FILE_MIME_TYPES) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = stringResource(R.string.text_tool_import_file))
                    }
                    IconButton(onClick = {
                        input = readTextFromClipboard(context)
                        importedFile = null
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.text_tool_paste))
                    }
                    IconButton(onClick = {
                        input = ""
                        importedFile = null
                        digests = emptyMap()
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 920.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (largeFile != null) {
                    TextToolLargeFileCard(file = largeFile)
                } else {
                    TextEditorField(
                        value = input,
                        onValueChange = {
                            input = it
                            importedFile = null
                        },
                        label = stringResource(R.string.text_tool_input),
                        placeholder = stringResource(R.string.text_hash_input_placeholder),
                        minLines = 7,
                        maxLines = 12
                    )
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                FilterChip(
                    selected = uppercase,
                    onClick = { uppercase = !uppercase },
                    label = { Text(stringResource(R.string.text_hash_uppercase)) }
                )

                HashAlgorithm.entries.forEach { algorithm ->
                    val digest = digests[algorithm].orEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = algorithm.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    enabled = digest.isNotEmpty(),
                                    onClick = {
                                        copyTextToClipboard(context, digest)
                                        showMessage(copied)
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = digest,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val HashAlgorithm.displayName: String
    get() = when (this) {
        HashAlgorithm.MD5 -> "MD5"
        HashAlgorithm.SHA_1 -> "SHA-1"
        HashAlgorithm.SHA_256 -> "SHA-256"
        HashAlgorithm.SHA_512 -> "SHA-512"
    }
