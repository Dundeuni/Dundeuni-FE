package com.dundueni.app.feature.floatingbutton.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dundueni.app.R

/** Floating inspection actions arranged around the separately-windowed toggle button. */
object FloatingMenuView {

    fun create(
        context: Context,
        opensTowardLeft: Boolean,
        onScreenScanClick: () -> Unit,
        onImageScanClick: () -> Unit,
        onTextScanClick: () -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(300), dp(280))
            setBackgroundColor(Color.TRANSPARENT)
            pivotX = if (opensTowardLeft) dp(300).toFloat() else 0f
            pivotY = dp(140).toFloat()
        }

        fun addItem(label: String, icon: Int, top: Int, inset: Int, onClick: () -> Unit) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
            val pill = TextView(context).apply {
                text = label
                textSize = 18f
                setTextColor(Color.rgb(18, 28, 44))
                gravity = Gravity.CENTER
                includeFontPadding = false
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(999).toFloat()
                }
                elevation = dp(4).toFloat()
            }
            val iconBubble = FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
                background = GradientDrawable().apply {
                    setColor(Color.rgb(225, 224, 255))
                    shape = GradientDrawable.OVAL
                }
                elevation = dp(4).toFloat()
                contentDescription = label
            }
            val iconView = ImageView(context).apply {
                setImageResource(icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                isDuplicateParentStateEnabled = true
                layoutParams = FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER)
            }
            iconBubble.addView(iconView)

            val gap = View(context).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) }
            if (opensTowardLeft) {
                row.addView(pill)
                row.addView(gap)
                row.addView(iconBubble, LinearLayout.LayoutParams(dp(48), dp(48)))
            } else {
                row.addView(iconBubble, LinearLayout.LayoutParams(dp(48), dp(48)))
                row.addView(gap)
                row.addView(pill)
            }

            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(48)).apply {
                gravity = (if (opensTowardLeft) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP
                topMargin = dp(top)
                if (opensTowardLeft) rightMargin = dp(inset) else leftMargin = dp(inset)
            }
            container.addView(row, params)
        }

        // Follow the Figma thumb-arch: the middle action sits farther from the button.
        val middleTop = (280 - 48) / 2
        val verticalOffset = 62
        addItem("AI 화면 검사", R.drawable.menu_icon_screen, middleTop - verticalOffset, 0, onScreenScanClick)
        addItem("이미지 검사", R.drawable.menu_icon_image, middleTop, 28, onImageScanClick)
        addItem("텍스트 검사", R.drawable.menu_icon_text, middleTop + verticalOffset, 30, onTextScanClick)
        return container
    }
}
