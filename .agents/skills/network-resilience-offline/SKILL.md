---
name: network-resilience-offline
description: Offline-first synchronization, resilient HTTP/WebSocket networking, WorkManager background task orchestration, network status observers, and exponential backoff retry policies.
---

# Network Resilience & Offline-First Sync Architecture

This skill equips the agent to design and implement robust offline-first synchronization, network monitoring, and guaranteed background task execution.

---

## 1. Single Source of Truth (SSOT) Architecture

All UI components observe the local database (Room / DataStore) exclusively. Remote network calls write directly to the local database, which automatically emits updated state to the UI:

```
┌──────────────┐          observe Flow          ┌──────────────┐
│  Room DB /   │ ─────────────────────────────> │  UI Screen   │
│ Local Cache  │                                │  (Compose)   │
└──────▲───────┘                                └──────────────┘
       │                                               │
       │ upsert                                        │ trigger action
       │                                               ▼
┌──────┴───────┐        fetch/mutate data       ┌──────────────┐
│  Repository  │ <───────────────────────────── │  ViewModel   │
└──────▲───────┘                                └──────────────┘
       │
       │ API Request
       ▼
┌──────────────┐
│  Remote API  │
└──────────────┘
```

---

## 2. Real-Time Network State Monitoring

Monitor network connectivity lifecycle without polling using `ConnectivityManager.NetworkCallback`:

```kotlin
class NetworkMonitor @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state emission
        val isInitiallyConnected = connectivityManager.activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
        trySend(isInitiallyConnected)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.flowOn(ioDispatcher).distinctUntilChanged()
}
```

---

## 3. Guaranteed Background Sync with WorkManager

Use `WorkManager` for guaranteed asynchronous sync tasks that persist through app kills and device reboots:

```kotlin
@HiltWorker
class FileSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileRepository: FileRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            fileRepository.syncWithRemote()
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<FileSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            workManager.enqueueUniqueWork(
                "FileSyncWork",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
```

---

## 4. Exponential Backoff with Jitter

When retrying failed transient network calls:

```kotlin
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: IOException) {
            // Add full jitter (0 to currentDelay) to prevent thundering herd problem
            val jitter = Random.nextLong(0, currentDelay)
            delay(currentDelay + jitter)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    return block() // Last attempt throws if failed
}
```

