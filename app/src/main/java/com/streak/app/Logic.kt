package com.streak.app

import android.app.*
import android.content.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Calendar

class Skin(
    val id: String, val n: String, val d: String, val cost: Int,
    val bg: Brush, val nav: Brush, val navText: Color, val card: Color,
    val text: Color, val sub: Color, val accent: Brush, val done: Brush,
    val rev: Brush, val tab: Brush, val ring: Color, val tabOff: Color,
    val r: Dp, val round: Boolean, val line: Color, val cell: Color,
    val miss: Color, val ios: Boolean = false
)

private fun c(v: Long) = Color(v)

val SKINS = listOf(
    Skin("ios", "iOS 6", "Linen, gloss and glass", 0,
        Brush.verticalGradient(listOf(c(0xFFD2CCBF), c(0xFFBDB6A6))),
        Brush.verticalGradient(0f to c(0xFFA3B6D1), 0.5f to c(0xFF6F8CB5), 0.51f to c(0xFF5B7BA8), 1f to c(0xFF5A79A5)),
        Color.White, Color.White, c(0xFF222222), c(0xFF4C566C),
        Brush.verticalGradient(0f to c(0xFF7AA0D8), 0.5f to c(0xFF3B6FC0), 0.51f to c(0xFF2A5BB0), 1f to c(0xFF3A70C4)),
        Brush.radialGradient(listOf(c(0xFF8FDC4F), c(0xFF2F7A10))),
        Brush.radialGradient(listOf(c(0xFFF5B04C), c(0xFFA85D00))),
        Brush.verticalGradient(0f to c(0xFF4A4A4A), 0.5f to c(0xFF1C1C1C), 0.51f to Color.Black, 1f to c(0xFF222222)),
        c(0xFF3B6FC0), c(0xFFAAAAAA), 10.dp, true, c(0xFFB9B9B9), c(0x22808080), c(0x33C83232), true),
    Skin("modern", "Modern", "Clean, flat and bright", 3,
        SolidColor(c(0xFFF2F2F7)), SolidColor(c(0xFFF9F9FB)), c(0xFF111111), Color.White,
        c(0xFF111111), c(0xFF8A8A8E), SolidColor(c(0xFF007AFF)), SolidColor(c(0xFF34C759)),
        SolidColor(c(0xFFFF9F0A)), SolidColor(c(0xFFF9F9FB)), c(0xFF007AFF), c(0xFF8A8A8E),
        16.dp, true, c(0xFFE5E5EA), c(0x22808080), c(0x33C83232)),
    Skin("material", "Material", "Android Material UI", 5,
        SolidColor(c(0xFFFEF7FF)), SolidColor(c(0xFF6750A4)), Color.White, c(0xFFF3EDF7),
        c(0xFF1D1B20), c(0xFF49454F), SolidColor(c(0xFF6750A4)), SolidColor(c(0xFF6750A4)),
        SolidColor(c(0xFFB3261E)), SolidColor(c(0xFFF3EDF7)), c(0xFF6750A4), c(0xFF49454F),
        12.dp, true, c(0xFFCAC4D0), c(0x22808080), c(0x33B3261E))
)

class Store(val ctx: Context) {
    private val p = ctx.getSharedPreferences("streak", 0)
    var name by mutableStateOf("My streak")
    val done = mutableStateMapOf<String, Int>()
    val rev = mutableStateMapOf<String, String>()
    val aw = mutableStateMapOf<String, Boolean>()
    var gems by mutableIntStateOf(0)
    var skin by mutableStateOf("ios")
    val own = mutableStateListOf("ios")
    var mon by mutableStateOf(true)
    var rm by mutableStateOf(false)
    var rh by mutableIntStateOf(20)
    var rmin by mutableIntStateOf(0)
    var vb by mutableStateOf(true)
    var start by mutableStateOf(LocalDate.now().toString())
    var awarded = 0

    init { load(); award(); save() }

    private fun dec(s: String) = s.split(",").filter { it.contains("=") }
        .map { it.substringBefore("=") to it.substringAfter("=") }

    fun load() {
        name = p.getString("name", "My streak")!!
        gems = p.getInt("gems", 0)
        skin = p.getString("skin", "ios")!!
        own.clear(); own.addAll(p.getString("own", "ios")!!.split(","))
        mon = p.getBoolean("mon", true); rm = p.getBoolean("rm", false)
        rh = p.getInt("rh", 20); rmin = p.getInt("rmin", 0); vb = p.getBoolean("vb", true)
        start = p.getString("start", LocalDate.now().toString())!!
        done.clear(); dec(p.getString("done", "")!!).forEach { done[it.first] = it.second.toIntOrNull() ?: 1 }
        rev.clear(); dec(p.getString("rev", "")!!).forEach { rev[it.first] = it.second }
        aw.clear(); p.getString("aw", "")!!.split(",").filter { it.isNotEmpty() }.forEach { aw[it] = true }
    }

    fun save() {
        p.edit().putString("name", name).putInt("gems", gems).putString("skin", skin)
            .putString("own", own.joinToString(",")).putBoolean("mon", mon).putBoolean("rm", rm)
            .putInt("rh", rh).putInt("rmin", rmin).putBoolean("vb", vb).putString("start", start)
            .putString("done", done.entries.joinToString(",") { "${it.key}=${it.value}" })
            .putString("rev", rev.entries.joinToString(",") { "${it.key}=${it.value}" })
            .putString("aw", aw.keys.joinToString(",")).apply()
    }

    fun reset() {
        p.edit().clear().apply(); Reminder.cancel(ctx)
        name = "My streak"; done.clear(); rev.clear(); aw.clear(); gems = 0; skin = "ios"
        own.clear(); own.add("ios"); mon = true; rm = false; rh = 20; rmin = 0; vb = true
        start = LocalDate.now().toString(); save()
    }

    fun today(): LocalDate = LocalDate.now()
    fun wkStart(d: LocalDate): LocalDate =
        d.minusDays(if (mon) (d.dayOfWeek.value - 1).toLong() else (d.dayOfWeek.value % 7).toLong())

    fun status(d: LocalDate) = done[d.toString()] ?: 0
    fun revAvail() = wkStart(today()).toString() !in rev

    fun award() {
        var w = wkStart(LocalDate.parse(start)); val cur = wkStart(today()); var n = 0
        while (w.isBefore(cur)) {
            val k = w.toString()
            if (k !in aw) { aw[k] = true; if (k !in rev) { gems++; n++ } }
            w = w.plusDays(7)
        }
        awarded = n
    }

    fun streaks(): Pair<Int, Int> {
        val t = today(); var cur = 0; var d = t
        if (status(d) == 0) d = d.minusDays(1)
        while (status(d) != 0) { cur++; d = d.minusDays(1) }
        var best = 0; var run = 0; var x = LocalDate.parse(start)
        while (!x.isAfter(t)) {
            if (status(x) != 0) { run++; best = maxOf(best, run) } else if (x != t) run = 0
            x = x.plusDays(1)
        }
        return cur to maxOf(best, cur)
    }

    fun toggleToday() {
        val k = today().toString()
        if (done[k] == 1) done.remove(k) else done[k] = 1
        save()
    }

    fun revive(d: LocalDate) {
        done[d.toString()] = 2; rev[wkStart(today()).toString()] = d.toString(); save()
    }
}

object Reminder {
    private fun pi(c: Context) = PendingIntent.getBroadcast(
        c, 1, Intent(c, ReminderReceiver::class.java).setAction("com.streak.app.REMIND"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(c: Context, s: Store) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, s.rh); set(Calendar.MINUTE, s.rmin); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setRepeating(
            AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi(c))
    }

    fun cancel(c: Context) = (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(c))

    fun notify(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("streak", "Streak reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(c, 2, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val title = c.getSharedPreferences("streak", 0).getString("name", "My streak")
        nm.notify(1, Notification.Builder(c, "streak").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText("Don’t break your streak! Check in today 🔥")
            .setContentIntent(open).setAutoCancel(true).build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            val s = Store(c); if (s.rm) Reminder.schedule(c, s)
        } else Reminder.notify(c)
    }
}
