package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Test

class T9CodeMapperTest {

    @Test
    fun testToCode() {
        assertEquals("64426", T9CodeMapper.toCode("ni hao"))
        assertEquals("7487832", T9CodeMapper.toCode("shu ru fa"))
        assertEquals("746946", T9CodeMapper.toCode("pin yin"))
        assertEquals("94664486", T9CodeMapper.toCode("zhong guo"))
        assertEquals("866428", T9CodeMapper.toCode("tong bu"))
        assertEquals("743944", T9CodeMapper.toCode("she zhi"))
        assertEquals("4689826", T9CodeMapper.toCode("hou xuan"))
        assertEquals("74263826", T9CodeMapper.toCode("qian duan"))
        assertEquals("43946", T9CodeMapper.toCode("he xin"))
        assertEquals("226458", T9CodeMapper.toCode("cang ku"))
    }

    @Test
    fun testToCodeWithIgnoredCharacters() {
        assertEquals("64426", T9CodeMapper.toCode("ni'hao"))
        assertEquals("64426", T9CodeMapper.toCode("ni-hao"))
        assertEquals("64426", T9CodeMapper.toCode("Ni Hao"))
        assertEquals("64426", T9CodeMapper.toCode("NI HAO"))
        assertEquals("64426", T9CodeMapper.toCode("n1i hao@"))
    }
}
