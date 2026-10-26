package com.auroratv.launcher

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.auroratv.launcher.assistant.Assistants
import java.util.Calendar

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class HomeActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var sky: ScenicLayer
    private lateinit var far: ScenicLayer
    private lateinit var mid: ScenicLayer
    private lateinit var fg: ScenicLayer
    private lateinit var clock: TextClock
    private lateinit var dateView: TextClock
    private lateinit var dockWrap: FrameLayout
    private lateinit var dock: RecyclerView
    private lateinit var adapter: DockAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val drift = ArrayList<ObjectAnimator>()

    private var unit = 1f
    private var lastW = 0
    private var lastH = 0
    private var ambient = false
    private var parallaxOn = true
    private var itemCount = 0
    private var lastFocusPos = 0
    private var clockOffset = 0f
    private var currentPalette: Palette? = null
    private var receiverRegistered = false

    private val idleRunnable = Runnable { enterAmbient() }
    private val reloadRunnable = Runnable { loadApps() }
    private val paletteTick = object : Runnable {
        override fun run() {
            applyPalette()
            handler.postDelayed(this, 60_000L)
        }
    }
    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.removeCallbacks(reloadRunnable)
            handler.postDelayed(reloadRunnable, 700L)
        }
    }

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unit = resources.displayMetrics.heightPixels / 1080f

        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        root.clipChildren = false
        setContentView(root)

        sky = ScenicLayer(this, ScenicLayer.SKY)
        far = ScenicLayer(this, ScenicLayer.FAR)
        mid = ScenicLayer(this, ScenicLayer.MID)
        fg = ScenicLayer(this, ScenicLayer.FG)

        clock = TextClock(this)
        clock.setIncludeFontPadding(false)
        clock.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        clock.setTextColor(Color.argb(235, 255, 255, 255))
        clock.gravity = Gravity.CENTER
        clock.setShadowLayer(14f * unit, 0f, 4f * unit, Color.argb(90, 0, 0, 0))

        dateView = TextClock(this)
        dateView.format12Hour = "EEEE d MMMM"
        dateView.format24Hour = "EEEE d MMMM"
        dateView.setTextColor(Color.argb(225, 255, 255, 255))
        dateView.setShadowLayer(8f * unit, 0f, 2f * unit, Color.argb(140, 0, 0, 0))

        // Z-order, back to front: sky, far, mid, CLOCK, foreground ridge, date, dock.
        root.addView(sky, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(far, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(mid, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(clock, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        root.addView(fg, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(dateView, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START))

        adapter = DockAdapter({ entry -> launch(entry) }, { pos, view -> onDockFocus(pos, view) })
        dock = RecyclerView(this)
        dock.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        dock.adapter = adapter
        dock.clipChildren = false
        dock.clipToPadding = false
        dock.itemAnimator = null
        dock.overScrollMode = View.OVER_SCROLL_NEVER

        dockWrap = FrameLayout(this)
        dockWrap.clipChildren = false
        dockWrap.clipToPadding = false
        dockWrap.addView(dock, FrameLayout.LayoutParams(MATCH, WRAP))
        root.addView(dockWrap, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))

        root.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            val w = r - l
            val h = b - t
            if (w > 0 && h > 0 && (w != lastW || h != lastH)) {
                lastW = w
                lastH = h
                applyMetrics(w, h)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            val f = IntentFilter()
            f.addAction(Intent.ACTION_PACKAGE_ADDED)
            f.addAction(Intent.ACTION_PACKAGE_REMOVED)
            f.addAction(Intent.ACTION_PACKAGE_REPLACED)
            f.addDataScheme("package")
            ContextCompat.registerReceiver(this, pkgReceiver, f, ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
        }
    }

    override fun onResume() {
        super.onResume()
        applyPrefs()
        loadApps()
        scheduleIdle()
        handler.removeCallbacks(paletteTick)
        handler.postDelayed(paletteTick, 60_000L)
    }

    override fun onPause() {
        super.onPause()
        // Stop everything so the TV can sleep normally; nothing runs while we are in the background.
        handler.removeCallbacks(idleRunnable)
        handler.removeCallbacks(paletteTick)
        leaveAmbient(false)
        stopDrift()
    }

    override fun onStop() {
        super.onStop()
        if (receiverRegistered) {
            unregisterReceiver(pkgReceiver)
            receiverRegistered = false
        }
    }

    // ---------------------------------------------------------------- layout & style

    private fun applyMetrics(w: Int, h: Int) {
        val safeX = (w * 0.05f).toInt()
        val safeY = (h * 0.05f).toInt()

        // Slightly widen the layers so parallax never reveals an edge.
        for (l in listOf(sky, far, mid, fg)) l.scaleX = 1.10f

        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.31f)
        updateClockMargin()

        dateView.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.028f)
        val dlp = dateView.layoutParams as FrameLayout.LayoutParams
        dlp.topMargin = safeY
        dlp.leftMargin = safeX
        dateView.layoutParams = dlp

        val padX = (24 * unit).toInt()
        dockWrap.setPadding(padX, (16 * unit).toInt(), padX, (14 * unit).toInt())
        dockWrap.background = glassDrawable()
        val lp = dockWrap.layoutParams as FrameLayout.LayoutParams
        lp.bottomMargin = safeY
        dockWrap.layoutParams = lp
        updateDockWidth()
    }

    private fun updateClockMargin() {
        if (lastH == 0) return
        val lp = clock.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = ((0.162f + clockOffset) * lastH).toInt()
        clock.layoutParams = lp
    }

    private fun updateDockWidth() {
        if (lastW == 0) return
        val cell = (132 * unit).toInt()
        val pad = (24 * unit).toInt()
        val want = itemCount * cell + pad * 2
        val max = (lastW * 0.84f).toInt()
        val lp = dockWrap.layoutParams as FrameLayout.LayoutParams
        lp.width = want.coerceIn(cell + pad * 2, max)
        dockWrap.layoutParams = lp
    }

    /** Frosted-glass look: translucent dark tint, light sheen, thin bright edge. (Tint only, no backdrop blur.) */
    private fun glassDrawable(): Drawable {
        val r = 44f * unit
        val base = GradientDrawable()
        base.shape = GradientDrawable.RECTANGLE
        base.cornerRadius = r
        base.setColor(Color.argb(120, 14, 14, 28))
        base.setStroke((1.5f * unit).toInt().coerceAtLeast(1), Color.argb(80, 255, 255, 255))
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(60, 255, 255, 255), Color.argb(8, 255, 255, 255))
        )
        sheen.shape = GradientDrawable.RECTANGLE
        sheen.cornerRadius = r
        return LayerDrawable(arrayOf(base, sheen))
    }

    private fun applyPrefs() {
        parallaxOn = Prefs.get(this, Options.PARALLAX) == 0

        when (Prefs.get(this, Options.CLOCK_FORMAT)) {
            1 -> {
                clock.format12Hour = "h:mm"
                clock.format24Hour = "h:mm"
            }
            2 -> {
                clock.format12Hour = "HH:mm"
                clock.format24Hour = "HH:mm"
            }
            else -> {
                clock.format12Hour = "h:mm"
                clock.format24Hour = "HH:mm"
            }
        }
        dateView.visibility = if (Prefs.get(this, Options.SHOW_DATE) == 0) View.VISIBLE else View.GONE

        clockOffset = when (Prefs.get(this, Options.CLOCK_HEIGHT)) {
            0 -> 0.04f
            2 -> -0.04f
            else -> 0f
        }
        updateClockMargin()

        if (!parallaxOn) {
            for (v in listOf<View>(sky, far, mid, clock, fg)) v.translationX = 0f
        }
        applyPalette()
    }

    private fun applyPalette() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val p = Palettes.forIndex(Prefs.get(this, Options.PALETTE), hour)
        if (p === currentPalette) return
        currentPalette = p
        sky.palette = p
        far.palette = p
        mid.palette = p
        fg.palette = p
    }

    // ---------------------------------------------------------------- apps

    private fun loadApps() {
        val includeAll = Prefs.get(this, Options.ALL_APPS) == 1
        Thread {
            val pm = packageManager
            val found = LinkedHashMap<String, AppEntry>()

            fun collect(category: String) {
                val query = Intent(Intent.ACTION_MAIN).addCategory(category)
                val results = try {
                    pm.queryIntentActivities(query, 0)
                } catch (e: Exception) {
                    emptyList()
                }
                for (ri in results) {
                    val ai = ri.activityInfo ?: continue
                    if (ai.packageName == packageName) continue
                    if (found.containsKey(ai.packageName)) continue
                    val launch = Intent(Intent.ACTION_MAIN)
                        .addCategory(category)
                        .setComponent(ComponentName(ai.packageName, ai.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    val label = try { ri.loadLabel(pm).toString() } catch (e: Exception) { ai.packageName }
                    val icon = try { ri.loadIcon(pm) } catch (e: Exception) { null }
                    found[ai.packageName] = AppEntry(label, icon, launch)
                }
            }

            collect(Intent.CATEGORY_LEANBACK_LAUNCHER)
            if (includeAll) collect(Intent.CATEGORY_LAUNCHER)

            val list = ArrayList<AppEntry>(found.values)
            list.sortBy { it.label.lowercase() }
            val tvSettings = Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            list.add(AppEntry("TV Settings", ContextCompat.getDrawable(this, R.drawable.ic_tv_settings), tvSettings))
            list.add(AppEntry("AuroraTV", ContextCompat.getDrawable(this, R.drawable.ic_settings_sliders), null, true))

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                itemCount = list.size
                adapter.submit(list)
                updateDockWidth()
                if (!ambient) restoreFocus()
            }
        }.start()
    }

    private fun launch(entry: AppEntry) {
        if (entry.isSettings) {
            openSettings()
            return
        }
        try {
            startActivity(entry.intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open " + entry.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun restoreFocus() {
        dock.post {
            val pos = lastFocusPos.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
            dock.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
        }
    }

    // ---------------------------------------------------------------- focus & parallax

    private fun onDockFocus(pos: Int, v: View) {
        lastFocusPos = pos
        val dx = (v.left + v.width / 2) - dock.width / 2
        if (dx != 0) dock.smoothScrollBy(dx, 0)
        applyParallax(pos)
    }

    private fun applyParallax(pos: Int) {
        if (!parallaxOn || itemCount < 2 || lastW == 0) return
        val frac = pos.toFloat() / (itemCount - 1) - 0.5f
        fun mv(v: View, f: Float) {
            v.animate()
                .translationX(-frac * f * lastW)
                .setDuration(700)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        mv(sky, 0.010f)
        mv(far, 0.025f)
        mv(mid, 0.045f)
        mv(clock, 0.060f)
        mv(fg, 0.090f)
    }

    // ---------------------------------------------------------------- ambient mode

    private fun scheduleIdle() {
        handler.removeCallbacks(idleRunnable)
        val ms = Options.ambientMillis(Prefs.get(this, Options.AMBIENT))
        if (ms > 0L && !ambient) handler.postDelayed(idleRunnable, ms)
    }

    private fun enterAmbient() {
        if (ambient) return
        ambient = true
        dockWrap.animate().alpha(0f).setDuration(900).withEndAction {
            if (ambient) dockWrap.visibility = View.INVISIBLE
        }.start()
        dateView.animate().alpha(0.6f).setDuration(900).start()
        if (Prefs.get(this, Options.DRIFT) == 0) startDrift()
    }

    private fun leaveAmbient(animated: Boolean) {
        if (!ambient) return
        ambient = false
        stopDrift()
        val d = if (animated) 350L else 0L
        dockWrap.visibility = View.VISIBLE
        dockWrap.animate().alpha(1f).setDuration(d).start()
        dateView.animate().alpha(1f).setDuration(d).start()
        restoreFocus()
    }

    private fun startDrift() {
        stopDrift()
        val u = unit
        val specs = listOf(
            Triple(sky as View, 4f, 47000L),
            Triple(far as View, 7f, 39000L),
            Triple(mid as View, 10f, 33000L),
            Triple(clock as View, 9f, 41000L),
            Triple(fg as View, 13f, 29000L)
        )
        for ((v, amp, dur) in specs) {
            val a = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, -amp * u, amp * u)
            a.duration = dur
            a.repeatMode = ValueAnimator.REVERSE
            a.repeatCount = ValueAnimator.INFINITE
            a.interpolator = AccelerateDecelerateInterpolator()
            a.start()
            drift.add(a)
        }
    }

    private fun stopDrift() {
        if (drift.isEmpty()) return
        for (a in drift) a.cancel()
        drift.clear()
        for (v in listOf<View>(sky, far, mid, clock, fg)) {
            v.animate().translationY(0f).setDuration(600).start()
        }
    }

    // ---------------------------------------------------------------- input

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ambient) {
            // First key press only wakes the screen; it is not passed on to the dock.
            if (event.action == KeyEvent.ACTION_DOWN) {
                leaveAmbient(true)
                scheduleIdle()
            }
            return true
        }
        scheduleIdle()
        return super.dispatchKeyEvent(event)
    }

    private fun runningAsHome(): Boolean = intent?.hasCategory(Intent.CATEGORY_HOME) == true

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (ambient) {
            leaveAmbient(true)
            scheduleIdle()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // As the Home screen there is nothing to go back to; as a normal app, Back leaves as usual.
        if (runningAsHome()) return
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                openSettings()
                return true
            }
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST -> {
                if (Assistants.provider.onVoiceKey(this)) return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
