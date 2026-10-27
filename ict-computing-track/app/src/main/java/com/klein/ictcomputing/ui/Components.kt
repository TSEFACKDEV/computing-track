package com.klein.ictcomputing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ContestColors.Background),
    ) {
        content()
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    background: Color = ContestColors.Surface,
    corner: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(background),
    ) {
        content()
    }
}

@Composable
fun ChipPill(
    text: String,
    color: Color = ContestColors.AccentCyan,
) {
    val bg = color.copy(alpha = 0.14f)
    val fg = color.copy(alpha = 0.95f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Meter(
    value: Float,
    color: Color = ContestColors.AccentCyan,
    track: Color = Color(0x1F96A3C4),
    height: Dp = 8.dp,
) {
    val safe = value.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safe)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.75f)))),
        )
    }
}

@Composable
fun MiniStat(
    label: String,
    value: String,
    color: Color = ContestColors.OnSurface,
) {
    CardBox(modifier = Modifier.padding(2.dp)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 14.dp)) {
            Text(label, color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ContestColors.OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun DotIndicator(
    total: Int,
    filled: Int,
    color: Color = ContestColors.AccentCyan,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) color else color.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
fun CodeBlock(code: List<String>) {
    if (code.isEmpty()) return
    CardBox(background = ContestColors.SurfaceLow, corner = 14.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            code.forEach { line ->
                Text(
                    line,
                    color = ContestColors.AccentCyan,
                    fontSize = 13.sp,
                    fontFamily = FontMono,
                )
            }
        }
    }
}

@Composable
fun DataTable(headers: List<String>, rows: List<List<String>>) {
    if (headers.isEmpty()) return
    // Largeur adaptée au contenu : les tableaux de 4-5 colonnes défilent horizontalement
    // au lieu d'écraser chaque cellule sur quelques caractères.
    val widths = headers.indices.map { i ->
        val longest = (sequenceOf(headers[i]) + rows.asSequence().mapNotNull { it.getOrNull(i) })
            .maxOfOrNull { it.length } ?: 10
        (longest.coerceIn(10, 34) * 7).dp
    }
    CardBox(background = ContestColors.SurfaceLow, corner = 14.dp) {
        Column(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
        ) {
            Row(Modifier.padding(vertical = 4.dp)) {
                headers.forEachIndexed { i, h ->
                    Text(
                        h,
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(widths[i]).padding(end = 8.dp),
                    )
                }
            }
            rows.forEach { row ->
                Row(
                    Modifier
                        .border(0.5.dp, ContestColors.Border.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                ) {
                    headers.indices.forEach { i ->
                        Text(
                            row.getOrNull(i).orEmpty(),
                            color = ContestColors.OnSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.width(widths[i]).padding(end = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}
