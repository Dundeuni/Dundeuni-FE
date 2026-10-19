# Dundueni Android Front-End

디지털 안전 서비스 **든든이(Dundueni)** 의 Android Front-End입니다.

사용자가 의심스러운 화면·사진·텍스트를 검사할 수 있도록  
**검사 입력 수집, Android 시스템 기능 연동, BE 분석 요청, 결과 표시 및 앱 UI**를 담당합니다.

```text
의심 콘텐츠 발견
→ Floating Button / 앱에서 검사 실행
→ 화면·사진·텍스트 입력 수집
→ BE에 분석 요청
→ 분석 결과 수신
→ 결과·대응 방법 표시 / 기록 / 공유
```

---

## FE 담당 범위

```text
0. FE 공통 기반

1. 화면 검사
2. 사진 검사
3. 텍스트 검사
4. 분석 결과
   └── 결과 공유 / 외부 연동

5. 홈
6. 온보딩
7. 로그인 / 회원가입
8. 검사 기록
9. 설정

10. 통합 / QA / 안정화
```

화면 검사는 `Floating Button + MediaProjection`,  
사진 검사는 `Photo Picker`를 주요 입력 방식으로 사용합니다.

각 입력은 FE에서 수집·가공한 뒤 BE에 전달하고, 분석 결과를 받아 UI에 표시합니다.

---

## 기본 구조

```text
com.dundueni.app
├── data/
│   ├── model/          데이터 모델 / Request·Response
│   └── remote/         BE API / 네트워크 통신
│
├── feature/
│   ├── capture/        MediaProjection / Photo Picker / 검사 입력
│   └── floatingbutton/ Overlay / Floating Button
│
├── navigation/         화면 이동 / Route
│
├── ui/
│   ├── component/      공통 Compose UI
│   ├── screen/         앱 화면
│   └── theme/          Color / Typography / Shape 등
│
├── test/               기능 검증용 임시 코드
│
└── MainActivity.kt     앱 진입점
```

새로운 코드는 **파일명이 아니라 역할을 기준으로 해당 Package에 배치**합니다.

`test/`는 MediaProjection, Photo Picker 등 기능을 실제 Flow에 연결하기 전 검증하기 위한 임시 영역입니다.

---

## 기술 스택

- Kotlin, Jetpack Compose, Material 3
- Android SDK, Navigation Compose
- Retrofit, OkHttp
- SYSTEM_ALERT_WINDOW / Foreground Service
- MediaProjection / Photo Picker
- **minSdk: API 26 (Android 8.0+)**

`minSdk 26` 결정 배경은 [`docs/minSdk-decision.md`](./docs/minSdk-decision.md)를 참고합니다.

---

## 협업

- FE 협업 규칙: [`FEConventions.md`](./FEConventions.md)

```text
main: 배포 / 데모 기준
dev: 개발 통합

feature/*
fix/*
refactor/*
docs/*
chore/*
→ dev를 대상으로 PR
```

기본 Workflow:

```text
Issue
→ dev에서 작업 Branch 생성
→ 개발 / Commit
→ dev PR
→ Review
→ Squash Merge
```