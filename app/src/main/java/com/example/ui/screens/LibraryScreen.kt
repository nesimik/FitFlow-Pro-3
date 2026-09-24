package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.core.Analytics
import com.example.core.Calc
import com.example.core.MuscleMap
import com.example.core.ProgressAnalytics
import com.example.core.Muscles
import com.example.core.TR
import com.example.core.formatDate
import com.example.core.formatDateShort
import com.example.core.formatMonthYear
import com.example.core.kg
import com.example.core.trimNum
import com.example.data.ExerciseEntity
import com.example.ui.AppViewModel
import com.example.ui.Routes
import com.example.ui.components.Badge
import com.example.ui.components.BodyMuscleMapPair
import com.example.ui.components.MuscleChipRow
import com.example.ui.components.MusclePrimarySecondaryLegend
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.FitCard
import com.example.ui.components.KeyValueRow
import com.example.ui.components.LineChart
import com.example.ui.components.OverlineText
import com.example.ui.components.PillTabs
import com.example.ui.components.RoundIconButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.theme.Palette
import com.example.ui.theme.fit

/* ================================= Kütüphane ================================= */

@Composable
fun LibraryScreen(vm: AppViewModel, nav: NavHostController) {
    val all by vm.exercises.collectAsStateWithLifecycle()
    val bestLifts by vm.bestLifts.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Hepsi") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }

    val filtered = remember(all, query, group, onlyFavorites) {
        all.asSequence()
            .filter { group == "Hepsi" || it.muscleGroup == group }
            .filter { !onlyFavorites || it.isFavorite }
            .filter { query.isBlank() || it.name.lowercase(TR).contains(query.lowercase(TR)) }
            .sortedWith(compareByDescending<ExerciseEntity> { it.isFavorite }.thenBy { it.name })
            .toList()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Hareket kütüphanesi",
            subtitle = "${all.size} hareket · ${all.count { it.isCustom }} özel",
            onBack = if (nav.previousBackStackEntry != null) { { nav.popBackStack() } } else null
        ) {
            RoundIconButton(Icons.Default.Add, MaterialTheme.fit.accent, 40.dp) { showCreate = true }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchField(query, { query = it }, "Hareket ara…")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                com.example.ui.components.ChoiceChip("★ Favoriler", onlyFavorites, { onlyFavorites = !onlyFavorites }, color = MaterialTheme.fit.gold)
                (listOf("Hepsi") + Muscles.all).forEach {
                    com.example.ui.components.ChoiceChip(
                        it, group == it, { group = it },
                        color = if (it == "Hepsi") MaterialTheme.fit.accent else Palette.muscle(it)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Default.FitnessCenter,
                "Hareket bulunamadı",
                "Filtreleri değiştir ya da kendi hareketini oluştur.",
                actionLabel = "Yeni hareket",
                onAction = { showCreate = true }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { ex ->
                    val best = bestLifts[ex.id]?.second ?: 0f
                    FitCard(
                        onClick = { nav.navigate("${Routes.EXERCISE}/${ex.id}") },
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MuscleAvatar(ex.muscleGroup)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    MuscleMap.summary(ex.name, ex.muscleGroup) +
                                        " · ${ex.equipment}" + if (ex.isCustom) " · özel" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.fit.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (best > 0f) {
                                Badge("1RM ${best.trimNum()}", MaterialTheme.fit.gold)
                                Spacer(Modifier.width(6.dp))
                            }
                            Icon(
                                if (ex.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                null,
                                tint = if (ex.isFavorite) MaterialTheme.fit.gold else MaterialTheme.fit.muted,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { vm.toggleFavorite(ex) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        ExerciseEditorDialog(
            initial = null,
            onSave = { vm.saveExercise(it); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
}

/* =============================== Hareket detayı ============================== */

@Composable
fun ExerciseDetailScreen(vm: AppViewModel, nav: NavHostController, exerciseId: Long) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val allSets by vm.allSets.collectAsStateWithLifecycle()
    val bodyWeight by vm.settings.weightKg.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val ex = exercises.firstOrNull { it.id == exerciseId }
    var metric by remember { mutableIntStateOf(0) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    if (ex == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hareket bulunamadı", color = MaterialTheme.fit.muted)
        }
        return
    }

    val progress = remember(allSets, exerciseId) { vm.progressFor(exerciseId) }
    val collapsedMonths = remember { mutableStateMapOf<String, Boolean>() }
    val progressReversed = remember(progress) { progress.reversed() }
    val groupedByMonth = remember(progressReversed) {
        progressReversed.groupBy { formatMonthYear(it.dateMillis) }
    }
    val sets = remember(allSets, exerciseId) {
        allSets.filter { it.exerciseId == exerciseId && it.isCompleted && !it.isWarmup }
    }
    val bestWeight = sets.maxOfOrNull { it.weightKg } ?: 0f
    val bestE1rm = sets.maxOfOrNull { Calc.e1rm(it.weightKg, it.reps) } ?: 0f
    val totalVolume = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
    val level = Calc.strengthLevel(ex.muscleGroup, Calc.strengthRatio(bestE1rm, bodyWeight))

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(ex.name, "${ex.muscleGroup} · ${ex.equipment}", onBack = { nav.popBackStack() }) {
            RoundIconButton(
                if (ex.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                if (ex.isFavorite) MaterialTheme.fit.gold else MaterialTheme.fit.muted,
                40.dp, MaterialTheme.fit.elevated
            ) { vm.toggleFavorite(ex) }
            Spacer(Modifier.width(8.dp))
            RoundIconButton(Icons.Default.Edit, MaterialTheme.fit.muted, 40.dp, MaterialTheme.fit.elevated) { showEdit = true }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("En ağır set", if (bestWeight > 0f) bestWeight.kg() else "—", Modifier.weight(1f), Icons.Default.FitnessCenter)
                    StatTile(
                        "Tahmini 1RM",
                        if (bestE1rm > 0f) bestE1rm.kg() else "—",
                        Modifier.weight(1f),
                        Icons.Default.EmojiEvents,
                        MaterialTheme.fit.gold,
                        caption = if (bestE1rm > 0f) level.first else null
                    )
                }
            }

            item {
                val activation = remember(ex.id, ex.name, ex.muscleGroup) {
                    MuscleMap.resolve(ex.name, ex.muscleGroup, ex.secondaryMuscles)
                }
                if (!activation.isEmpty) {
                    val accent = MaterialTheme.fit.accent
                    val mapColors = remember(activation, accent) {
                        val m = LinkedHashMap<String, androidx.compose.ui.graphics.Color>()
                        activation.secondary.forEach { m[it] = accent.copy(alpha = 0.34f) }
                        activation.primary.forEach { m[it] = accent }
                        m
                    }
                    FitCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OverlineText("ÇALIŞAN KASLAR", modifier = Modifier.weight(1f))
                            MusclePrimarySecondaryLegend()
                        }
                        Spacer(Modifier.height(10.dp))
                        BodyMuscleMapPair(colors = mapColors, height = 230.dp, showLabels = true)
                        Spacer(Modifier.height(14.dp))
                        if (activation.primary.isNotEmpty()) {
                            OverlineText("Birincil")
                            Spacer(Modifier.height(6.dp))
                            MuscleChipRow(activation.primary.map { it to accent })
                        }
                        if (activation.secondary.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            OverlineText("Destek kaslar")
                            Spacer(Modifier.height(6.dp))
                            MuscleChipRow(activation.secondary.map { it to accent.copy(alpha = 0.6f) })
                        }
                    }
                }
            }

            if (ex.videoUrl.isNotBlank()) {
                item {
                    FitCard(onClick = { openUrl(context, ex.videoUrl) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.fit.danger, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Tekniği izle", style = MaterialTheme.typography.titleSmall)
                                Text("Videoyu tarayıcıda aç", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted)
                            }
                        }
                    }
                }
            }

            if (ex.instructions.isNotBlank() || ex.tips.isNotBlank()) {
                item {
                    FitCard {
                        OverlineText("Nasıl yapılır")
                        Spacer(Modifier.height(8.dp))
                        if (ex.instructions.isNotBlank()) {
                            Text(ex.instructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (ex.tips.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.fit.accent.copy(alpha = 0.08f))
                                    .padding(10.dp)
                            ) {
                                Text("💡 ${ex.tips}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.fit.muted)
                            }
                        }
                        if (ex.secondaryMuscles.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Yardımcı kaslar", ex.secondaryMuscles)
                        }
                    }
                }
            }

            if (progress.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader("Gelişim", "${progress.size} seans kaydı")
                        Spacer(Modifier.height(10.dp))
                        PillTabs(listOf("1RM", "En ağır", "Hacim", "Tekrar"), metric) { metric = it }
                        Spacer(Modifier.height(12.dp))
                        FitCard {
                            LineChart(
                                values = progress.map {
                                    when (metric) {
                                        0 -> it.e1rm
                                        1 -> it.topWeight
                                        2 -> it.volume
                                        else -> it.totalReps.toFloat()
                                    }
                                },
                                labels = progress.map { formatDateShort(it.dateMillis) },
                                suffix = if (metric == 3) "" else " kg",
                                height = 180.dp
                            )
                        }
                    }
                }
            }

            if (sets.isNotEmpty()) {
                item {
                    FitCard {
                        OverlineText("Genel toplam")
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow("Toplam set", "${sets.size}")
                        KeyValueRow("Toplam tekrar", "${sets.sumOf { it.reps }}")
                        KeyValueRow("Toplam hacim", com.example.core.formatTonnage(totalVolume))
                        KeyValueRow("Güç seviyesi", level.first, Palette.muscle(ex.muscleGroup))
                        val trend = remember(allSets, exerciseId) {
                            ProgressAnalytics.trendPct(allSets.filter { it.exerciseId == exerciseId })
                        }
                        if (trend != 0f) {
                            KeyValueRow(
                                "Son 4 seans eğilimi",
                                (if (trend > 0f) "+" else "") + "%" + trend.toInt(),
                                if (trend > 0f) MaterialTheme.fit.success else MaterialTheme.fit.warning
                            )
                        }
                    }
                }
            }

            item {
                val bestByRange = remember(allSets, exerciseId) {
                    ProgressAnalytics.bestSetsByRange(allSets.filter { it.exerciseId == exerciseId })
                }
                if (bestByRange.size >= 2) {
                    FitCard {
                        OverlineText("Tekrar aralığına göre en iyi setler")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Aynı harekette hem ağır hem yüksek tekrar çalıştıysan, hangi bantta ne kadar güçlü olduğunu gösterir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.fit.muted
                        )
                        Spacer(Modifier.height(10.dp))
                        bestByRange.forEach { b ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Badge("${b.range} tekrar", MaterialTheme.fit.accent)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${b.weight.trimNum()} kg × ${b.reps}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "1RM ${b.e1rm.trimNum()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.fit.muted
                                )
                            }
                        }
                    }
                }
            }

            if (progress.isNotEmpty()) {
                item { SectionHeader("Seans geçmişi") }
                groupedByMonth.forEach { (month, monthProgressList) ->
                    val isMonthCollapsed = collapsedMonths[month] == true

                    item(key = "month_$month") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { collapsedMonths[month] = !isMonthCollapsed }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isMonthCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.fit.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                OverlineText(month, MaterialTheme.fit.accent)
                            }
                            Text(
                                "${monthProgressList.size} seans",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.fit.muted
                            )
                        }
                    }

                    if (!isMonthCollapsed) {
                        items(monthProgressList, key = { "p_${month}_${it.dateMillis}" }) { p ->
                            FitCard(contentPadding = PaddingValues(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(formatDate(p.dateMillis), style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${p.sets} set · ${p.totalReps} tekrar · ${p.volume.trimNum()} kg hacim",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.fit.muted
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(p.topWeight.kg(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.fit.accent)
                                        Text("1RM ${p.e1rm.trimNum()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyState(
                        Icons.Default.FitnessCenter,
                        "Henüz kayıt yok",
                        "Bu hareketi bir antrenmanda yaptığında gelişim grafiğin burada oluşacak."
                    )
                }
            }

            if (ex.isCustom) {
                item {
                    com.example.ui.components.GhostButton(
                        "Hareketi sil", { showDelete = true },
                        Modifier.fillMaxWidth(), Icons.Default.Delete, MaterialTheme.fit.danger
                    )
                }
            }
        }
    }

    if (showEdit) {
        ExerciseEditorDialog(
            initial = ex,
            onSave = { vm.saveExercise(it); showEdit = false },
            onDismiss = { showEdit = false }
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Hareketi sil",
            text = "Bu hareket kütüphaneden silinecek. Geçmiş antrenman kayıtların korunur.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = { vm.deleteExercise(ex); showDelete = false; nav.popBackStack() },
            onDismiss = { showDelete = false }
        )
    }
}
