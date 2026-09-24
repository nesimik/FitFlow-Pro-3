package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.core.LoadStatus
import com.example.core.MuscleMap
import com.example.core.Muscles
import com.example.core.formatDate
import com.example.core.formatDurationShort
import com.example.core.formatTonnage
import com.example.core.relativeDay
import com.example.core.startOfWeek
import com.example.core.todayWeekday
import com.example.core.weekdayName
import com.example.data.RoutineDayEntity
import com.example.ui.AppViewModel
import com.example.ui.Routes
import com.example.ui.components.AccentButton
import com.example.ui.components.Badge
import com.example.ui.components.DistributionBars
import com.example.ui.components.DistributionItem
import com.example.ui.components.DonutChart
import com.example.ui.components.FitCard
import com.example.ui.components.GradientCard
import com.example.ui.components.BodyMuscleMapPair
import com.example.ui.components.LineChart
import com.example.ui.components.LocalMenuAction
import com.example.ui.components.RoundIconButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.ThinProgress
import com.example.ui.components.WeekStrip
import com.example.ui.components.DeloadAlertBox
import com.example.ui.components.DeloadDesignDialog
import com.example.ui.theme.Palette
import com.example.ui.theme.onColorFor
import com.example.ui.theme.fit

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val name by vm.settings.userName.collectAsStateWithLifecycle()
    val stats by vm.dashboard.collectAsStateWithLifecycle()
    val days by vm.routineDays.collectAsStateWithLifecycle()
    val routine by vm.activeRoutine.collectAsStateWithLifecycle()
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val active by vm.activeWorkout.collectAsStateWithLifecycle()
    val weekly by vm.weeklySeries.collectAsStateWithLifecycle()
    val muscleSetsInfo by vm.weeklyMuscleSetsInfo.collectAsStateWithLifecycle()
    val muscleSets = muscleSetsInfo.setsPerMuscle
    val periodLabel = muscleSetsInfo.periodLabel
    val goal by vm.settings.weeklyGoal.collectAsStateWithLifecycle()
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val deloadRecommendation by vm.deloadRecommendation.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val allSets by vm.allSets.collectAsStateWithLifecycle()
    var showDeloadDesignDialog by remember { mutableStateOf(false) }
    val hasPreDeloadBackup = remember(vm.settings) { vm.settings.getPreDeloadBackup().isNotBlank() }

    val today = todayWeekday()
    val suggested = remember(days, workouts, today) { suggestDay(days, workouts, today) }
    var showCancelActiveDialog by remember { mutableStateOf(false) }

    if (showCancelActiveDialog) {
        AlertDialog(
            onDismissRequest = { showCancelActiveDialog = false },
            title = { Text("Devam Eden Seansı Sonlandır") },
            text = { Text("Aktif antrenman seansını kapatmak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelActiveDialog = false
                        vm.cancelActiveWorkout()
                    }
                ) {
                    Text("Evet, Sonlandır", color = MaterialTheme.fit.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelActiveDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    val doneWeekdays = remember(workouts) {
        val weekStart = startOfWeek(System.currentTimeMillis())
        workouts.filter { it.startedAt >= weekStart }
            .map {
                val c = java.util.Calendar.getInstance().apply { timeInMillis = it.startedAt }
                (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1
            }.toSet()
    }
    val plannedWeekdays = remember(days) { days.map { it.weekday }.filter { it in 1..7 }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Greeting(name, stats.streakDays) }

        if (deloadRecommendation.shouldDeloadNow || deloadRecommendation.isCurrentlyDeloadWeek) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    DeloadAlertBox(
                        recommendation = deloadRecommendation,
                        hasBackup = hasPreDeloadBackup,
                        onToggleDeloadWeek = { active -> vm.setDeloadWeekActive(active) },
                        onOpenDesignDialog = { showDeloadDesignDialog = true },
                        onRestoreOriginalRoutine = { vm.restorePreDeloadRoutine() }
                    )
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                if (active != null) {
                    ResumeCard(
                        title = active!!.title,
                        onResume = { nav.navigate("${Routes.WORKOUT}/${active!!.id}") },
                        onCancel = { showCancelActiveDialog = true }
                    )
                } else {
                    TodayCard(
                        day = suggested,
                        routineName = routine?.name ?: "Program yok",
                        exerciseCount = suggested?.let { d -> allItems.count { it.dayId == d.id } } ?: 0,
                        isToday = suggested?.weekday == today,
                        onStart = {
                            vm.startWorkout(suggested) { id -> nav.navigate("${Routes.WORKOUT}/$id") }
                        },
                        onOpenProgram = {
                            nav.navigate(Routes.ROUTINES) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        if (active == null && suggested != null) {
            item {
                val plan = remember(suggested, allItems, allSets, workouts, deloadRecommendation) {
                    vm.planFor(suggested.id)
                }
                if (plan.isNotEmpty()) {
                    Box(Modifier.padding(horizontal = 16.dp)) { TodayPlanCard(plan) }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader("Bu hafta", "$goal antrenman hedefi")
                Spacer(Modifier.height(12.dp))
                FitCard {
                    WeekStrip(doneWeekdays, today, plannedWeekdays)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${stats.thisWeekWorkouts} / $goal antrenman",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            formatTonnage(stats.thisWeekVolume),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.fit.accent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(
                        if (goal <= 0) 0f else stats.thisWeekWorkouts / goal.toFloat(),
                        color = if (stats.thisWeekWorkouts >= goal) MaterialTheme.fit.success else MaterialTheme.fit.accent
                    )
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    "Toplam seans", "${stats.totalWorkouts}",
                    Modifier.weight(1f), Icons.Default.CheckCircle, MaterialTheme.fit.accent,
                    caption = if (stats.daysSinceLast >= 0) "Son: ${relativeDay(stats.lastWorkoutMillis)}" else null,
                    onClick = { nav.navigate(Routes.HISTORY) }
                )
                StatTile(
                    "Kaldırılan", formatTonnage(stats.totalVolume),
                    Modifier.weight(1f), Icons.Default.Bolt, MaterialTheme.fit.gold,
                    caption = "${stats.totalSets} set",
                    onClick = {
                        vm.setStatsTab(0)
                        nav.navigate(Routes.STATS) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    "Seri", "${stats.streakWeeks} hafta",
                    Modifier.weight(1f), Icons.Default.LocalFireDepartment, Palette.warning,
                    caption = if (stats.streakDays > 1) "${stats.streakDays} gün üst üste" else null
                )
                StatTile(
                    "Ort. süre", formatDurationShort(stats.avgDurationSec),
                    Modifier.weight(1f), Icons.Default.Timer, Palette.violet,
                    caption = "Toplam ${formatDurationShort(stats.totalDurationSec)}"
                )
            }
        }

        if (weekly.any { it.volume > 0f || it.sets > 0 || it.workouts > 0 }) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Hacim trendi", "Son 8 hafta kaldırılan toplam tonaj") {
                        Badge("Detay", MaterialTheme.fit.accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    FitCard(onClick = {
                        vm.setStatsTab(0)
                        nav.navigate(Routes.STATS) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        LineChart(
                            values = weekly.map { it.volume },
                            labels = weekly.map { it.label },
                            suffix = " kg",
                            height = 150.dp
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader("Kas dengesi", "$periodLabel set sayısı / önerilen aralık")
                Spacer(Modifier.height(12.dp))
                FitCard {
                    val items = Muscles.all.filter { it != Muscles.CARDIO }.map { g ->
                        val count = muscleSets[g] ?: 0
                        val range = Muscles.weeklyTarget(g)
                        DistributionItem(
                            label = g,
                            value = count.toFloat(),
                            color = Palette.muscle(g),
                            caption = "hedef ${range.first}-${range.last}",
                            max = range.last.toFloat().coerceAtLeast(1f)
                        )
                    }

                    val activeItems = items.filter { it.value > 0f }
                    val totalSets = items.sumOf { it.value.toInt() }

                    if (activeItems.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DonutChart(
                                items = activeItems,
                                size = 120.dp,
                                stroke = 18.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$totalSets",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "toplam set",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.fit.muted
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                activeItems.sortedByDescending { it.value }.take(5).forEach { d ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(9.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(d.color)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            text = d.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${d.value.toInt()} set",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.fit.muted
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.fit.cardBorder, thickness = 0.5.dp)
                        Spacer(Modifier.height(14.dp))
                    }

                    DistributionBars(
                        items = items,
                        valueSuffix = " set"
                    )
                }
            }
        }

        item {
            // Bugün ekranında harita "toparlanma" modunda: hangi kaslar bugün hazır?
            // (Haftalık hacim dengesi İlerleme → Kaslar sekmesinde.)
            val recovery = remember(allSets, exercises) { vm.recoveryNow() }
            if (recovery.isNotEmpty()) {
                val mapColors = remember(recovery) {
                    recovery.mapValues { (_, r) -> com.example.ui.components.MuscleColors.forRecovery(r.state) }
                }
                val tired = recovery.values.filter { it.state != com.example.core.RecoveryState.FRESH }
                    .sortedBy { it.readiness }

                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Toparlanma", "Son günlerde çalıştırdığın kasların bugünkü durumu")
                    Spacer(Modifier.height(12.dp))
                    FitCard(onClick = {
                        vm.setStatsTab(1)
                        nav.navigate(Routes.STATS) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        BodyMuscleMapPair(colors = mapColors, height = 220.dp, showLabels = true)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.example.ui.components.MuscleColors.recoveryLegend.forEach { (c, text) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
                                    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (tired.isEmpty()) {
                            Text(
                                "Tüm kasların toparlanmış görünüyor — tam yüklenmeye hazırsın.",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.components.MuscleColors.fresh
                            )
                        } else {
                            tired.take(5).forEach { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                            .background(com.example.ui.components.MuscleColors.forRecovery(r.state))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        MuscleMap.label(r.key),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "%${(r.readiness * 100).toInt()}" + if (r.readyAt > 0L)
                                            " · hazır ${com.example.core.formatWeekday(r.readyAt)} ${com.example.core.formatTime(r.readyAt)}" else "",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.fit.muted
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tahmin: son setlerine, zorluklarına ve kasın tipik toparlanma süresine göre hesaplanır.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.fit.muted.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        if (workouts.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Son antrenmanlar") {
                        Text(
                            "Tümü",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.fit.accent,
                            modifier = Modifier.clickable { nav.navigate(Routes.HISTORY) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        workouts.take(3).forEach { w ->
                            val vol = com.example.core.Analytics.workoutVolume(w.id, vm.allSets.value)
                            FitCard(
                                onClick = { nav.navigate("${Routes.WORKOUT_DETAIL}/${w.id}") },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(w.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${relativeDay(w.startedAt)} · ${formatDurationShort(w.durationSeconds)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.fit.muted
                                        )
                                    }
                                    Text(formatTonnage(vol), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.fit.accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader("Hızlı erişim")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Serbest\nantrenman", Icons.Default.Add, Modifier.weight(1f)) {
                        vm.startWorkout(null) { id -> nav.navigate("${Routes.WORKOUT}/$id") }
                    }
                    QuickAction("Araçlar", Icons.Default.Calculate, Modifier.weight(1f)) { nav.navigate(Routes.TOOLS) }
                    QuickAction("Ölçüm", Icons.Default.MonitorWeight, Modifier.weight(1f)) { nav.navigate(Routes.BODY) }
                    QuickAction("Notlar", Icons.Default.EditNote, Modifier.weight(1f)) { nav.navigate(Routes.NOTES) }
                }
            }
        }
    }

    if (showDeloadDesignDialog) {
        DeloadDesignDialog(
            routineName = routine?.name ?: "Aktif Program",
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
}

/* -------------------------------- Parçalar --------------------------------- */

@Composable
private fun Greeting(name: String, streakDays: Int) {
    val menuAction = LocalMenuAction.current
    Row(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (menuAction != null) {
            RoundIconButton(
                Icons.Default.Menu,
                MaterialTheme.colorScheme.onSurface,
                40.dp,
                MaterialTheme.fit.elevated
            ) { menuAction() }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(greetingText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted)
            Text(name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                formatDate(System.currentTimeMillis()) + " · " + weekdayName(todayWeekday()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.fit.muted,
                maxLines = 1
            )
        }
        if (streakDays > 0) {
            Surface(shape = RoundedCornerShape(14.dp), color = Palette.warning.copy(alpha = 0.14f)) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Palette.warning, modifier = Modifier.size(16.dp))
                    Text("$streakDays", style = MaterialTheme.typography.titleSmall, color = Palette.warning)
                }
            }
        }
    }
}

private fun greetingText(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 6 -> "İyi geceler"
        h < 12 -> "Günaydın"
        h < 18 -> "İyi günler"
        else -> "İyi akşamlar"
    }
}

@Composable
private fun TodayCard(
    day: RoutineDayEntity?,
    routineName: String,
    exerciseCount: Int,
    isToday: Boolean,
    onStart: () -> Unit,
    onOpenProgram: () -> Unit
) {
    val accent = MaterialTheme.fit.accent
    GradientCard(
        colors = listOf(accent, accent.copy(alpha = 0.72f)),
        contentPadding = PaddingValues(20.dp)
    ) {
        val on = onColorFor(accent)
        Text(
            if (isToday) "BUGÜNÜN ANTRENMANI" else "SIRADAKİ ANTRENMAN",
            style = MaterialTheme.typography.labelSmall,
            color = on.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            day?.name ?: "Program oluştur",
            style = MaterialTheme.typography.displaySmall,
            color = on,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            day?.focus?.takeIf { it.isNotBlank() } ?: routineName,
            style = MaterialTheme.typography.bodyMedium,
            color = on.copy(alpha = 0.85f),
            maxLines = 2
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (day != null) {
                Surface(shape = RoundedCornerShape(10.dp), color = on.copy(alpha = 0.16f)) {
                    Text(
                        "$exerciseCount hareket",
                        style = MaterialTheme.typography.labelMedium,
                        color = on,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = on.copy(alpha = 0.16f)) {
                    Text(
                        if (day.weekday in 1..7) weekdayName(day.weekday) else "Serbest gün",
                        style = MaterialTheme.typography.labelMedium,
                        color = on,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = on,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (day != null) onStart() else onOpenProgram() }
        ) {
            Row(
                Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (day != null) "Antrenmana başla" else "Program oluştur",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun ResumeCard(
    title: String,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    GradientCard(
        colors = listOf(Palette.success, Palette.success.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(20.dp),
        onClick = onResume
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DEVAM EDEN SEANS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Seansı Sonlandır",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Palette.success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Seansa dön", style = MaterialTheme.typography.titleMedium, color = Palette.success)
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FitCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.fit.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.fit.accent, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fit.muted, maxLines = 2)
    }
}

/* --------------------------------- Yardımcı -------------------------------- */

private fun suggestDay(
    days: List<RoutineDayEntity>,
    workouts: List<com.example.data.WorkoutEntity>,
    today: Int
): RoutineDayEntity? {
    if (days.isEmpty()) return null
    days.firstOrNull { it.weekday == today }?.let { return it }
    // En uzun süredir yapılmamış günü öner
    val lastByDay = workouts.filter { it.routineDayId != null }
        .groupBy { it.routineDayId!! }
        .mapValues { entry -> entry.value.maxOf { it.startedAt } }
    return days.minByOrNull { lastByDay[it.id] ?: 0L }
}


/* ---------------------------- Bugünün hedefleri ---------------------------- */

/** Seans başlamadan önce: her hareket için bugünkü ağırlık × tekrar ve ilerleme yönü. */
@Composable
private fun TodayPlanCard(plan: List<Pair<String, com.example.core.Prescription>>) {
    FitCard {
        com.example.ui.components.OverlineText("Bugünün hedefleri")
        Spacer(Modifier.height(10.dp))
        plan.forEachIndexed { i, (name, rx) ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    actionIcon(rx.action), null,
                    tint = actionColor(rx.action),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (rx.action == com.example.core.ProgressAction.FIRST) "İlk kayıt" else rx.headline,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = if (rx.action == com.example.core.ProgressAction.HOLD) MaterialTheme.colorScheme.onSurface
                            else actionColor(rx.action)
                )
            }
            if (rx.isPlateau || rx.regressing) {
                Text(
                    if (rx.regressing) "Geriliyor — form ve toparlanmayı kontrol et"
                    else "Plato: ${rx.stalledSessions} seanstır yeni zirve yok",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.Palette.warning,
                    modifier = Modifier.padding(start = 26.dp)
                )
            }
        }
    }
}
