package io.github.xiwei753.pinyin.t9

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        io.github.xiwei753.pinyin.t9.data.DictionaryManager.initAsync(this)
        val dict = io.github.xiwei753.pinyin.t9.data.DictionaryManager.instance
        val dictStatusText = findViewById<TextView>(R.id.text_main_dict_status)
        if (dict == null) {
            dictStatusText.text = "词库状态: 正在加载中..."
            dictStatusText.setTextColor(android.graphics.Color.parseColor("#1976D2"))
        } else if (dict.isFallback) {
            dictStatusText.text = "词库状态: 加载失败，已回退 (${dict.loadedWordCount} 词)"
            dictStatusText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            dictStatusText.text = "词库已加载 ${dict.loadedWordCount} 词"
            dictStatusText.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        }

        // Add auto-refresh polling just in case it takes a moment to copy DB on first launch
        if (dict == null) {
            dictStatusText.postDelayed(object : Runnable {
                override fun run() {
                    val newDict = io.github.xiwei753.pinyin.t9.data.DictionaryManager.instance
                    if (newDict != null) {
                        if (newDict.isFallback) {
                            dictStatusText.text = "词库状态: 加载失败，已回退 (${newDict.loadedWordCount} 词)"
                            dictStatusText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                        } else {
                            dictStatusText.text = "词库已加载 ${newDict.loadedWordCount} 词"
                            dictStatusText.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                        }
                    } else {
                        dictStatusText.postDelayed(this, 500)
                    }
                }
            }, 500)
        }

        findViewById<Button>(R.id.btn_enable_ime).setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_switch_ime).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
