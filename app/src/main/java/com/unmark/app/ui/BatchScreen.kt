package com.unmark.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.unmark.app.R
import com.unmark.app.util.BatchItemResult
import com.unmark.app.util.BatchOutcome
import com.unmark.app.util.BatchProcessor
import com.unmark.app.util.ImageUtils
import com.unmark.app.util.NormalizedRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var selectedRegion by remember { mutableStateOf<NormalizedRect?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<List<BatchItemResult>?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris = uris
            previewBitmap = null
            selectedRegion = null
            results = null
            scope.launch(Dispatchers.IO) {
                previewBitmap = ImageUtils.loadBitmap(context, uris.first())
            }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            val preview = previewBitmap
            if (preview != null) {
                Text(
                    stringResource(R.string.batch_draw_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(preview.width.toFloat() / preview.height.toFloat())
                        .onSizeChanged { canvasSizePx = it }
                ) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(preview) {
                                fun clamp(offset: Offset): Offset = Offset(
                                    offset.x.coerceIn(0f, size.width.toFloat()),
                                    offset.y.coerceIn(0f, size.height.toFloat())
                                )
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val c = clamp(offset)
                                        dragStart = c
                                        dragCurrent = c
                                    },
                                    onDragEnd = {
                                        val start = dragStart
                                        val current = dragCurrent
                                        if (start != null && current != null && size.width > 0 && size.height > 0) {
                                            val left = minOf(start.x, current.x) / size.width
                                            val right = maxOf(start.x, current.x) / size.width
                                            val top = minOf(start.y, current.y) / size.height
                                            val bottom = maxOf(start.y, current.y) / size.height
                                            if (right - left > 0.02f && bottom - top > 0.02f) {
                                                selectedRegion = NormalizedRect(left, top, right, bottom)
                                            }
                                        }
                                        dragStart = null
                                        dragCurrent = null
                                    },
                                    onDragCancel = { dragStart = null; dragCurrent = null },
                                    onDrag = { change, _ -> dragCurrent = clamp(change.position) }
                                )
                            }
                    ) {
                        val start = dragStart
                        val current = dragCurrent
                        if (start != null && current != null) {
                            drawRect(
                                color = Color(0xFF7C5CFC).copy(alpha = 0.35f),
                                topLeft = Offset(minOf(start.x, current.x), minOf(start.y, current.y)),
                                size = Size(kotlin.math.abs(current.x - start.x), kotlin.math.abs(current.y - start.y))
                            )
                        } else {
                            selectedRegion?.let { region ->
                                drawRect(
                                    color = Color(0xFF7C5CFC).copy(alpha = 0.35f),
                                    topLeft = Offset(region.left * size.width, region.top * size.height),
                                    size = Size(
                                        (region.right - region.left) * size.width,
                                        (region.bottom - region.top) * size.height
                                    )
                                )
                            }
                        }
                    }
                }

                if (selectedRegion != null) {
                    OutlinedButton(
                        onClick = { selectedRegion = null },
                        enabled = !isProcessing,
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text(stringResource(R.string.batch_clear_area)) }
                }
            }

            Button(
                onClick = {
                    val uris = pickedUris
                    val region = selectedRegion ?: return@Button
                    isProcessing = true
                    progressDone = 0
                    progressTotal = uris.size
                    results = null
                    scope.launch(Dispatchers.Default) {
                        val outcome = BatchProcessor.processAll(context, uris, region) { done, total ->
                            progressDone = done
                            progressTotal = total
                        }
                        results = outcome
                        isProcessing = false
                    }
                },
                enabled = pickedUris.isNotEmpty() && selectedRegion != null && !isProcessing,
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
                val failed = list.count { it.outcome == BatchOutcome.FAILED_TO_LOAD }
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.batch_summary_erased, erased),
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (failed > 0) {
                            Text(stringResource(R.string.batch_summary_failed, failed))
                        }
                    }
                }
            }
        }
    }
}
