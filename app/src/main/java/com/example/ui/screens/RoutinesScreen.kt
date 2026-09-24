package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.core.trimNum
import com.example.core.weekdayName
import com.example.data.ExerciseEntity
import com.example.core.weekdayShort
import com.example.data.RoutineDayEntity
import com.example.data.RoutineItemEntity
import com.example.data.WorkoutEntity
import com.example.ui.AppViewModel
import com.example.ui.Routes
import com.example.ui.components.AccentButton
import com.example.ui.components.Badge
import com.example.ui.components.ChoiceChip
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.FitCard
import com.example.ui.components.FitTextField
import com.example.ui.components.GhostButton
import com.example.ui.components.OverlineText
import com.example.ui.components.RoundIconButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.WeakLinkRecommendationDialog
import com.example.ui.components.DeloadAlertBox
import com.example.ui.components.DeloadDesignDialog
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.ui.platform.LocalContext
import com.example.core.MuscleMap
import com.example.ui.components.BodyMuscleMap
import com.example.ui.components.BodyMuscleMapPair
import com.example.ui.components.BodyView
import com.example.ui.theme.Palette
import com.example.ui.theme.fit

/* ================================ Program listesi ============================ */

@Composable
fun RoutinesScreen(vm: AppViewModel, nav: NavHostController) {
    val routines by vm.routines.collectAsStateWithLifecycle()
    val active by vm.activeRoutine.collectAsStateWithLifecycle()
    val days by vm.routineDays.collectAsStateWithLifecycle()
    val activeWorkout by vm.activeWorkout.collectAsStateWithLifecycle()
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }
    val deloadRecommendation by vm.deloadRecommendation.collectAsStateWithLifecycle()
    val allSets by vm.allSets.collectAsStateWithLifecycle()
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    var showDeloadDesignDialog by remember { mutableStateOf(false) }
    val hasPreDeloadBackup = remember(vm.settings) { vm.settings.getPreDeloadBackup().isNotBlank() }

    var showAddRoutine by remember { mutableStateOf(false) }
    var showAddDay by remember { mutableStateOf(false) }
    var showWeakLinkAdvisor by remember { mutableStateOf(false) }
    var showProgramMuscleMap by remember { mutableStateOf(false) }
    var dayToDelete by remember { mutableStateOf<RoutineDayEntity?>(null) }
    var dayToReplaceFromHistory by remember { mutableStateOf<RoutineDayEntity?>(null) }

    if (showWeakLinkAdvisor) {
        WeakLinkRecommendationDialog(
            vm = vm,
            onDismiss = { showWeakLinkAdvisor = false }
        )
    }

    if (showProgramMuscleMap) {
        ProgramMuscleMapDialog(
            vm = vm,
            onDismiss = { showProgramMuscleMap = false }
        )
    }

    if (showDeloadDesignDialog) {
        DeloadDesignDialog(
            routineName = active?.name ?: "Aktif Program",
            routineDays = days,
            allItems = allItems,
            exercises = exercises,
            allSets = allSets,
            workouts = workouts,
            onDismiss = { showDeloadDesignDialog = false },
            onApplyDeload = { reducePct, customAdjustments ->
                vm.applyDeloadToActiveRoutine(
                    reduceWeightsPercent = reducePct,
                    customAdjustments = customAdjustments
                ) {
                    showDeloadDesignDialog = false
                }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Program",
            subtitle = active?.description ?: "Antrenman planını yönet",
            onBack = if (nav.previousBackStackEntry != null) { { nav.popBackStack() } } else null
        ) {
            RoundIconButton(Icons.Default.Add, MaterialTheme.fit.accent, 40.dp) { showAddDay = true }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (active != null && (deloadRecommendation.shouldDeloadNow || deloadRecommendation.isCurrentlyDeloadWeek)) {
                item {
                    DeloadAlertBox(
                        recommendation = deloadRecommendation,
                        hasBackup = hasPreDeloadBackup,
                        onToggleDeloadWeek = { activeDeload -> vm.setDeloadWeekActive(activeDeload) },
                        onOpenDesignDialog = { showDeloadDesignDialog = true },
                        onRestoreOriginalRoutine = { vm.restorePreDeloadRoutine() },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routines.forEach { r ->
                        ChoiceChip(r.name, r.id == active?.id, { vm.selectRoutine(r.id) })
                    }
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.fit.elevated,
                        modifier = Modifier.clickable { showAddRoutine = true }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.fit.accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Yeni program", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.fit.accent)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GhostButton(
                        text = "Program Kas Haritası",
                        onClick = { showProgramMuscleMap = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Accessibility,
                        color = MaterialTheme.fit.accent
                    )
                    GhostButton(
                        text = "Akıllı Öneriler",
                        onClick = { showWeakLinkAdvisor = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome
                    )
                }
            }

            if (days.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.CalendarMonth,
                        "Bu programda gün yok",
                        "Antrenman günlerini ekleyip her güne hareketlerini yerleştir.",
                        actionLabel = "Gün ekle",
                        onAction = { showAddDay = true }
                    )
                }
            } else {
                items(days, key = { it.id }) { day ->
                    val items = remember(allItems, day.id) { allItems.filter { it.dayId == day.id }.sortedBy { it.orderIndex } }
                    DayCard(
                        day = day,
                        items = items,
                        activeWorkout = activeWorkout,
                        nameOf = { id -> exerciseMap[id]?.name ?: "?" },
                        groupOf = { id -> exerciseMap[id]?.muscleGroup ?: "Diğer" },
                        onOpen = { nav.navigate("${Routes.DAY}/${day.id}") },
                        onStart = { vm.startWorkout(day) { id -> nav.navigate("${Routes.WORKOUT}/$id") } },
                        onContinue = { workoutId -> nav.navigate("${Routes.WORKOUT}/$workoutId") },
                        onDuplicate = { vm.duplicateDay(day) },
                        onReplaceFromHistory = { dayToReplaceFromHistory = day },
                        onDelete = { dayToDelete = day }
                    )
                }
            }

            item {
                GhostButton("Gün ekle", { showAddDay = true }, Modifier.fillMaxWidth(), Icons.Default.Add, MaterialTheme.fit.accent)
            }
        }
    }

    if (showAddRoutine) {
        RoutineDialog(
            onSave = { name, desc ->
                vm.addRoutine(name, desc, "#22D3EE")
                showAddRoutine = false
            },
            onDismiss = { showAddRoutine = false }
        )
    }

    if (showAddDay) {
        DayDialog(
            initial = null,
            onSave = { name, focus, weekday ->
                vm.addDay(name, focus, weekday)
                showAddDay = false
            },
            onDismiss = { showAddDay = false }
        )
    }

    dayToDelete?.let { d ->
        ConfirmDialog(
            title = "Günü sil",
            text = "\"${d.name}\" ve içindeki tüm hareket ayarları silinecek. Geçmiş antrenman kayıtların etkilenmez.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = { vm.deleteDay(d); dayToDelete = null },
            onDismiss = { dayToDelete = null }
        )
    }

    if (dayToReplaceFromHistory != null) {
        ReplaceDayFromHistoryDialog(
            vm = vm,
            day = dayToReplaceFromHistory!!,
            onDismiss = { dayToReplaceFromHistory = null }
        )
    }
}

@Composable
private fun DayCard(
    day: RoutineDayEntity,
    items: List<RoutineItemEntity>,
    activeWorkout: WorkoutEntity?,
    nameOf: (Long) -> String,
    groupOf: (Long) -> String,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onContinue: (Long) -> Unit,
    onDuplicate: () -> Unit,
    onReplaceFromHistory: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val groups = items.map { groupOf(it.exerciseId) }.distinct()
    val isCurrentActiveDay = activeWorkout != null && !activeWorkout.isFinished && activeWorkout.routineDayId == day.id

    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(day.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (day.focus.isNotBlank()) {
                    Text(day.focus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted, maxLines = 2)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                RoundIconButton(Icons.Default.History, MaterialTheme.fit.accent, 36.dp, MaterialTheme.fit.elevated) {
                    onReplaceFromHistory()
                }
                Box {
                    RoundIconButton(Icons.Default.MoreVert, MaterialTheme.fit.muted, 36.dp, Color.Transparent) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Geçmiş Seans ile Değiştir") },
                            onClick = { menu = false; onReplaceFromHistory() },
                            leadingIcon = { Icon(Icons.Default.History, null, tint = MaterialTheme.fit.accent) }
                        )
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            onClick = { menu = false; onOpen() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Kopyala") },
                            onClick = { menu = false; onDuplicate() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            onClick = { menu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.fit.danger) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (day.weekday in 1..7) Badge(weekdayName(day.weekday), MaterialTheme.fit.accent)
            Badge("${items.size} hareket", MaterialTheme.fit.muted)
            val supersetCount = items.filter { it.supersetGroup > 0 }.map { it.supersetGroup }.distinct().size
            if (supersetCount > 0) {
                Badge("⚡ $supersetCount Süperset", Palette.warning)
            }
        }

        if (groups.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                groups.take(5).forEach { g ->
                    Box(
                        Modifier
                            .height(6.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Palette.muscle(g))
                    )
                }
            }
        }

        if (items.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val summaryText = items.take(4).joinToString(" · ") {
                val base = nameOf(it.exerciseId)
                if (it.supersetGroup > 0) "⚡ $base (SS${it.supersetGroup})" else base
            } + if (items.size > 4) " · +${items.size - 4}" else ""
            Text(
                summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.fit.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isCurrentActiveDay && activeWorkout != null) {
                AccentButton("Seansa Devam Et", { onContinue(activeWorkout.id) }, Modifier.weight(1f), Icons.Default.PlayArrow)
            } else {
                AccentButton("Başla", onStart, Modifier.weight(1f), Icons.Default.PlayArrow)
            }
            GhostButton("Düzenle", onOpen, Modifier.weight(1f), Icons.Default.Edit)
        }
    }
}

/* ================================ Gün düzenleyici ============================ */

@Composable
fun DayEditorScreen(vm: AppViewModel, nav: NavHostController, dayId: Long) {
    val days by vm.allDays.collectAsStateWithLifecycle()
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val day = days.firstOrNull { it.id == dayId }
    val items = allItems.filter { it.dayId == dayId }.sortedBy { it.orderIndex }

    var showPicker by remember { mutableStateOf(false) }
    var showSupersetPicker by remember { mutableStateOf(false) }
    var itemToCombine by remember { mutableStateOf<RoutineItemEntity?>(null) }
    var showEditDay by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<RoutineItemEntity?>(null) }

    if (day == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Gün bulunamadı", color = MaterialTheme.fit.muted)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            day.name,
            day.focus.ifBlank { if (day.weekday in 1..7) weekdayName(day.weekday) else "Serbest gün" },
            onBack = { nav.popBackStack() }
        ) {
            RoundIconButton(Icons.Default.Edit, MaterialTheme.fit.muted, 40.dp, MaterialTheme.fit.elevated) { showEditDay = true }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.Add,
                        "Henüz hareket yok",
                        "Kütüphaneden hareket ekleyerek bu günü oluştur.",
                        actionLabel = "Hareket ekle",
                        onAction = { showPicker = true }
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                val ex = vm.exerciseById(item.exerciseId)
                val isDuration = ex?.trackingType == ExerciseEntity.TRACK_DURATION
                RoutineItemCard(
                    item = item,
                    name = ex?.name ?: "Bilinmeyen hareket",
                    group = ex?.muscleGroup ?: "Diğer",
                    videoUrl = ex?.videoUrl ?: "",
                    isDuration = isDuration,
                    expanded = expanded == item.id,
                    onToggleExpand = { expanded = if (expanded == item.id) null else item.id },
                    onUpdate = vm::updateItem,
                    onToggleWarmup = { vm.toggleItemWarmup(item) },
                    onRequestCombine = { itemToCombine = item },
                    onSeparateSuperset = { vm.setRoutineItemSuperset(dayId, item.id, 0) },
                    onDissolveSuperset = { group -> vm.dissolveRoutineSuperset(dayId, group) },
                    onRenameExercise = { newName ->
                        if (newName.isNotBlank()) {
                            vm.updateItem(item.copy(customName = newName))
                        }
                    },
                    onUpdateVideoUrl = { newUrl ->
                        if (ex != null) {
                            vm.saveExercise(ex.copy(videoUrl = newUrl.trim()))
                        }
                    },
                    onDelete = { itemToDelete = item },
                    onMoveUp = { vm.moveItem(dayId, item.id, true) },
                    onMoveDown = { vm.moveItem(dayId, item.id, false) }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Hareket ekle", { showPicker = true }, Modifier.weight(1f), Icons.Default.Add, MaterialTheme.fit.accent)
                    GhostButton("Süperset ekle", { showSupersetPicker = true }, Modifier.weight(1f), Icons.Default.Bolt, Palette.warning)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            AccentButton(
                "Bu günle antrenmana başla",
                { vm.startWorkout(day) { id -> nav.navigate("${Routes.WORKOUT}/$id") } },
                Modifier.fillMaxWidth(),
                Icons.Default.PlayArrow
            )
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            vm = vm,
            title = "${day.name} için hareket",
            onPick = { ex -> vm.addItem(dayId, ex.id); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }

    if (showSupersetPicker) {
        SupersetPickerDialog(
            vm = vm,
            title = "${day.name} için Süperset",
            onPickSuperset = { exList ->
                vm.addSupersetToDay(dayId, exList.map { it.id })
                showSupersetPicker = false
            },
            onDismiss = { showSupersetPicker = false }
        )
    }

    if (itemToCombine != null) {
        val currentItem = itemToCombine!!
        val currentExName = vm.exerciseById(currentItem.exerciseId)?.name ?: ""
        CombineExerciseDialog(
            title = "Süperset Olarak Birleştir",
            sourceName = currentItem.customName.ifBlank { currentExName },
            candidates = items.filter { it.id != currentItem.id },
            candidateName = { it.customName.ifBlank { vm.exerciseById(it.exerciseId)?.name ?: "" } },
            candidateGroup = { vm.exerciseById(it.exerciseId)?.muscleGroup ?: "Diğer" },
            onCombineWith = { target ->
                vm.combineRoutineItems(dayId, currentItem.id, target.id)
                itemToCombine = null
            },
            onDismiss = { itemToCombine = null }
        )
    }

    if (showEditDay) {
        DayDialog(
            initial = day,
            onSave = { name, focus, weekday ->
                vm.updateDay(day.copy(name = name, focus = focus, weekday = weekday))
                showEditDay = false
            },
            onDismiss = { showEditDay = false }
        )
    }

    itemToDelete?.let { i ->
        ConfirmDialog(
            title = "Hareketi çıkar",
            text = "Bu hareket bu günden çıkarılacak.",
            confirmLabel = "Çıkar",
            destructive = true,
            onConfirm = { vm.deleteItem(i); itemToDelete = null },
            onDismiss = { itemToDelete = null }
        )
    }
}

@Composable
private fun RoutineItemCard(
    item: RoutineItemEntity,
    name: String,
    group: String,
    videoUrl: String = "",
    isDuration: Boolean = false,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (RoutineItemEntity) -> Unit,
    onToggleWarmup: () -> Unit,
    onRequestCombine: () -> Unit,
    onSeparateSuperset: () -> Unit,
    onDissolveSuperset: (Int) -> Unit,
    onRenameExercise: (String) -> Unit,
    onUpdateVideoUrl: ((String) -> Unit)? = null,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val displayName = if (item.customName.isNotBlank()) item.customName else name
    var currentName by remember(item.id, item.customName, name) { mutableStateOf(displayName) }

    val isInSuperset = item.supersetGroup > 0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isInSuperset) 1.5.dp else 1.dp,
            if (isDragging) MaterialTheme.fit.accent else if (isInSuperset) Palette.warning.copy(alpha = 0.6f) else MaterialTheme.fit.cardBorder
        ),
        shadowElevation = if (isDragging) 8.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetY
            }
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        offsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                        if (offsetY > 70f) {
                            onMoveDown()
                            offsetY = 0f
                        } else if (offsetY < -70f) {
                            onMoveUp()
                            offsetY = 0f
                        }
                    }
                )
            }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Basılı tutarak sırayı değiştir",
                    tint = if (isDragging) MaterialTheme.fit.accent else MaterialTheme.fit.muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                MuscleAvatar(group, 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(currentName.ifBlank { displayName }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val repUnit = if (isDuration) "sn" else "tekrar"
                    val repRangeStr = if (item.repMin == item.repMax) "${item.repMin} $repUnit" else "${item.repMin}-${item.repMax} $repUnit"
                    Text(
                        "${item.targetSets} set × $repRangeStr" +
                            (if (item.targetWeight > 0f) " · ${item.targetWeight.trimNum()} kg" else "") +
                            " · ${item.restSeconds} sn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.fit.muted
                    )
                }
                Spacer(Modifier.width(4.dp))
                if (isInSuperset) {
                    Badge("⚡ SS ${item.supersetGroup}", Palette.warning)
                    Spacer(Modifier.width(4.dp))
                }
                if (item.isWarmup) {
                    Badge("Isınma", Palette.warning)
                } else {
                    Badge("Ana", MaterialTheme.fit.accent)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val title = currentName.ifBlank { displayName }
                        val targetUrl = if (videoUrl.isNotBlank()) videoUrl else "https://www.youtube.com/results?search_query=${android.net.Uri.encode("$title egzersizi")}"
                        openUrl(context, targetUrl)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "YouTube videosunu izle",
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(2.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.fit.muted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FitTextField(
                        value = currentName,
                        onValueChange = {
                            currentName = it
                            onRenameExercise(it)
                        },
                        label = "Hareket Adı"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniNumberField("Set", item.targetSets.toString(), Modifier.weight(1f)) {
                            onUpdate(item.copy(targetSets = it.toIntOrNull()?.coerceIn(1, 20) ?: item.targetSets))
                        }
                        MiniNumberField(if (isDuration) "Min süre (sn)" else "Min tekrar", item.repMin.toString(), Modifier.weight(1f)) {
                            onUpdate(item.copy(repMin = it.toIntOrNull() ?: item.repMin))
                        }
                        MiniNumberField(if (isDuration) "Maks süre (sn)" else "Maks tekrar", item.repMax.toString(), Modifier.weight(1f)) {
                            onUpdate(item.copy(repMax = it.toIntOrNull() ?: item.repMax))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniNumberField("Başlangıç kg", item.targetWeight.trimNum(), Modifier.weight(1f), decimal = true) {
                            onUpdate(item.copy(targetWeight = it.replace(',', '.').toFloatOrNull() ?: 0f))
                        }
                        MiniNumberField("Dinlenme (sn)", item.restSeconds.toString(), Modifier.weight(1f)) {
                            onUpdate(item.copy(restSeconds = it.toIntOrNull() ?: item.restSeconds))
                        }
                    }
                    FitTextField(
                        value = videoUrl,
                        onValueChange = { onUpdateVideoUrl?.invoke(it) },
                        label = "YouTube / Video Linki"
                    )
                    FitTextField(item.note, { onUpdate(item.copy(note = it)) }, "Not (tempo, kavrama, vb.)")

                    // Süperset Yönetimi Bölümü
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.fit.elevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isInSuperset) {
                                Icon(Icons.Default.Bolt, null, tint = Palette.warning, modifier = Modifier.size(18.dp))
                                Text("Süperset Grubu ${item.supersetGroup}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = onSeparateSuperset) {
                                    Text("Ayrıl", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fit.muted)
                                }
                                TextButton(onClick = { onDissolveSuperset(item.supersetGroup) }) {
                                    Text("Dağıt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fit.danger)
                                }
                            } else {
                                Icon(Icons.Default.Link, null, tint = MaterialTheme.fit.accent, modifier = Modifier.size(18.dp))
                                Text("Süperset değil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted, modifier = Modifier.weight(1f))
                                TextButton(onClick = onRequestCombine) {
                                    Text("⚡ Başka Hareketle Birleştir", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fit.accent)
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChoiceChip(
                            if (item.isWarmup) "Isınma hareketi" else "Ana hareket",
                            item.isWarmup,
                            onToggleWarmup,
                            color = Palette.warning
                        )
                        Spacer(Modifier.weight(1f))
                        RoundIconButton(Icons.Default.ArrowUpward, MaterialTheme.fit.muted, 36.dp, MaterialTheme.fit.elevated, onMoveUp)
                        RoundIconButton(Icons.Default.ArrowDownward, MaterialTheme.fit.muted, 36.dp, MaterialTheme.fit.elevated, onMoveDown)
                        RoundIconButton(Icons.Default.Delete, MaterialTheme.fit.danger, 36.dp, MaterialTheme.fit.danger.copy(alpha = 0.12f), onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniNumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    Column(modifier) {
        OverlineText(label)
        Spacer(Modifier.height(5.dp))
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = {
                val f = it.filter { c -> c.isDigit() || (decimal && (c == '.' || c == ',')) }.take(6)
                text = f
                onChange(f)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleSmall,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.fit.accent,
                unfocusedBorderColor = MaterialTheme.fit.cardBorder,
                cursorColor = MaterialTheme.fit.accent
            )
        )
    }
}

/* ---------------------------------- Diyaloglar ------------------------------ */

@Composable
private fun RoutineDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .imePadding()
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Yeni program kaydet", style = MaterialTheme.typography.titleLarge)
                FitTextField(name, { name = it }, "Program adı", placeholder = "Örn: 5 Günlük Split")
                FitTextField(desc, { desc = it }, "Açıklama", singleLine = false, minLines = 2)

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GhostButton("Vazgeç", onDismiss, Modifier.weight(1f))
                    AccentButton("Oluştur", {
                        if (name.isNotBlank()) onSave(name.trim(), desc.trim())
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayDialog(
    initial: RoutineDayEntity?,
    onSave: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var focus by remember { mutableStateOf(initial?.focus ?: "") }
    var weekday by remember { mutableStateOf(initial?.weekday ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .imePadding()
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(if (initial == null) "Yeni gün kaydet" else "Günü düzenle", style = MaterialTheme.typography.titleLarge)
                FitTextField(name, { name = it }, "Gün adı", placeholder = "Örn: A Günü / Push")
                FitTextField(focus, { focus = it }, "Odak", placeholder = "Örn: Göğüs, Omuz, Triceps")
                Column {
                    OverlineText("Haftanın günü")
                    Spacer(Modifier.height(7.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChoiceChip("Serbest", weekday == 0, { weekday = 0 })
                        (1..7).forEach { d ->
                            ChoiceChip(weekdayShort(d), weekday == d, { weekday = d })
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GhostButton("Vazgeç", onDismiss, Modifier.weight(1f))
                    AccentButton("Kaydet", {
                        if (name.isNotBlank()) onSave(name.trim(), focus.trim(), weekday)
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

/* ============================ Program Kas Haritası Diyaloğu ========================== */

@Composable
fun ProgramMuscleMapDialog(
    vm: AppViewModel,
    onDismiss: () -> Unit
) {
    val activeRoutine by vm.activeRoutine.collectAsStateWithLifecycle()
    val days by vm.routineDays.collectAsStateWithLifecycle()
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()

    var selectedDayId by remember { mutableStateOf<Long?>(null) } // null = Tüm Program
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var fullScreenView by remember { mutableStateOf(BodyView.FRONT) }

    val currentDayItems = remember(selectedDayId, allItems, days) {
        if (selectedDayId == null) {
            val dayIds = days.map { it.id }.toSet()
            allItems.filter { it.dayId in dayIds }
        } else {
            allItems.filter { it.dayId == selectedDayId }
        }
    }

    val targetExercises = remember(currentDayItems, exercises) {
        val exMap = exercises.associateBy { it.id }
        currentDayItems.mapNotNull { item -> exMap[item.exerciseId] }
    }

    val primaryColor = MaterialTheme.fit.accent
    val secondaryColor = Palette.warning

    val muscleColors = remember(targetExercises, primaryColor, secondaryColor) {
        val map = mutableMapOf<String, Color>()
        targetExercises.forEach { ex ->
            val resolved = MuscleMap.resolve(ex.name, ex.muscleGroup, ex.secondaryMuscles)
            resolved.primary.forEach { m -> map[m] = primaryColor }
            resolved.secondary.forEach { m ->
                if (!map.containsKey(m)) {
                    map[m] = secondaryColor
                }
            }
        }
        map
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(Modifier.padding(16.dp)) {
                // Başlık ve Kapat Butonu
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null, tint = MaterialTheme.fit.accent)
                        Column {
                            Text(
                                text = "Program Kas Haritası",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = activeRoutine?.name ?: "Aktif Program",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.fit.muted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Gün Seçici Çip Satırı
                Text("Gün Seçimi:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fit.muted)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceChip(
                        label = "Tüm Program",
                        selected = selectedDayId == null,
                        onClick = { selectedDayId = null }
                    )
                    days.forEach { day ->
                        ChoiceChip(
                            label = day.name,
                            selected = selectedDayId == day.id,
                            onClick = { selectedDayId = day.id }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Bilgi Satırı
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.fit.elevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Badge("Birincil Kas", primaryColor)
                        Badge("İkincil / Destekçi", secondaryColor)
                    }
                    Text(
                        text = "${targetExercises.size} hareket",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.fit.muted
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Çift Vücut Kas Haritası
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.fit.elevated, RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    BodyMuscleMapPair(
                        colors = muscleColors,
                        height = 250.dp,
                        selected = selectedMuscle,
                        onMuscleTap = { selectedMuscle = it },
                        showLabels = true
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Tam Ekran Yap Butonu
                AccentButton(
                    text = "Tam Ekran Gör",
                    onClick = {
                        fullScreenView = BodyView.FRONT
                        isFullScreen = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Fullscreen
                )
            }
        }
    }

    // Tam Ekran Görünümü Diyaloğu
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Tam Ekran Üst Bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Tam Ekran Kas Haritası",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                if (fullScreenView == BodyView.FRONT) "Ön Görünüm (Ön Kas Grupları)" else "Arka Görünüm (Arka Kas Grupları)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.fit.accent
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ön / Arka Çevirme Butonu
                            GhostButton(
                                text = if (fullScreenView == BodyView.FRONT) "Arkayı Çevir" else "Öne Çevir",
                                onClick = {
                                    fullScreenView = if (fullScreenView == BodyView.FRONT) BodyView.BACK else BodyView.FRONT
                                },
                                icon = Icons.Default.Autorenew,
                                color = MaterialTheme.fit.accent
                            )

                            // Çarpı (Kapat) Butonu
                            RoundIconButton(
                                icon = Icons.Default.Close,
                                tint = MaterialTheme.fit.muted,
                                size = 40.dp,
                                background = MaterialTheme.fit.elevated
                            ) {
                                isFullScreen = false
                            }
                        }
                    }

                    // Gün Seçim Çipleri (Tam ekranda da seçilebilir)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChoiceChip(
                            label = "Tüm Program",
                            selected = selectedDayId == null,
                            onClick = { selectedDayId = null }
                        )
                        days.forEach { day ->
                            ChoiceChip(
                                label = day.name,
                                selected = selectedDayId == day.id,
                                onClick = { selectedDayId = day.id }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tam Sayfa Tek Kas Haritası Görünümü
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodyMuscleMap(
                            view = fullScreenView,
                            colors = muscleColors,
                            modifier = Modifier.fillMaxSize(),
                            selected = selectedMuscle,
                            onMuscleTap = { selectedMuscle = it }
                        )
                    }

                    // Alt Bilgi / Çarpı Paneli
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.fit.elevated,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "💡 Hareketlerini görmek için herhangi bir kasa dokunun.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.fit.muted
                            )
                            if (selectedMuscle != null) {
                                TextButton(onClick = { selectedMuscle = null }) {
                                    Text("Seçimi Temizle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bir kasa dokunulduğunda hareket listesini gösteren alt diyalog
    if (selectedMuscle != null) {
        MuscleExercisesDialog(
            muscleKey = selectedMuscle!!,
            selectedDayId = selectedDayId,
            days = days,
            allItems = allItems,
            exercises = exercises,
            onDismiss = { selectedMuscle = null }
        )
    }
}

/* ============================ Seçili Kas Egzersiz Listesi Diyaloğu ========================== */

@Composable
private fun MuscleExercisesDialog(
    muscleKey: String,
    selectedDayId: Long?,
    days: List<RoutineDayEntity>,
    allItems: List<RoutineItemEntity>,
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val muscleLabel = MuscleMap.label(muscleKey)

    val dayMap = remember(days) { days.associateBy { it.id } }
    val programExerciseIds = remember(allItems, days) {
        val activeDayIds = days.map { it.id }.toSet()
        allItems.filter { it.dayId in activeDayIds }.map { it.exerciseId to it.dayId }
    }

    val matchingExercises = remember(exercises, muscleKey, selectedDayId, days, allItems) {
        val dayOrderMap = days.mapIndexed { index, d -> d.id to index }.toMap()
        val activeDayIds = days.map { it.id }.toSet()
        val programItemsByExercise = allItems.filter { it.dayId in activeDayIds }
            .groupBy { it.exerciseId }

        data class ExSortKey(
            val priority: Int,
            val dayOrder: Int,
            val itemOrder: Int,
            val isSecondary: Boolean,
            val name: String
        )

        exercises.mapNotNull { ex ->
            val resolved = MuscleMap.resolve(ex.name, ex.muscleGroup, ex.secondaryMuscles)
            val isPrimary = resolved.primary.contains(muscleKey)
            val isSecondary = resolved.secondary.contains(muscleKey)
            if (!isPrimary && !isSecondary) return@mapNotNull null

            val itemsForEx = programItemsByExercise[ex.id] ?: emptyList()

            val priority: Int
            val dayOrder: Int
            val itemOrder: Int

            if (selectedDayId != null) {
                val inSelectedDayItem = itemsForEx.find { it.dayId == selectedDayId }
                if (inSelectedDayItem != null) {
                    priority = 0 // En başta: Seçili günün hareketleri
                    dayOrder = 0
                    itemOrder = inSelectedDayItem.orderIndex
                } else if (itemsForEx.isNotEmpty()) {
                    priority = 1 // İkinci sırada: Programın diğer günlerindeki hareketler
                    dayOrder = itemsForEx.minOf { dayOrderMap[it.dayId] ?: 999 }
                    itemOrder = itemsForEx.minOf { it.orderIndex }
                } else {
                    priority = 2 // Programda olmayan genel kütüphane hareketleri
                    dayOrder = 0
                    itemOrder = 0
                }
            } else {
                if (itemsForEx.isNotEmpty()) {
                    priority = 0 // Program hareketleri (gün ve hareket sırasına göre)
                    dayOrder = itemsForEx.minOf { dayOrderMap[it.dayId] ?: 999 }
                    itemOrder = itemsForEx.minOf { it.orderIndex }
                } else {
                    priority = 1 // Programda olmayan kütüphane hareketleri
                    dayOrder = 0
                    itemOrder = 0
                }
            }

            Pair(ex, isPrimary) to ExSortKey(priority, dayOrder, itemOrder, !isPrimary, ex.name)
        }.sortedWith(compareBy(
            { it.second.priority },
            { it.second.dayOrder },
            { it.second.itemOrder },
            { it.second.isSecondary },
            { it.second.name }
        )).map { it.first }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(Modifier.padding(16.dp)) {
                // Başlık ve Çarpı Kapatma Butonu
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "💪 $muscleLabel Hareketleri",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$muscleLabel kasını çalıştıran program & kütüphane hareketleri",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.fit.muted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.fit.elevated, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.fit.accent
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (matchingExercises.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "Hareket Bulunamadı",
                        text = "Kütüphanede $muscleLabel kasını hedefleyen egzersiz henüz eklenmemiş."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(matchingExercises, key = { it.first.id }) { (ex, isPrimary) ->
                            val dayIdsInProgram = programExerciseIds.filter { it.first == ex.id }.map { it.second }.distinct()
                            val inSelectedDay = selectedDayId != null && dayIdsInProgram.contains(selectedDayId)

                            FitCard(
                                container = if (inSelectedDay) MaterialTheme.fit.accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = if (inSelectedDay) MaterialTheme.fit.accent.copy(alpha = 0.4f) else MaterialTheme.fit.cardBorder
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isPrimary) {
                                                Badge("1.0 Set (Birincil)", MaterialTheme.fit.success)
                                            } else {
                                                Badge("0.5 Set (İkincil)", Palette.warning)
                                            }

                                            Badge(ex.muscleGroup, MaterialTheme.fit.muted)
                                        }

                                        if (dayIdsInProgram.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            val dayNames = dayIdsInProgram.mapNotNull { dayMap[it]?.name }.joinToString(", ")
                                            Badge("Programda Var ($dayNames)", MaterialTheme.fit.accent)
                                        }
                                    }

                                    // Video izle butonu
                                    IconButton(
                                        onClick = {
                                            val targetUrl = "https://www.youtube.com/results?search_query=${android.net.Uri.encode("${ex.name} egzersizi yapılışı")}"
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Video İzle",
                                            tint = Color(0xFFFF0000),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ======================== Geçmiş Seans ile Değiştir Dialog ===================== */

@Composable
fun ReplaceDayFromHistoryDialog(
    vm: AppViewModel,
    day: RoutineDayEntity,
    onDismiss: () -> Unit
) {
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val allSets by vm.allSets.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedWorkoutToConfirm by remember { mutableStateOf<WorkoutEntity?>(null) }

    val finishedWorkouts = remember(workouts, searchQuery) {
        workouts.filter { w ->
            w.isFinished && (
                searchQuery.isBlank() ||
                w.title.contains(searchQuery, ignoreCase = true) ||
                w.routineName.contains(searchQuery, ignoreCase = true)
            )
        }.sortedByDescending { it.startedAt }
    }

    if (selectedWorkoutToConfirm != null) {
        val targetW = selectedWorkoutToConfirm!!
        val setsInWorkout = remember(allSets, targetW.id) {
            allSets.filter { it.workoutId == targetW.id }
        }
        val exNames = remember(setsInWorkout) {
            setsInWorkout.map { it.exerciseName }.distinct()
        }

        AlertDialog(
            onDismissRequest = { selectedWorkoutToConfirm = null },
            icon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.fit.accent) },
            title = { Text("Program Gününü Değiştir") },
            text = {
                Text(
                    "\"${day.name}\" gününün tüm mevcut hareketleri silinecek ve yerine \"${targetW.title}\" (${com.example.core.formatDate(targetW.startedAt)}) antrenmanındaki ${exNames.size} hareket aktarılacaktır.\n\nOnaylıyor musunuz?"
                )
            },
            confirmButton = {
                AccentButton(
                    text = "Evet, Değiştir",
                    onClick = {
                        vm.replaceRoutineDayWithWorkout(day.id, targetW.id) {
                            selectedWorkoutToConfirm = null
                            onDismiss()
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedWorkoutToConfirm = null }) {
                    Text("İptal")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(Modifier.padding(16.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.fit.accent)
                        Column {
                            Text(
                                text = "Geçmiş Seans ile Değiştir",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${day.name} gününe geçmiş antrenman içeriğini aktarın",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.fit.muted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (workouts.size > 3) {
                    FitTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Ara",
                        placeholder = "Geçmiş antrenmanlarda ara...",
                        modifier = Modifier.fillMaxWidth(),
                        leading = { Icon(Icons.Default.Search, null, tint = MaterialTheme.fit.muted) }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (finishedWorkouts.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "Geçmiş Antrenman Bulunamadı",
                        text = if (searchQuery.isBlank()) "Henüz tamamlanmış bir antrenman seansınız bulunmuyor." else "Aramanızla eşleşen geçmiş seans bulunamadı."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(finishedWorkouts, key = { it.id }) { workout ->
                            val wSets = remember(allSets, workout.id) {
                                allSets.filter { it.workoutId == workout.id }
                            }
                            val exNames = remember(wSets) {
                                wSets.map { it.exerciseName }.distinct()
                            }

                            FitCard(
                                onClick = { selectedWorkoutToConfirm = workout }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = workout.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (workout.routineName.isNotBlank()) {
                                                Badge(workout.routineName, MaterialTheme.fit.accent)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${com.example.core.formatDate(workout.startedAt)} · ${com.example.core.formatDurationShort(workout.durationSeconds)} · ${exNames.size} hareket",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.fit.muted
                                        )
                                        if (exNames.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = exNames.joinToString(" · "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.fit.muted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    GhostButton(
                                        text = "Yükle",
                                        onClick = { selectedWorkoutToConfirm = workout },
                                        icon = Icons.Default.SwapHoriz,
                                        color = MaterialTheme.fit.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
