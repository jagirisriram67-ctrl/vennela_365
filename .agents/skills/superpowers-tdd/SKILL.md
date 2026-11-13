---
name: superpowers-tdd
description: Strict Test-Driven Development (TDD) Red-Green-Refactor execution engine. Enforces interface-first testability, writing failing tests first, minimal passing code, and intentional boundary refactoring.
---

# Superpowers Test-Driven Development (TDD) Engine

This skill locks the agent into a disciplined Red-Green-Refactor cycle for every functional change.

---

## 1. The Strict 3-Phase TDD Loop

```
  ┌────────────────────────────────────────────────────────┐
  │ 1. RED                                                 │
  │ Write a precise, failing test against an interface or  │
  │ expected behavior before writing production code.       │
  └───────────────────────────┬────────────────────────────┘
                              │ Run test: MUST FAIL
                              ▼
  ┌────────────────────────────────────────────────────────┐
  │ 2. GREEN                                               │
  │ Implement the simplest, cleanest production code       │
  │ required to make the test pass. No premature features. │
  └───────────────────────────┬────────────────────────────┘
                              │ Run test: MUST PASS
                              ▼
  ┌────────────────────────────────────────────────────────┐
  │ 3. REFACTOR                                            │
  │ Clean up structure, remove code smells, optimize       │
  │ performance while keeping all tests 100% green.        │
  └────────────────────────────────────────────────────────┘
```

---

## 2. Testing Kotlin Coroutines & Flows with Turbine

Never use `Thread.sleep()` or arbitrary delays. Use `StandardTestDispatcher`, `runTest`, and `app.cash.turbine.test`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeFileRepository()
    private lateinit var viewModel: FileExplorerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FileExplorerViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadFiles_emitsLoadingThenSuccessState() = runTest {
        viewModel.state.test {
            // Initial state
            val initial = awaitItem()
            assertThat(initial.isLoading).isFalse()

            // Trigger action
            viewModel.onAction(ExplorerAction.OnRefresh)

            // Expect loading state
            val loading = awaitItem()
            assertThat(loading.isLoading).isTrue()

            // Advance virtual time
            testDispatcher.scheduler.advanceUntilIdle()

            // Expect success state
            val success = awaitItem()
            assertThat(success.isLoading).isFalse()
            assertThat(success.files).isNotEmpty()
        }
    }
}
```

---

## 3. Fake vs Mock Strategy
- **Prefer In-Memory Fakes over Mocks**: Fakes maintain internal state (e.g. a `MutableList` or `MutableStateFlow` backing a `FakeFileRepository`), accurately simulating real-world database or network behavior without brittle stubbing (`every { ... } returns ...`).
- **Use Mocks (Mockk) for Verification of Side-Effects**: Use Mockk when verifying external interactions that produce no observable state return (e.g., analytics logger, hardware vibrator trigger).

---

## 4. Compose UI Component Testing

Use `createComposeRule()` or `createAndroidComposeRule<MainActivity>()`:

```kotlin
@RunWith(AndroidJUnit4::class)
class FileCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fileCard_displaysNameAndTriggersClick() {
        var clicked = false
        val testItem = FileUiItem(name = "Document.pdf", sizeFormatted = "1.2 MB")

        composeTestRule.setContent {
            FileCard(
                item = testItem,
                onClick = { clicked = true }
            )
        }

        // Verify content rendered
        composeTestRule.onNodeWithText("Document.pdf").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.2 MB").assertIsDisplayed()

        // Verify click action
        composeTestRule.onNodeWithText("Document.pdf").performClick()
        assertThat(clicked).isTrue()
    }
}
```

