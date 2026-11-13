---
name: android-super-brain
description: 8-in-1 Android Super-Brain architecture skill. Specializes in multi-module scaling, MVI (Model-View-Intent) unidirectional state management, resilient WebSockets/streaming, unified multi-module error handling (Result<T, RootError>), and crash-proof offline sync.
---

# 8-in-1 Android Super-Brain Architecture

This skill equips the agent with advanced enterprise multi-module scaling, MVI state loops, resilient real-time networking, and typed error handling inspired by top industry standards.

---

## 1. Multi-Module Project Structure

```
├── app/                        # Application entry, navigation root, dependency graph assembly
├── core/
│   ├── common/                 # Base extensions, dispatchers, time utilities
│   ├── designsystem/           # Tokens, themes, glass components, icons, typography
│   ├── model/                  # Shared domain data classes & entity contracts
│   ├── database/               # Room entities, DAOs, migrations, DB builder
│   ├── network/                # Ktor / OkHttp clients, WebSocket handlers, DTOs
│   └── domain/                 # Base UseCase abstractions, RootError contracts
└── feature/
    ├── home/                   # Feature presentation & feature-specific domain
    ├── viewer/                 # Universal file viewer presentation
    ├── search/                 # Search presentation & index logic
    └── settings/               # Preferences & theme selection
```

### Module Boundary Rules
- Feature modules NEVER depend on each other directly (`:feature:home` ➔ `:feature:search` is forbidden).
- Inter-feature communication occurs via deep links, Jetpack Navigation 3 routes, or domain events emitted through `:core:domain`.
- `:core:designsystem` only depends on `:core:common` and Material 3 / Compose libraries.

---

## 2. MVI (Model-View-Intent) State Loop

Replace scattered ViewModel methods with strict Unidirectional MVI:

```
           ┌──────────────┐
           │ User Gesture │
           └──────┬───────┘
                  │ emits
                  ▼
         ┌────────────────┐
         │ Action / Intent│
         └────────┬───────┘
                  │ ViewModel processes
                  ▼
         ┌────────────────┐
         │ State & Effect │
         └────────┬───────┘
                  │ renders & executes
                  ▼
         ┌────────────────┐
         │ Jetpack Compose│
         └────────────────┘
```

### Triad Contract Pattern: State, Action, Event

```kotlin
// 1. Immutable State
data class ExplorerState(
    val files: List<FileUiItem> = emptyList(),
    val currentPath: String = "/",
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedFile: FileUiItem? = null
)

// 2. Sealed User Actions (Intentions)
sealed interface ExplorerAction {
    data class OnFileClick(val file: FileUiItem) : ExplorerAction
    data class OnQueryChanged(val query: String) : ExplorerAction
    data class OnDeleteFile(val id: String) : ExplorerAction
    data object OnToggleSearch : ExplorerAction
    data object OnRefresh : ExplorerAction
}

// 3. One-Time Side Effects (Single-consumption Events)
sealed interface ExplorerEvent {
    data class ShowToast(val message: String) : ExplorerEvent
    data class NavigateToViewer(val uri: String, val mimeType: String) : ExplorerEvent
}

// 4. MVI ViewModel
class ExplorerViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExplorerState())
    val state = _state.asStateFlow()

    // Single-event channel to prevent event replay on recomposition or configuration change
    private val _eventChannel = Channel<ExplorerEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun onAction(action: ExplorerAction) {
        when (action) {
            is ExplorerAction.OnFileClick -> handleFileClick(action.file)
            is ExplorerAction.OnQueryChanged -> updateQuery(action.query)
            is ExplorerAction.OnDeleteFile -> deleteFile(action.id)
            ExplorerAction.OnToggleSearch -> _state.update { it.copy(isSearching = !it.isSearching) }
            ExplorerAction.OnRefresh -> refreshData()
        }
    }

    private fun handleFileClick(file: FileUiItem) {
        viewModelScope.launch {
            _eventChannel.send(ExplorerEvent.NavigateToViewer(file.uri, file.mimeType))
        }
    }
}
```

### Safe Event Consumption in Compose
```kotlin
@Composable
fun ExplorerScreen(
    state: ExplorerState,
    events: Flow<ExplorerEvent>,
    onAction: (ExplorerAction) -> Unit,
    onNavigateToViewer: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    ObserveAsEvents(events) { event ->
        when (event) {
            is ExplorerEvent.NavigateToViewer -> onNavigateToViewer(event.uri, event.mimeType)
            is ExplorerEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
        }
    }
    // Render UI...
}

@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(onEvent)
        }
    }
}
```

---

## 3. Resilient Multi-Module Error Handling

Never throw raw exceptions across architectural boundaries. Use typed, sealed errors:

```kotlin
// Root Error Marker Interface
sealed interface RootError

// Categorized Data Errors
sealed interface DataError : RootError {
    enum class Network : DataError {
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        NO_INTERNET,
        SERIALIZATION,
        UNKNOWN
    }

    enum class Local : DataError {
        DISK_FULL,
        FILE_NOT_FOUND,
        PERMISSION_DENIED,
        CORRUPTED_FILE,
        UNKNOWN
    }
}

// Result Type Monad
sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : RootError, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(error)
    }
}

typealias EmptyResult<E> = Result<Unit, E>
```

---

## 4. Resilient WebSockets & Real-Time Sync Engine

When implementing WebSocket communication (chat, live synchronization, active telemetry):

1. **State Machine**:
   - `Disconnected` ➔ `Connecting` ➔ `Connected` ➔ `Reconnecting(attempt, nextDelayMs)`
2. **Heartbeat & Keep-Alive**:
   - Send ping frames every 30 seconds; expect pong within 10 seconds or trigger reconnection.
3. **Exponential Backoff**:
   - Initial retry: 1s, 2s, 4s, 8s, up to max 30s jittered delay.
4. **Lifecycle-Aware Connectivity**:
   - Pause connections on `AppLifecycle.ON_STOP` when in background (unless running foreground service).
   - Instantly reconnect upon `AppLifecycle.ON_START` and network availability.

