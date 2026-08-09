package com.unmark.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unmark.app.R
import com.unmark.app.util.BatchItemResult
import com.unmark.app.util.BatchOutcome
import com.unmark.app.util.BatchProcessor
import com.unmark.app.util.VendorWatermark
import com.unmark.app.util.VendorWatermarks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fixedVendor by remember { mutableStateOf<VendorWatermark?>(null) }
    var vendorPickerExpanded by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<List<BatchItemResult>?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris = uris
            results = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !isProcessing) {
                        Text(stringResource(R.string.batch_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.batch_hint),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedButton(
                onClick = {
                    pickLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (pickedUris.isEmpty()) stringResource(R.string.batch_pick_photos)
                    else stringResource(R.string.batch_photos_selected, pickedUris.size)
                )
            }

            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.batch_area_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        fixedVendor?.displayName ?: stringResource(R.string.batch_area_auto),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { fixedVendor = null },
                            enabled = !isProcessing
                        ) { Text(stringResource(R.string.batch_area_use_auto)) }
                        Box {
                            OutlinedButton(
                                onClick = { vendorPickerExpanded = true },
                                enabled = !isProcessing
                            ) { Text(stringResource(R.string.batch_area_pick_vendor)) }
                            DropdownMenu(
                                expanded = vendorPickerExpanded,
                                onDismissRequest = { vendorPickerExpanded = false }
                            ) {
                                for (vendor in VendorWatermarks.KNOWN) {
                                    DropdownMenuItem(
                                        text = { Text(vendor.displayName) },
                                        onClick = { fixedVendor = vendor; vendorPickerExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val uris = pickedUris
                    isProcessing = true
                    progressDone = 0
                    progressTotal = uris.size
                    results = null
                    scope.launch(Dispatchers.Default) {
                        val outcome = BatchProcessor.processAll(context, uris, fixedVendor) { done, total ->
                            progressDone = done
                            progressTotal = total
                        }
                        results = outcome
                        isProcessing = false
                    }
                },
                enabled = pickedUris.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(
                    if (isProcessing) stringResource(R.string.batch_processing, progressDone, progressTotal)
                    else stringResource(R.string.batch_process, pickedUris.size)
                )
            }

            if (isProcessing) {
                LinearProgressIndicator(
                    progress = { if (progressTotal == 0) 0f else progressDone.toFloat() / progressTotal },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            results?.let { list ->
                val erased = list.count { it.outcome == BatchOutcome.ERASED }
                val skipped = list.count { it.outcome == BatchOutcome.SKIPPED_NO_MATCH }
                val failed = list.count { it.outcome == BatchOutcome.FAILED_TO_LOAD }
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.batch_summary_erased, erased),
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (skipped > 0) {
                            Text(stringResource(R.string.batch_summary_skipped, skipped))
                        }
                        if (failed > 0) {
                            Text(stringResource(R.string.batch_summary_failed, failed))
                        }
                    }
                }
            }
        }
    }
}
