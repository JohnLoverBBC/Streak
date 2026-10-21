package com.streak.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.util.Locale

fun Context.t(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

class MainActivity : ComponentActivity() {
    lateinit var store: Store
    private val perm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) { store.rm = true; store.save(); Reminder.schedule(this, store); t("Reminder set") }
        else t("Allow notifications in Android settings")
    }
    private val permTest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) Reminder.notify(this) else t("Permission denied")
    }
    private fun needsPerm() = Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun enableReminder() {
        if (needsPerm()) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { store.rm = true; store.save(); Reminder.schedule(this, store) }
    }
    fun disableReminder() { store.rm = false; store.save(); Reminder.cancel(this) }
    fun testNotif() { if (needsPerm()) permTest.launch(Manifest.permission.POST_NOTIFICATIONS) else { Reminder.notify(this); t("Sent") } }
    fun permOk() = !needsPerm()
    fun buzz() { if (store.vb) getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        store = Store(this)
        if (store.awarded > 0) t("💎 +${store.awarded} gem(s) from unused revive!")
        setContent { App(this, store) }
    }
}

@Composable fun App(a: MainActivity, s: Store) {
    val sk = SKINS.first { it.id == s.skin }
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(sk.bg)) {
        Box(Modifier.fillMaxWidth().height(46.dp).background(sk.nav), contentAlignment = Alignment.Center) {
            Text(s.name, color = sk.navText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("💎 ${s.gems}", color = sk.navText, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
            when (tab) {
                0 -> CalScreen(a, s, sk)
                1 -> ShopScreen(a, s, sk)
                2 -> SettingsScreen(a, s, sk) { tab = 3 }
                else -> DebugScreen(a, s, sk) { tab = 2 }
            }
        }
        Row(Modifier.fillMaxWidth().background(sk.tab)) {
            listOf("📅" to "Calendar", "💎" to "Shop", "⚙️" to "Settings").forEachIndexed { i, (e, l) ->
                Column(Modifier.weight(1f).clickable { tab = i }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(e, fontSize = 20.sp)
                    Text(l, fontSize = 11.sp, color = if (tab == i) sk.ring else sk.tabOff)
                }
            }
        }
    }
}

@Composable fun Panel(sk: Skin, content: @Composable ColumnScope.() -> Unit) {
    val sh = RoundedCornerShape(sk.r)
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)
        .then(if (sk.ios) Modifier.border(1.dp, sk.line, sh) else Modifier).clip(sh).background(sk.card), content = content)
}

@Composable fun Btn(t: String, sk: Skin, on: Boolean = true, click: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(if (sk.ios) 8.dp else 14.dp))
        .background(sk.accent).alpha(if (on) 1f else 0.5f).clickable(enabled = on) { click() }.padding(12.dp),
        contentAlignment = Alignment.Center) { Text(t, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}

@Composable fun RowScope.Stat(v: String, l: String, sk: Skin) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = sk.text)
        Text(l, fontSize = 12.sp, color = sk.sub)
    }
}

@Composable fun CalScreen(a: MainActivity, s: Store, sk: Skin) {
    val ctx = LocalContext.current
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var confirm by remember { mutableStateOf<LocalDate?>(null) }
    val (cur, best) = s.streaks()
    val today = LocalDate.now()
    Panel(sk) { Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Stat("$cur", "Current", sk); Stat("$best", "Best", sk); Stat("${s.gems}", "Gems 💎", sk) } }
    Panel(sk) { Column(Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◀", Modifier.clickable { month = month.minusMonths(1) }.padding(12.dp), color = sk.text)
            Text(month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = sk.text)
            Text("▶", Modifier.clickable { month = month.plusMonths(1) }.padding(12.dp), color = sk.text)
        }
        Row(Modifier.fillMaxWidth()) {
            (if (s.mon) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("S", "M", "T", "W", "T", "F", "S")).forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = sk.sub, fontWeight = FontWeight.Bold) }
        }
        val off = if (s.mon) month.dayOfWeek.value - 1 else month.dayOfWeek.value % 7
        val n = month.lengthOfMonth()
        for (r in 0 until (off + n + 6) / 7) Row(Modifier.fillMaxWidth()) {
            for (c in 0..6) {
                val day = r * 7 + c - off + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                    if (day in 1..n) {
                        val d = month.withDayOfMonth(day); val st = s.status(d)
                        val fut = d.isAfter(today)
                        val miss = !fut && st == 0 && !d.isBefore(LocalDate.parse(s.start)) && d.isBefore(today)
                        val sh = if (sk.round) CircleShape else RoundedCornerShape(6.dp)
                        var m = Modifier.fillMaxSize().clip(sh)
                        m = when { st == 1 -> m.background(sk.done); st == 2 -> m.background(sk.rev)
                            miss -> m.background(sk.miss); else -> m.background(sk.cell) }
                        if (d == today) m = m.border(2.dp, sk.ring, sh)
                        Box(m.alpha(if (fut) 0.35f else 1f).clickable(enabled = !fut) {
                            when {
                                d == today -> { s.toggleToday(); a.buzz(); if (s.status(d) == 1) ctx.t("Streak continued! 🔥") }
                                st != 0 -> {}
                                d.isBefore(LocalDate.parse(s.start)) -> ctx.t("That’s before you started.")
                                java.time.temporal.ChronoUnit.DAYS.between(d, today) > 7 -> ctx.t("Revive only reaches back 7 days.")
                                !s.revAvail() -> ctx.t("Revive already used this week.")
                                else -> confirm = d
                            }
                        }, contentAlignment = Alignment.Center) {
                            Text(if (st == 2) "⟳" else "$day", color = if (st != 0) Color.White else sk.text,
                                fontWeight = if (st != 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    } }
    Btn(if (s.status(today) == 1) "Checked in ✓ (tap to undo)" else "Check in today", sk) {
        s.toggleToday(); a.buzz(); if (s.status(today) == 1) ctx.t("Streak continued! 🔥") }
    Text(if (s.revAvail()) "🔄 Revive ready this week. Tap a missed (red) day from the last 7 days."
        else "🔄 Revive used this week. A new one arrives next week.",
        Modifier.padding(6.dp), color = sk.sub, fontSize = 13.sp)
    confirm?.let { d ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Use revive?") },
            text = { Text("Restore $d? You only get one revive per week.") },
            confirmButton = { TextButton({ s.revive(d); a.buzz(); confirm = null; ctx.t("Streak revived! 🔄") }) { Text("Revive") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } })
    }
}

@Composable fun ShopScreen(a: MainActivity, s: Store, sk: Skin) {
    val ctx = LocalContext.current
    Text("SKINS", color = sk.sub, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
    Panel(sk) { SKINS.forEach { k ->
        val owned = k.id in s.own; val active = s.skin == k.id
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(k.nav))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(k.n, fontWeight = FontWeight.Bold, color = sk.text); Text(k.d, fontSize = 13.sp, color = sk.sub) }
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(sk.accent).alpha(if (active) 0.5f else 1f)
                .clickable(enabled = !active) {
                    if (!owned) { if (s.gems < k.cost) { ctx.t("Need ${k.cost} gems. You have ${s.gems}."); return@clickable }
                        s.gems -= k.cost; s.own.add(k.id); ctx.t("Unlocked ${k.n}!") }
                    s.skin = k.id; s.save(); a.buzz()
                }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(if (active) "Active" else if (owned) "Apply" else "💎 ${k.cost}", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    } }
    Text("Every week you don’t use your revive, it turns into 1 gem 💎 on the next week.", color = sk.sub, fontSize = 13.sp, modifier = Modifier.padding(6.dp))
}

@Composable fun Toggle(on: Boolean, sk: Skin, change: (Boolean) -> Unit) {
    if (sk.ios) Box(Modifier.width(76.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
        .background(if (on) Color(0xFF3B7FD8) else Color(0xFFAAAAAA)).clickable { change(!on) }) {
        Text(if (on) "ON" else "OFF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(if (on) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 10.dp))
        Box(Modifier.padding(1.dp).size(26.dp).clip(CircleShape).background(Color.White)
            .align(if (on) Alignment.CenterEnd else Alignment.CenterStart))
    } else Switch(checked = on, onCheckedChange = change)
}

@Composable fun Item(sk: Skin, label: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = sk.text, fontSize = 16.sp); content() }
    Box(Modifier.fillMaxWidth().height(1.dp).background(sk.line))
}

@Composable fun SettingsScreen(a: MainActivity, s: Store, sk: Skin, openDebug: () -> Unit) {
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var askReset by remember { mutableStateOf(false) }
    val ts = TextStyle(color = sk.sub, fontSize = 16.sp, textAlign = TextAlign.End)
    Panel(sk) {
        Item(sk, "Name") { BasicTextField(s.name, { s.name = it; s.save() }, Modifier.weight(1f), singleLine = true,
            textStyle = ts, cursorBrush = SolidColor(sk.ring)) }
        Item(sk, "Week starts Monday") { Toggle(s.mon, sk) { s.mon = it; s.save() } }
        Item(sk, "Daily reminder") { Toggle(s.rm, sk) { if (it) a.enableReminder() else a.disableReminder() } }
        Item(sk, "Reminder time") { Text(String.format("%02d:%02d", s.rh, s.rmin), color = sk.sub, fontSize = 16.sp,
            modifier = Modifier.clickable { TimePickerDialog(ctx, { _, h, m -> s.rh = h; s.rmin = m; s.save()
                if (s.rm) Reminder.schedule(ctx, s) }, s.rh, s.rmin, true).show() }) }
        Item(sk, "Vibration") { Toggle(s.vb, sk) { s.vb = it; s.save() } }
        Item(sk, "Developer code") { BasicTextField(code, { code = it; if (it == "2009") { code = ""; openDebug() } },
            Modifier.weight(1f), singleLine = true, textStyle = ts, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), cursorBrush = SolidColor(sk.ring)) }
        Text("Reset all data", Modifier.fillMaxWidth().clickable { askReset = true }.padding(14.dp),
            color = Color(0xFFC0392B), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
    if (askReset) AlertDialog(onDismissRequest = { askReset = false }, title = { Text("Reset?") },
        text = { Text("Erase all streak data, gems and skins?") },
        confirmButton = { TextButton({ s.reset(); askReset = false }) { Text("Erase") } },
        dismissButton = { TextButton({ askReset = false }) { Text("Cancel") } })
}

@Composable fun DebugScreen(a: MainActivity, s: Store, sk: Skin, close: () -> Unit) {
    Text("DEBUG", color = sk.sub, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
    Panel(sk) { Text(listOf("Android SDK: ${Build.VERSION.SDK_INT}", "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
        "Notification permission: ${a.permOk()}", "Reminder: ${s.rm} @ ${String.format("%02d:%02d", s.rh, s.rmin)}",
        "Gems: ${s.gems}   Skin: ${s.skin}", "Owned: ${s.own.joinToString()}", "Start: ${s.start}",
        "Days logged: ${s.done.size}", "Revive used this week: ${!s.revAvail()}", "Week start: ${s.wkStart(LocalDate.now())}")
        .joinToString("\n"), Modifier.padding(12.dp), color = sk.text, fontSize = 13.sp) }
    Btn("Send test notification", sk) { a.testNotif() }
    Btn("+5 gems", sk) { s.gems += 5; s.save() }
    Btn("Unlock all skins", sk) { SKINS.forEach { if (it.id !in s.own) s.own.add(it.id) }; s.save() }
    Btn("Reset this week’s revive", sk) { s.rev.remove(s.wkStart(LocalDate.now()).toString()); s.save() }
    Btn("Close debug", sk) { close() }
}
