# Kotlin Android Attendance Clicker — Detailed Design Document

**Document version:** 1.0  
**Target platform:** Android  
**Primary language:** Kotlin  
**Recommended UI toolkit:** Jetpack Compose  
**Primary function:** Replace a handheld mechanical attendance clicker with a durable, one-tap Android app.

---

## 1. Product Summary

The app is a minimal Android attendance counter designed to behave like a physical metal tally clicker. The user should be able to open the app, see a large number, and tap one very large button to increment that number. The current count must survive app restarts, device reboots, and normal Android lifecycle events.

The interface is intentionally sparse:

1. Display the current count.
2. Provide one very large increment button.
3. Provide one small decrement button.
4. Provide one small edit button that allows direct numeric editing.
5. Provide haptic feedback on increment.
6. Avoid nonessential controls, navigation, menus, analytics, accounts, or network behavior.

This app is meant to be reliable in real-world use cases such as event entry, class attendance, crowd counting, or manual check-in counting.

---

## 2. Goals

### 2.1 Functional Goals

- Show the current attendance count prominently.
- Increment the count by exactly 1 when the user taps the large button.
- Persist the count durably on-device.
- Decrement the count by exactly 1 when the user taps the smaller decrement button.
- Allow direct editing of the count through a modal dialog.
- Trigger a short haptic response when incrementing.
- Keep decrement and edit actions visually secondary.
- Work offline.
- Require no account.
- Require no network connection.
- Require no cloud sync.
- Be usable one-handed on a phone.
- Be usable on tablets.
- Keep the app state stable during screen rotations and lifecycle transitions.

### 2.2 Non-Goals

The first version should not include:

- Multiple counters.
- Named sessions.
- Attendance rosters.
- CSV export.
- Cloud backup.
- Bluetooth clicker support.
- Sound effects.
- WebView UI.
- Authentication.
- Ads.
- Analytics.
- Remote configuration.
- Complex settings.
- Custom vibration pattern tuning.

These could be considered later, but they are intentionally out of scope for the first native Android version.

---

## 3. User Experience Requirements

### 3.1 Primary Screen

The entire app should fit on one screen.

Recommended layout from top to bottom:

1. Large count display.
2. Huge increment button.
3. Bottom row containing:
   - Small decrement button.
   - Small edit button.

The increment button should dominate the screen because it is the main action. The app should feel like a digital version of a physical clicker, not like a form-based productivity app.

### 3.2 Visual Hierarchy

The visual priority should be:

1. Current count.
2. Increment button.
3. Decrement button.
4. Edit button.

The decrement and edit buttons should be reachable but intentionally less prominent. This reduces accidental edits or decrements during rapid counting.

### 3.3 Suggested Text

- Increment button: `+1`
- Decrement button: `−1`
- Edit button: `Edit`
- Edit dialog title: `Edit count`
- Edit dialog primary action: `Save`
- Edit dialog secondary action: `Cancel`

### 3.4 Haptic Feedback

When the user taps the large increment button, the app should produce a short haptic feedback event.

The haptic feedback should:

- Occur only on increment.
- Not block the count update.
- Respect system-level haptic settings where possible.
- Fail silently if haptics are unavailable.
- Avoid requiring the `VIBRATE` permission if implemented with `View.performHapticFeedback` or Compose's haptic feedback abstraction.

### 3.5 Editing

When the user taps `Edit`:

- Show a modal dialog.
- Pre-fill the input with the current count.
- Select or focus the number field.
- Use a numeric keyboard.
- Allow saving any valid integer.
- Default invalid or empty input to either the previous value or `0`, depending on chosen product behavior.

Recommended behavior: invalid or empty input should not save and should show an inline error.

For the simplest implementation, empty input can be treated as `0`, but this is slightly more dangerous because a mistaken blank save can erase the count. A better user-safe version validates input before saving.

---

## 4. Platform and Technology Choices

### 4.1 Language: Kotlin

Kotlin should be used for all application code. It is the default modern language for Android development and integrates cleanly with Jetpack libraries, coroutines, Compose, and DataStore.

### 4.2 UI Toolkit: Jetpack Compose

Use Jetpack Compose for the UI.

Reasons:

- The UI is small and declarative.
- State-driven rendering matches the app's behavior.
- Compose simplifies modal dialogs, large touch targets, and adaptive layouts.
- The UI can be implemented in a single `MainActivity.kt` plus a small ViewModel/repository structure.

### 4.3 Persistence: Jetpack DataStore Preferences

Use Preferences DataStore for durable local persistence.

The app only needs to store one piece of data:

```text
count: Int
```

Preferences DataStore is appropriate because:

- The data model is tiny.
- No relational database is needed.
- No schema migration is required for version 1.
- It uses Kotlin coroutines and Flow.
- It is the recommended modern replacement for many simple SharedPreferences use cases.

Room would be unnecessary for version 1.

### 4.4 Architecture Pattern

Use a lightweight MVVM-style architecture:

```text
UI Layer
  MainActivity
  AttendanceClickerScreen
  EditCountDialog

State Holder
  ClickerViewModel

Data Layer
  CounterRepository
  Preferences DataStore
```

This keeps persistence out of composables and makes the app easier to test.

---

## 5. Application Architecture

### 5.1 Module Structure

For a small app, a single Android app module is sufficient.

Recommended package structure:

```text
app/
  src/main/java/com/example/attendanceclicker/
    MainActivity.kt
    ClickerViewModel.kt
    CounterRepository.kt
    ClickerUiState.kt
    AttendanceClickerScreen.kt
```

Alternative for a very small app:

```text
app/
  src/main/java/com/example/attendanceclicker/
    MainActivity.kt
```

However, keeping repository and ViewModel separate is preferable even for a small app because it keeps persistence, state, and UI concerns cleanly separated.

### 5.2 Data Flow

```text
User taps +1
  -> UI calls viewModel.increment()
  -> ViewModel updates repository
  -> Repository writes new count to DataStore
  -> DataStore emits new value
  -> ViewModel exposes updated UiState
  -> Compose recomposes count display
```

### 5.3 State Ownership

The durable count should be owned by the data layer.

The ViewModel should expose:

```kotlin
data class ClickerUiState(
    val count: Int = 0,
    val isLoading: Boolean = true,
    val editDialogVisible: Boolean = false,
    val editText: String = ""
)
```

For a very small implementation, dialog state can live in Compose via `rememberSaveable`, while the durable count remains in the ViewModel.

Recommended split:

- Durable count: ViewModel + repository + DataStore.
- Temporary edit dialog text: Compose `rememberSaveable`.
- Dialog visible state: Compose `rememberSaveable` or ViewModel.

Either approach is acceptable. For this app, keeping modal UI state in Compose is simple and clean.

---

## 6. Persistence Design

### 6.1 Stored Values

Use one Preferences DataStore file:

```text
clicker_prefs
```

Use one key:

```text
count
```

Type:

```kotlin
intPreferencesKey("count")
```

Default value:

```kotlin
0
```

### 6.2 DataStore Definition

Create the DataStore once at top level:

```kotlin
private val Context.dataStore by preferencesDataStore(name = "clicker_prefs")
```

This should be defined outside a class at file scope.

### 6.3 Repository Responsibilities

`CounterRepository` should:

- Expose `countFlow: Flow<Int>`.
- Provide `suspend fun setCount(value: Int)`.
- Provide `suspend fun increment()`.
- Provide `suspend fun decrement()`.

However, increment and decrement require care. DataStore writes are transactional inside `edit`, so the repository should update based on the current preference value within `edit`.

Example:

```kotlin
class CounterRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val Count = intPreferencesKey("count")
    }

    val countFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[Keys.Count] ?: 0
        }

    suspend fun setCount(value: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.Count] = value
        }
    }

    suspend fun increment() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] = current + 1
        }
    }

    suspend fun decrement() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] = current - 1
        }
    }
}
```

### 6.4 Durability Expectations

DataStore persists on disk. The count should survive:

- Activity recreation.
- Process death after persistence completes.
- App restart.
- Device reboot.
- Screen rotation.

The app should not rely only on `remember`, `rememberSaveable`, or in-memory ViewModel state for the count.

---

## 7. ViewModel Design

### 7.1 Responsibilities

`ClickerViewModel` should:

- Convert repository `Flow<Int>` into Compose-observable state.
- Provide simple intent methods:
  - `increment()`
  - `decrement()`
  - `setCount(value: Int)`
- Launch persistence operations in `viewModelScope`.
- Avoid direct references to Compose UI classes.

### 7.2 Example ViewModel

```kotlin
class ClickerViewModel(
    private val repository: CounterRepository
) : ViewModel() {

    val count: StateFlow<Int> =
        repository.countFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    fun increment() {
        viewModelScope.launch {
            repository.increment()
        }
    }

    fun decrement() {
        viewModelScope.launch {
            repository.decrement()
        }
    }

    fun setCount(value: Int) {
        viewModelScope.launch {
            repository.setCount(value)
        }
    }
}
```

### 7.3 ViewModel Creation

For a small app, use a custom `ViewModelProvider.Factory`.

If using Hilt later, dependency injection can be added, but Hilt is unnecessary for version 1.

Simple factory:

```kotlin
class ClickerViewModelFactory(
    private val repository: CounterRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClickerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClickerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## 8. UI Design

### 8.1 Compose Screen

Recommended composable structure:

```kotlin
@Composable
fun AttendanceClickerScreen(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

This keeps the screen reusable and testable.

### 8.2 Layout

Use a `Column` filling the screen:

```text
Column
  Text(count)
  Button(+1)
  Row
    Button(−1)
    Button(Edit)
```

The large button should use `Modifier.weight(1f)` so it takes the majority of available vertical space.

### 8.3 Suggested Compose Layout

```kotlin
@Composable
fun AttendanceClickerScreen(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editVisible by rememberSaveable { mutableStateOf(false) }
    var editText by rememberSaveable { mutableStateOf("") }

    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10))
            .padding(20.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF5F5F5),
            fontSize = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onIncrement()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 260.dp),
            shape = RoundedCornerShape(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color(0xFF0F0F10)
            )
        ) {
            Text(
                text = "+1",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onDecrement,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242427),
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Text("−1", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = {
                    editText = count.toString()
                    editVisible = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242427),
                    contentColor = Color(0xFFF5F5F5)
                )
            ) {
                Text("Edit", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (editVisible) {
        EditCountDialog(
            initialText = editText,
            onDismiss = { editVisible = false },
            onSave = { newCount ->
                onSetCount(newCount)
                editVisible = false
            }
        )
    }
}
```

### 8.4 Haptic Feedback Choice

Compose exposes `LocalHapticFeedback`, which is usually enough for a simple UI event.

Potential Compose call:

```kotlin
val haptics = LocalHapticFeedback.current

haptics.performHapticFeedback(HapticFeedbackType.LongPress)
```

Although `LongPress` is not semantically perfect for a clicker increment, it is a widely available Compose haptic type.

If the app needs more explicit Android platform haptic constants, use a `View` reference and call:

```kotlin
view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
```

However, `CONFIRM` requires API 30+. For broad compatibility, either:

- Use Compose `HapticFeedbackType.LongPress`, or
- Use API-gated constants with fallback.

Recommended version 1 approach:

```kotlin
val view = LocalView.current

fun performIncrementHaptic() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
```

This gives a better semantic haptic on Android 11+ while preserving older-device behavior.

### 8.5 Edit Dialog

The edit dialog should contain:

- Title.
- Numeric input field.
- Cancel button.
- Save button.
- Optional validation error.

Recommended validation:

- Accept only valid signed integers.
- Reject empty input.
- Reject values outside `Int.MIN_VALUE..Int.MAX_VALUE`.
- Keep the dialog open on invalid input.

For attendance use, negative values probably should not be needed. But because decrement exists, the data model may allow negative values. The product decision should be explicit.

Recommended product decision:

- Allow `0` and positive integers.
- Do not allow negative values from edit.
- Decrement from `0` can either:
  - Stop at `0`, or
  - Allow `-1`.

For a physical attendance clicker replacement, counts should not become negative. The safer behavior is:

- Increment: `count + 1`
- Decrement: `maxOf(0, count - 1)`
- Edit: allow integers `>= 0`

This design recommends clamping at `0`.

If the earlier HTML/Expo versions allowed negative counts, Android can either preserve that behavior for consistency or intentionally improve it. For attendance counting, nonnegative-only is recommended.

### 8.6 Nonnegative Count Policy

Recommended repository implementation:

```kotlin
suspend fun decrement() {
    dataStore.edit { preferences ->
        val current = preferences[Keys.Count] ?: 0
        preferences[Keys.Count] = maxOf(0, current - 1)
    }
}
```

Recommended edit validation:

```kotlin
val parsed = editText.toIntOrNull()
if (parsed == null || parsed < 0) {
    showError = true
} else {
    onSave(parsed)
}
```

---

## 9. Accessibility Design

### 9.1 Content Descriptions

Buttons should have accessibility labels:

- Increment: `Increment attendance count`
- Decrement: `Decrement attendance count`
- Edit: `Edit attendance count`

The visible count should be readable by screen readers.

### 9.2 Touch Targets

All touch targets should be at least 48 dp tall. This app exceeds that:

- Increment button: minimum 260 dp.
- Decrement button: 68 dp.
- Edit button: 68 dp.

### 9.3 Dynamic Type

The count should scale reasonably, but not overflow badly.

Use:

- `maxLines = 1`
- `softWrap = false`
- Potentially adaptive text size or `BoxWithConstraints` if very large counts are expected.

For version 1, `maxLines = 1` is enough.

### 9.4 TalkBack Behavior

For TalkBack users:

- The current count should be read as text.
- Increment button should announce its label.
- The count update should be obvious after tapping.
- Consider using a live region if TalkBack does not announce updates reliably.

Compose semantics can be added:

```kotlin
Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
}
```

### 9.5 Color Contrast

Use high contrast:

- Dark background: `#0F0F10`
- Main text: `#F5F5F5`
- Primary button background: `#F5F5F5`
- Primary button text: `#0F0F10`
- Small button background: `#242427`
- Decrement text: `#FF6B6B`

---

## 10. Lifecycle and Reliability

### 10.1 Startup

On startup:

1. Activity starts.
2. ViewModel subscribes to DataStore.
3. UI initially shows `0` or a loading placeholder.
4. DataStore emits persisted count.
5. UI updates.

Given the app is tiny, showing `0` briefly is acceptable. If this is considered risky, show no count until loaded.

### 10.2 Process Death

Because the count is persisted to DataStore on each change, process death should not lose previously written counts.

There is a small theoretical risk if the process is killed immediately after a tap before persistence completes. To minimize this:

- Write immediately on increment.
- Do not debounce count writes.
- Do not batch count changes in memory.
- Use DataStore `edit` for each increment.

### 10.3 Rapid Tapping

Rapid tapping must not lose increments.

Potential issue:

- If the ViewModel reads `count` from current state and writes `count + 1`, rapid taps may race.

Recommended solution:

- Repository `increment()` should update inside `dataStore.edit`.
- Do not calculate increment solely from a potentially stale UI state.

This transactional update is important.

### 10.4 Orientation

The app should either:

- Support rotation naturally, or
- Lock portrait orientation.

Recommended: support portrait by default and allow tablets to use their chosen orientation unless there is a strong product reason to lock.

The UI should be robust under recreation because count is persisted and edit dialog state is temporary.

---

## 11. Haptics Implementation Details

### 11.1 Recommended API

Use action-based haptic feedback rather than a raw vibrator pattern.

Recommended implementation inside Compose:

```kotlin
@Composable
fun rememberIncrementHaptic(): () -> Unit {
    val view = LocalView.current

    return remember(view) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}
```

Usage:

```kotlin
val performIncrementHaptic = rememberIncrementHaptic()

Button(
    onClick = {
        performIncrementHaptic()
        onIncrement()
    }
) {
    Text("+1")
}
```

### 11.2 Why Not Raw Vibrator?

The raw `Vibrator` / `VibratorManager` APIs are useful for custom vibration waveforms, alarms, games, or specialized effects. This app only needs a short confirmation tap. System haptic constants are better because they:

- Better match platform conventions.
- Respect device-level tuning.
- Avoid over-customizing the feel.
- Avoid unnecessary permission concerns for simple UI feedback.
- Are more likely to feel native across different Android devices.

### 11.3 Failure Behavior

If haptic feedback does not happen:

- The count should still increment.
- No error should be shown.
- The UI should not block.
- No retry is needed.

Haptics are a tactile enhancement, not core functionality.

---

## 12. Error Handling

### 12.1 Persistence Error

DataStore write failures should be rare.

Possible handling:

- Log the error.
- Keep UI responsive.
- Optionally show a Snackbar: `Could not save count`.

For a minimal clicker, avoid noisy error UI unless persistence repeatedly fails.

### 12.2 Invalid Edit Input

Invalid edit input should be handled inline in the dialog.

Examples:

- Empty input.
- Non-numeric input.
- Negative input, if nonnegative-only policy is chosen.
- Too-large integer.

Suggested error text:

```text
Enter a valid number 0 or higher.
```

### 12.3 Count Overflow

If count is stored as `Int`, maximum value is 2,147,483,647.

For attendance counting, `Int` is almost certainly sufficient. But for robustness:

- Do not increment past `Int.MAX_VALUE`.
- If reached, keep count unchanged.
- Optionally show error.

Repository:

```kotlin
preferences[Keys.Count] =
    if (current == Int.MAX_VALUE) Int.MAX_VALUE else current + 1
```

---

## 13. Testing Plan

### 13.1 Unit Tests

Test `CounterRepository` behavior with a fake DataStore or abstraction.

Cases:

- Default count is `0`.
- `setCount(5)` emits `5`.
- `increment()` from `0` emits `1`.
- `increment()` repeatedly preserves every increment.
- `decrement()` from `5` emits `4`.
- `decrement()` from `0` remains `0`, if nonnegative policy is used.
- Large count does not overflow.
- Invalid edit values are rejected at validation layer.

### 13.2 ViewModel Tests

Cases:

- ViewModel exposes initial count.
- `increment()` calls repository increment.
- `decrement()` calls repository decrement.
- `setCount()` calls repository set.
- Flow updates are reflected in UI state.

Use coroutine test utilities.

### 13.3 Compose UI Tests

Cases:

- Count text is visible.
- Tapping `+1` updates displayed count.
- Tapping `−1` updates displayed count.
- Tapping `Edit` opens dialog.
- Saving valid edit updates displayed count.
- Invalid edit displays validation error.
- Canceling edit leaves count unchanged.
- Large button has correct accessibility label.
- Small buttons have correct accessibility labels.

### 13.4 Manual Device Tests

Test on at least:

- One recent Android phone.
- One older Android phone if available.
- One tablet or emulator tablet.
- Device with haptics enabled.
- Device with touch vibration disabled.
- Dark mode.
- Large system font setting.
- App restart.
- Device rotation.
- Rapid tapping for 100+ taps.

### 13.5 Rapid Tap Test

Manual procedure:

1. Open app.
2. Reset count to `0`.
3. Tap increment rapidly 50 times.
4. Confirm count is exactly `50`.
5. Close app.
6. Reopen app.
7. Confirm count is still `50`.

---

## 14. Build Configuration

### 14.1 Minimum SDK

Recommended:

```kotlin
minSdk = 23
```

Rationale:

- Covers a broad range of Android devices.
- Compose supports modern Android development targets.
- Haptic fallback can use older constants.

### 14.2 Target SDK

Use the latest stable target SDK available to the project at implementation time.

### 14.3 Dependencies

Expected dependencies:

```kotlin
implementation(platform("androidx.compose:compose-bom:<latest-stable>"))
implementation("androidx.activity:activity-compose:<latest-stable>")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<latest-stable>")
implementation("androidx.datastore:datastore-preferences:<latest-stable>")

debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

Version numbers should be selected from the current AndroidX/Compose BOM versions when implementing.

---

## 15. Android Manifest

The app should need very few manifest entries.

No internet permission is required.

No vibration permission should be required if using view haptic feedback.

Example:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:theme="@style/Theme.AttendanceClicker"
        android:label="Attendance Clicker"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

Optional:

- Disable backup if counts are device-local and should not transfer.
- Keep backup enabled if users expect their count to survive device migration.

Recommended for this simple tool:

```xml
android:allowBackup="true"
```

---

## 16. Theme

Use a dark, minimal theme.

Material 3 can be used but heavily simplified.

Recommended colors:

```kotlin
private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF0F0F10),
    surface = Color(0xFF1B1B1F),
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF0F0F10),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5)
)
```

The app can ignore dynamic color for consistency, or support it if desired. For a tool-like clicker, fixed colors are preferable.

---

## 17. Privacy and Security

### 17.1 Data Collected

The app collects no personal data.

The only persisted data is:

```text
Current counter value
```

### 17.2 Network

The app performs no network requests.

### 17.3 Permissions

The app should require no runtime permissions.

### 17.4 Analytics

No analytics should be included in version 1.

### 17.5 Backups

If Android Auto Backup is enabled, the count may be included in device backup. This is generally acceptable because the count is not personal, but the product owner should decide.

---

## 18. Edge Cases

### 18.1 App Opens for First Time

Expected:

```text
Count = 0
```

### 18.2 User Taps Increment While Count Is Loading

Simplest approach:

- Show `0` initial state.
- DataStore usually emits quickly.

More robust approach:

- Use `isLoading`.
- Disable increment until loaded.

Recommended for this app:

- Keep UI immediately usable.
- DataStore load is fast enough for practical use.

### 18.3 User Enters Very Large Number

If input exceeds `Int.MAX_VALUE`, reject it.

Error:

```text
Enter a valid number 0 or higher.
```

### 18.4 User Enters Negative Number

Recommended: reject it.

### 18.5 User Decrements at Zero

Recommended: stay at zero.

### 18.6 Haptics Disabled by System

Expected:

- No haptic feedback occurs.
- Count still increments.

### 18.7 Device Has Weak or No Haptic Motor

Expected:

- No visible app error.
- Count still increments.

---

## 19. Implementation Sequence

### Step 1: Create Android Project

Create a new Android Studio project:

- Template: Empty Activity
- Language: Kotlin
- UI: Jetpack Compose
- Minimum SDK: 23 or higher

### Step 2: Add Dependencies

Add DataStore and lifecycle/Compose dependencies.

### Step 3: Implement Repository

Create `CounterRepository.kt`.

### Step 4: Implement ViewModel

Create `ClickerViewModel.kt`.

### Step 5: Implement UI

Create composables:

- `AttendanceClickerScreen`
- `EditCountDialog`

### Step 6: Wire MainActivity

In `MainActivity`:

- Create repository.
- Create ViewModel via factory.
- Collect `count`.
- Render Compose UI.

### Step 7: Add Haptics

Add `rememberIncrementHaptic()` helper and call it before or after `onIncrement`.

Recommended ordering:

```kotlin
performIncrementHaptic()
onIncrement()
```

The haptic is immediate and the data write happens asynchronously.

### Step 8: Test Persistence

Manually verify:

- Increment count.
- Kill app.
- Reopen app.
- Count is preserved.

### Step 9: Test Rapid Taps

Verify no missed increments.

### Step 10: Polish Accessibility

Add content descriptions and verify TalkBack.

---

## 20. Complete Example Skeleton

### 20.1 CounterRepository.kt

```kotlin
package com.example.attendanceclicker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.clickerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clicker_prefs"
)

class CounterRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val Count = intPreferencesKey("count")
    }

    val countFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[Keys.Count] ?: 0
        }

    suspend fun setCount(value: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.Count] = value.coerceAtLeast(0)
        }
    }

    suspend fun increment() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] =
                if (current == Int.MAX_VALUE) Int.MAX_VALUE else current + 1
        }
    }

    suspend fun decrement() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] = maxOf(0, current - 1)
        }
    }
}
```

### 20.2 ClickerViewModel.kt

```kotlin
package com.example.attendanceclicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClickerViewModel(
    private val repository: CounterRepository
) : ViewModel() {

    val count: StateFlow<Int> =
        repository.countFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    fun increment() {
        viewModelScope.launch {
            repository.increment()
        }
    }

    fun decrement() {
        viewModelScope.launch {
            repository.decrement()
        }
    }

    fun setCount(value: Int) {
        viewModelScope.launch {
            repository.setCount(value)
        }
    }
}

class ClickerViewModelFactory(
    private val repository: CounterRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClickerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClickerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### 20.3 MainActivity.kt

```kotlin
package com.example.attendanceclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: ClickerViewModel by viewModels {
        ClickerViewModelFactory(
            CounterRepository(applicationContext.clickerDataStore)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val count by viewModel.count.collectAsState()

            AttendanceClickerTheme {
                AttendanceClickerScreen(
                    count = count,
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onSetCount = viewModel::setCount
                )
            }
        }
    }
}
```

### 20.4 Haptics Helper

```kotlin
package com.example.attendanceclicker

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberIncrementHaptic(): () -> Unit {
    val view = LocalView.current

    return remember(view) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}
```

### 20.5 AttendanceClickerScreen.kt

```kotlin
package com.example.attendanceclicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AttendanceClickerScreen(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editVisible by rememberSaveable { mutableStateOf(false) }
    var editText by rememberSaveable { mutableStateOf("") }
    val performIncrementHaptic = rememberIncrementHaptic()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10))
            .padding(20.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Current count: $count"
                },
            color = Color(0xFFF5F5F5),
            fontSize = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Button(
            onClick = {
                performIncrementHaptic()
                onIncrement()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 260.dp)
                .semantics {
                    contentDescription = "Increment attendance count"
                },
            shape = RoundedCornerShape(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color(0xFF0F0F10)
            )
        ) {
            Text(
                text = "+1",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onDecrement,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .semantics {
                        contentDescription = "Decrement attendance count"
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242427),
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Text("−1", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = {
                    editText = count.toString()
                    editVisible = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .semantics {
                        contentDescription = "Edit attendance count"
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242427),
                    contentColor = Color(0xFFF5F5F5)
                )
            ) {
                Text("Edit", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (editVisible) {
        EditCountDialog(
            value = editText,
            onValueChange = { editText = it },
            onDismiss = { editVisible = false },
            onSave = { parsed ->
                onSetCount(parsed)
                editVisible = false
            }
        )
    }
}

@Composable
fun EditCountDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var showError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit count")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        showError = false
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = showError,
                    label = { Text("Count") }
                )

                if (showError) {
                    Text(
                        text = "Enter a valid number 0 or higher.",
                        color = Color(0xFFFF6B6B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = value.toIntOrNull()
                    if (parsed == null || parsed < 0) {
                        showError = true
                    } else {
                        onSave(parsed)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

## 21. Future Enhancements

Potential future additions:

- Reset button with confirmation.
- Optional multiple counters.
- Optional session name.
- Optional export to CSV.
- Optional visual flash on increment.
- Optional wearable companion.
- Optional hardware volume-button increment.
- Optional lock mode to hide decrement/edit while counting.
- Optional count history for undo.

These should not be included in version 1 unless specifically required.

---

## 22. Open Product Decisions

Before implementation, confirm:

1. Should decrement below zero be allowed?
   - Recommendation: no.
2. Should edit allow negative numbers?
   - Recommendation: no.
3. Should backup preserve the counter across device transfer?
   - Recommendation: yes, unless the count must be strictly device-local.
4. Should haptics trigger on decrement too?
   - Recommendation: no.
5. Should the app lock portrait orientation?
   - Recommendation: no, but design primarily for portrait.

---

## 23. References

- Jetpack Compose is Android's recommended modern toolkit for native UI:  
  https://developer.android.com/compose
- DataStore documentation, including Preferences DataStore and recommended architecture placement:  
  https://developer.android.com/topic/libraries/architecture/datastore
- Android haptic feedback guide using `View.performHapticFeedback`:  
  https://developer.android.com/develop/ui/views/haptics/haptic-feedback
- Android haptics API guidance and `HapticFeedbackConstants`:  
  https://developer.android.com/develop/ui/views/haptics/haptics-apis
- Compose state documentation:  
  https://developer.android.com/develop/ui/compose/state
