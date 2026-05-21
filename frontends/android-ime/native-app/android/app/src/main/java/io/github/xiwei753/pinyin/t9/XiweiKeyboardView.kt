package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

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
    private var trackedKey: KeyboardKey? = null
    private var longPressFired = false
    private var longPressCheckPending = false

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
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, x, y, 0)
                if (key == null) return false
                if (key.role == KeyboardKeyRole.PLACEHOLDER) return false
                if (key.role == KeyboardKeyRole.LEFT_RAIL_READING && key.label.isEmpty()) return false

                trackedKey = key
                longPressFired = false
                longPressCheckPending = true

                pressedKeyId = key.id
                invalidate()

                when (key.role) {
                    KeyboardKeyRole.SPECIAL, KeyboardKeyRole.SYMBOL_TAB -> onHapticSpecial?.invoke()
                    else -> onHapticTap?.invoke()
                }

                longPressRunnable = Runnable {
                    if (longPressCheckPending && trackedKey != null) {
                        longPressFired = true
                        val lk = trackedKey!!
                        longPressedKeyId = lk.id
                        invalidate()

                        onHapticLongPress?.invoke()

                        if (lk.id == "enter") {
                            onEnterLongPress?.invoke()
                        } else if (lk.id == "del") {
                            startDeleteRepeat()
                        }
                    }
                }
                mainHandler.postDelayed(longPressRunnable!!, 400L)

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (trackedKey == null) return false
                val currentKey = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, x, y, 0)
                if (currentKey?.id != trackedKey?.id) {
                    cancelKeyboardLongPress()
                    pressedKeyId = null
                    longPressedKeyId = null
                    trackedKey = null
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelKeyboardLongPress()
                stopDeleteRepeat()

                val key = trackedKey
                trackedKey = null
                pressedKeyId = null
                longPressedKeyId = null
                invalidate()

                if (key != null && !longPressFired) {
                    dispatchAction(key)
                }

                return true
            }
        }

        return false
    }

    private fun dispatchAction(key: KeyboardKey) {
        val action = key.action
        when {
            action == "separator" -> onKeyAction?.invoke("separator")
            action.startsWith("digit:") -> {
                val digit = action.removePrefix("digit:")
                onKeyAction?.invoke("digit:$digit")
            }
            action == "del" -> onKeyAction?.invoke("del")
            action == "retype" -> onKeyAction?.invoke("retype")
            action == "enter" -> onEnterShortPress?.invoke()
            action == "space" -> onKeyAction?.invoke("space")
            action.startsWith("toggle:") -> {
                val toggle = action.removePrefix("toggle:")
                onKeyAction?.invoke("toggle:$toggle")
            }
            action.startsWith("punct:") -> {
                val punct = action.removePrefix("punct:")
                onKeyAction?.invoke("punct:$punct")
            }
            action.startsWith("reading:") -> {
                val indexStr = action.removePrefix("reading:")
                val index = indexStr.toIntOrNull() ?: return
                onKeyAction?.invoke("reading:$index")
            }
            action.startsWith("symtab:") -> {
                val cat = action.removePrefix("symtab:")
                onKeyAction?.invoke("symtab:$cat")
            }
            action == "symbol:commit" -> {
                val text = key.actionPayload ?: return
                onKeyAction?.invoke("symbol:commit:$text")
            }
        }
    }

    private fun cancelKeyboardLongPress() {
        if (longPressCheckPending) {
            longPressRunnable?.let { mainHandler.removeCallbacks(it) }
            longPressRunnable = null
            longPressCheckPending = false
        }
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
        trackedKey = null
        pressedKeyId = null
        longPressedKeyId = null
    }

    internal fun simulateLongPress() {
        val lk = trackedKey ?: return
        longPressFired = true
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
