# PB-09 발표용 캡처

이슈 #6의 발표 자료다. 현재 앱의 Compose 화면을 샘플 상태로 렌더링해 PNG 12장을 캡처했다. 모든 이미지 하단에 **UI 미리보기 · 샘플 인증**과 **실제 API 미연결**을 표시했다.

- 재촬영: **2026-10-17**.
- 코드 기준: `fix/6-auth-ui-qa`, `65e09ac` 기반의 로컬 QA 수정사항 포함.
- 최신 로그인 화면의 뒤로가기와 캐릭터·로고, 현재 약관·이메일·가입 정보·완료 팝업을 반영했다.
- `AuthPresentationCaptureTest` 1개 통과로 12장을 생성했고, 12장 모두 눈으로 확인했다.
- 분류: 정상 7장 / 오류 4장 / 완료 1장. 이 폴더의 PNG는 이전 발표 캡처를 교체한 결과다.

실제 로그인·이메일 발송·가입 성공 증빙이 아니다. 입력값은 샘플이며 비밀번호는 가린 상태다. 키보드를 열지 않아 클립보드 추천 문구가 포함되지 않는다.

- 캡처 환경: Android 16 / API 36 에뮬레이터
- PNG 크기: 780 × 2160px, 논리 화면 390 × 1080dp
- 한눈에 보기: [발표용 갤러리](index.html)
- 이전 `../captures/`의 진단용 캡처는 발표 자료에서 제외한다.

| 번호 | 화면 | 분류 | PNG |
| --- | --- | --- | --- |
| 01 | 로그인 입력 전 | 정상 | [열기](01-login-empty.png) |
| 02 | 로그인 입력 후 | 정상 | [열기](02-login-filled.png) |
| 03 | 로그인 인증 오류 | 오류 | [열기](03-login-error.png) |
| 04 | 약관 동의 전 | 정상 | [열기](04-terms-initial.png) |
| 05 | 필수 약관 3개 동의 | 정상 | [열기](05-terms-required.png) |
| 06 | 이메일 인증 입력 전 | 정상 | [열기](06-email-initial.png) |
| 07 | 인증번호 발송 후 | 정상 | [열기](07-email-sent.png) |
| 08 | 인증번호 불일치 | 오류 | [열기](08-email-error.png) |
| 09 | 인증 시간 만료 | 오류 | [열기](09-email-expired.png) |
| 10 | 비밀번호·닉네임 조건 충족 | 정상 | [열기](10-credentials-valid.png) |
| 11 | 비밀번호 불일치·닉네임 초과 | 오류 | [열기](11-credentials-error.png) |
| 12 | 회원가입 완료 팝업 | 완료 | [열기](12-signup-completed.png) |

발표의 기본 흐름은 **02 → 05 → 07 → 10 → 12**, 오류 처리 설명에는 **03·08·09·11**을 사용한다.

## 재생성

`AuthPresentationCaptureTest`는 실제 앱 화면 컴포넌트와 입력 상태를 사용한다. API 요청이나 키보드 입력은 수행하지 않고, 각 상태의 문구 표시와 키보드 숨김을 검사한 뒤 화면 전환이 끝나면 캡처한다. 사진을 편집하거나 기존 화면에 문구를 합성하지 않는다. 하단 표시는 캡처용 테스트 프레임에서 렌더링한다.

PowerShell에서 저장소 루트를 기준으로 실행한다. 캡처 전 에뮬레이터의 기존 크기·밀도·회전 설정을 기록하고, 캡처 후 복원한다.

```powershell
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
& $adb shell wm size 780x2160
& $adb shell wm density 320
& $adb shell settings put system accelerometer_rotation 0
& $adb shell settings put system user_rotation 0
$env:JAVA_HOME = 'D:\Android\Jdk\temurin-17'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dundueni.app.feature.auth.AuthPresentationCaptureTest' '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true' --console=plain
```

PNG는 에뮬레이터의 `/sdcard/Android/data/com.dundueni.app/files/pb09-presentation/`에 생성된다. 로컬로 복사해 시각 검토한 뒤 사용한다. 이번 캡처 후에는 화면 크기·밀도를 기기 기본값, 글자 크기를 100%, 회전을 기존 자동 설정으로 복원했다. 화면 키보드 표시 설정은 유지했다.
