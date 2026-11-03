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

## PB-02 로컬 이미지 분석 API 연결

기존 `local.properties`의 SDK 설정을 유지하고 다음 값을 추가합니다.
아래 주소는 자리표시자이며 실제 BE 실행 호스트와 포트로 바꿔야 합니다.

```properties
analysis.api.mode=REAL
analysis.api.baseUrl=http://<BE에접근가능한호스트>:<BE실행포트>/
analysis.api.mockResults=true
```

- 설정이 없으면 `FAKE` 모드이며 HTTP 요청 없이 FE 샘플 결과를 표시합니다.
- `REAL`은 Retrofit으로 요청합니다. 오류가 나도 FAKE로 전환하지 않습니다.
- `mockResults=true`는 **BE Mock 결과 안내**를 표시하는 로컬 설정입니다. 전송 방식이나 서버 응답을 바꾸지 않습니다. 현재 작업 범위에 맞춰 기본값은 `true`입니다. BE 응답에 Mock 여부 필드는 가정하지 않습니다.
- 실제 AI 결과를 반환하는 서버로 전환할 때는 `mockResults=false`로 명시합니다. 서버 주소만 바뀌고 응답 성격·계약이 같다면 Base URL만 교체하면 됩니다.
- 값은 BuildConfig에 반영되므로 변경 후 Gradle Sync 및 재빌드·재실행이 필요합니다.
- Base URL은 `/`로 끝나는 서버 루트 주소를 사용합니다. 요청 경로는 `/api/analysis`입니다.
- HTTP는 debug Manifest에서만 허용합니다. release에서는 기존 정책을 유지하므로 HTTPS 서버를 사용합니다.

접속 환경에 따라 호스트를 지정합니다.

| 환경 | 연결 방법 |
|---|---|
| Android Studio 에뮬레이터 | 호스트에 `10.0.2.2`, 포트에 실제 BE 포트 사용 |
| USB 실기기 | `adb reverse tcp:<기기포트> tcp:<BE포트>` 실행 후 `http://127.0.0.1:<기기포트>/` 사용 |
| 동일 네트워크 실기기 | 개발 PC의 LAN 주소 사용. BE 바인딩 주소와 방화벽 포트 허용 확인 |

요청은 `POST /api/analysis`, Multipart `type=IMAGE`, `file=PNG/JPEG`입니다.
현재 FE는 HTTP `202`, `isSuccess=true`, `code=ANALYSIS202`, `result.status=COMPLETED` 응답을 완료로 처리합니다.
점수·위험 수준·근거 등 선택 필드는 null이어도 표시 가능하지만, 응답 envelope와 완료 상태는 필요합니다.
GET 조회가 없으므로 `QUEUED`/`ANALYZING`을 받으면 미완료로 안내합니다. 재시도는 같은 이미지로 새 POST를 보냅니다.

REAL 결과 화면은 PB-05의 헤더·카드·점수 표시를 재사용하며 AI 점수를 SAFE/CAUTION/DANGER로 변환하지 않습니다.
오류는 이미지 준비, 서버 설정, 연결 실패, HTTP 상태, 시간 초과, 응답 형식, 분석 미완료/실패로 구분합니다.

검증 명령:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

REAL 통신 테스트는 MockWebServer를 사용하므로 BE 서버 없이 실행됩니다. 실제 BE 접속 성공을 의미하지는 않습니다.
Activity 수준의 FE Fake 화면 테스트는 FAKE 빌드에서 실행되며 REAL 빌드에서는 건너뜁니다.
실제 BE 연결 시 사진 선택과 화면 캡처 각각에 대해 BE 요청 수신, 완료 결과 표시, 실패 후 같은 이미지 재시도를 확인해야 합니다.

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
