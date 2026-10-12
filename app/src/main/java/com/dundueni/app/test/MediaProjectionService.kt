package com.dundueni.app.test


/*
    Foreground Service 실행
        ↓
    resultCode + data 받기
        ↓
    MediaProjection 객체 생성
        ↓
    화면 크기 확인
        ↓
    ImageReader 생성
        ↓
    ImageReader 리스너 등록
        ↓
    VirtualDisplay 생성
        ↓
    MediaProjection
        ↓
    VirtualDisplay
        ↓
    ImageReader.surface
        ↓
    실제 화면 Image 수신
        ↓
    Image → Bitmap 변환
*/


