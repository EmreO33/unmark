package com.unmark.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.unmark.app.util.VendorWatermark
import com.unmark.app.util.VendorWatermarks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class Stroke(val points: List<Offset>, val radiusPx: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onOpenBatch: () -> Unit = {}) {
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
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var detectedVendor by remember { mutableStateOf<VendorWatermark?>(null) }
    var selectedVendorRegion by remember { mutableStateOf<VendorWatermark?>(null) }
    var vendorPickerExpanded by remember { mutableStateOf(false) }

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
            detectedVendor = null
            selectedVendorRegion = null
            scope.launch(Dispatchers.IO) {
                val findings = MetadataInspector.inspect(context, uri)
                metadataFindings = findings
                val vendor = VendorWatermarks.detect(context, uri)
                detectedVendor = vendor
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (baseBitmap == null) {
                        Box {
                            HamburgerIcon(onClick = { menuExpanded = true })
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_about)) },
                                    onClick = { menuExpanded = false; showAboutDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_batch)) },
                                    onClick = { menuExpanded = false; onOpenBatch() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_source)) },
                                    onClick = {
                                        menuExpanded = false
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EmreO33/unmark"))
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_privacy)) },
                                    onClick = {
                                        menuExpanded = false
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://github.com/EmreO33/unmark/blob/master/PRIVACY.md")
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_license)) },
                                    onClick = {
                                        menuExpanded = false
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://github.com/EmreO33/unmark/blob/master/LICENSE")
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            VendorWatermarkCard(
                detected = detectedVendor,
                selected = selectedVendorRegion,
                pickerExpanded = vendorPickerExpanded,
                onSetPickerExpanded = { vendorPickerExpanded = it },
                onSelect = { selectedVendorRegion = it; vendorPickerExpanded = false },
                onClear = { selectedVendorRegion = null },
                modifier = Modifier.padding(bottom = 12.dp)
            )

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

                    selectedVendorRegion?.let { vendor ->
                        val region = vendor.region
                        drawRect(
                            color = Color(0xFF7C5CFC).copy(alpha = 0.35f),
                            topLeft = Offset(region.left * size.width, region.top * size.height),
                            size = androidx.compose.ui.geometry.Size(
                                (region.right - region.left) * size.width,
                                (region.bottom - region.top) * size.height
                            )
                        )
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
                        val hasSelection = strokes.isNotEmpty() || selectedVendorRegion != null
                        if (size.width == 0 || size.height == 0 || !hasSelection) return@Button
                        val maskBitmap = buildMaskBitmap(bitmap, strokes, size)
                        selectedVendorRegion?.let { vendor ->
                            AndroidCanvas(maskBitmap).drawRect(
                                vendor.region.left * bitmap.width,
                                vendor.region.top * bitmap.height,
                                vendor.region.right * bitmap.width,
                                vendor.region.bottom * bitmap.height,
                                AndroidPaint().apply { color = android.graphics.Color.WHITE }
                            )
                        }
                        isProcessing = true
                        scope.launch {
                            val result = Inpainter.inpaint(bitmap, maskBitmap)
                            baseBitmap = result
                            strokes.clear()
                            selectedVendorRegion = null
                            isProcessing = false
                        }
                    },
                    enabled = (strokes.isNotEmpty() || selectedVendorRegion != null) && !isProcessing,
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
private fun VendorWatermarkCard(
    detected: VendorWatermark?,
    selected: VendorWatermark?,
    pickerExpanded: Boolean,
    onSetPickerExpanded: (Boolean) -> Unit,
    onSelect: (VendorWatermark) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (selected != null) {
                Text(
                    stringResource(R.string.vendor_area_selected, selected.displayName),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(R.string.vendor_area_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
                OutlinedButton(onClick = onClear) { Text(stringResource(R.string.vendor_clear_area)) }
            } else {
                if (detected != null) {
                    Text(
                        stringResource(R.string.vendor_detected, detected.displayName),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSelect(detected) }) {
                            Text(stringResource(R.string.vendor_highlight_area))
                        }
                        Box {
                            OutlinedButton(onClick = { onSetPickerExpanded(true) }) {
                                Text(stringResource(R.string.vendor_not_this))
                            }
                            VendorDropdown(pickerExpanded, onSetPickerExpanded, onSelect)
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.vendor_not_detected),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box {
                        OutlinedButton(onClick = { onSetPickerExpanded(true) }) {
                            Text(stringResource(R.string.vendor_choose_manually))
                        }
                        VendorDropdown(pickerExpanded, onSetPickerExpanded, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorDropdown(
    expanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    onSelect: (VendorWatermark) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = { onSetExpanded(false) }) {
        for (vendor in VendorWatermarks.KNOWN) {
            DropdownMenuItem(
                text = { Text(vendor.displayName) },
                onClick = { onSelect(vendor) }
            )
        }
    }
}

@Composable
private fun HamburgerIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = { Text(stringResource(R.string.about_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_close)) }
        }
    )
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
