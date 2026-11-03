package com.dundueni.app.feature.phoneinput

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneInputValidationTest {

    @Test
    fun testFormatPhoneNumber_mobile() {
        assertEquals("010-1234-5678", formatPhoneNumber("01012345678"))
        assertEquals("010-123-4567", formatPhoneNumber("0101234567"))
        assertEquals("011-987-6543", formatPhoneNumber("0119876543"))
    }

    @Test
    fun testFormatPhoneNumber_seoulLandline() {
        assertEquals("02-123-4567", formatPhoneNumber("021234567"))
        assertEquals("02-1234-5678", formatPhoneNumber("0212345678"))
    }

    @Test
    fun testFormatPhoneNumber_representativeNumber() {
        assertEquals("1588-1234", formatPhoneNumber("15881234"))
        assertEquals("1577-1234", formatPhoneNumber("15771234"))
    }

    @Test
    fun testIsValidPhoneNumber() {
        assertTrue(isValidPhoneNumber("01012345678"))
        assertTrue(isValidPhoneNumber("010-1234-5678"))
        assertTrue(isValidPhoneNumber("02-123-4567"))
        assertTrue(isValidPhoneNumber("1588-1234"))

        assertFalse(isValidPhoneNumber(""))
        assertFalse(isValidPhoneNumber("123"))
        assertFalse(isValidPhoneNumber("010123"))
        assertFalse(isValidPhoneNumber("abcdefghijk"))
    }
}
