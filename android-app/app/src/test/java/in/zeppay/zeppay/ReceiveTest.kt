package `in`.zeppay.zeppay

import `in`.zeppay.zeppay.data.ProfileCheck
import `in`.zeppay.zeppay.data.QrImage
import `in`.zeppay.zeppay.data.ReceiveLink
import `in`.zeppay.zeppay.data.UpiParser
import `in`.zeppay.zeppay.scan.decodePixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Profile checks, the Receive QR's link, and a real encode -> decode round trip. */
class ReceiveTest {

    @Test fun upiIdValidation() {
        listOf("asha@okaxis", "asha.k-1_2@ybl", "9876543210@paytm", "  ravi@sbi  ").forEach { assertEquals(it, "", ProfileCheck.vpa(it)) }
        assertEquals("blank is allowed (name first)", "", ProfileCheck.vpa(""))
        listOf("asha", "asha@", "@okaxis", "a@okaxis", "asha@ok axis", "asha@1bank", "as ha@okaxis", "asha@@okaxis", "asha@ok.axis").forEach {
            assertNotEquals(it, "", ProfileCheck.vpa(it))
        }
        assertEquals("A UPI ID looks like name@bank", ProfileCheck.vpa("asha"))
    }

    @Test fun nameValidation() {
        assertEquals("", ProfileCheck.name("Asha Kumar"))
        assertEquals("Enter your name", ProfileCheck.name("   "))
        assertNotEquals("", ProfileCheck.name("x".repeat(41)))
    }

    @Test fun linkWithNameOnly() {
        assertEquals("upi://pay?pa=asha@okaxis&pn=Asha%20Kumar", ReceiveLink.build("asha@okaxis", "Asha Kumar"))
    }

    @Test fun linkWithAmountAndNote() {
        assertEquals("upi://pay?pa=asha@okaxis&pn=Asha&am=250.00&tn=Rent%20%26%20bills",
            ReceiveLink.build(" asha@okaxis ", "Asha", "250", "Rent & bills"))
        assertEquals("upi://pay?pa=asha@okaxis&pn=Asha&am=99.50", ReceiveLink.build("asha@okaxis", "Asha", "99.5"))
    }

    @Test fun linkEncodesNonAsciiAndReserved() {
        assertEquals("upi://pay?pa=ram@okaxis&pn=%E0%A4%B0%E0%A4%BE%E0%A4%AE&tn=a%2Bb%3Dc", ReceiveLink.build("ram@okaxis", "राम", note = "a+b=c"))
    }

    @Test fun linkWithoutNameLeavesPnOut() {
        assertEquals("upi://pay?pa=asha@okaxis", ReceiveLink.build("asha@okaxis", " "))
    }

    @Test fun invalidInputsGiveNoLink() {
        assertNull(ReceiveLink.build("asha", "Asha"))
        assertNull(ReceiveLink.build("asha@okaxis", "Asha", "0.5"))
        assertNull(ReceiveLink.build("asha@okaxis", "Asha", "100000.01"))
        assertNull(ReceiveLink.build("asha@okaxis", "Asha", "12.345"))
        assertNull(ReceiveLink.build("asha@okaxis", "Asha", "abc"))
        assertNull(ReceiveLink.build("asha@okaxis", "Asha", note = "x".repeat(51)))
        assertEquals("", ReceiveLink.checkAmount("100000"))
    }

    @Test fun qrRoundTrip() {
        val link = ReceiveLink.build("asha.k@okaxis", "Asha Kumar", "250", "Tea")!!
        val (w, h, px) = QrImage.pixels(link, scale = 8)
        val decoded = decodePixels(w, h, px)
        assertEquals(link, decoded)
        val p = UpiParser.parse(decoded!!)!!
        assertEquals("asha.k@okaxis", p.vpa)
        assertEquals("Asha Kumar", p.name)
        assertEquals("250", p.amount)
        assertEquals("Tea", p.note)
    }

    @Test fun qrRoundTripNonAscii() {
        val link = ReceiveLink.build("ram@okaxis", "राम कुमार")!!
        val (w, h, px) = QrImage.pixels(link, scale = 6)
        assertEquals(link, decodePixels(w, h, px))
        assertEquals("राम कुमार", UpiParser.parse(link)!!.name)
    }

    @Test fun qrIsBlackOnWhiteWithQuietZone() {
        val (w, _, px) = QrImage.pixels("upi://pay?pa=asha@okaxis", scale = 4)
        assertTrue("top-left corner is quiet zone", px[0] == 0xFFFFFFFF.toInt() && px[w + 1] == 0xFFFFFFFF.toInt())
        assertTrue(px.all { it == 0xFFFFFFFF.toInt() || it == 0xFF000000.toInt() })
    }
}
