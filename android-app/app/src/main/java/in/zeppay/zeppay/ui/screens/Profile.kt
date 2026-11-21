package `in`.zeppay.zeppay.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import `in`.zeppay.zeppay.ZepViewModel
import `in`.zeppay.zeppay.data.ProfileCheck
import `in`.zeppay.zeppay.data.QrImage
import `in`.zeppay.zeppay.data.ReceiveLink
import `in`.zeppay.zeppay.ui.Screen
import `in`.zeppay.zeppay.ui.kit.*
import `in`.zeppay.zeppay.ui.theme.ZIcon
import `in`.zeppay.zeppay.ui.theme.ZType
import `in`.zeppay.zeppay.ui.theme.Zep
import java.io.File

/* ---------------- Profile ---------------- */

@Composable
fun ProfileScreen(vm: ZepViewModel, thenReceive: Boolean) {
    val c = Zep.c
    val profile by vm.profile.collectAsState()
    val photo by vm.photo.collectAsState()
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var vpa by rememberSaveable { mutableStateOf(profile.vpa) }
    var tried by rememberSaveable { mutableStateOf(false) }
    val nameErr = ProfileCheck.name(name)
    val vpaErr = ProfileCheck.vpa(vpa).ifEmpty { if (thenReceive && vpa.isBlank()) "Add your UPI ID to show your QR" else "" }
    // The system photo picker: no storage permission, it grants just the one picture.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.setPhoto(it) } }

    fun save() {
        tried = true
        if (nameErr.isNotEmpty() || vpaErr.isNotEmpty()) return
        vm.saveProfile(name, vpa)
        vm.showToast("Saved on this phone")
        if (thenReceive && vpa.isNotBlank()) vm.replace(Screen.Receive) else vm.back()
    }

    Page("Your profile", { vm.back() }, footer = { ActionButton("Save", ::save, Modifier.fillMaxWidth(), icon = ZIcon.Check) }) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name, 112.dp, photo)
                Spacer(Modifier.height(14.dp))
                PlainButton(if (photo == null) "Add photo" else "Change photo", {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, icon = ZIcon.Camera)
                if (photo != null) TextAction("Remove photo", { vm.removePhoto() }, icon = ZIcon.Trash)
            }
        }
        Field(name, { name = it }, "Your name", placeholder = "Your full name", caps = KeyboardCapitalization.Words,
            error = if (tried) nameErr.ifEmpty { null } else null)
        Field(vpa, { vpa = it.trim() }, "Your UPI ID", placeholder = "name@bank", keyboard = KeyboardType.Email, onDone = ::save,
            error = if (tried || (vpa.isNotEmpty() && vpa.contains('@'))) vpaErr.ifEmpty { null } else null)
        Fine("Find your UPI ID in your bank's app, or dial *99# and choose My Profile. ZepPay checks only that it looks like name@bank; it can't check that it's yours or that it works.")
        Fine("Saved on this phone only. ZepPay uses your name and UPI ID just to draw your Receive QR, and never sends them anywhere.")
        if (profile.hasVpa) TileRow("Show my QR", profile.vpa, ZIcon.Qr, { vm.go(Screen.Receive) })
    }
}

/* ---------------- Receive ---------------- */

@Composable
fun ReceiveScreen(vm: ZepViewModel) {
    val c = Zep.c
    val ctx = LocalContext.current
    val profile by vm.profile.collectAsState()
    val photo by vm.photo.collectAsState()
    if (!profile.hasVpa) { LaunchedEffect(Unit) { vm.replace(Screen.Profile(thenReceive = true)) }; return }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val amountErr = ReceiveLink.checkAmount(amount)
    val noteErr = ReceiveLink.checkNote(note)
    // While a field is invalid, keep showing the last good QR rather than a wrong one.
    var lastGood by remember { mutableStateOf<String?>(null) }
    val link = ReceiveLink.build(profile.vpa, profile.name, amount, note)?.also { lastGood = it } ?: lastGood ?: ReceiveLink.build(profile.vpa, profile.name)
    val qr = remember(link) { link?.let { qrBitmap(it) } }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null || link == null) return@rememberLauncherForActivityResult
        val ok = try { ctx.contentResolver.openOutputStream(uri)?.use { shareCard(link, profile.name, profile.vpa, amount).compress(Bitmap.CompressFormat.PNG, 100, it) } == true } catch (_: Exception) { false }
        vm.showToast(if (ok) "QR saved" else "Couldn't save the QR")
    }

    Page("Receive money", { vm.back() }, footer = {
        ActionButton("Share QR", { link?.let { shareQr(ctx, it, profile.name, profile.vpa, amount) } }, Modifier.fillMaxWidth(), icon = ZIcon.Share, enabled = link != null)
        PlainButton("Save QR", { save.launch("zeppay-qr.png") }, Modifier.fillMaxWidth(), icon = ZIcon.Save, enabled = link != null)
    }) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(profile.name.ifBlank { profile.vpa }, 48.dp, photo)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(profile.name.ifBlank { "Pay me" }, style = ZType.label, color = c.text)
                        Text(profile.vpa, style = ZType.small, color = c.muted)
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Always dark modules on white, in both themes: that's what scanners read best.
                if (qr != null) Box(Modifier.clip(RoundedCornerShape(20.dp)).background(c.qrPaper).padding(8.dp)
                    .semantics { contentDescription = "QR code to pay ${profile.vpa}" }) {
                    Image(qr.asImageBitmap(), null, Modifier.size(260.dp), filterQuality = FilterQuality.None)
                }
                Spacer(Modifier.height(12.dp))
                val amt = amount.trim().takeIf { it.isNotEmpty() && amountErr.isEmpty() }?.toDoubleOrNull()
                Text(if (amt != null) "Asks for ${money(amt)}" else "Any amount: the payer types it", style = ZType.bodyBold, color = c.text, textAlign = TextAlign.Center)
                TextAction("Copy UPI ID", {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("UPI ID", profile.vpa))
                    vm.showToast("UPI ID copied")
                }, icon = ZIcon.Copy)
            }
        }
        Field(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, "Amount (optional)", placeholder = "Leave empty for any amount",
            keyboard = KeyboardType.Decimal, error = amountErr.ifEmpty { null })
        Field(note, { note = it }, "Note (optional)", placeholder = "Rent, tea, groceries", error = noteErr.ifEmpty { null })
        Panel(Modifier.fillMaxWidth()) {
            Text("This QR doesn't confirm any payment", style = ZType.bodyBold, color = c.text)
            Spacer(Modifier.height(4.dp))
            Fine("ZepPay can't see money arriving. Before you hand anything over, check your bank's SMS, your bank's app or your statement. A payer's screen saying \"paid\" isn't proof.")
        }
        TileRow("Edit name or UPI ID", null, ZIcon.Edit, { vm.go(Screen.Profile()) })
    }
}

/** The QR alone, 10 px per module. */
private fun qrBitmap(link: String): Bitmap {
    val (w, h, px) = QrImage.pixels(link, scale = 10)
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}

/** The image people share or save: the QR with the name, UPI ID and (if set) amount underneath. */
private fun shareCard(link: String, name: String, vpa: String, amount: String): Bitmap {
    val qr = qrBitmap(link)
    val pad = 48; val lineH = 64
    val lines = listOfNotNull(name.takeIf { it.isNotBlank() }, vpa, amount.trim().toDoubleOrNull()?.let { "Amount: " + money(it) })
    val out = Bitmap.createBitmap(qr.width + pad * 2, qr.height + pad * 2 + lines.size * lineH, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(out)
    cv.drawColor(android.graphics.Color.WHITE)
    cv.drawBitmap(qr, pad.toFloat(), pad.toFloat(), null)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 40f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    lines.forEachIndexed { i, t -> cv.drawText(t, out.width / 2f, (qr.height + pad + 40 + i * lineH).toFloat(), paint) }
    return out
}

/** Shares the QR picture through Android's share sheet. The file sits in ZepPay's cache; the receiving app gets read access to that one file. */
private fun shareQr(ctx: Context, link: String, name: String, vpa: String, amount: String) {
    try {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "zeppay-qr.png")
        f.outputStream().use { shareCard(link, name, vpa, amount).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "Pay me on UPI: $vpa").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("QR", uri)
        ctx.startActivity(Intent.createChooser(send, "Share your QR").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        android.widget.Toast.makeText(ctx, "Couldn't share the QR", android.widget.Toast.LENGTH_SHORT).show()
    }
}
