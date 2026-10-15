# PB-09 인증 UI QA

2026-10-15, `feature/4-auth-ui` 로컬 작업 기준. API 36 에뮬레이터에서 debug 샘플로 확인했다. 실제 이메일 발송·계정 생성·토큰 저장·인증 복원은 검증 범위 밖이다.

## 발견 및 수정

| 항목 | 문제 | 수정 및 검증 |
| --- | --- | --- |
| 이메일 인증 후 이동 | ViewModel의 비동기 응답 콜백이 이전 Activity를 참조해 회전·백그라운드 전환 중 이동할 수 있었다. | 이동 요청을 상태에 보관하고 현재 Activity가 RESUMED일 때 한 번 소비한다. 뒤로 돌아왔을 때는 다음 버튼을 다시 눌러야 이동한다. 상태 회귀 테스트와 Activity 이동·회전 테스트 통과. |
| 로그인 처리 중 보기 버튼 | 입력과 제출은 잠겼지만 비밀번호 보기 버튼과 ViewModel 토글은 활성 상태였다. | UI와 ViewModel 양쪽에서 처리 중 토글을 차단한다. 지연 응답 테스트와 UI 비활성 검사 통과. |
| 큰 글자에서 입력·버튼 표시 | 글자 크기 150%에서 인증번호 받기 문구가 고정 너비 버튼 가장자리에 붙었다. 필드 고정 높이도 더 큰 글자에 대응하지 못했다. | 130% 초과 시 이메일 필드와 발송 버튼을 세로 배치한다. 필드와 가입 완료 버튼은 최소 높이를 유지하면서 내용에 따라 늘어난다. |
| 키보드에서 로그인 버튼 접근 | 키보드를 열면 로고가 공간을 차지해 로그인 버튼까지 스크롤해야 했다. | 키보드 표시 중 로고 영역을 접고 로그인 버튼을 키보드 위에 고정한다. 입력 영역에는 버튼 높이만큼 여백을 확보한다. |
| 가로 화면의 회원가입 입력칸 | 키보드·고정 헤더·하단 여백이 본문 공간을 모두 차지해 입력칸이 사라졌다. | 키보드 표시 중 헤더는 48dp, 하단 여백은 위·아래 8dp로 줄인다. debug 설명은 키보드를 닫으면 다시 표시한다. 입력칸과 하단 버튼의 경계를 실제 IME 경계와 비교한다. |
| 이메일 키보드 완료 동작 | 번호 발송 전 Next는 입력 불가능한 인증번호 칸으로 이동할 수 없었다. | 발송 전에는 Done으로 발송을 요청하고, 유효한 번호가 있는 동안에는 Next로 인증번호 칸에 이동한다. |

## 확인 결과

| 흐름·상태 | 확인 방식 | 결과 |
| --- | --- | --- |
| 로그인 빈 이메일·비밀번호, 이메일 형식, 샘플 인증 실패, 입력 수정 시 오류 해제 | UI·ViewModel 테스트 / 수동 빈 입력 | 통과 |
| 필수 3개 동의, 선택 동의 없이 진행, 전체 선택·해제, 전문 버튼, 뒤로가기 | 상태·UI 테스트 / 실제 Activity 흐름 | 통과 |
| 이메일 형식·빈 입력, 숫자 6자리, 중복 요청 차단 | UI·ViewModel 테스트 / 수동 빈 입력 | 통과 |
| 인증번호 불일치, 수정, 재발송 시 번호·오류 초기화 | 에뮬레이터 흐름 테스트 / 수동 | 통과 |
| 앱 복귀 시 타이머 재계산, 만료 후 다음 단계 차단 | ViewModel 테스트 / 실제 180초 경과 후 에뮬레이터 복귀 | 통과 |
| 이메일 변경 시 기존 인증 상태·요청 무효화 | ViewModel 테스트 | 통과 |
| 비밀번호 조건·일치·표시/숨김, 닉네임 초과·공백, 오류 수정 시 완료 활성화 | UI·ViewModel 테스트 / 에뮬레이터 흐름 테스트 | 통과 |
| 이메일 인증 및 3단계에서 회전 후 입력 상태 유지 | 에뮬레이터 흐름 테스트 | 통과 |
| 3단계 → 이메일 → 약관 뒤로가기, 약관 선택 유지 | 에뮬레이터 흐름 테스트 | 통과 |
| 완료 팝업의 입력 이메일·닉네임, 홈 버튼으로 기존 시작 화면 복귀 | 에뮬레이터 흐름 테스트 | 통과 |
| 실제 연결이 없는 상태에서 성공·이메일 발송을 표시하지 않음 | ViewModel 테스트 | 통과 |
| 큰 글자 150%, 세로·가로 전환, 발송 버튼 문구와 입력칸 접근 | 수정 후 에뮬레이터 화면 확인 | 통과 |
| 실제 소프트 키보드의 세로·가로 화면에서 로그인·이메일·인증번호·비밀번호·확인·닉네임 입력칸과 하단 버튼 접근 | 에뮬레이터에서 전체 노드 높이와 IME 상단 좌표 비교 | 통과 |
| 로그인 Next/Done, 이메일 발송 Done, 인증번호 Done, 비밀번호·확인 Next, 닉네임 Done | 에뮬레이터 키보드 동작 테스트 | 통과 |

인증 로컬 테스트 29개와 에뮬레이터 Activity 흐름·키보드 테스트 4개를 통과했다. debug·release APK 빌드도 통과했다. 실제 API 연결 후 정상·실패 응답과 화면 이동을 다시 점검해야 한다.

## 재실행

PowerShell에서 저장소 루트 기준으로 실행한다. 에뮬레이터는 미리 켜 둔다.

```powershell
$env:JAVA_HOME = 'D:\Android\Jdk\temurin-17'
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest --tests 'com.dundueni.app.feature.auth.*' --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dundueni.app.feature.auth.AuthFlowQaTest' --console=plain
```

에뮬레이터 테스트는 테스트 종료 후 앱을 제거할 수 있다. 다시 확인하려면 Android Studio에서 app debug를 실행한다.

키보드 테스트는 API 30 이상에서 실행하며, Gboard의 화면 키보드 표시를 켜야 한다. 키보드 애니메이션이 끝난 뒤 입력칸·버튼 전체 높이가 표시되고 IME 상단을 넘지 않는지 검사한다.

## 캡처

`captures/`의 계정·비밀번호·인증번호는 QA 샘플이다. 미리보기 캡처를 실제 가입·인증 연동 증빙으로 사용하지 않는다. 큰 글자 수정 전 캡처는 비교 자료이며 발표용 최종 화면은 수정 후 캡처를 사용한다.

| 용도 | 캡처 |
| --- | --- |
| 로그인 입력·빈 입력 오류 | `login-filled.png`, `login-empty-errors.png` |
| 약관 기본·필수 동의 | `terms-initial.png`, `terms-required-selected.png` |
| 이메일 발송·불일치·재발송 | `email-code-sent.png`, `email-code-mismatch.png`, `email-resent.png` |
| 3분 경과 후 만료 | `email-expired-after-background.png` |
| 큰 글자 수정 후 | `email-large-font-fixed.png`, `email-landscape-large-font-fixed.png` |
| 가입 정보 입력·오류·완료 | `credentials-initial.png`, `credentials-errors.png`, `credentials-valid.png`, `signup-completed.png` |
| 큰 글자 완료 팝업 | `signup-completed-large-font.png` |
| 키보드 수정 후 로그인 | `login-soft-keyboard-fixed.png`, `login-keyboard-next-fixed.png`, `login-landscape-keyboard-fixed.png` |
| 키보드 수정 후 이메일·가입 정보 | `email-soft-keyboard-fixed.png`, `email-landscape-keyboard-fixed.png`, `credentials-soft-keyboard-fixed.png`, `credentials-landscape-keyboard-fixed.png` |

첫 QA에서는 Gboard가 하단 키보드 대신 도구 모음만 표시했다. 후속 QA에서 Android 설정과 별도로 Gboard의 `Physical keyboard → Show on-screen keyboard`가 꺼져 있음을 확인했다. 이 항목과 Android의 `show_ime_with_hard_keyboard`를 켜서 하단 키보드를 표시했다. 키보드 설정은 사용자의 요청대로 켜 둔다. `login-ime-toolbar.png`는 이전 환경의 비교 자료다.

설정 경로: 에뮬레이터 Settings → System → Keyboard → On-screen keyboard → Gboard → Physical keyboard → Show on-screen keyboard. 입력칸을 누르면 하단 키보드가 표시된다.

## 남은 확인

- 약관 원문과 입력 정책 확정 후 내용 반영.
- 실제 API 연결 후 발송·인증·가입·로그인과 서버 오류 메시지 표시 확인.
- 구현된 홈·프로필 화면으로 이동 연결 후 목적지 확인.
- 코드 리뷰 및 PR 병합. QA 수행만으로 WBS의 개발 완료나 인증 연동 완료로 판정하지 않는다.
