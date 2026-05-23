package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import io.github.xiwei753.pinyin.imecore.ImeInputAction

class XiweiKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var layoutModel: KeyboardLayoutModel? = null
    var palette: ThemePalette? = null
    var density: Float = context.resources.displayMetrics.density
    var keyboardMode: KeyboardMode = KeyboardMode.ChineseT9
    var activeSymCategory: String? = null
    var lastTextMode: KeyboardMode = KeyboardMode.ChineseT9

    var pressedKeyId: String? = null
    var longPressedKeyId: String? = null

    var onKeyAction: ((String) -> Unit)? = null
    var onInputAction: ((ImeInputAction) -> Unit)? = null
    var onEnterShortPress: (() -> Unit)? = null
    var onEnterLongPress: (() -> Unit)? = null
    var onDeleteRepeat: (() -> Unit)? = null
    var onHapticTap: (() -> Unit)? = null
    var onHapticSpecial: (() -> Unit)? = null
    var onHapticLongPress: (() -> Unit)? = null
    var requestRebuildLayout: (() -> Unit)? = null

    private val renderer = KeyboardRenderer()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var deleteRepeatActive = false
    private val deleteRepeatInterval = 80L
    private val deleteRepeatHandler = Handler(Looper.getMainLooper())
    private var deleteRepeatRunnable: Runnable? = null

    private var longPressRunnable: Runnable? = null
    private var longPressCheckPending = false
    private var longPressPointerId = -1

    private val pointerMap = SparseArray<PointerState>()

    private class PointerState {
        var key: KeyboardKey? = null
        var longPressFired = false
        var committed = false
        var pressedKeyId: String? = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val model = layoutModel ?: return
        val p = palette ?: return

        renderer.drawKeyboard(
            canvas = canvas,
            layoutModel = model,
            palette = p,
            density = density,
            keyboardMode = keyboardMode,
            activeSymCategory = activeSymCategory,
            pressedKeyId = pressedKeyId,
            longPressedKeyId = longPressedKeyId,
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            requestRebuildLayout?.invoke()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val model = layoutModel ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = 0
                val pointerId = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                handlePointerDown(model, pointerId, x, y)
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                handlePointerDown(model, pointerId, x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val ps = pointerMap.get(pointerId) ?: continue
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val currentKey = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, x, y, 0)
                    if (currentKey?.id != ps.key?.id) {
                        if (ps.pressedKeyId != null) {
                            ps.key = null
                            ps.committed = false
                            ps.longPressFired = false
                            ps.pressedKeyId = null
                        }
                        cancelLongPressForPointer(pointerId)
                    }
                }
                updatePressedKeyId()
                invalidate()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                handlePointerUp(pointerId)
                return true
            }

            MotionEvent.ACTION_UP -> {
                val idx = 0
                val pointerId = event.getPointerId(idx)
                handlePointerUp(pointerId)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelAllPointers()
                stopDeleteRepeat()
                pressedKeyId = null
                longPressedKeyId = null
                invalidate()
                return true
            }
        }

        return false
    }

    private fun handlePointerDown(model: KeyboardLayoutModel, pointerId: Int, x: Float, y: Float) {
        val key = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, x, y, 0)
        if (key == null) return
        if (key.role == KeyboardKeyRole.PLACEHOLDER) return
        if (key.role == KeyboardKeyRole.RAIL_READING && key.label.isEmpty()) return

        val ps = PointerState()
        ps.key = key
        ps.pressedKeyId = key.id
        pointerMap.put(pointerId, ps)
        updatePressedKeyId()
        invalidate()

        val isDigitDown = isDigitKey(key)

        when (key.role) {
            KeyboardKeyRole.SPECIAL, KeyboardKeyRole.RAIL_SYMBOL_CATEGORY -> onHapticSpecial?.invoke()
            else -> onHapticTap?.invoke()
        }

        if (isDigitDown) {
            ps.committed = true
            dispatchDigitAction(key)
        } else {
            scheduleLongPressForPointer(pointerId, key)
        }
    }

    private fun handlePointerUp(pointerId: Int) {
        val ps = pointerMap.get(pointerId) ?: return
        val key = ps.key
        cancelLongPressForPointer(pointerId)

        if (key != null && !ps.longPressFired && ps.committed && isDigitKey(key)) {
            // Digit keys: already committed on DOWN, nothing on UP
        } else if (key != null && !ps.longPressFired && !ps.committed) {
            dispatchAction(key)
        }

        pointerMap.remove(pointerId)
        updatePressedKeyId()
        longPressedKeyId = if (pointerId == longPressPointerId) {
            cancelKeyboardLongPress()
            null
        } else longPressedKeyId
        invalidate()
    }

    private fun isDigitKey(key: KeyboardKey): Boolean {
        return key.action.startsWith("digit:") || key.action == "separator"
    }

    private fun dispatchDigitAction(key: KeyboardKey) {
        when {
            key.action == "separator" -> {
                onInputAction?.invoke(ImeInputAction.SeparatorPressed) ?: onKeyAction?.invoke("separator")
            }
            key.action.startsWith("digit:") -> {
                val digit = key.action.removePrefix("digit:")
                onInputAction?.invoke(ImeInputAction.DigitPressed(digit)) ?: onKeyAction?.invoke("digit:$digit")
            }
        }
    }

    private fun dispatchAction(key: KeyboardKey) {
        val action = key.action
        when {
            action == "separator" -> onInputAction?.invoke(ImeInputAction.SeparatorPressed) ?: onKeyAction?.invoke("separator")
            action.startsWith("digit:") -> {
                val digit = action.removePrefix("digit:")
                onInputAction?.invoke(ImeInputAction.DigitPressed(digit)) ?: onKeyAction?.invoke("digit:$digit")
            }
            action == "del" -> onInputAction?.invoke(ImeInputAction.DeletePressed) ?: onKeyAction?.invoke("del")
            action == "retype" -> onInputAction?.invoke(ImeInputAction.ClearComposing) ?: onKeyAction?.invoke("retype")
            action == "enter" -> onInputAction?.invoke(ImeInputAction.EnterShortPressed) ?: onEnterShortPress?.invoke()
            action == "space" -> onInputAction?.invoke(ImeInputAction.SpacePressed) ?: onKeyAction?.invoke("space")
            action.startsWith("toggle:") -> {
                val toggle = action.removePrefix("toggle:")
                val inputAction = when (toggle) {
                    "symbol" -> ImeInputAction.ToggleSymbol
                    "number" -> ImeInputAction.ToggleNumber
                    "english" -> ImeInputAction.ToggleChineseEnglish
                    else -> null
                }
                if (inputAction != null) onInputAction?.invoke(inputAction) ?: onKeyAction?.invoke("toggle:$toggle")
            }
            action.startsWith("punct:") -> {
                val punct = action.removePrefix("punct:")
                onInputAction?.invoke(ImeInputAction.PunctuationCommitted(punct)) ?: onKeyAction?.invoke("punct:$punct")
            }
            action.startsWith("reading:") -> {
                val indexStr = action.removePrefix("reading:")
                val index = indexStr.toIntOrNull() ?: return
                onInputAction?.invoke(ImeInputAction.ReadingSelected(index)) ?: onKeyAction?.invoke("reading:$index")
            }
            action.startsWith("symtab:") -> {
                val cat = action.removePrefix("symtab:")
                onInputAction?.invoke(ImeInputAction.SymbolCategorySelected(cat)) ?: onKeyAction?.invoke("symtab:$cat")
            }
            action == "symbol:commit" -> {
                val text = key.actionPayload ?: return
                onInputAction?.invoke(ImeInputAction.SymbolCommitted(text)) ?: onKeyAction?.invoke("symbol:commit:$text")
            }
        }
    }

    private fun scheduleLongPressForPointer(pointerId: Int, key: KeyboardKey) {
        cancelKeyboardLongPress()
        longPressPointerId = pointerId
        longPressCheckPending = true

        longPressRunnable = Runnable {
            if (longPressCheckPending) {
                val ps = pointerMap.get(pointerId)
                if (ps != null && ps.key?.id == key.id) {
                    ps.longPressFired = true
                    longPressedKeyId = key.id
                    invalidate()

                    onHapticLongPress?.invoke()

                    if (key.id == "enter") {
                        onEnterLongPress?.invoke()
                    } else if (key.id == "del") {
                        startDeleteRepeat()
                    }
                }
            }
        }
        mainHandler.postDelayed(longPressRunnable!!, 400L)
    }

    private fun cancelLongPressForPointer(pointerId: Int) {
        if (longPressPointerId == pointerId) {
            cancelKeyboardLongPress()
        }
    }

    private fun updatePressedKeyId() {
        pressedKeyId = null
        for (i in 0 until pointerMap.size()) {
            val ps = pointerMap.valueAt(i)
            if (ps.pressedKeyId != null) {
                pressedKeyId = ps.pressedKeyId
                return
            }
        }
    }

    private fun cancelAllPointers() {
        for (i in 0 until pointerMap.size()) {
            pointerMap.valueAt(i).let {
                it.key = null
                it.committed = false
                it.longPressFired = false
                it.pressedKeyId = null
            }
        }
        pointerMap.clear()
    }

    private fun cancelKeyboardLongPress() {
        longPressCheckPending = false
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable = null
        longPressPointerId = -1
    }

    private fun startDeleteRepeat() {
        if (deleteRepeatActive) return
        deleteRepeatActive = true
        val runnable = object : Runnable {
            override fun run() {
                if (deleteRepeatActive) {
                    onDeleteRepeat?.invoke()
                    deleteRepeatHandler.postDelayed(this, deleteRepeatInterval)
                }
            }
        }
        deleteRepeatRunnable = runnable
        deleteRepeatHandler.postDelayed(runnable, deleteRepeatInterval)
    }

    private fun stopDeleteRepeat() {
        deleteRepeatActive = false
        deleteRepeatRunnable?.let { deleteRepeatHandler.removeCallbacks(it) }
        deleteRepeatRunnable = null
    }

    fun destroy() {
        stopDeleteRepeat()
        cancelKeyboardLongPress()
        cancelAllPointers()
        pressedKeyId = null
        longPressedKeyId = null
    }

    internal fun simulateLongPress() {
        if (pointerMap.size() == 0) return
        val ps = pointerMap.valueAt(0)
        val lk = ps.key ?: return
        ps.longPressFired = true
        longPressedKeyId = lk.id
        cancelKeyboardLongPress()
        invalidate()
        onHapticLongPress?.invoke()
        if (lk.id == "enter") {
            onEnterLongPress?.invoke()
        } else if (lk.id == "del") {
            startDeleteRepeat()
        }
    }
}
