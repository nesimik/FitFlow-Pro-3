package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.core.formatDuration
import com.example.core.formatTonnage
import com.example.core.trimNum
import com.example.data.ExerciseEntity
import com.example.data.SessionExercise
import com.example.data.WorkoutSetEntity
import com.example.ui.AppViewModel
import com.example.ui.components.AccentButton
import com.example.ui.components.Badge
import com.example.ui.components.CheckCircle
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.FitCard
import com.example.ui.components.FitTextField
import com.example.ui.components.GhostButton
import com.example.ui.components.RoundIconButton
import com.example.ui.theme.Palette
import com.example.ui.theme.fit

@Composable
fun ActiveWorkoutScreen(vm: AppViewModel, nav: NavHostController) {
    val workout by vm.activeWorkout.collectAsStateWithLifecycle()
    val exercises by vm.sessionExercises.collectAsStateWithLifecycle()
    val elapsed by vm.elapsedSeconds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPicker by remember { mutableStateOf(false) }
    var showSupersetPicker by remember { mutableStateOf(false) }
    var exerciseToCombine by remember { mutableStateOf<SessionExercise?>(null) }
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf<SessionExercise?>(null) }

    val listState = rememberLazyListState()
    var highlightedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        vm.scrollToExerciseEvent.collect { targetId ->
            val index = exercises.indexOfFirst { it.exerciseId == targetId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                highlightedId = targetId
                delay(2000)
                if (highlightedId == targetId) {
                    highlightedId = null
                }
            }
        }
    }

    if (workout == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aktif seans yok", color = MaterialTheme.fit.muted)
        }
        return
    }

    BackHandler { nav.popBackStack() }

    val totalSets = exercises.sumOf { it.totalSets }
    val doneSets = exercises.sumOf { it.completedSets }
    val volume = exercises.sumOf { it.volume.toDouble() }.toFloat()

    Column(Modifier.fillMaxSize()) {
        SessionTopBar(
            title = workout!!.title,
            elapsed = elapsed,
            volume = volume,
            done = doneSets,
            total = totalSets,
            onMinimize = { nav.popBackStack() },
            onFinish = { showFinish = true },
            onDiscard = { showDiscard = true }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exercises, key = { "${it.exerciseId}_${it.order}" }) { se ->
                ExerciseLogCard(
                    se = se,
                    suggestion = vm.suggestionFor(se),
                    isHighlighted = highlightedId == se.exerciseId,
                    onToggleSet = { set ->
                        if (se.trackingType == ExerciseEntity.TRACK_DURATION) {
                            vm.toggleTimedSetDone(set, set.durationSeconds, se.restSeconds)
                        } else {
                            vm.toggleSetDone(set, se.restSeconds)
                        }
                    },
                    onUpdateSet = vm::updateSet,
                    onDeleteSet = vm::deleteSet,
                    onAddSet = { vm.addSetRow(se.order, se.exerciseId) },
                    onRemoveExercise = { vm.removeExerciseFromSession(se.order) },
                    onToggleWarmup = { vm.toggleExerciseWarmup(se.order, !se.isWarmup) },
                    onRequestCombine = { exerciseToCombine = se },
                    onSeparateSuperset = { vm.setSessionExerciseSuperset(se.order, 0) },
                    onDissolveSuperset = { group -> vm.dissolveSessionSuperset(group) },
                    onStartRest = { vm.startRest(se.restSeconds, se.name, se.exerciseId) },
                    onOpenVideo = { openUrl(context, se.videoUrl) },
                    onShowTips = { showTips = se },
                    onRenameExercise = { newName ->
                        if (newName.isNotBlank()) {
                            vm.renameSessionExercise(se.order, newName)
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        "Hareket ekle",
                        { showPicker = true },
                        Modifier.weight(1f),
                        Icons.Default.Add,
                        MaterialTheme.fit.accent
                    )
                    GhostButton(
                        "Süperset ekle",
                        { showSupersetPicker = true },
                        Modifier.weight(1f),
                        Icons.Default.Bolt,
                        Palette.warning
                    )
                }
            }

            if (exercises.isEmpty()) {
                item {
                    Text(
                        "Bu seansta henüz hareket yok. Yukarıdaki butonla ekleyebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.fit.muted,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            vm = vm,
            title = "Seansa hareket ekle",
            excludeIds = exercises.map { it.exerciseId }.toSet(),
            onPick = { ex ->
                vm.addExerciseToSession(ex.id)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showSupersetPicker) {
        SupersetPickerDialog(
            vm = vm,
            title = "Seansa Süperset Ekle",
            onPickSuperset = { exList ->
                vm.addSupersetToWorkout(exList.map { it.id })
                showSupersetPicker = false
            },
            onDismiss = { showSupersetPicker = false }
        )
    }

    if (exerciseToCombine != null) {
        val current = exerciseToCombine!!
        CombineExerciseDialog(
            title = "Süperset Olarak Birleştir",
            sourceName = current.name,
            candidates = exercises.filter { it.order != current.order },
            candidateName = { it.name },
            candidateGroup = { it.muscleGroup },
            onCombineWith = { target ->
                vm.combineSessionExercises(current.order, target.order)
                exerciseToCombine = null
            },
            onDismiss = { exerciseToCombine = null }
        )
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "Seansı kaydetmeden sonlandır",
            text = "Bu antrenman kaydedilmeden silinecek. Emin misin?",
            confirmLabel = "Kaydetmeden Çık",
            destructive = true,
            onConfirm = {
                showDiscard = false
                vm.discardWorkout { nav.popBackStack() }
            },
            onDismiss = { showDiscard = false }
        )
    }

    if (showFinish) {
        FinishDialog(
            doneSets = doneSets,
            volume = volume,
            elapsed = elapsed,
            onDismiss = { showFinish = false },
            onFinish = { notes, feeling ->
                showFinish = false
                vm.finishWorkout(notes, feeling) { nav.popBackStack() }
            }
        )
    }

    showTips?.let { se ->
        AlertDialog(
            onDismissRequest = { showTips = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(se.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        vm.suggestionFor(se),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.fit.accent
                    )
                    val ex = vm.exerciseById(se.exerciseId)
                    if (!ex?.instructions.isNullOrBlank()) {
                        Text(ex!!.instructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.fit.muted)
                    }
                    if (!ex?.tips.isNullOrBlank()) {
                        Text("İpucu: ${ex!!.tips}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted)
                    }
                    if (se.previous.isNotEmpty()) {
                        Text("Önceki seans", style = MaterialTheme.typography.titleSmall)
                        se.previous.forEach {
                            Text(
                                "${it.setNumber}. set  ·  ${it.weightKg.trimNum()} kg × ${it.reps}" +
                                    if (it.rpe > 0f) "  ·  RPE ${it.rpe.trimNum()}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.fit.muted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTips = null }) {
                    Text("Kapat", color = MaterialTheme.fit.accent)
                }
            }
        )
    }
}

/* ------------------------------- Üst çubuk --------------------------------- */

@Composable
private fun SessionTopBar(
    title: String,
    elapsed: Int,
    volume: Float,
    done: Int,
    total: Int,
    onMinimize: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Default.Close, MaterialTheme.fit.muted, 38.dp, MaterialTheme.fit.elevated) { onDiscard() }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDuration(elapsed), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.fit.accent)
                }
                Box {
                    RoundIconButton(Icons.Default.MoreVert, MaterialTheme.fit.muted, 38.dp, MaterialTheme.fit.elevated) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Seansı sil") },
                            onClick = { menu = false; onDiscard() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.fit.danger) }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.fit.success,
                    modifier = Modifier.clickable { onFinish() }
                ) {
                    Text(
                        "Kaydet",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge("$done / $total set", MaterialTheme.fit.accent)
                Badge(formatTonnage(volume), MaterialTheme.fit.gold)
            }
        }
    }
}

/* ---------------------------- Hareket kayıt kartı --------------------------- */

@Composable
private fun ExerciseLogCard(
    se: SessionExercise,
    suggestion: String,
    isHighlighted: Boolean = false,
    onToggleSet: (WorkoutSetEntity) -> Unit,
    onUpdateSet: (WorkoutSetEntity) -> Unit,
    onDeleteSet: (WorkoutSetEntity) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    onToggleWarmup: () -> Unit,
    onRequestCombine: () -> Unit,
    onSeparateSuperset: () -> Unit,
    onDissolveSuperset: (Int) -> Unit,
    onStartRest: () -> Unit,
    onOpenVideo: () -> Unit,
    onShowTips: () -> Unit,
    onRenameExercise: ((String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(se.name) { mutableStateOf(se.name) }
    val isDuration = se.trackingType == ExerciseEntity.TRACK_DURATION
    val isRepsOnly = se.trackingType == ExerciseEntity.TRACK_REPS
    val isInSuperset = se.supersetGroup > 0

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Hareketi Yeniden Adlandır") },
            text = {
                FitTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = "Yeni Hareket Adı"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        if (renameText.isNotBlank()) {
                            onRenameExercise?.invoke(renameText)
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    FitCard(
        border = if (isHighlighted) MaterialTheme.fit.accent else if (isInSuperset) Palette.warning.copy(alpha = 0.6f) else MaterialTheme.fit.cardBorder,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MuscleAvatar(se.muscleGroup, 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        se.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isInSuperset) {
                        Badge("⚡ SS ${se.supersetGroup}", Palette.warning)
                    }
                    if (se.isWarmup) {
                        Badge("Isınma", Palette.warning)
                    } else {
                        Badge("Ana", MaterialTheme.fit.accent)
                    }
                }
                Text(
                    "Hedef ${se.targetRepMin}-${se.targetRepMax} tekrar · ${se.restSeconds} sn dinlenme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.fit.muted
                )
            }
            if (se.isDone) Badge("Tamam", MaterialTheme.fit.success)
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = {
                    val targetUrl = if (se.videoUrl.isNotBlank()) se.videoUrl else "https://www.youtube.com/results?search_query=${android.net.Uri.encode("${se.name} egzersizi")}"
                    openUrl(context, targetUrl)
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "YouTube videosunu izle",
                    tint = Color(0xFFFF0000),
                    modifier = Modifier.size(24.dp)
                )
            }
            Box {
                RoundIconButton(Icons.Default.MoreVert, MaterialTheme.fit.muted, 34.dp, Color.Transparent) { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (isInSuperset) {
                        DropdownMenuItem(
                            text = { Text("Süpersetten ayır") },
                            onClick = { menu = false; onSeparateSuperset() },
                            leadingIcon = { Icon(Icons.Default.LinkOff, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Tüm süperseti dağıt") },
                            onClick = { menu = false; onDissolveSuperset(se.supersetGroup) },
                            leadingIcon = { Icon(Icons.Default.Bolt, null, tint = Palette.warning) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("⚡ Başka hareketle süperset yap...") },
                            onClick = { menu = false; onRequestCombine() },
                            leadingIcon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.fit.accent) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (se.isWarmup) "Ana harekete çevir" else "Isınma hareketine çevir") },
                        onClick = { menu = false; onToggleWarmup() },
                        leadingIcon = { Icon(Icons.Default.Lightbulb, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Teknik & öneri") },
                        onClick = { menu = false; onShowTips() },
                        leadingIcon = { Icon(Icons.Default.Lightbulb, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hareketi yeniden adlandır") },
                        onClick = { menu = false; showRenameDialog = true },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("YouTube'da Aç / Ara") },
                        onClick = {
                            menu = false
                            val targetUrl = if (se.videoUrl.isNotBlank()) se.videoUrl else "https://www.youtube.com/results?search_query=${android.net.Uri.encode("${se.name} egzersizi")}"
                            openUrl(context, targetUrl)
                        },
                        leadingIcon = { Icon(Icons.Default.PlayCircle, null, tint = Color(0xFFFF0000)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Dinlenmeyi başlat") },
                        onClick = { menu = false; onStartRest() },
                        leadingIcon = { Icon(Icons.Default.Timer, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hareketi çıkar") },
                        onClick = { menu = false; onRemoveExercise() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.fit.danger) }
                    )
                }
            }
        }

        if (suggestion.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.fit.accent.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.fit.accent, modifier = Modifier.size(15.dp))
                Text(suggestion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sütun başlıkları
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderCell("SET", Modifier.width(34.dp))
            HeaderCell("ÖNCEKİ", Modifier.weight(1.5f))
            if (isDuration) {
                HeaderCell("SÜRE (sn)", Modifier.weight(1.4f))
            } else {
                HeaderCell("KG", Modifier.weight(1f))
                HeaderCell("TEKRAR", Modifier.weight(1f))
            }
            HeaderCell("RPE", Modifier.weight(0.8f))
            Spacer(Modifier.width(38.dp))
        }
        Spacer(Modifier.height(4.dp))

        se.sets.forEach { set ->
            val prevSet = se.previous.firstOrNull { it.setNumber == set.setNumber } ?: se.previous.lastOrNull()
            SetRow(
                set = set,
                previous = prevSet,
                isDuration = isDuration,
                isRepsOnly = isRepsOnly,
                onToggle = {
                    val resolved = set.copy(
                        weightKg = if (set.weightKg > 0f) set.weightKg else (prevSet?.weightKg ?: set.weightKg),
                        reps = if (set.reps > 0) set.reps else (prevSet?.reps ?: set.reps),
                        durationSeconds = if (set.durationSeconds > 0) set.durationSeconds else (prevSet?.durationSeconds ?: set.durationSeconds)
                    )
                    onToggleSet(resolved)
                },
                onUpdate = onUpdateSet,
                onDelete = { onDeleteSet(set) }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onAddSet() }
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.fit.muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Set ekle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.fit.muted)
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.fit.muted,
        textAlign = TextAlign.Center,
        modifier = modifier,
        maxLines = 1
    )
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    previous: WorkoutSetEntity?,
    isDuration: Boolean,
    isRepsOnly: Boolean,
    onToggle: () -> Unit,
    onUpdate: (WorkoutSetEntity) -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val bg = if (set.isCompleted) MaterialTheme.fit.success.copy(alpha = 0.08f) else Color.Transparent

    // Önceki haftadan kalan ağırlık/tekrar/süre varsa ve henüz girilmemişse otomatik olarak kaydet
    LaunchedEffect(set.id, previous) {
        if (previous != null) {
            var needsSync = false
            var newWeight = set.weightKg
            var newReps = set.reps
            var newDur = set.durationSeconds
            if (set.weightKg <= 0f && previous.weightKg > 0f) {
                newWeight = previous.weightKg
                needsSync = true
            }
            if (set.reps <= 0 && previous.reps > 0) {
                newReps = previous.reps
                needsSync = true
            }
            if (set.durationSeconds <= 0 && previous.durationSeconds > 0) {
                newDur = previous.durationSeconds
                needsSync = true
            }
            if (needsSync) {
                onUpdate(set.copy(weightKg = newWeight, reps = newReps, durationSeconds = newDur))
            }
        }
    }

    val effDuration = if (set.durationSeconds > 0) set.durationSeconds else (previous?.durationSeconds ?: 0)
    val effWeight = if (set.weightKg > 0f) set.weightKg else (previous?.weightKg ?: 0f)
    val effReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (set.isWarmup) Palette.warning.copy(alpha = 0.16f)
                        else MaterialTheme.fit.elevated
                    )
                    .clickable { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (set.isWarmup) "W" else "${set.setNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (set.isWarmup) Palette.warning else MaterialTheme.fit.muted
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (set.isWarmup) "Normal set yap" else "Isınma seti yap") },
                    onClick = { menu = false; onUpdate(set.copy(isWarmup = !set.isWarmup)) }
                )
                DropdownMenuItem(
                    text = { Text("Setı sil") },
                    onClick = { menu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.fit.danger) }
                )
            }
        }

        Text(
            previous?.let {
                if (isDuration) "${it.durationSeconds} sn"
                else if (it.weightKg > 0f) "${it.weightKg.trimNum()}×${it.reps}"
                else "${it.reps} tekrar"
            } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.fit.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.5f)
        )

        if (isDuration) {
            NumberCell(
                key = "d${set.id}",
                initial = if (effDuration > 0) effDuration.toString() else "",
                hint = previous?.durationSeconds?.toString() ?: "0",
                modifier = Modifier.weight(1.4f)
            ) { onUpdate(set.copy(durationSeconds = it.toIntOrNull() ?: 0)) }
        } else {
            NumberCell(
                key = "w${set.id}",
                initial = if (effWeight > 0f) effWeight.trimNum() else "",
                hint = previous?.weightKg?.trimNum() ?: "0",
                decimal = true,
                modifier = Modifier.weight(1f)
            ) { onUpdate(set.copy(weightKg = it.replace(',', '.').toFloatOrNull() ?: 0f)) }

            NumberCell(
                key = "r${set.id}",
                initial = if (effReps > 0) effReps.toString() else "",
                hint = previous?.reps?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            ) { onUpdate(set.copy(reps = it.toIntOrNull() ?: 0)) }
        }

        NumberCell(
            key = "e${set.id}",
            initial = if (set.rpe > 0f) set.rpe.trimNum() else "",
            hint = "–",
            decimal = true,
            modifier = Modifier.weight(0.8f)
        ) { onUpdate(set.copy(rpe = it.replace(',', '.').toFloatOrNull() ?: 0f)) }

        Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
            CheckCircle(set.isCompleted, onToggle, size = 28.dp)
        }
    }
}

/**
 * Küçük sayısal hücre. Yazarken imlecin kaymaması için metin yerel tutulur,
 * her değişiklikte veritabanına yazılır.
 */
@Composable
private fun NumberCell(
    key: String,
    initial: String,
    hint: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onChange: (String) -> Unit
) {
    var text by remember(key) { mutableStateOf(initial) }
    Box(
        modifier
            .padding(horizontal = 3.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.fit.elevated),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)
                text = filtered
                onChange(filtered)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(MaterialTheme.fit.accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text(
                            hint,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.fit.muted.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/* ------------------------------ Bitirme akışı ------------------------------ */

@Composable
private fun FinishDialog(
    doneSets: Int,
    volume: Float,
    elapsed: Int,
    onDismiss: () -> Unit,
    onFinish: (String, Int) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf(0) }
    val faces = listOf("😵" to 1, "🙁" to 2, "😐" to 3, "🙂" to 4, "🔥" to 5)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Antrenmanı bitir", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryPill("Süre", formatDuration(elapsed), Modifier.weight(1f))
                    SummaryPill("Set", "$doneSets", Modifier.weight(1f))
                    SummaryPill("Hacim", formatTonnage(volume), Modifier.weight(1f))
                }
                Column {
                    Text("Nasıl geçti?", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        faces.forEach { (emoji, value) ->
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (feeling == value) MaterialTheme.fit.accent.copy(alpha = 0.2f)
                                        else MaterialTheme.fit.elevated
                                    )
                                    .border(
                                        1.dp,
                                        if (feeling == value) MaterialTheme.fit.accent else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { feeling = value },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
                FitTextField(notes, { notes = it }, "Seans notu (isteğe bağlı)", singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { onFinish(notes, feeling) }) {
                Text("Bitir ve kaydet", color = MaterialTheme.fit.success, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", color = MaterialTheme.fit.muted) }
        }
    )
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.fit.elevated)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}
