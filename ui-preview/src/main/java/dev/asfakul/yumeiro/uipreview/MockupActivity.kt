package dev.asfakul.yumeiro.uipreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

private val Ink = Color(0xFF08090B)
private val Surface1 = Color(0xFF15171A)
private val Surface2 = Color(0xFF202328)
private val Surface3 = Color(0xFF292D33)
private val Hairline = Color(0xFF34383F)
private val Paper = Color(0xFFF4F2EC)
private val Muted = Color(0xFFA6A9AF)
private val Gold = Color(0xFFF2C46F)
private val GoldWash = Color(0x2EF2C46F)
private val Motion = spring<Float>(dampingRatio = 0.92f, stiffness = 480f)

class MockupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent { YumeiroMockupApp() }
    }
}

private enum class Destination(val label: String) {
    HOME("Home"), LIBRARY("Library")
}

private enum class ContextWindow { NONE, SEARCH, SETTINGS, DETAILS, PLAYER }

private data class TitleItem(
    val name: String,
    val tagline: String,
    val meta: String,
    val genre: String,
    val progress: Float,
    val watched: String,
    val palette: List<Color>,
    val monogram: String,
    val badge: String = "",
)

private val libraryTitles = listOf(
    TitleItem("The Quiet Sea", "Somewhere, the ocean is listening.", "S1 · E06  ·  24 min left", "Sci-fi", .62f, "Continue episode 6", listOf(Color(0xFF123B49), Color(0xFF478A83), Color(0xFFE0B77A)), "QS", "NEW EPISODE"),
    TitleItem("Aster & Ash", "Every kingdom has its unfinished story.", "S2 · E03  ·  11 min left", "Fantasy", .81f, "Continue episode 3", listOf(Color(0xFF40243C), Color(0xFF9A5364), Color(0xFFE0A477)), "AA", "NEW"),
    TitleItem("Blue Hour", "The last light changes everything.", "Movie  ·  1h 18m remaining", "Drama", .38f, "Resume film", listOf(Color(0xFF172B53), Color(0xFF42658C), Color(0xFFB7C6C8)), "BH"),
    TitleItem("Paper Moons", "A summer that refuses to end.", "S1 · E09  ·  18 min left", "Slice of life", .54f, "Continue episode 9", listOf(Color(0xFF59452C), Color(0xFFB98D55), Color(0xFFE6D2A8)), "PM"),
    TitleItem("Afterlight", "The truth arrives after the sun goes down.", "Movie  ·  2h 04m", "Mystery", 0f, "Watch now", listOf(Color(0xFF263B39), Color(0xFF597C6C), Color(0xFFC5B68A)), "AL", "JUST ADDED"),
    TitleItem("Red Thread", "One small decision. A thousand consequences.", "S1 · E01  ·  42 min", "Thriller", 0f, "Start series", listOf(Color(0xFF4A2022), Color(0xFFB64D43), Color(0xFFE0A17B)), "RT"),
)

private val featuredTitles = listOf(
    libraryTitles[0],
    libraryTitles[4],
    libraryTitles[1],
)

@Composable
private fun YumeiroMockupApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var contextWindow by rememberSaveable { mutableStateOf(ContextWindow.NONE) }
    var selectedTitle by rememberSaveable { mutableStateOf(libraryTitles.first().name) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var libraryFilter by rememberSaveable { mutableStateOf("Watching") }
    var savedTitles by rememberSaveable { mutableStateOf(setOf(libraryTitles[4].name, libraryTitles[5].name)) }
    var mutedAutoplay by rememberSaveable { mutableStateOf(false) }
    var wifiOnly by rememberSaveable { mutableStateOf(true) }

    val selected = libraryTitles.firstOrNull { it.name == selectedTitle } ?: libraryTitles.first()
    val openTitle: (TitleItem) -> Unit = {
        selectedTitle = it.name
        contextWindow = ContextWindow.DETAILS
    }

    BackHandler(enabled = contextWindow != ContextWindow.NONE) {
        contextWindow = ContextWindow.NONE
        LocalFocusManager.current.clearFocus()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Ink,
            background = Ink,
            onBackground = Paper,
            surface = Surface1,
            onSurface = Paper,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            outline = Hairline,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(280)) { if (targetState == Destination.LIBRARY) it / 18 else -it / 18 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { if (targetState == Destination.LIBRARY) -it / 32 else it / 32 })
                },
                label = "main-destination",
            ) { screen ->
                when (screen) {
                    Destination.HOME -> HomeScreen(
                        onOpen = openTitle,
                        onSettings = { contextWindow = ContextWindow.SETTINGS },
                    )
                    Destination.LIBRARY -> LibraryScreen(
                        filter = libraryFilter,
                        onFilter = { libraryFilter = it },
                        savedTitles = savedTitles,
                        onOpen = openTitle,
                        onSettings = { contextWindow = ContextWindow.SETTINGS },
                    )
                }
            }

            AnimatedVisibility(
                visible = contextWindow != ContextWindow.NONE,
                enter = fadeIn(tween(190)),
                exit = fadeOut(tween(150)),
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .66f)))
            }

            AnimatedContent(
                targetState = contextWindow,
                transitionSpec = {
                    val enter = fadeIn(tween(230)) +
                        scaleIn(initialScale = .965f, animationSpec = spring(dampingRatio = .9f, stiffness = 420f)) +
                        slideInVertically(initialOffsetY = { it / 14 }, animationSpec = spring(dampingRatio = .92f, stiffness = 430f))
                    val exit = fadeOut(tween(150)) +
                        scaleOut(targetScale = .985f, animationSpec = tween(160)) +
                        slideOutVertically(targetOffsetY = { it / 20 }, animationSpec = tween(170))
                    enter togetherWith exit
                },
                label = "context-window",
            ) { window ->
                when (window) {
                    ContextWindow.NONE -> Unit
                    ContextWindow.SEARCH -> SearchWindow(
                        query = searchQuery,
                        onQuery = { searchQuery = it },
                        onClose = { contextWindow = ContextWindow.NONE },
                        onOpen = openTitle,
                    )
                    ContextWindow.SETTINGS -> SettingsWindow(
                        autoplayOff = mutedAutoplay,
                        onAutoplayOff = { mutedAutoplay = it },
                        wifiOnly = wifiOnly,
                        onWifiOnly = { wifiOnly = it },
                        onClose = { contextWindow = ContextWindow.NONE },
                    )
                    ContextWindow.DETAILS -> DetailWindow(
                        item = selected,
                        saved = selectedTitle in savedTitles,
                        onSave = {
                            savedTitles = if (selectedTitle in savedTitles) savedTitles - selectedTitle else savedTitles + selectedTitle
                        },
                        onBack = { contextWindow = ContextWindow.NONE },
                        onPlay = { contextWindow = ContextWindow.PLAYER },
                        onOpen = openTitle,
                    )
                    ContextWindow.PLAYER -> PlayerWindow(
                        item = selected,
                        onBack = { contextWindow = ContextWindow.DETAILS },
                    )
                }
            }

            if (contextWindow == ContextWindow.NONE) {
                FloatingDock(
                    selected = destination,
                    onSelect = { destination = it },
                    onSearch = {
                        searchQuery = ""
                        contextWindow = ContextWindow.SEARCH
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    eyebrow: String,
    title: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(eyebrow.uppercase(), color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
            Text(title, color = Paper, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp)
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Surface1).border(1.dp, Color.White.copy(alpha = .06f), CircleShape),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Paper, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onOpen: (TitleItem) -> Unit,
    onSettings: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { featuredTitles.size })
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(27.dp),
    ) {
        ScreenHeader(
            eyebrow = "YUMEIRO  ·  WATCH NOW",
            title = "A world worth staying in.",
            onSettings = onSettings,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 18.dp, end = 44.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            HeroFeature(feature = featuredTitles[page], onOpen = { onOpen(featuredTitles[page]) })
        }
        Row(
            Modifier.fillMaxWidth().padding(top = (-15).dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(featuredTitles.size) { index ->
                val active = pagerState.currentPage == index
                val width by animateDpAsState(if (active) 18.dp else 5.dp, animationSpec = Motion, label = "hero-page-indicator")
                Box(
                    Modifier.padding(horizontal = 3.dp).width(width).height(4.dp)
                        .clip(CircleShape).background(if (active) Gold else Surface3),
                )
            }
        }

        ContentRail(
            title = "Continue watching",
            subtitle = "Pick up where you left off",
            items = libraryTitles.take(4),
            kind = RailKind.CONTINUE,
            onOpen = onOpen,
        )

        TopTenRail(items = libraryTitles.drop(1).take(5), onOpen = onOpen)

        ContentRail(
            title = "Recently added",
            subtitle = "Fresh arrivals for your next night in",
            items = listOf(libraryTitles[4], libraryTitles[5], libraryTitles[2], libraryTitles[1]),
            kind = RailKind.POSTER,
            onOpen = onOpen,
        )

        MoodRail(onOpen = onOpen)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun HeroFeature(feature: TitleItem, onOpen: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(feature.palette))
            .clickable(onClick = onOpen),
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 58.dp, y = (-56).dp).size(250.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(150.dp)
                .clip(CircleShape).border(1.dp, Color.White.copy(alpha = .20f), CircleShape),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 56.dp).size(80.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = .08f)),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .02f),
                    .34f to Color.Black.copy(alpha = .06f),
                    .64f to Color.Black.copy(alpha = .62f),
                    1f to Ink.copy(alpha = .98f),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 21.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (feature.badge.isNotBlank()) {
                Row(
                    Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .28f))
                        .border(1.dp, Color.White.copy(alpha = .12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Gold))
                    Spacer(Modifier.width(7.dp))
                    Text(feature.badge, color = Paper, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
            }
            Text(feature.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.1).sp, lineHeight = 36.sp)
            Text(feature.tagline, color = Color.White.copy(alpha = .84f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("SERIES  ·  " + feature.genre.uppercase() + "  ·  2026", color = Color.White.copy(alpha = .66f), fontSize = 9.sp, letterSpacing = 1.1.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = Ink),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 19.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = onOpen,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = .16f), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private enum class RailKind { CONTINUE, POSTER }

@Composable
private fun ContentRail(
    title: String,
    subtitle: String,
    items: List<TitleItem>,
    kind: RailKind,
    onOpen: (TitleItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RailHeading(title, subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { item ->
                when (kind) {
                    RailKind.CONTINUE -> ContinueTile(item) { onOpen(item) }
                    RailKind.POSTER -> PosterTile(item) { onOpen(item) }
                }
            }
        }
    }
}

@Composable
private fun RailHeading(title: String, subtitle: String = "") {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, color = Paper, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.25).sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ContinueTile(item: TitleItem, onClick: () -> Unit) {
    Column(Modifier.width(246.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().height(139.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(item.palette)),
        ) {
            PosterGlyph(item, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .70f)))))
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(27.dp).clip(CircleShape).background(Color.White.copy(alpha = .94f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.meta, color = Color.White.copy(alpha = .72f), fontSize = 9.sp, maxLines = 1)
                }
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More options", tint = Color.White.copy(alpha = .8f))
            }
        }
        LinearProgressIndicator(
            progress = { item.progress },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = Gold,
            trackColor = Surface3,
        )
    }
}

@Composable
private fun PosterTile(item: TitleItem, onClick: () -> Unit) {
    Column(Modifier.width(132.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(item.palette))) {
            PosterGlyph(item, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .62f)))))
            if (item.badge.isNotBlank()) {
                Text(
                    item.badge,
                    Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 4.dp),
                    color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp,
                )
            }
            Text(item.genre.uppercase(), Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.White.copy(alpha = .9f), fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        }
        Text(item.name, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("2026  ·  " + item.genre, color = Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun PosterGlyph(item: TitleItem, modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 37.dp, y = (-25).dp).size(155.dp)
                .clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(alpha = .24f), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.Center).size(115.dp).clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = .22f), CircleShape),
        )
        Box(
            Modifier.align(Alignment.Center).size(63.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = .08f)),
        )
        Text(
            item.monogram,
            Modifier.align(Alignment.Center),
            color = Color.White.copy(alpha = .78f),
            fontSize = 30.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun TopTenRail(items: List<TitleItem>, onOpen: (TitleItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RailHeading("Top 10 tonight", "The titles everyone is talking about")
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items(items) { item ->
                Row(
                    Modifier.width(173.dp).height(164.dp).clickable { onOpen(item) },
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        (items.indexOf(item) + 1).toString(),
                        color = Color(0xFF74777D),
                        fontSize = 70.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-5).sp,
                        modifier = Modifier.offset(x = 8.dp, y = 2.dp),
                    )
                    Box(
                        Modifier.padding(start = 0.dp, bottom = 2.dp).size(width = 105.dp, height = 149.dp)
                            .clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(item.palette)),
                    ) {
                        PosterGlyph(item, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .74f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                            Text(item.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.genre, color = Color.White.copy(alpha = .76f), fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodRail(onOpen: (TitleItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RailHeading("A little more of what you love", "Hand-picked collections")
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            val collections = listOf(
                Triple("Slow Sundays", "Unhurried stories", Color(0xFF465D50)),
                Triple("After dark", "Mysteries & thrillers", Color(0xFF463345)),
                Triple("Faraway worlds", "Sci-fi & fantasy", Color(0xFF294A62)),
            )
            items(collections) { collection ->
                Row(
                    Modifier.width(220.dp).height(88.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(collection.third, Ink)))
                        .clickable { onOpen(libraryTitles[if (collection.first == "After dark") 5 else 0]) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(collection.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(collection.second, color = Color.White.copy(alpha = .7f), fontSize = 9.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = .8f))
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    filter: String,
    onFilter: (String) -> Unit,
    savedTitles: Set<String>,
    onOpen: (TitleItem) -> Unit,
    onSettings: () -> Unit,
) {
    val filters = listOf("Watching", "Watchlist", "Downloaded")
    val visible = when (filter) {
        "Watchlist" -> libraryTitles.filter { it.name in savedTitles }
        "Downloaded" -> libraryTitles.filter { it.name == "Blue Hour" }
        else -> libraryTitles.filter { it.progress > 0f }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            eyebrow = "YOUR COLLECTION",
            title = "Your library.",
            onSettings = onSettings,
        )
        Text("All the stories you want to come back to.", color = Muted, fontSize = 12.sp)
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(Surface1).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            filters.forEach { choice ->
                val selected = filter == choice
                Box(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (selected) Surface3 else Color.Transparent)
                        .clickable { onFilter(choice) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(choice, color = if (selected) Paper else Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
        AnimatedContent(
            targetState = filter,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 20 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically(tween(150)) { -it / 28 })
            },
            label = "library-filter",
        ) { selectedFilter ->
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedFilter.uppercase(), color = Gold, fontSize = 9.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(visible.size.toString() + " TITLES", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                if (visible.isEmpty()) {
                    EmptyCollection()
                } else {
                    visible.forEach { item ->
                        LibraryRow(item, onClick = { onOpen(item) }, showProgress = selectedFilter == "Watching")
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(item: TitleItem, onClick: () -> Unit, showProgress: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(width = 88.dp, height = 100.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(item.palette))) {
            PosterGlyph(item, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.name, color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.genre + "  ·  2026", color = Muted, fontSize = 10.sp)
            Text(item.meta, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showProgress) {
                LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = Gold, trackColor = Surface3)
            } else {
                Text(if (item.progress > 0f) "IN PROGRESS" else "SAVED FOR LATER", color = Gold, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun EmptyCollection() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(Surface1), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.BookmarkBorder, null, tint = Gold, modifier = Modifier.size(23.dp))
        }
        Text("Nothing in this shelf yet", color = Paper, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("Titles you save will appear here.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun DetailWindow(
    item: TitleItem,
    saved: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpen: (TitleItem) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(390.dp)) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(item.palette)))
            PosterGlyph(item, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = .24f), Color.Transparent, Ink.copy(alpha = .97f))),
                ),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                RoundAction(Icons.Rounded.Cast, "Cast", {})
                Spacer(Modifier.width(8.dp))
                RoundAction(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Save", onSave, active = saved)
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("YUMEIRO ORIGINAL", color = Gold, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text(item.name, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                Text("2026  ·  " + item.genre + "  ·  1 season  ·  16+", color = Color.White.copy(alpha = .78f), fontSize = 10.sp)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(item.tagline + " " + "A quiet choice becomes a journey neither of them expected.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = Ink),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (item.progress > 0f) "Resume" else "Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                FilledTonalButton(onClick = onSave, modifier = Modifier.height(48.dp), shape = CircleShape) {
                    Icon(if (saved) Icons.Rounded.Check else Icons.Rounded.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (saved) "In Library" else "Add to Library", fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Episodes", color = Paper, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Season 1", color = Muted, fontSize = 10.sp)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
            repeat(5) { index ->
                EpisodeRow(index, item)
            }
            RailHeading("Trailers", "A first look")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                items(listOf("Official trailer", "Behind the scenes")) { title ->
                    Box(Modifier.width(224.dp).height(126.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(item.palette)), contentAlignment = Alignment.Center) {
                        PosterGlyph(item, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)))
                        Box(Modifier.size(39.dp).clip(CircleShape).background(Color.White.copy(alpha = .92f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Ink)
                        }
                        Text(title, Modifier.align(Alignment.BottomStart).padding(11.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            ContentRail(
                title = "You might also like",
                subtitle = "More stories in this universe",
                items = libraryTitles.filter { it.name != item.name }.take(4),
                kind = RailKind.POSTER,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun EpisodeRow(index: Int, item: TitleItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(width = 108.dp, height = 69.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(item.palette)), contentAlignment = Alignment.Center) {
            PosterGlyph(item, Modifier.fillMaxSize())
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = .42f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("0" + (index + 1) + "  ·  " + listOf("The Arrival", "Low Tide", "A Small Promise", "Signal Noise", "The Long Way")[index], color = Paper, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("24 min  ·  " + if (index == 0) "The signal returns." else "The story continues.", color = Muted, fontSize = 9.sp)
        }
        Icon(Icons.Rounded.MoreHoriz, "Episode options", tint = Muted)
    }
}

@Composable
private fun PlayerWindow(item: TitleItem, onBack: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction(Icons.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Season 1  ·  Episode 06", color = Muted, fontSize = 9.sp)
            }
            RoundAction(Icons.Rounded.Cast, "Cast", {})
            Spacer(Modifier.width(7.dp))
            RoundAction(Icons.Rounded.Tune, "Playback settings", { menu = !menu })
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Gold, modifier = Modifier.size(33.dp))
                }
                Text("UI PREVIEW", color = Gold, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Video surface placeholder", color = Muted, fontSize = 11.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("12:48", color = Color.White, fontSize = 9.sp)
                LinearProgressIndicator(progress = { .43f }, modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape), color = Gold, trackColor = Surface3)
                Text("24:06", color = Color.White, fontSize = 9.sp)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Rounded.Subtitles, "Subtitles", { menu = !menu })
                IconButton(onClick = { playing = !playing }, modifier = Modifier.size(58.dp).clip(CircleShape).background(Gold)) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", tint = Ink, modifier = Modifier.size(29.dp))
                }
                RoundAction(Icons.Rounded.SkipNext, "Next episode", { menu = !menu })
            }
            AnimatedVisibility(visible = menu, enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 8 }, exit = fadeOut(tween(130)) + slideOutVertically(tween(150)) { it / 12 }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Surface1).padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("PLAYBACK", color = Gold, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                    Text("Subtitles  ·  English", color = Paper, fontSize = 11.sp)
                    Text("Audio  ·  Japanese", color = Paper, fontSize = 11.sp)
                    Text("Quality  ·  Auto", color = Paper, fontSize = 11.sp)
                }
            }
            Text("NOW PLAYING", color = Gold, fontSize = 8.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
            Text("A quiet transmission arrives from somewhere beyond the coastline.", color = Paper, fontSize = 12.sp)
            Text("Visual prototype only. No stream is loaded.", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SearchWindow(
    query: String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onOpen: (TitleItem) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val matches = remember(query) {
        if (query.isBlank()) libraryTitles.take(4) else libraryTitles.filter {
            it.name.contains(query, ignoreCase = true) || it.genre.contains(query, ignoreCase = true) || it.tagline.contains(query, ignoreCase = true)
        }
    }
    LaunchedEffect(Unit) {
        delay(230)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    Column(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding().imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 13.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FIND YOUR NEXT STORY", color = Gold, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("Search Yumeiro", color = Paper, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { keyboard?.hide(); focusManager.clearFocus(); onClose() }, modifier = Modifier.size(42.dp).clip(CircleShape).background(Surface1)) {
                Icon(Icons.Rounded.Close, "Close search", tint = Paper)
            }
        }
        Text(if (query.isBlank()) "A GOOD PLACE TO START" else matches.size.toString() + " MATCHES", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (matches.isEmpty()) {
                Text("No titles found. Try a different title or genre.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp))
            } else {
                matches.forEach { item ->
                    SearchResult(item) {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        onOpen(item)
                    }
                }
            }
        }
        SearchComposer(
            query = query,
            onQuery = onQuery,
            onClose = {
                keyboard?.hide()
                focusManager.clearFocus()
                onClose()
            },
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun SearchResult(item: TitleItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(width = 65.dp, height = 77.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(item.palette))) {
            PosterGlyph(item, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(item.genre + "  ·  2026", color = Muted, fontSize = 9.sp)
            Text(item.tagline, color = Muted.copy(alpha = .8f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun SearchComposer(
    query: String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF24262B))
            .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(28.dp))
            .padding(start = 16.dp, end = 7.dp, top = 7.dp, bottom = 7.dp)
            .animateContentSize(spring(dampingRatio = .9f, stiffness = 520f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Search, null, tint = Muted, modifier = Modifier.size(19.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(color = Paper, fontSize = 13.sp),
            cursorBrush = SolidColor(Gold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("Search titles, genres, moods", color = Muted, fontSize = 12.sp)
                    inner()
                }
            },
        )
        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(tween(110)) + scaleIn(initialScale = .8f), exit = fadeOut(tween(90)) + scaleOut(targetScale = .8f)) {
            IconButton(onClick = { onQuery("") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, "Clear", tint = Muted, modifier = Modifier.size(17.dp))
            }
        }
        Box(Modifier.size(39.dp).clip(CircleShape).background(Gold).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, "Close search", tint = Ink, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsWindow(
    autoplayOff: Boolean,
    onAutoplayOff: (Boolean) -> Unit,
    wifiOnly: Boolean,
    onWifiOnly: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(30.dp)).background(Surface1)
            .padding(horizontal = 20.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PREFERENCES", color = Gold, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("Settings", color = Paper, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(42.dp).clip(CircleShape).background(Surface2)) {
                Icon(Icons.Rounded.Close, "Close settings", tint = Paper)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsGroup("PLAYBACK") {
                SettingsSwitch("Autoplay next episode", "Continue to the next episode automatically", autoplayOff, onAutoplayOff)
                SettingsItem("Preferred quality", "Auto · Best available", Icons.Rounded.Tune)
                SettingsItem("Subtitles", "English · Default", Icons.Rounded.Subtitles)
            }
            SettingsGroup("DOWNLOADS") {
                SettingsSwitch("Wi-Fi only", "Avoid using mobile data for downloads", wifiOnly, onWifiOnly)
                SettingsItem("Download location", "Internal storage", Icons.Rounded.Download)
            }
            SettingsGroup("APP") {
                SettingsItem("Appearance", "Tsuki Night", Icons.Rounded.Settings)
                SettingsItem("Motion", "Expressive · Reduced bounce", Icons.Rounded.FavoriteBorder)
            }
            Text("YUMEIRO UI PREVIEW  ·  BACKEND NOT CONNECTED", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable Column.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF1D2024)).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 9.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 9.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = .36f))
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape),
    ) {
        Icon(icon, description, tint = if (active) Gold else Paper, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FloatingDock(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding()
            .padding(start = 17.dp, end = 17.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier.weight(1f, fill = false).widthIn(max = 255.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color(0xF51B1D20))
                .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(28.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destination ->
                val active = selected == destination
                val weight by animateFloatAsState(
                    targetValue = if (active) 1.42f else 1f,
                    animationSpec = spring(dampingRatio = .9f, stiffness = 500f),
                    label = "dock-tab-weight",
                )
                Row(
                    Modifier.weight(weight).height(48.dp).clip(CircleShape)
                        .background(if (active) GoldWash else Color.Transparent)
                        .clickable { onSelect(destination) }
                        .padding(horizontal = if (active) 12.dp else 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (destination == Destination.HOME) Icons.Rounded.Home else Icons.Rounded.LibraryBooks,
                        destination.label,
                        tint = if (active) Gold else Muted,
                        modifier = Modifier.size(20.dp),
                    )
                    AnimatedVisibility(visible = active, enter = fadeIn(tween(120)) + scaleIn(initialScale = .92f), exit = fadeOut(tween(90)) + scaleOut(targetScale = .92f)) {
                        Text(destination.label, Modifier.padding(start = 7.dp), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(Gold)
                .border(1.dp, Color.White.copy(alpha = .22f), CircleShape)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Ink, modifier = Modifier.size(24.dp))
        }
    }
}
