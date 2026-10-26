package com.auroratv.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppEntry(
    val label: String,
    val icon: Drawable?,
    val intent: Intent?,
    val isSettings: Boolean = false
)

class DockAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onFocused: (Int, View) -> Unit
) : RecyclerView.Adapter<DockAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()

    class VH(
        val item: LinearLayout,
        val plate: FrameLayout,
        val icon: ImageView,
        val label: TextView
    ) : RecyclerView.ViewHolder(item)

    fun submit(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    private fun plateBg(focused: Boolean, u: Float): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = 22f * u
        if (focused) {
            d.setColor(Color.argb(80, 255, 255, 255))
            d.setStroke((3f * u).toInt().coerceAtLeast(2), Color.WHITE)
        } else {
            d.setColor(Color.argb(40, 255, 255, 255))
            d.setStroke((1f * u).toInt().coerceAtLeast(1), Color.argb(50, 255, 255, 255))
        }
        return d
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val u = ctx.resources.displayMetrics.heightPixels / 1080f
        val cell = (132 * u).toInt()
        val plateSize = (100 * u).toInt()

        val item = LinearLayout(ctx)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.layoutParams = RecyclerView.LayoutParams(cell, ViewGroup.LayoutParams.WRAP_CONTENT)
        item.isFocusable = true
        item.isClickable = true
        item.clipChildren = false

        val plate = FrameLayout(ctx)
        plate.layoutParams = LinearLayout.LayoutParams(plateSize, plateSize)
        plate.background = plateBg(false, u)
        plate.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 22f * u)
            }
        }
        plate.clipToOutline = true

        val icon = ImageView(ctx)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        val ip = (12 * u).toInt()
        icon.setPadding(ip, ip, ip, ip)
        plate.addView(icon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val label = TextView(ctx)
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * u)
        label.setTextColor(Color.WHITE)
        label.gravity = Gravity.CENTER
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        label.alpha = 0f
        label.setShadowLayer(6f * u, 0f, 2f * u, Color.argb(160, 0, 0, 0))

        val labelLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        labelLp.topMargin = (8 * u).toInt()
        item.addView(plate)
        item.addView(label, labelLp)

        val vh = VH(item, plate, icon, label)

        item.setOnClickListener {
            val p = vh.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onClick(items[p])
        }
        item.setOnFocusChangeListener { v, has ->
            val s = if (has) 1.16f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(170).start()
            plate.background = plateBg(has, u)
            label.animate().alpha(if (has) 1f else 0f).setDuration(150).start()
            if (has) {
                val p = vh.bindingAdapterPosition
                if (p != RecyclerView.NO_POSITION) onFocused(p, v)
            }
        }
        return vh
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.icon.setImageDrawable(e.icon)
        holder.label.text = e.label
        holder.item.contentDescription = e.label
        if (!holder.item.isFocused) {
            holder.item.scaleX = 1f
            holder.item.scaleY = 1f
            holder.label.alpha = 0f
        }
    }
}
