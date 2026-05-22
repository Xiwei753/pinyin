# Android IME Architecture

This stage keeps the input core in Kotlin instead of Rust. The Android IME is still moving quickly, and the main risk is not CPU performance but unclear ownership between input state, Android side effects, and rendering. A Kotlin core lets Android tests cover the reducer directly while preserving the current T9 engine and dictionary behavior. Rust can replace the same core boundary later without rewriting Android views.

## Layers

Input flow:

`InputAction -> ImeStateMachine -> ImeUiState + ImeSideEffect -> Android render + side effect execution`

### Platform-Independent Core

Package: `io.github.xiwei753.pinyin.imecore`

This package must not import `android.*`.

It owns:

- `InputMode`
- `ImeInputAction`
- `ImeSideEffect`
- `ImeUiState`
- `CompositionState`
- `CandidateStripState`
- `PreeditState`
- `KeyboardSurfaceState`
- `RailState`
- `SymbolPanelState`
- `ThemeTokens`
- `LayoutTokens`
- `ImeStateMachine`

The core may depend on pure Kotlin model interfaces such as `T9InputEngine`. It describes side effects like `CommitText`, `SendDelete`, or `FinishComposingText`, but it never executes Android `InputConnection` calls.

### Android Adapter

Package: `io.github.xiwei753.pinyin.t9`

`XiweiT9ImeService` is the adapter. It initializes the state machine through `KeyboardActionHandler`, translates Android lifecycle/view events into `ImeInputAction`, executes `ImeSideEffect` against `InputConnection`, and renders `KeyboardUiState`.

`T9EngineAdapter` wraps the existing Kotlin `T9Engine` behind the core `T9InputEngine` interface. This keeps dictionary and candidate behavior unchanged.

### Android Render Layer

`CandidateViewController` renders the candidate strip and floating preedit from state. Candidate clicks emit `ImeInputAction.CandidateSelected(index)` and use the current candidate snapshot.

`XiweiKeyboardView` draws a `KeyboardLayoutModel`, hit-tests keys, and emits `ImeInputAction`.

`KeyboardLayoutBuilder` maps `KeyboardUiState` and `KeyboardSurfaceState` to `KeyboardLayoutModel`.

`KeyboardRenderer` draws `KeyboardLayoutModel` with `ThemePalette` and tokenized layout values.

## Import Rules

- `io.github.xiwei753.pinyin.imecore` must not import `android.*`.
- Android lifecycle, `InputConnection`, `View`, `Canvas`, `Resources`, and XML access stay in `io.github.xiwei753.pinyin.t9`.
- Dictionary source files and generated assets are not part of this architecture boundary.

## Linux Reuse

A future Linux frontend should send the same `ImeInputAction` values to the Kotlin core or an equivalent implementation, execute returned `ImeSideEffect` values through the Linux IM framework, and render from `ImeUiState`. Linux should not reuse Android `InputMethodService`, `View`, or XML classes.

## Future Rust Boundary

If Rust is introduced later, it should replace the platform-independent core boundary: reducer/state machine, composition state, and possibly the T9 engine/dictionary lookup. Android and Linux should keep the same action/state/effect contract so UI and platform adapters are not thrown away.
