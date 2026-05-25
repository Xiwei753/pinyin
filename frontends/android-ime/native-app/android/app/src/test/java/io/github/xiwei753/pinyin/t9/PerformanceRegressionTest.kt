package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.imecore.InputMode
import io.github.xiwei753.pinyin.imecore.ImeStateMachine
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.ArgumentMatchers.anyString

class PerformanceRegressionTest {

    @Test
    fun testSwitchChineseEnglish20Times() {
        val dictionary = mock(DictionaryProvider::class.java)
        val engine = T9Engine(dictionary)
        val adapter = T9EngineAdapter(engine)
        val machine = ImeStateMachine()
        machine.attachEngine(adapter)

        assertEquals(InputMode.ChineseT9, machine.mode)

        for (i in 0 until 20) {
            machine.dispatch(ImeInputAction.ToggleChineseEnglish)
        }

        // Verify state is correct and no crash
        assertEquals(InputMode.ChineseT9, machine.mode)
    }

    @Test
    fun testDictionaryNotPreparedOnSwitch() {
        val dictionary = mock(DictionaryProvider::class.java)
        val engine = mock(T9Engine::class.java)
        val adapter = T9EngineAdapter(engine)
        val machine = ImeStateMachine()
        machine.attachEngine(adapter)

        machine.dispatch(ImeInputAction.ToggleChineseEnglish)
        verify(engine, never()).inputDigit(anyString())
    }

}
