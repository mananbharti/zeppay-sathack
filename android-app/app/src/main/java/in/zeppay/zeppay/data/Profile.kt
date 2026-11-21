package `in`.zeppay.zeppay.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

/** The user's own details, kept on this phone only (Store keys profileName / profileVpa / profilePhoto). */
data class Profile(val name: String = "", val vpa: String = "", val photo: String = "") {
    val hasVpa get() = vpa.isNotBlank()
}

/** Checks for the Profile form. "" = fine. */
object ProfileCheck {
    const val MAX_NAME = 40

    fun name(raw: String): String {
        val n = raw.trim()
        if (n.isEmpty()) return "Enter your name"
        if (n.length > MAX_NAME) return "Use at most $MAX_NAME letters"
        return ""
    }

    /** Blank is allowed (you can save a name first); anything typed must look like name@bank. */
    fun vpa(raw: String): String {
        val v = raw.trim()
        if (v.isEmpty()) return ""
        if (!v.contains('@')) return "A UPI ID looks like name@bank"
        if (!Lib.isValidVpa(v)) return "Check your UPI ID: letters, numbers, dot, dash or underscore, then @bank"
        return ""
    }
}

/**
 * Builds the link inside the Receive QR: upi://pay?pa=<UPI ID>&pn=<name>[&am=<amount>][&tn=<note>].
 * Any UPI app that scans it opens a payment TO this UPI ID. It moves no money by itself and
 * ZepPay never learns whether anyone paid.
 */
object ReceiveLink {
    const val MAX_AMOUNT = 100000.0
    const val MAX_NOTE = 50

    /** Optional amount. "" = fine (blank or valid). */
    fun checkAmount(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val n = t.toDoubleOrNull() ?: return "Enter a number, like 250"
        if (n < 1) return "Minimum is ₹1"
        if (n > MAX_AMOUNT) return "UPI allows up to ₹1,00,000 per payment"
        if (Math.round(n * 100).toDouble() != n * 100) return "Use at most 2 decimals"
        return ""
    }

    fun checkNote(raw: String): String = if (raw.trim().length > MAX_NOTE) "Use at most $MAX_NOTE letters" else ""

    /** null when the UPI ID is invalid or the amount/note fail their checks. */
    fun build(vpa: String, name: String, amount: String = "", note: String = ""): String? {
        val pa = vpa.trim()
        if (!Lib.isValidVpa(pa) || checkAmount(amount).isNotEmpty() || checkNote(note).isNotEmpty()) return null
        val sb = StringBuilder("upi://pay?pa=").append(enc(pa, keep = "@"))
        name.trim().takeIf { it.isNotEmpty() }?.let { sb.append("&pn=").append(enc(it)) }
        amount.trim().takeIf { it.isNotEmpty() }?.let { sb.append("&am=").append(String.format(Locale.US, "%.2f", it.toDouble())) }
        note.trim().takeIf { it.isNotEmpty() }?.let { sb.append("&tn=").append(enc(it)) }
        return sb.toString()
    }

    /** RFC 3986 percent-encoding (UTF-8), spaces as %20; unreserved characters and [keep] stay as they are. */
    fun enc(s: String, keep: String = ""): String {
        val out = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val ch = (b.toInt() and 0xFF).toChar()
            if (ch.isLetterOrDigit() && ch.code < 128 || ch in "-._~" || ch in keep) out.append(ch)
            else out.append('%').append(String.format(Locale.US, "%02X", b.toInt() and 0xFF))
        }
        return out.toString()
    }
}

/** A real, scannable QR for a text, via ZXing. Pure: returns pixels so tests can decode them back. */
object QrImage {
    fun matrix(text: String): BitMatrix = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4),
    )

    /** ARGB pixels: [scale] px per module, black on white (scanners expect dark on light). */
    fun pixels(text: String, scale: Int = 12): Triple<Int, Int, IntArray> {
        val m = matrix(text)
        val w = m.width * scale; val h = m.height * scale
        val dark = 0xFF000000.toInt(); val light = 0xFFFFFFFF.toInt()
        val px = IntArray(w * h) { i -> if (m.get((i % w) / scale, (i / w) / scale)) dark else light }
        return Triple(w, h, px)
    }
}
