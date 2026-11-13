package com.vennela365.viewer.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.CatAmber
import com.vennela365.viewer.theme.CatEmerald
import com.vennela365.viewer.theme.CatRose
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.aeroGlass

enum class PiiCategory(val label: String, val color: Color) {
    EMAIL("Email", CatEmerald),
    CREDIT_CARD("Card", CatRose),
    PHONE("Phone", CatAmber),
    FINANCIAL("Salary/Price", CatAmber),
    IDENTIFIER("Gov ID/SSN", CatRose)
}

@Immutable
data class PiiMatch(
    val range: IntRange,
    val originalText: String,
    val category: PiiCategory
)

object PrivacyEngine {

    val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    val CARD_REGEX = Regex("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b")
    val PHONE_REGEX = Regex("\\+?\\d{1,3}[-.\\s]?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}")
    val FINANCIAL_REGEX = Regex("[$€£₹]\\s?\\d+(?:,\\d{3})*(?:\\.\\d+)?")
    val SSN_REGEX = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")

    /**
     * Scans input text and returns non-overlapping merged PII matches.
     */
    fun scanPii(text: String): List<PiiMatch> {
        val raw = mutableListOf<PiiMatch>()

        EMAIL_REGEX.findAll(text).forEach { m -> raw.add(PiiMatch(m.range, m.value, PiiCategory.EMAIL)) }
        CARD_REGEX.findAll(text).forEach { m -> raw.add(PiiMatch(m.range, m.value, PiiCategory.CREDIT_CARD)) }
        PHONE_REGEX.findAll(text).forEach { m -> raw.add(PiiMatch(m.range, m.value, PiiCategory.PHONE)) }
        FINANCIAL_REGEX.findAll(text).forEach { m -> raw.add(PiiMatch(m.range, m.value, PiiCategory.FINANCIAL)) }
        SSN_REGEX.findAll(text).forEach { m -> raw.add(PiiMatch(m.range, m.value, PiiCategory.IDENTIFIER)) }

        val sorted = raw.sortedBy { it.range.first }
        val merged = mutableListOf<PiiMatch>()

        for (match in sorted) {
            val last = merged.lastOrNull()
            if (last == null || match.range.first > last.range.last) {
                merged.add(match)
            }
            // Overlapping intervals are suppressed in favor of earlier/higher-priority category
        }

        return merged
    }

    /**
     * Replaces sensitive matches with frosted redacted tokens safely with interval merging.
     */
    fun maskPii(text: String, revealedMatches: Set<Int> = emptySet()): String {
        val matches = scanPii(text)
        if (matches.isEmpty()) return text

        val sb = StringBuilder()
        var lastIndex = 0

        matches.forEachIndexed { index, match ->
            if (match.range.first >= lastIndex) {
                sb.append(text.substring(lastIndex, match.range.first))
                if (index in revealedMatches) {
                    sb.append(match.originalText)
                } else {
                    sb.append("[•••• ${match.category.label} ••••]")
                }
                lastIndex = match.range.last + 1
            }
        }

        if (lastIndex < text.length) {
            sb.append(text.substring(lastIndex))
        }

        return sb.toString()
    }
}

/**
 * Directional Privacy Curtain & PII Masking Overlay.
 *
 * Implements:
 * 1. Tap-to-peek automatic PII masking with safe interval merging.
 * 2. WindowManager FLAG_SECURE protection against OS recents and screenshot leaks.
 * 3. Directional micro-louvre angle privacy filter.
 */
@Composable
fun BoxScope.PrivacyCurtain(
    isActive: Boolean,
    onClose: () -> Unit,
    enableMicroLouvre: Boolean,
    onToggleMicroLouvre: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    // Secure OS Window against Task Switcher & screenshot leaks
    DisposableEffect(isActive) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Directional Micro-Louvre Angle Vignette Filter
    if (enableMicroLouvre) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0.00f to Color.Black.copy(alpha = 0.85f),
                    0.15f to Color.Black.copy(alpha = 0.25f),
                    0.50f to Color.Transparent,
                    0.85f to Color.Black.copy(alpha = 0.25f),
                    1.00f to Color.Black.copy(alpha = 0.85f)
                ),
                size = size
            )
        }
    }

    // Top Privacy HUD Card
    Box(
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .aeroGlass(shape = RoundedCornerShape(22.dp), elevation = 14.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CatEmerald.copy(alpha = 0.25f))
                        .border(1.dp, CatEmerald.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CatEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Directional Privacy Active",
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PII Masked • Recents Blocked",
                        color = palette.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Angle Louvre",
                    color = palette.textSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = enableMicroLouvre,
                    onCheckedChange = onToggleMicroLouvre,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accent.secondary,
                        checkedTrackColor = accent.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(width = 36.dp, height = 24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Privacy",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
