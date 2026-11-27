package dev.asfakul.yumeiro.ui.screens.video

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.asfakul.yumeiro.online.OnlineLink
import dev.asfakul.yumeiro.online.OnlineSession
import dev.asfakul.yumeiro.online.ProviderCircuitBreaker
import dev.asfakul.yumeiro.ui.theme.DangerTone
import dev.asfakul.yumeiro.ui.theme.LocalMtPalette
import dev.asfakul.yumeiro.ui.theme.WarningTone
import dev.asfakul.yumeiro.ui.theme.Yume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSourceSheet(
    currentPositionMs: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val session by OnlineSession.state.collectAsState()
    val state = session ?: return
    val p = LocalMtPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Yume.shapeSheet,
        containerColor = p.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Sources",
                        color = p.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${state.links.size} mirrors · ${state.api}",
                        color = p.textSecondary,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = { OnlineSession.retry(context) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh mirrors", tint = p.accent)
                }
            }

            if (!state.linksDone) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = p.accent,
                    trackColor = p.surfaceContainer,
                )
            }

            Spacer(Modifier.height(12.dp))

            val visibleLinks = remember(state.links, state.shownLinks) {
                state.links.take(state.shownLinks)
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(visibleLinks, key = { idx, l -> "${idx}_${l.url}" }) { index, link ->
                    val isActive = index == state.linkIndex
                    val isFailed = link.url in state.failed
                    MirrorRow(
                        link = link,
                        isActive = isActive,
                        isFailed = isFailed,
                        onClick = {
                            OnlineSession.switchSource(context, index, currentPositionMs)
                            onDismiss()
                        },
                    )
                }

                if (state.hiddenLinks > 0) {
                    item("load_more") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            FilledTonalButton(onClick = { OnlineSession.loadMore() }) {
                                Text("Show ${state.hiddenLinks} more mirrors")
                            }
                        }
                    }
                }

                if (state.linksDone && state.links.isEmpty()) {
                    item("empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(state.message ?: "No playable sources found.", color = p.textSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { OnlineSession.retry(context) }) {
                                Text("Retry extraction", color = p.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MirrorRow(
    link: OnlineLink,
    isActive: Boolean,
    isFailed: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalMtPalette.current
    val health = remember(link.url) { ProviderCircuitBreaker.snapshot(link.url) }
    val bg by animateColorAsState(
        targetValue = if (isActive) p.accent.copy(alpha = 0.16f) else p.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "mirrorBg",
    )
    val border = if (isActive) BorderStroke(1.25.dp, p.accent) else BorderStroke(0.5.dp, p.outlineVariant.copy(alpha = 0.35f))

    val dotColor: Color = when {
        isFailed || health.status == ProviderCircuitBreaker.Status.TRIPPED -> DangerTone
        health.status == ProviderCircuitBreaker.Status.DEGRADED -> WarningTone
        health.status == ProviderCircuitBreaker.Status.HEALTHY -> Color(0xFF4ADE80)
        else -> p.textTertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFailed && !isActive) 0.55f else 1f)
            .clip(Yume.shapeMd)
            .clickable(onClick = onClick),
        shape = Yume.shapeMd,
        color = bg,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = link.name.ifBlank { link.source },
                    color = p.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    if (link.qualityLabel.isNotBlank()) add(link.qualityLabel)
                    if (link.formatLabel.isNotBlank()) add(link.formatLabel)
                    if (health.avgLatencyMs > 0) add("${health.avgLatencyMs} ms")
                    if (isFailed) add("Failed")
                }.joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, color = p.textSecondary, fontSize = 12.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = when {
                    isActive -> Icons.Default.CheckCircle
                    isFailed -> Icons.Default.ErrorOutline
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = if (isActive) p.accent else p.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
