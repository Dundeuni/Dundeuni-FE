package com.dundueni.app.feature.floatingbutton.view

import android.content.Context
import android.widget.ImageButton
import android.widget.ImageView
import com.dundueni.app.R

/**
 * 화면 위에 표시할 캐릭터 ImageButton을 만드는 객체다.
 * Context와 클릭 시 실행할 동작을 받아 버튼 View를 반환한다.
 * 버튼의 이미지와 접근성 설명을 구성하고 클릭을 서비스에 전달한다.
 * 실제 화면 표시와 메뉴 열기·닫기는 FloatingButtonService가 담당한다.
 * 메뉴 상태에 따라 캐릭터와 닫기 아이콘을 바꾸며, 서비스를 종료하지는 않는다.
 */
object FloatingButtonView {

    fun create(context: Context, onClick: () -> Unit): ImageButton {

        return ImageButton(context).apply {

            // 든든이 캐릭터 이미지 사용
            setImageResource(R.drawable.dundeuni_mascot_default)
            contentDescription = "메뉴 열기"

            // 기본 버튼 배경 제거
            background = null

            // 이미지가 버튼 영역 안에 맞게 표시
            scaleType = ImageView.ScaleType.FIT_CENTER

            // 불필요한 내부 여백 제거
            setPadding(0, 0, 0, 0)

            // 클릭 동작
            setOnClickListener {
                onClick()
            }
        }
    }

    fun setMenuOpen(button: ImageButton, isOpen: Boolean) {
        button.setImageResource(
            if (isOpen) R.drawable.ic_floating_close else R.drawable.dundeuni_mascot_default
        )
        button.contentDescription = if (isOpen) "메뉴 닫기" else "메뉴 열기"
    }
}
