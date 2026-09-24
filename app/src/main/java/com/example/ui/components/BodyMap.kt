package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.MuscleMap
import com.example.ui.theme.fit
import kotlin.math.max
import kotlin.math.min

/* ==========================================================================
 * Anatomik kas haritası
 *
 * Geometri BodyData.kt içinde; anatomik bir segmentasyon görselinden piksel
 * bazlı iz sürülerek çıkarıldı. Her kas bölgesi ayrı kapalı çokgen kümesi
 * olduğu için tek tek renklendirilebilir.
 *
 * Çizim dört katmanda yapılır — düz renk yerine derinlik veren bu sıralama:
 *   1. gövde silueti (hafif dikey degrade + dış hat)
 *   2. çalışılmayan kaslar (siluetten bir ton açık, kendi degradesiyle)
 *   3. vurgulanan kaslar (üstten aydınlık, alta doğru koyulaşan degrade)
 *   4. kas sınırları (arka plan renginde ince oluk) + seçili kasın parlak hattı
 *
 * Uygulamaya hiçbir görsel dosyası eklenmez; her şey vektörel çizilir.
 * ========================================================================== */

private const val BW = 114f
private const val BH = 210f

enum class BodyView { FRONT, BACK }

/* ------------------------------ Ayrıştırma -------------------------------- */

private fun parsePolygons(data: String): List<List<Offset>> =
    data.split(';').mapNotNull { poly ->
        val pts = poly.trim().split(' ').mapNotNull { pair ->
            val i = pair.indexOf(',')
            if (i <= 0) return@mapNotNull null
            val x = pair.substring(0, i).toFloatOrNull() ?: return@mapNotNull null
            val y = pair.substring(i + 1).toFloatOrNull() ?: return@mapNotNull null
            Offset(x, y)
        }
        if (pts.size >= 3) pts else null
    }

/** Chaikin köşe kesme — taşma yapmadan yumuşatır, kas sınırları birbirine girmez. */
private fun chaikin(points: List<Offset>, iterations: Int): List<Offset> {
    var pts = points
    repeat(iterations) {
        if (pts.size < 3) return pts
        val out = ArrayList<Offset>(pts.size * 2)
        for (i in pts.indices) {
            val p = pts[i]
            val q = pts[(i + 1) % pts.size]
            out.add(Offset(p.x * 0.75f + q.x * 0.25f, p.y * 0.75f + q.y * 0.25f))
            out.add(Offset(p.x * 0.25f + q.x * 0.75f, p.y * 0.25f + q.y * 0.75f))
        }
        pts = out
    }
    return pts
}

private fun toPath(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

private fun buildPaths(data: String, smoothing: Int = 1): List<Path> =
    parsePolygons(data).map { toPath(chaikin(it, smoothing)) }

/* -------------------------------- Bölgeler -------------------------------- */

private class Region(val key: String, val paths: List<Path>) {
    val bounds: List<Rect> by lazy { paths.map { it.getBounds() } }

    /** En küçük parçanın alanı — üst üste binen bölgelerde dokunmayı çözerken kullanılır. */
    val area: Float by lazy {
        bounds.minOfOrNull { max(it.width * it.height, 0.01f) } ?: Float.MAX_VALUE
    }

    fun contains(point: Offset): Boolean = bounds.any { it.contains(point) }
}

private val frontBody: List<Path> by lazy { buildPaths(BodyData.FRONT_BODY, 1) }
private val backBody: List<Path> by lazy { buildPaths(BodyData.BACK_BODY, 1) }
private val frontRegions: List<Region> by lazy {
    BodyData.FRONT.map { (key, data) -> Region(key, buildPaths(data)) }
}
private val backRegions: List<Region> by lazy {
    BodyData.BACK.map { (key, data) -> Region(key, buildPaths(data)) }
}

/* --------------------------- Çizim yardımcıları ---------------------------- */

/** Bir yolu, kendi sınırlarına göre dikey degradeyle doldurur — hacim hissi verir. */
private fun DrawScope.fillShaded(path: Path, bounds: Rect, top: Color, bottom: Color) {
    drawPath(
        path,
        Brush.verticalGradient(
            colors = listOf(top, bottom),
            startY = bounds.top,
            endY = bounds.bottom.coerceAtLeast(bounds.top + 0.01f)
        )
    )
}

private fun Color.lighten(amount: Float): Color = lerp(this, Color.White, amount)
private fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount)

/* -------------------------------- Composable ------------------------------ */

/**
 * Tek bir vücut görünümü.
 *
 * @param colors anahtarı bulunan kaslar bu renkle boyanır, diğerleri sönük kalır
 * @param selected dokunulan/seçilen kas — parlak hatla çerçevelenir
 */
@Composable
fun BodyMuscleMap(
    view: BodyView,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    selected: String? = null,
    onMuscleTap: ((String) -> Unit)? = null
) {
    val regions = if (view == BodyView.FRONT) frontRegions else backRegions
    val bodyPaths = if (view == BodyView.FRONT) frontBody else backBody

    val bg = MaterialTheme.colorScheme.background
    val isDark = MaterialTheme.fit.isDark
    val accent = MaterialTheme.fit.accent

    // Gövde ve çalışılmayan kas tonları — koyu temada slate, açık temada gri
    val silTop = if (isDark) Color(0xFF39465A) else Color(0xFFCBD4E1)
    val silBottom = if (isDark) Color(0xFF27313F) else Color(0xFFB4BFCE)
    val restTop = if (isDark) Color(0xFF4A5A72) else Color(0xFFDCE3EC)
    val restBottom = if (isDark) Color(0xFF36445A) else Color(0xFFC6D0DD)
    val groove = if (isDark) Color(0xFF1A2130) else Color(0xFFFFFFFF)
    val rim = if (isDark) Color(0xFF5A6C86) else Color(0xFF9AA7B8)

    val glow by animateFloatAsState(if (selected != null) 1f else 0f, tween(220), label = "sel")

    // Mor / Lacivert (Deep Purple / Navy Blue) belirgin seçim tonları
    val exactSelTop = Color(0xFF581C87)     // Koyu Mor
    val exactSelBottom = Color(0xFF1E1B4B)  // Koyu Lacivert / Gece Mavisi

    Canvas(
        modifier.pointerInput(view, onMuscleTap) {
            if (onMuscleTap == null) return@pointerInput
            detectTapGestures { tap ->
                val s = min(size.width / BW, size.height / BH)
                val dx = (size.width - BW * s) / 2f
                val dy = (size.height - BH * s) / 2f
                val local = Offset((tap.x - dx) / s, (tap.y - dy) / s)
                regions.filter { it.contains(local) }.minByOrNull { it.area }
                    ?.let { onMuscleTap(it.key) }
            }
        }
    ) {
        val s = min(size.width / BW, size.height / BH)
        val dx = (size.width - BW * s) / 2f
        val dy = (size.height - BH * s) / 2f

        withTransform({
            translate(dx, dy)
            scale(s, s, Offset.Zero)
        }) {
            // 1) gövde silueti
            bodyPaths.forEach { p ->
                fillShaded(p, p.getBounds(), silTop, silBottom)
            }
            bodyPaths.forEach { p ->
                drawPath(p, rim.copy(alpha = 0.55f), style = Stroke(0.7f, join = StrokeJoin.Round))
            }

            // 2) çalışılmayan kaslar
            regions.forEach { r ->
                val isExact = selected != null && r.key == selected
                val c = colors[r.key]

                if (c == null || c.alpha <= 0.01f) {
                    if (isExact) {
                        r.paths.forEachIndexed { i, p -> fillShaded(p, r.bounds[i], exactSelTop, exactSelBottom) }
                    } else {
                        val alpha = if (selected != null) 0.5f else 1f
                        r.paths.forEachIndexed { i, p ->
                            fillShaded(p, r.bounds[i], restTop.copy(alpha = alpha), restBottom.copy(alpha = alpha))
                        }
                    }
                }
            }

            // 3) vurgulanan / çalışılan kaslar
            regions.forEach { r ->
                val isExact = selected != null && r.key == selected
                val c = colors[r.key]

                if (c != null && c.alpha > 0.01f) {
                    if (isExact) {
                        r.paths.forEachIndexed { i, p -> fillShaded(p, r.bounds[i], exactSelTop, exactSelBottom) }
                    } else {
                        val baseC = if (selected != null) c.copy(alpha = c.alpha * 0.45f) else c
                        r.paths.forEachIndexed { i, p ->
                            fillShaded(p, r.bounds[i], baseC.lighten(0.16f), baseC.darken(0.14f))
                        }
                    }
                }
            }

            // 4) kas sınırları — arka plan renginde ince oluk
            val grooveStroke = Stroke(0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            regions.forEach { r ->
                r.paths.forEach { drawPath(it, groove.copy(alpha = 0.75f), style = grooveStroke) }
            }

            // 5) yalnızca dokunulan seçili kas — mor/lacivert parlak hat + elektrik vurgusu
            if (selected != null && glow > 0.01f) {
                regions.filter { it.key == selected }.forEach { r ->
                    r.paths.forEach { p ->
                        // Geniş mor parlama halkası
                        drawPath(p, Color(0xFFA855F7).copy(alpha = 0.55f * glow), style = Stroke(4.2f, join = StrokeJoin.Round))
                        // Net iç kontur (Parlak açık mor / lacivert)
                        drawPath(p, Color(0xFFC084FC).copy(alpha = 0.95f * glow), style = Stroke(1.8f, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

/** Ön + arka görünümü yan yana gösterir. */
@Composable
fun BodyMuscleMapPair(
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp,
    selected: String? = null,
    onMuscleTap: ((String) -> Unit)? = null,
    showLabels: Boolean = true
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(BodyView.FRONT to "Ön", BodyView.BACK to "Arka").forEach { (view, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BodyMuscleMap(
                        view = view,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth().height(height),
                        selected = selected,
                        onMuscleTap = onMuscleTap
                    )
                    if (showLabels) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.fit.muted
                        )
                    }
                }
            }
        }

        if (selected != null && onMuscleTap != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .clickable { onMuscleTap(selected) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Seçimi Temizle",
                        tint = MaterialTheme.fit.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Temizle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** Yoğunluk ölçeği açıklaması. */
@Composable
fun MuscleHeatLegend(
    modifier: Modifier = Modifier,
    lowLabel: String = "Az",
    highLabel: String = "Çok",
    color: Color = MaterialTheme.fit.accent
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(lowLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.18f, 0.38f, 0.58f, 0.78f, 1f).forEach { a ->
                Box(
                    Modifier
                        .size(width = 22.dp, height = 8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = a))
                )
            }
        }
        Text(highLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
    }
}

/** Birincil / ikincil kas göstergesi (hareket detayı için). */
@Composable
fun MusclePrimarySecondaryLegend(
    primaryColor: Color = MaterialTheme.fit.accent,
    secondaryColor: Color = MaterialTheme.fit.accent.copy(alpha = 0.42f),
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(primaryColor to "Birincil", secondaryColor to "Destek").forEach { (c, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.fit.muted)
            }
        }
    }
}

/** Tıklanabilir kas etiketi listesi — haritanın altında özet gösterir. */
@Composable
fun MuscleChipRow(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
    selected: String? = null,
    onClick: ((String) -> Unit)? = null
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (key, color) ->
                    val active = key == selected
                    val chipBg = if (active) Color(0xFF3730A3).copy(alpha = 0.35f) else color.copy(alpha = 0.12f)
                    val dotColor = if (active) Color(0xFFA855F7) else color
                    val textColor = if (active) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(chipBg)
                            .then(if (onClick != null) Modifier.clickable { onClick(key) } else Modifier)
                            .padding(horizontal = 7.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(dotColor))
                        Text(
                            MuscleMap.label(key),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            maxLines = 1,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
