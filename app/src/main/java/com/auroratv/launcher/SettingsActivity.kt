package com.auroratv.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class SettingsActivity : Activity() {

    private lateinit var sky: ScenicLayer
    private var u = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        u = dm.heightPixels / 1080f

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        sky = ScenicLayer(this, ScenicLayer.SKY)
        root.addView(sky, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val scrim = View(this)
        scrim.setBackgroundColor(Color.argb(175, 4, 5, 14))
        root.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        applyBackdrop()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val vpad = (dm.heightPixels * 0.07f).toInt()
        col.setPadding(0, vpad, 0, vpad)

        val title = TextView(this)
        title.text = "AuroraTV settings"
        title.setTextColor(Color.WHITE)
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 46f * u)
        title.setPadding((28 * u).toInt(), 0, 0, (20 * u).toInt())
        col.addView(title)

        for (o in Options.ALL) {
            if (o === Options.HOME_ALIAS) {
                addRow(col, o.title, o.values, { if (isAliasEnabled()) 1 else 0 }, { i -> setAlias(i == 1) })
            } else {
                addRow(col, o.title, o.values, { Prefs.get(this, o) }, { i ->
                    Prefs.set(this, o, i)
                    if (o === Options.PALETTE) applyBackdrop()
                })
            }
        }

        addAction(col, "Open TV system settings (Wi-Fi, display, sound...)") {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't open TV settings", Toast.LENGTH_SHORT).show()
            }
        }
        addAction(col, "Go to original TV home screen") { OriginalHome.launch(this) }
        addAction(col, "Done") { finish() }

        val about = TextView(this)
        about.text = "AuroraTV 0.1.0  \u00B7  no network access  \u00B7  no tracking  \u00B7  original home is one click away"
        about.setTextColor(Color.argb(150, 255, 255, 255))
        about.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * u)
        about.setPadding((28 * u).toInt(), (16 * u).toInt(), 0, 0)
        col.addView(about)

        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(
            scroll,
            FrameLayout.LayoutParams((dm.widthPixels * 0.62f).toInt(), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
        )
        setContentView(root)
    }

    private fun applyBackdrop() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        sky.palette = Palettes.forIndex(Prefs.get(this, Options.PALETTE), hour)
    }

    private fun rowBg(focused: Boolean): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = 18f * u
        if (focused) {
            d.setColor(Color.argb(70, 255, 255, 255))
            d.setStroke((3f * u).toInt().coerceAtLeast(2), Color.WHITE)
        } else {
            d.setColor(Color.argb(30, 255, 255, 255))
            d.setStroke((1f * u).toInt().coerceAtLeast(1), Color.argb(40, 255, 255, 255))
        }
        return d
    }

    private fun rowLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (84 * u).toInt())
        lp.bottomMargin = (8 * u).toInt()
        return lp
    }

    private fun makeRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.isFocusable = true
        row.isClickable = true
        row.setPadding((28 * u).toInt(), 0, (28 * u).toInt(), 0)
        row.background = rowBg(false)
        row.setOnFocusChangeListener { v, has -> v.background = rowBg(has) }
        return row
    }

    private fun addAction(col: LinearLayout, text: String, onClick: () -> Unit) {
        val row = makeRow()
        val t = TextView(this)
        t.text = text
        t.setTextColor(Color.WHITE)
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28f * u)
        row.addView(t)
        row.setOnClickListener { onClick() }
        col.addView(row, rowLp())
    }

    private fun addRow(col: LinearLayout, title: String, values: List<String>, get: () -> Int, set: (Int) -> Unit) {
        val row = makeRow()
        val t = TextView(this)
        t.text = title
        t.setTextColor(Color.WHITE)
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28f * u)
        row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val v = TextView(this)
        v.setTextColor(Color.argb(235, 255, 214, 160))
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28f * u)
        row.addView(v)

        fun refresh() {
            v.text = "\u2039  " + values[get().coerceIn(0, values.size - 1)] + "  \u203A"
        }

        fun cycle(d: Int) {
            val n = values.size
            set((get() + d + n) % n)
            refresh()
        }

        refresh()
        row.setOnClickListener { cycle(1) }
        row.setOnKeyListener { _, keyCode, ev ->
            if (ev.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                cycle(if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1)
                true
            } else {
                false
            }
        }
        col.addView(row, rowLp())
    }

    private fun aliasComponent() = ComponentName(this, "com.auroratv.launcher.HomeAlias")

    private fun isAliasEnabled(): Boolean =
        packageManager.getComponentEnabledSetting(aliasComponent()).let {
            // DEFAULT means "whatever the manifest says", and the manifest has the alias enabled.
            it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || it == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }

    private fun setAlias(on: Boolean) {
        packageManager.setComponentEnabledSetting(
            aliasComponent(),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        if (on) {
            Toast.makeText(
                this,
                "Press Home, choose AuroraTV and tap Always.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Forget any "Always use AuroraTV" choice, then show the original home screen.
            @Suppress("DEPRECATION")
            packageManager.clearPackagePreferredActivities(packageName)
            Toast.makeText(this, "AuroraTV is no longer your Home screen.", Toast.LENGTH_LONG).show()
            OriginalHome.launch(this)
        }
    }
}
