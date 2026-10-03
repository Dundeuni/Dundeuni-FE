package com.dundueni.app.feature.floatingbutton

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import com.dundueni.app.R

object FloatingButtonView {

    fun create(context: Context): View {

        return ImageButton(context).apply {

            // 든든이 캐릭터 이미지 사용
            setImageResource(R.drawable.dundeuni_mascot_default)

            // 기본 버튼 배경 제거
            background = null

            // 이미지가 버튼 영역 안에 맞게 표시
            scaleType = ImageView.ScaleType.FIT_CENTER

            // 불필요한 내부 여백 제거
            setPadding(0, 0, 0, 0)

            // 클릭 동작
            setOnClickListener {
                // 나중에 MediaProjection 연결
            }
        }
    }
}