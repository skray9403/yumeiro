package dev.asfakul.yumeiro.uipreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0D0E10)
private val Surface1 = Color(0xFF17191C)
private val Surface2 = Color(0xFF202328)
private val Line = Color(0xFF30343A)
private val Paper = Color(0xFFF2F1EC)
private val Muted = Color(0xFFA7A9AE)
private val Gold = Color(0xFFF3C36B)

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

private enum class Destination(val label: String) { HOME("Home"), EXPLORE("Explore"), LIBRARY("Library") }

private data class TitleItem(
    val name: String,
    val detail: String,
    val genre: String,
    val progress: Float,
    val colors: List<Color>,
    val initials: String,
)

private val titles = listOf(
    TitleItem("The Quiet Sea", "S1 · E06  |  24 min left", "Sci-fi", .62f, listOf(Color(0xFF123B49), Color(0xFF478A83), Color(0xFFE0B77A)), "QS"),
    TitleItem("Aster & Ash", "S2 · E03  |  11 min left", "Fantasy", .81f, listOf(Color(0xFF40243C), Color(0xFF9A5364), Color(0xFFE0A477)), "AA"),
    TitleItem("Blue Hour", "Movie  |  1h 18m", "Drama", .38f, listOf(Color(0xFF172B53), Color(0xFF42658C), Color(0xFFB7C6C8)), "BH"),
    TitleItem("Paper Moons", "S1 · E09  |  18 min left", "Slice of life", .54f, listOf(Color(0xFF59452C), Color(0xFFB98D55), Color(0xFFE6D2A8)), "PM"),
    TitleItem("Afterlight", "Movie  |  2h 04m", "Mystery", 0f, listOf(Color(0xFF263B39), Color(0xFF597C6C), Color(0xFFC5B68A)), "AL"),
    TitleItem("Red Thread", "S1 · E01  |  New", "Thriller", 0f, listOf(Color(0xFF4A2022), Color(0xFFB64D43), Color(0xFFE0A17B)), "RT"),
)

@Composable
private fun YumeiroMockupApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var page by rememberSaveable { mutableStateOf("root") }
    var selectedTitle by rememberSaveable { mutableStateOf(titles[0].name) }
    var query by rememberSaveable { mutableStateOf("") }
    var libraryFilter by rememberSaveable { mutableStateOf("Watching") }
    var saved by rememberSaveable { mutableStateOf(false) }
    val item = titles.firstOrNull { it.name == selectedTitle } ?: titles.first()
    val openDetail: (TitleItem) -> Unit = { selectedTitle = it.name; page = "detail" }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, onPrimary = Ink, background = Ink, onBackground = Paper, surface = Surface1, onSurface = Paper, surfaceVariant = Surface2, onSurfaceVariant = Muted, outline = Line)) {
        Box(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
            when (page) {
                "detail" -> DetailScreen(item, saved, { saved = !saved }, { page = "root" }, { page = "player" })
                "player" -> PlayerScreen(item) { page = "detail" }
                "search" -> SearchScreen(query, { query = it }, { page = "root" }, openDetail)
                else -> when (destination) {
                    Destination.HOME -> HomeScreen({ page = "search" }, { destination = Destination.EXPLORE }, openDetail)
                    Destination.EXPLORE -> ExploreScreen(openDetail)
                    Destination.LIBRARY -> LibraryScreen(libraryFilter, { libraryFilter = it }, openDetail)
                }
            }
            if (page == "root") FloatingDock(destination, { destination = it }, { page = "search" }, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun HomeScreen(onSearch: () -> Unit, onExplore: () -> Unit, onOpen: (TitleItem) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(25.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YUMEIRO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("Find your next world.", color = Paper, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp)
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(46.dp).clip(CircleShape).background(Surface1)) { Icon(Icons.Rounded.Search, "Search", tint = Paper) }
            Spacer(Modifier.width(9.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) { Text("S", color = Gold, fontWeight = FontWeight.Bold) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading("TONIGHT'S PICK", "A little escape")
            HeroFeature { onOpen(titles[4]) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            SectionHeading("PICK UP AGAIN", "View all")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) { items(titles.take(4)) { item -> ContinueCard(item) { onOpen(item) } } }
        }
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            SectionHeading("MADE FOR YOUR MOOD", "Explore")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(titles.drop(1)) { item -> CoverCard(item) { onOpen(item) } } }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Surface1).clickable(onClick = onExplore).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Not sure what to watch?", color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Browse by mood, genre or runtime.", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Gold)
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Paper, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(action, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeroFeature(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(238.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF203D43), Color(0xFF17272F), Color(0xFF30202B)))).clickable(onClick = onClick)) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 42.dp, y = (-34).dp).size(210.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x88E5BC80), Color(0x005A8075)))))
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 32.dp).size(112.dp).clip(CircleShape).border(1.dp, Color(0x55F2D6A2), CircleShape))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC07090B)))).padding(horizontal = 20.dp, vertical = 19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("FEATURED  ·  SERIES", color = Gold, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Text("The Quiet Sea", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sci-fi  •  2026  •  1 season", color = Color(0xFFD4D6D5), fontSize = 11.sp, modifier = Modifier.weight(1f))
                Row(Modifier.clip(CircleShape).background(Gold).padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(16.dp))
                    Text("Explore", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(item: TitleItem, onClick: () -> Unit) {
    Column(Modifier.width(218.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CoverArt(item, Modifier.size(width = 86.dp, height = 78.dp), true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.detail, color = Muted, fontSize = 10.sp, maxLines = 2, lineHeight = 14.sp)
                Text("CONTINUE", color = Gold, fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = Gold, trackColor = Surface2)
    }
}

@Composable
private fun CoverCard(item: TitleItem, onClick: () -> Unit) {
    Column(Modifier.width(126.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoverArt(item, Modifier.fillMaxWidth().height(164.dp))
        Text(item.name, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.genre, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CoverArt(item: TitleItem, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(if (compact) 15.dp else 19.dp)).background(Brush.linearGradient(item.colors))) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-20).dp).size(if (compact) 80.dp else 138.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f)))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-18).dp, y = 22.dp).size(if (compact) 66.dp else 110.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = .25f), CircleShape))
        Text(item.initials, Modifier.align(Alignment.Center), color = Color.White.copy(alpha = .84f), fontSize = if (compact) 23.sp else 35.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
        if (!compact) Text(item.genre.uppercase(), Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.White.copy(alpha = .9f), fontSize = 8.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExploreScreen(onOpen: (TitleItem) -> Unit) {
    var genre by rememberSaveable { mutableStateOf("For you") }
    val genres = listOf("For you", "Anime", "Series", "Movies", "Mystery", "Drama")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("EXPLORE", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Choose your mood.", color = Paper, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = {}, modifier = Modifier.clip(CircleShape).background(Surface1)) { Icon(Icons.Rounded.Tune, "Filters", tint = Paper) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            genres.forEach { value -> FilterChip(selected = genre == value, onClick = { genre = value }, label = { Text(value, fontSize = 12.sp) }, shape = CircleShape, border = BorderStroke(1.dp, if (genre == value) Gold.copy(alpha = .35f) else Line)) }
        }
        HeroFeature { onOpen(titles[0]) }
        Text("ALL WORLDS", color = Paper, fontSize = 11.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
        titles.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                rowItems.forEach { item ->
                    Column(Modifier.weight(1f).clickable { onOpen(item) }, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        CoverArt(item, Modifier.fillMaxWidth().height(190.dp))
                        Text(item.name, color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.genre + "  ·  2026", color = Muted, fontSize = 10.sp)
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryScreen(filter: String, onFilter: (String) -> Unit, onOpen: (TitleItem) -> Unit) {
    val filters = listOf("Watching", "Watchlist", "Completed")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("YOUR COLLECTION", color = Gold, fontSize = 10.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
            Text("Keep your place.", color = Paper, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("Everything you saved, in one quiet corner.", color = Muted, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().clip(CircleShape).background(Surface1).padding(4.dp)) {
            filters.forEach { choice ->
                val active = filter == choice
                Box(Modifier.weight(1f).clip(CircleShape).background(if (active) Surface2 else Color.Transparent).clickable { onFilter(choice) }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                    Text(choice, color = if (active) Paper else Muted, fontSize = 11.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        val shown = when (filter) { "Watchlist" -> titles.takeLast(3); "Completed" -> titles.drop(2).take(2); else -> titles.take(4) }
        shown.forEach { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface1).clickable { onOpen(item) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                CoverArt(item, Modifier.size(width = 78.dp, height = 94.dp), true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.name, color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(item.genre, color = Muted, fontSize = 11.sp)
                    Text(item.detail, color = Muted, fontSize = 10.sp)
                    if (filter == "Watching") LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = Gold, trackColor = Surface2)
                    else Text(if (filter == "Watchlist") "SAVED FOR LATER" else "COMPLETED", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
            }
        }
    }
}

@Composable
private fun DetailScreen(item: TitleItem, saved: Boolean, onSave: () -> Unit, onBack: () -> Unit, onPlay: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            CoverArt(item, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Ink))))
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(Color(0x88000000))) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
            IconButton(onClick = onSave, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(Color(0x88000000))) { Icon(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Save", tint = if (saved) Gold else Color.White) }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("YUMEIRO ORIGINAL", color = Gold, fontSize = 9.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Text(item.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text("2026  ·  " + item.genre + "  ·  1 season  ·  16+", color = Color(0xFFD0D0D0), fontSize = 11.sp)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("A signal from the edge of the map changes everything. One quiet choice becomes a journey neither of them expected.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), shape = CircleShape) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Watch now", fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(onClick = onSave, modifier = Modifier.height(48.dp), shape = CircleShape) {
                    Icon(if (saved) Icons.Rounded.Check else Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text(if (saved) "Saved" else "My list")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EPISODES", color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                Text("Season 1", color = Muted, fontSize = 11.sp)
            }
            repeat(5) { index ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(width = 104.dp, height = 66.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(item.colors)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("0" + (index + 1) + "  ·  " + listOf("The Arrival", "Low Tide", "A Small Promise", "Signal Noise", "The Long Way")[index], color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("24 min  ·  A new beginning.", color = Muted, fontSize = 10.sp)
                    }
                    Icon(Icons.Rounded.MoreHoriz, "Episode options", tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(item: TitleItem, onBack: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(false) }
    var showNext by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Paper) }
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Season 1 · Episode 06", color = Muted, fontSize = 10.sp)
            }
            IconButton(onClick = { showNext = !showNext }) { Icon(Icons.Rounded.LibraryAddCheck, "Episodes", tint = Paper) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp).background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(74.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = Gold, modifier = Modifier.size(34.dp)) }
                Text("UI PREVIEW", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Video surface placeholder", color = Muted, fontSize = 12.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("12:48", color = Color.White, fontSize = 10.sp)
                LinearProgressIndicator(progress = { .43f }, modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape), color = Gold, trackColor = Color(0xFF515151))
                Text("24:06", color = Color.White, fontSize = 10.sp)
            }
        }
        Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = {}) { Icon(Icons.Rounded.Info, "Info", tint = Muted) }
                IconButton(onClick = { playing = !playing }, modifier = Modifier.size(58.dp).clip(CircleShape).background(Gold)) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Ink, modifier = Modifier.size(30.dp)) }
                IconButton(onClick = { showNext = !showNext }) { Icon(Icons.Rounded.SkipNext, "Next episode", tint = Paper) }
                IconButton(onClick = { showNext = !showNext }) { Icon(Icons.Rounded.Tune, "Settings", tint = Muted) }
            }
            if (showNext) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("UP NEXT", color = Gold, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Text("Episode 07 · The Shape of Distance", color = Paper, fontSize = 13.sp)
                    Text("Subtitles · English    Audio · Japanese    Quality · Auto", color = Muted, fontSize = 10.sp)
                }
            } else {
                Text("NOW PLAYING", color = Gold, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Text("A quiet transmission arrives from somewhere beyond the coastline.", color = Paper, fontSize = 13.sp)
                Text("Visual prototype only. No stream is loaded.", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SearchScreen(query: String, onQuery: (String) -> Unit, onBack: () -> Unit, onOpen: (TitleItem) -> Unit) {
    val results = titles.filter { it.name.contains(query, true) || it.genre.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Paper) }
            Text("Search", color = Paper, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Titles, genres, moods…", color = Muted) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Gold) }, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Line, focusedTextColor = Paper, unfocusedTextColor = Paper, cursorColor = Gold))
        Text(if (query.isBlank()) "A FEW PLACES TO START" else results.size.toString() + " RESULTS", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        if (results.isEmpty()) Text("Nothing here yet. Try another title or genre.", color = Muted, fontSize = 13.sp)
        results.forEach { item ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(item) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                CoverArt(item, Modifier.size(width = 72.dp, height = 82.dp), true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, color = Paper, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.genre + "  ·  2026", color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
            }
        }
    }
}

@Composable
private fun FloatingDock(selected: Destination, onSelect: (Destination) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 17.dp, end = 17.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Row(Modifier.weight(1f, fill = false).widthIn(max = 370.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xF51B1D20)).border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(28.dp)).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Destination.entries.forEach { destination ->
                val active = selected == destination
                val weight by animateFloatAsState(if (active) 1.55f else 1f, animationSpec = spring(dampingRatio = .82f, stiffness = 560f), label = "dock-tab")
                Row(Modifier.weight(weight).height(48.dp).clip(CircleShape).background(if (active) Color(0xFF39352B) else Color.Transparent).clickable { onSelect(destination) }.padding(horizontal = if (active) 12.dp else 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (destination) { Destination.HOME -> Icons.Rounded.Home; Destination.EXPLORE -> Icons.Rounded.Explore; Destination.LIBRARY -> Icons.Rounded.BookmarkBorder }, destination.label, tint = if (active) Gold else Muted, modifier = Modifier.size(21.dp))
                    AnimatedVisibility(visible = active, enter = fadeIn(tween(120)) + expandHorizontally(), exit = fadeOut(tween(90)) + shrinkHorizontally()) {
                        Text(destination.label, Modifier.padding(start = 7.dp), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        IconButton(onClick = onSearch, modifier = Modifier.size(58.dp).clip(CircleShape).background(Gold).border(1.dp, Color.White.copy(alpha = .18f), CircleShape)) {
            Icon(Icons.Rounded.Search, "Search", tint = Ink, modifier = Modifier.size(24.dp))
        }
    }
}
