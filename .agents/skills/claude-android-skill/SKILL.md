---
name: claude-android-skill
description: Forces native Android code to follow Google's official "Now in Android" Clean Architecture standards. Enforces strict layer separation across UI, Domain, and Data layers, Hilt Dependency Injection, and Room database patterns with Flow.
---

# Now in Android (NiA) Clean Architecture Standards

This skill enforces Google's official Now in Android (NiA) Clean Architecture guidelines for modern native Android development.

## 1. Architectural Layers & Inward Dependency Rule

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│  Activities / Fragments / Jetpack Compose Screens       │
│  ViewModels (StateFlow<UiState>)                       │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
┌───────────────────────────▼────────────────────────────┐
│                    Domain Layer                        │
│  UseCases / Interactors (Pure Kotlin, Single Purpose)  │
│  Domain Models (Pure Kotlin Data Classes)              │
│  Repository Interfaces                                 │
└───────────────────────────┬────────────────────────────┘
                            │ depends on / implements
┌───────────────────────────▼────────────────────────────┐
│                     Data Layer                         │
│  Repository Implementations (Single Source of Truth)   │
│  Local Data Source (Room Database, EncryptedPrefs)     │
│  Remote Data Source (Ktor / Retrofit REST, WebSockets) │
└────────────────────────────────────────────────────────┘
```

### Strict Boundaries
1. **Presentation (UI) Layer** must NEVER import Room DAOs, database entities, or network DTOs directly. All data must come via ViewModels through Domain UseCases or Repositories.
2. **Domain Layer** must be pure Kotlin/Java with ZERO Android SDK dependencies (`android.*` imports are banned).
3. **Data Layer** serves as the Single Source of Truth (SSOT). Repositories mediate between local cache (Room) and remote API (Ktor/Retrofit).

---

## 2. UI Layer Conventions (Jetpack Compose)

### State Management & StateFlow
- Expose a single immutable `StateFlow<ScreenUiState>` from ViewModels.
- Consume state in Compose using `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`.
- State class must be `@Immutable` or `@Stable`.

```kotlin
// UI State Model
@Immutable
data class FileViewerUiState(
    val isLoading: Boolean = false,
    val fileItem: FileItem? = null,
    val error: String? = null
)

// ViewModel
@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val getFileUseCase: GetFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileViewerUiState(isLoading = true))
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    fun loadFile(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getFileUseCase(uriString)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
                .collect { file ->
                    _uiState.update { it.copy(isLoading = false, fileItem = file, error = null) }
                }
        }
    }
}
```

### Composable Screen Splitting: Stateful vs Stateless
Always create a stateful route wrapper and a stateless screen composable:

```kotlin
@Composable
fun FileViewerRoute(
    viewModel: FileViewerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileViewerScreen(
        uiState = uiState,
        onAction = viewModel::handleAction,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun FileViewerScreen(
    uiState: FileViewerUiState,
    onAction: (FileAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure stateless rendering based on uiState
}
```

---

## 3. Domain Layer Conventions

- Each UseCase has a single public method using `operator fun invoke()`:
- UseCases handle business logic, validation, and combining multiple repositories:

```kotlin
class GetFavoriteFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(): Flow<List<FileItem>> {
        return fileRepository.getFilesStream()
            .map { files -> files.filter { it.isFavorite } }
    }
}
```

---

## 4. Data Layer & Repository Pattern

### Repository Contract (Domain)
```kotlin
interface FileRepository {
    fun getFilesStream(): Flow<List<FileItem>>
    suspend fun getFileById(id: String): FileItem?
    suspend fun toggleFavorite(id: String)
    suspend fun syncWithRemote()
}
```

### Repository Implementation (Data)
Offline-first: Write to local Room DB first, read from local Room DB as Flow, and fetch remote updates asynchronously.

```kotlin
class OfflineFirstFileRepository @Inject constructor(
    private val fileDao: FileDao,
    private val networkDataSource: RemoteFileDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileRepository {

    override fun getFilesStream(): Flow<List<FileItem>> {
        return fileDao.getAllFilesAsFlow()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun syncWithRemote() = withContext(ioDispatcher) {
        val remoteFiles = networkDataSource.fetchFiles()
        fileDao.upsertFiles(remoteFiles.map { it.toEntity() })
    }
}
```

---

## 5. Hilt Dependency Injection Rules

1. Define injection modules in `:core:di` or `:data:di`.
2. Use `@Binds` over `@Provides` when binding an implementation to an interface:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: OfflineFirstFileRepository
    ): FileRepository
}
```

3. Qualify dispatchers with custom `@Qualifier` annotations:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

