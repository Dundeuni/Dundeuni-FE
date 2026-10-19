package com.dundueni.app.feature.floatingbutton.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * 플로팅 버튼을 눌렀을 때 검사 방식을 선택할 메뉴 View를 만든다.
 * 화면 검사·이미지 검사·빈 항목의 클릭 동작을 외부에서 전달받는다.
 * 각 항목을 하나의 컨테이너에 배치해 FloatingButtonService에 반환한다.
 * 메뉴를 화면에 붙이거나 검사 흐름을 실행하는 일은 서비스가 맡는다.
 * 빈 항목은 아직 기능이 정해지지 않은 자리이며 현재 서비스의 동작도 비어 있다.
 */
object FloatingMenuView {

    fun create(
        context: Context,
        onScreenScanClick: () -> Unit,
        onImageScanClick: () -> Unit,
        onEmptyClick: () -> Unit
    ): View {

        val density = context.resources.displayMetrics.density

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                (300 * density).toInt(),
                (280 * density).toInt()
            )

            setBackgroundColor(Color.TRANSPARENT)
        }

        // AI 화면 검사
        val screenScanButton = createMenuButton(
            context = context,
            text = "AI 화면 검사"
        ).apply {

            layoutParams = FrameLayout.LayoutParams(
                (150 * density).toInt(),
                (56 * density).toInt()
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                rightMargin = (70 * density).toInt()
                topMargin = (25 * density).toInt()
            }

            setOnClickListener {
                onScreenScanClick()
            }
        }

        // 이미지 검사
        val imageScanButton = createMenuButton(
            context = context,
            text = "이미지 검사"
        ).apply {

            layoutParams = FrameLayout.LayoutParams(
                (140 * density).toInt(),
                (56 * density).toInt()
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                rightMargin = (120 * density).toInt()
            }

            setOnClickListener {
                onImageScanClick()
            }
        }

        // 빈 버튼
        val emptyButton = createMenuButton(
            context = context,
            text = ""
        ).apply {

            layoutParams = FrameLayout.LayoutParams(
                (130 * density).toInt(),
                (56 * density).toInt()
            ).apply {
                gravity = Gravity.END or Gravity.BOTTOM
                rightMargin = (70 * density).toInt()
                bottomMargin = (25 * density).toInt()
            }

            setOnClickListener {
                onEmptyClick()
            }
        }

        // 항목을 하나의 메뉴로 묶어 서비스가 창 단위로 열고 닫을 수 있게 한다.
        container.addView(screenScanButton)
        container.addView(imageScanButton)
        container.addView(emptyButton)

        return container
    }


    private fun createMenuButton(
        context: Context,
        text: String
    ): TextView {

        val density = context.resources.displayMetrics.density

        return TextView(context).apply {

            this.text = text

            textSize = 18f
            setTextColor(Color.rgb(17, 27, 43))

            gravity = Gravity.CENTER

            elevation = 6 * density

            isClickable = true
            isFocusable = true

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28 * density
            }
        }
    }
}