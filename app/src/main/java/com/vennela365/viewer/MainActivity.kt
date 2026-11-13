package com.vennela365.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTheme
import com.vennela365.viewer.ui.screens.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Let the glass dock draw into the gesture bar; no translucent system scrim.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }

        handleIntent(intent)

        setContent {
            val activeTheme by viewModel.activeTheme.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val accent by viewModel.accent.collectAsState()
            val appFontStyle by viewModel.appFontStyle.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            // System Back navigation
            BackHandler(enabled = currentScreen !is Screen.Home && currentScreen !is Screen.Splash) {
                if (!viewModel.navigateBack()) {
                    finish()
                }
            }

            VennelaTheme(
                activeTheme = activeTheme,
                themeMode = themeMode,
                accent = accent,
                fontStyle = appFontStyle
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenNavigator(
                        currentScreen = currentScreen,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri -> openSingle(uri) }
            }

            Intent.ACTION_SEND -> {
                val uri = intent.parcelableStreamExtra() ?: intent.data
                uri?.let { openSingle(it) }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.streamExtras()
                if (uris.isNotEmpty()) {
                    // Open the first file and hand the rest to the view model so
                    // a multi-file share is not silently reduced to one item.
                    openSingle(uris.first())
                    viewModel.openUris(uris.drop(1).map { it to getFileNameFromUri(it) })
                }
            }
        }
    }

    private fun openSingle(uri: Uri) {
        viewModel.openUri(uri, getFileNameFromUri(uri))
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableStreamExtra(): Uri? =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun Intent.streamExtras(): List<Uri> =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return name ?: uri.lastPathSegment
    }
}

@Composable
fun ScreenNavigator(
    currentScreen: Screen,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Read system animation scale in composition: `transitionSpec` is not composable.
    // Honours "Remove animations" so reduced-motion users get an instant cross-fade.
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        val scale = runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        scale > 0.01f
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            // Direction-aware "aurora lift": deeper screens push in from the
            // right with a scale-up, back-navigation pulls them in from the
            // left, and both cross-fade. Collapses to an instant cross-fade
            // when the user has animations switched off.
            val forward = screenRank(targetState) >= screenRank(initialState)
            val enterDuration = if (animationsEnabled) LiquidMotion.durationSlow else LiquidMotion.durationInstant
            val exitDuration = if (animationsEnabled) LiquidMotion.durationFast else LiquidMotion.durationInstant

            val enterOffset: (Int) -> Int = { full -> if (forward) full / 3 else -full / 4 }
            val exitOffset: (Int) -> Int = { full -> if (forward) -full / 7 else full / 7 }

            val enter = if (animationsEnabled) {
                slideInHorizontally(
                    animationSpec = tween(enterDuration, easing = LiquidMotion.glideEasing),
                    initialOffsetX = enterOffset
                ) + fadeIn(tween(enterDuration, easing = LiquidMotion.glideEasing)) +
                    scaleIn(
                        initialScale = if (forward) 0.90f else 0.945f,
                        animationSpec = tween(enterDuration, easing = LiquidMotion.glideEasing)
                    )
            } else {
                slideInHorizontally(animationSpec = tween(enterDuration)) { 0 } +
                    fadeIn(tween(LiquidMotion.durationInstant))
            }

            val exit = if (animationsEnabled) {
                slideOutHorizontally(
                    animationSpec = tween(exitDuration, easing = LiquidMotion.exitEasing),
                    targetOffsetX = exitOffset
                ) + fadeOut(tween(exitDuration, easing = LiquidMotion.exitEasing)) +
                    scaleOut(
                        targetScale = 0.97f,
                        animationSpec = tween(exitDuration, easing = LiquidMotion.exitEasing)
                    )
            } else {
                slideOutHorizontally(animationSpec = tween(exitDuration)) { 0 } +
                    fadeOut(tween(LiquidMotion.durationInstant))
            }

            enter togetherWith exit
        },
        label = "ScreenTransition",
        modifier = modifier
    ) { screen ->
        when (screen) {
            Screen.Splash -> SplashScreen(viewModel)
            Screen.Home -> HomeScreen(viewModel)
            Screen.FileExplorer -> FileExplorerScreen(viewModel)
            Screen.AllFiles -> DeviceStorageScreen(viewModel)
            Screen.UniversalSearch -> UniversalSearchScreen(viewModel)
            Screen.RecentFiles -> RecentFilesScreen(viewModel)
            Screen.Favorites -> FavoritesScreen(viewModel)
            Screen.Categories -> CategoriesScreen(viewModel)
            Screen.DocumentViewer -> DocumentViewerScreen(viewModel)
            Screen.PdfViewer -> PdfViewerScreen(viewModel)
            Screen.ImageViewer -> ImageViewerScreen(viewModel)
            Screen.CodeViewer -> CodeViewerScreen(viewModel)
            Screen.MarkdownViewer -> MarkdownViewerScreen(viewModel)
            Screen.MermaidViewer -> MermaidViewerScreen(viewModel)
            Screen.AudioPlayer -> AudioPlayerScreen(viewModel)
            Screen.VideoPlayer -> VideoPlayerScreen(viewModel)
            Screen.ArchiveBrowser -> ArchiveBrowserScreen(viewModel)
            Screen.FileMetadata -> FileMetadataScreen(viewModel)
            Screen.UnknownFile -> UnknownFileViewerScreen(viewModel)
            Screen.UnsupportedFile -> UnsupportedFileScreen(viewModel)
            Screen.Settings -> SettingsScreen(viewModel)
            Screen.Permissions -> PermissionsScreen(viewModel)
            Screen.ErrorScreen -> ErrorScreen(viewModel)
            Screen.ThemeSelection -> ThemeSelectionScreen(viewModel)
            Screen.ShareScreen -> ShareScreen(viewModel)
            Screen.SpreadsheetViewer -> SpreadsheetViewerScreen(viewModel)
            Screen.PresentationViewer -> PresentationViewerScreen(viewModel)
            Screen.UniversalLensViewer -> VennelaUniversalViewerScreen(viewModel)
            // ---- Extended inventory renders through functional supplemental screens ----
            else -> SupplementalScreen(screen, viewModel)
        }
    }
}

/**
 * Rough depth order for navigation transitions. Library screens sit above
 * viewers so "deeper" navigation feels like moving into a document.
 */
private fun screenRank(screen: Screen): Int = when (screen) {
    Screen.Splash -> 0
    Screen.Home -> 1
    Screen.FileExplorer, Screen.AllFiles, Screen.RecentFiles, Screen.Favorites,
    Screen.Categories, Screen.UniversalSearch -> 2
    Screen.Settings, Screen.Permissions, Screen.ThemeSelection, Screen.ErrorScreen,
    Screen.ShareScreen -> 3
    else -> 4
}
