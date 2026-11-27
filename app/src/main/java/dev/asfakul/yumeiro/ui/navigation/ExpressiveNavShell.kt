package dev.asfakul.yumeiro.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.asfakul.yumeiro.ui.theme.LocalMtPalette
import kotlinx.coroutines.CancellationException

enum class StreamTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    WATCH(Screen.Videos.route, "Watch", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_tab_watch"),
    SEARCH(Screen.OnlineSearch.route, "Search", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_search"),
    SOURCES(Screen.SourcesHub.route(0), "Sources", Icons.Filled.Extension, Icons.Outlined.Extension, "nav_tab_sources"),
}

private val ExpressiveSpatialSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessHigh,
)

@Composable
fun ExpressiveStreamShell(
    currentTab: StreamTab,
    onSelectTab: (StreamTab) -> Unit,
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = currentTab != StreamTab.WATCH) { flow ->
        try {
            flow.collect { event -> backProgress = event.progress }
            backProgress = 0f
            onSelectTab(StreamTab.WATCH)
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    val contentScale by animateFloatAsState(
        targetValue = 1f - (backProgress * 0.06f),
        animationSpec = ExpressiveSpatialSpring,
        label = "predictiveBackScale",
    )

    Box(Modifier.fillMaxSize()) {
        content(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
        )

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        ) {
            FloatingMorphNavBar(
                currentTab = currentTab,
                onSelectTab = onSelectTab,
            )
        }
    }
}

@Composable
fun FloatingMorphNavBar(
    currentTab: StreamTab,
    onSelectTab: (StreamTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalMtPalette.current
    Surface(
        modifier = modifier.height(60.dp),
        shape = CircleShape,
        color = p.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, p.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StreamTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) p.accent else p.surfaceContainerHigh,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tabBg",
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) p.onAccent else p.textSecondary,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tabFg",
                )
                val horizPad by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 14.dp,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessHigh),
                    label = "tabPad",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = ExpressiveSpatialSpring,
                    label = "iconScale",
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { if (!isSelected) onSelectTab(tab) },
                        )
                        .padding(horizontal = horizPad)
                        .testTag(tab.tag)
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                            contentDescription = tab.label
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) +
                            expandHorizontally(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessHigh)),
                        exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                            shrinkHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessHigh)),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tab.label,
                                color = fg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
