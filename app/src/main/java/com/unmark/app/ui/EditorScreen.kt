package com.unmark.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.unmark.app.R
import com.unmark.app.inpaint.Inpainter
import com.unmark.app.util.ImageStore
import com.unmark.app.util.ImageUtils
import com.unmark.app.util.MetadataFindings
import com.unmark.app.util.MetadataInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class Stroke(val points: List<Offset>, val radiusPx: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    val strokes = remember { mutableStateListOf<Stroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var brushRadius by remember { mutableFloatStateOf(36f) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastSavedUriString by remember { mutableStateOf<String?>(null) }
    var metadataFindings by remember { mutableStateOf<MetadataFindings?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ImageUtils.loadBitmap(context, uri)
            baseBitmap = bitmap
            strokes.clear()
            currentStrokePoints = emptyList()
            lastSavedUriString = null
            metadataFindings = null
            scope.launch(Dispatchers.IO) {
                val findings = MetadataInspector.inspect(context, uri)
                metadataFindings = findings
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val bitmap = baseBitmap

        if (bitmap == null) {
            EmptyState(padding) {
                pickImageLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.brush_hint),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            metadataFindings?.let { findings ->
                MetadataCard(findings, modifier = Modifier.padding(bottom = 12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .onSizeChanged { canvasSizePx = it }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(bitmap) {
                            fun clampToCanvas(offset: Offset): Offset {
                                val maxX = size.width.toFloat()
                                val maxY = size.height.toFloat()
                                return Offset(
                                    offset.x.coerceIn(0f, maxX),
                                    offset.y.coerceIn(0f, maxY)
                                )
                            }

                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints = listOf(clampToCanvas(offset))
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.size > 1) {
                                        strokes.add(Stroke(currentStrokePoints, brushRadius))
                                    }
                                    currentStrokePoints = emptyList()
                                },
                                onDragCancel = { currentStrokePoints = emptyList() },
                                onDrag = { change, _ ->
                                    currentStrokePoints = currentStrokePoints + clampToCanvas(change.position)
                                }
                            )
                        }
                ) {
                    val allStrokes = strokes.map { it.points to it.radiusPx } +
                        listOf(currentStrokePoints to brushRadius)
                    for ((points, radius) in allStrokes) {
                        if (points.size < 2) continue
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = Color(0xFF7C5CFC).copy(alpha = 0.55f),
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = radius * 2,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.brush_size), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = brushRadius,
                    onValueChange = { brushRadius = it },
                    valueRange = 12f..80f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                    enabled = strokes.isNotEmpty() && !isProcessing
                ) { Text(stringResource(R.string.undo)) }

                OutlinedButton(
                    onClick = { strokes.clear() },
                    enabled = strokes.isNotEmpty() && !isProcessing
                ) { Text(stringResource(R.string.reset)) }

                Button(
                    onClick = {
                        val size = canvasSizePx
                        if (size.width == 0 || size.height == 0 || strokes.isEmpty()) return@Button
                        val maskBitmap = buildMaskBitmap(bitmap, strokes, size)
                        isProcessing = true
                        scope.launch {
                            val result = Inpainter.inpaint(bitmap, maskBitmap)
                            baseBitmap = result
                            strokes.clear()
                            isProcessing = false
                        }
                    },
                    enabled = strokes.isNotEmpty() && !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isProcessing) stringResource(R.string.erasing) else stringResource(R.string.erase))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { baseBitmap = null; strokes.clear() },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.start_over)) }

                OutlinedButton(
                    onClick = {
                        val uri = ImageStore.saveToGallery(context, bitmap)
                        lastSavedUriString = uri?.toString()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (uri != null) context.getString(R.string.saved_confirmation) else "Save failed"
                            )
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.save)) }

                OutlinedButton(
                    onClick = {
                        val uriString = lastSavedUriString
                        if (uriString != null) {
                            val intent = ImageStore.shareIntent(android.net.Uri.parse(uriString))
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                        } else {
                            val uri = ImageStore.saveToGallery(context, bitmap)
                            lastSavedUriString = uri?.toString()
                            if (uri != null) {
                                val intent = ImageStore.shareIntent(uri)
                                context.startActivity(android.content.Intent.createChooser(intent, null))
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

@Composable
private fun MetadataCard(findings: MetadataFindings, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (findings.isClean) {
                Text(stringResource(R.string.metadata_clean), style = MaterialTheme.typography.labelLarge)
            } else {
                Text(stringResource(R.string.metadata_found_prefix), style = MaterialTheme.typography.labelLarge)
                if (findings.exifTags.isNotEmpty()) {
                    Text("• " + stringResource(R.string.metadata_exif_tags, findings.exifTags.size))
                }
                if (findings.hasXmp) {
                    Text("• " + stringResource(R.string.metadata_xmp))
                }
                if (findings.hasC2pa) {
                    Text("• " + stringResource(R.string.metadata_c2pa))
                }
                Text(
                    text = stringResource(R.string.metadata_will_be_stripped),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.no_image_selected), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onPick, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.pick_image))
            }
        }
    }
}

/** Rasterizes the drawn strokes (in on-screen canvas coordinates) into a bitmap-space mask. */
private fun buildMaskBitmap(source: Bitmap, strokes: List<Stroke>, canvasSizePx: IntSize): Bitmap {
    val mask = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(mask)
    val paint = AndroidPaint().apply {
        color = android.graphics.Color.WHITE
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        isAntiAlias = true
    }

    val scaleX = source.width.toFloat() / canvasSizePx.width.toFloat()
    val scaleY = source.height.toFloat() / canvasSizePx.height.toFloat()

    for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        val path = AndroidPath()
        val first = stroke.points.first()
        path.moveTo(first.x * scaleX, first.y * scaleY)
        for (point in stroke.points.drop(1)) {
            path.lineTo(point.x * scaleX, point.y * scaleY)
        }
        paint.strokeWidth = stroke.radiusPx * 2 * ((scaleX + scaleY) / 2f)
        canvas.drawPath(path, paint)
    }

    return mask
}
