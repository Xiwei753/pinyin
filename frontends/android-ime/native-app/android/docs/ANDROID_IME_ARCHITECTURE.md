# Android IME Architecture

长期路线和禁止项见 `TECHNICAL_ROADMAP.md`；本文件只解释当前 Android IME 分层细节。

This stage keeps the input core in Kotlin instead of Rust. The Android IME is still moving quickly, and the main risk is not CPU performance but unclear ownership between input state, Android side effects, and rendering. A Kotlin core lets Android tests cover the reducer directly while preserving the current T9 engine and dictionary behavior. Rust can replace the same core boundary later without rewriting Android views.

Commit `645ca3e` introduced the first platform-independent core skeleton. The second stage tightened the boundary so render code cannot refresh candidates and the candidate snapshot has one owner, `ImeStateMachine`. The third stage starts decoupling UI state from internal T9 engine models: UI-visible candidates are now `CandidateSnapshotItem`, not `t9.core.Candidate`.

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
- `CandidateSnapshotItem`
- `PreeditState`
- `KeyboardSurfaceState`
- `RailState`
- `SymbolPanelState`
- `ThemeTokens`
- `LayoutTokens`
- `ImeStateMachine`

The core may depend on pure Kotlin model interfaces such as `T9InputEngine`. It describes side effects like `CommitText`, `SendDelete`, or `FinishComposingText`, but it never executes Android `InputConnection` calls.

`ImeStateMachine` is the only owner of candidate snapshots. It refreshes candidates while reducing input actions such as `DigitPressed`, `SeparatorPressed`, `DeletePressed`, `ReadingSelected`, `CandidateLimitChanged`, and mode toggles. `ImeUiState.candidateStrip.candidates` is the render-time snapshot. Render code must only read it.

`ImeUiState`, `CandidateStripState`, and `ImeStateMachine.currentCandidates` expose only `CandidateSnapshotItem`. The state machine may keep private engine candidate selections so `CandidateSelected(index)` can commit the exact current snapshot entry without recomputing candidates. `imecore` must not expose `t9.core.Candidate` through public UI state.

`EnterShortPressed` in the core describes default composing/newline behavior only. Android-specific `EditorInfo` action selection is handled by the Android adapter through `EnterActionPolicy`; `EditorInfo` logic must not enter `imecore`.

### Android Adapter

Package: `io.github.xiwei753.pinyin.t9`

`XiweiT9ImeService` is the adapter. It initializes the state machine through `KeyboardActionHandler`, translates Android lifecycle/view events into `ImeInputAction`, executes `ImeSideEffect` against `InputConnection`, and renders `KeyboardUiState`.

`T9EngineAdapter` wraps the existing Kotlin `T9Engine` behind the core `T9InputEngine` interface. It maps internal `t9.core.Candidate` values to `CandidateSnapshotItem` for UI state while retaining an opaque commit handle for the original candidate. This keeps `T9Engine.commitCandidate(candidate)` and user dictionary recording behavior unchanged without re-querying candidates.

`KeyboardActionHandler` is now a compatibility adapter around `ImeStateMachine`. Old methods such as `onDigitPressed`, `onSpace`, `onCandidateClick`, `switchKeyboardMode`, and `refreshCandidates` are deprecated wrappers only. New business logic must go into `ImeStateMachine` as `ImeInputAction` reduction, not into the wrapper.

`XiweiT9ImeService` must not maintain a second symbol category source. Symbol category selection comes from `handler.uiState().currentSymbolCategory` / `ImeUiState.symbolPanel`; `buildKeyboardUiState()` only maps core state to Android `KeyboardUiState`.

`EnterActionPolicy` is an Android adapter concern. `KeyboardActionHandler.onEnterShortPress()` is allowed to remain as the special path that first commits composing through core behavior and then applies Android `EditorInfo` action policy, but that policy must not move into `imecore`.

### Android Render Layer

`CandidateViewController` renders the candidate strip and floating preedit from state. Candidate clicks emit `ImeInputAction.CandidateSelected(index)` and use the current candidate snapshot.

`CandidateViewController` is pure render. It must not call `T9Engine`, `KeyboardActionHandler.refreshCandidates`, or recompute candidate lists. Preedit visibility comes from `PreeditState`; candidate items come from `CandidateStripState`, and only `CandidateSnapshotItem.text` is displayed.

`XiweiKeyboardView` draws a `KeyboardLayoutModel`, hit-tests keys, and emits `ImeInputAction`.

`KeyboardLayoutBuilder` maps `KeyboardUiState` and `KeyboardSurfaceState` to `KeyboardLayoutModel`.

`KeyboardLayoutBuilder` is state-to-layout only. It uses typed `RailKind` values such as `Readings`, `Punctuation`, `SymbolCategories`, and `NumberAux`; it must not infer behavior from string names.

`KeyboardRenderer` draws `KeyboardLayoutModel` with `ThemePalette` and tokenized layout values.

## Import Rules

- `io.github.xiwei753.pinyin.imecore` must not import `android.*`.
- `imecore` public UI state must not expose `t9.core.Candidate`.
- Android lifecycle, `InputConnection`, `View`, `Canvas`, `Resources`, and XML access stay in `io.github.xiwei753.pinyin.t9`.
- Dictionary source files and generated assets are not part of this architecture boundary.

## Linux Reuse

A future Linux frontend should send the same `ImeInputAction` values to the Kotlin core or an equivalent implementation, execute returned `ImeSideEffect` values through the Linux IM framework, and render from `ImeUiState`. Linux should not reuse Android `InputMethodService`, `View`, or XML classes.

## Future Rust Boundary

If Rust is introduced later, it should replace the platform-independent core boundary: reducer/state machine, composition state, and possibly the T9 engine/dictionary lookup. Android and Linux should keep the same action/state/effect contract so UI and platform adapters are not thrown away.
