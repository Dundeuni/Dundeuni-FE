# PB-09 인증 UI QA

2026-10-15 기록은 `feature/4-auth-ui`의 debug 샘플을 대상으로 한다. 아래 재점검은 홈·세션 처리 변경을 반영한 별도 기록이다. 실제 이메일 발송·계정 생성·서버 인증은 검증 범위 밖이다.

## 최신 코드 재점검 (2026-10-17)

- 기준: `dev`의 `65e09ac` → `fix/6-auth-ui-qa` 작업 브랜치.
- 환경: Android 16 / API 36 에뮬레이터, 소프트 키보드 표시 켬.
- debug 로그인은 MOCK 세션을 저장한다. 회원가입 완료는 debug 미리보기에서만 확인하며, 실제 이메일 발송·가입·서버 로그인 성공을 뜻하지 않는다.
- 이전 발표 캡처와 아래 10월 15일 통과 기록을 최신 코드의 검증 결과로 대체 사용하지 않는다.

| 항목 | 발견 내용·확인 방식 | 수정 |
| --- | --- | --- |
| 로그인 키보드 가림 | 실제 키보드에서 로그인 버튼 경계 검사 실패. `adjustNothing` 상태에서 비밀번호 칸과 로그인 버튼이 키보드에 가려지는 화면을 확인했다. | `adjustResize`와 IME 여백 적용. 키보드 표시 중 로고를 접고 로그인 버튼을 키보드 위에 고정한다. 세로·가로에서 입력칸과 버튼 전체 경계를 검사한다. |
| 로그인 정보 저장 실패 | 기존 성공 콜백에서 저장 예외가 처리되지 않았다. 저장 함수에 실패를 주입해 확인한다. | ViewModel에서 저장 실패를 안내하고 이동하지 않는다. 재시도 성공 시 비밀번호를 지우고 홈 이동을 준비한다. |
| 로그인 비동기 응답과 화면 수명 | 성공 콜백이 Activity를 참조하고 있었다. 응답 전 화면을 백그라운드로 보낸 뒤 성공 응답을 전달하는 테스트를 추가했다. | 저장 후 이동 요청을 ViewModel 상태에 보관한다. 현재 Lifecycle이 RESUMED일 때 한 번 소비해 이동한다. |
| QA 테스트 진입점 | 이전 홈의 로그인·미리보기 버튼과 완료 후 문구를 찾고 있었다. | 현재 `My 쉼터` 로그인 진입과 홈의 `빠른 검사` 표시를 확인한다. 미리보기는 debug Activity를 직접 열어 샘플 회원가입을 점검한다. Activity·키보드 전환 완료도 기다린다. |

검증 항목:

- 로그인 이메일·비밀번호 입력, Next/Done, 세로·가로 키보드 접근, 성공 후 홈 이동.
- 샘플 회원가입의 필수 약관, 이메일 불일치·재발송·성공, 비밀번호·닉네임 오류 수정, 완료·홈 이동.
- 이메일·가입 정보 회전 시 상태 유지, 뒤로가기 후 다음 단계 자동 재진입 방지.
- Android Keystore를 사용하는 저장소의 세션 저장, 저장소 인스턴스 재생성 후 읽기, 평문 토큰 미저장, Activity 재생성 후 로그인 상태 표시.
- 로그아웃 후 저장소 비움과 로그인 이동, 백그라운드에서 만료·인증 무효화 후 복귀 시 로그인 이동.
- 저장 실패 후 재시도, 이동 대기 중 중복 로그인·저장 차단, 백그라운드 성공 응답 후 복귀 시 한 번 이동.

`AuthSessionQaTest`의 인증 실패는 런타임에 로컬 BACKEND 샘플 토큰을 무효화하는 방식이다. 실제 서버 401 응답 검증과 구분한다. 토큰 첨부·HTTP 401·AUTH401 처리는 로컬 `BearerAuthInterceptorTest`로 확인한다.

검증 결과:

| 검사 | 결과 |
| --- | --- |
| 전체 로컬 테스트 | 124개 통과, 실패 0개 |
| `AuthFlowQaTest` | 4개 통과. 이전 세션을 테스트 전에 보관·초기화하고 종료 후 복원하도록 격리한 뒤 재실행 |
| `AuthSessionQaTest` | 4개 통과 |
| `LoginLifecycleQaTest` | 1개 통과 |
| 기존 `AnalysisResultScreenTest` | 5개 통과 |
| debug·release APK | 빌드 통과. release는 unsigned APK 생성 확인이며 설치·실행 검증은 아님 |
| 앱 강제 종료 후 재실행 | 프로세스 변경 확인 후 `My 쉼터`에서 로그인 상태 표시 유지 |
| 변경 파일 공백 검사 | `git diff --check` 통과 |

에뮬레이터 14개 항목은 분리 실행과 재실행 결과를 합산한 수다. 테스트의 이전 세션 잔류와 화면 전환 대기 문제도 수정했다. 물리 저장소 장애는 강제로 발생시키지 않았으며, 저장 실패 처리는 함수에 예외를 주입한 로컬 테스트 결과다.

실제 이메일 발송·계정 생성·서버 로그인·401 응답, 실기기와 다른 Android 버전은 남아 있다. 인증 흐름 QA 후 2026-10-17에 [발표 캡처 12장](presentation/README.md)을 최신 UI로 재생성했다. 모든 이미지에 `UI 미리보기 · 샘플 인증`과 `실제 API 미연결`을 표시했으며, 키보드·클립보드 추천 문구가 없는지 12장 모두 확인했다. 커밋·푸시·PR 변경은 수행하지 않았다.

## 2026-10-15 기록

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

## PB-02·PB-05 회귀 점검 (2026-10-15)

`feature/4-auth-ui`의 `a7b47f1`에서 Android 16 / API 36 에뮬레이터로 확인했다. 검사 모드는 `FAKE`이며 실제 BE·AI 분석을 검증한 결과는 아니다.

| 점검 항목 | 확인 결과 |
| --- | --- |
| PB-02 오버레이 권한 안내 → 권한 허용 → 시작 버튼 재선택 | 플로팅 버튼 표시 |
| 플로팅 메뉴 열기·닫기 | 화면 검사·이미지 검사 항목 표시 및 메뉴 닫기 |
| 화면 공유 승인 취소 → 재진입 | 버튼 복구 후 다시 승인 창 표시 |
| 앱 시작 화면 공유 승인 → 캡처 → 검사 결과 | 샘플 주의 결과 표시, 결과 뒤로가기로 시작 화면 복귀 |
| 사진 선택 취소 → 다시 선택 → 검사 결과 | 취소 상태 표시 후 선택한 QA PNG의 샘플 결과 표시, 뒤로가기로 사진 선택 화면 복귀 |
| 로그인 화면 진입·뒤로가기 후 PB-05 진입 | 기존 결과 미리보기 진입 |
| PB-05 안전·주의·위험 전환, 근거·점수·하단 버튼 | 화면 테스트 5개 통과 및 Activity에서 세 단계 전환·스크롤 확인 |
| 도메인 미리보기 | `bit.ly` 안내 창 표시 및 닫기, 링크 접속 없음 |
| 신고 버튼 | 112·118 전화 앱 선택 창 표시 및 뒤로가기로 닫기 |
| 샘플 결과 보관 버튼 | 보관 완료 상태로 전환하지 않음. 샘플은 저장하지 않는 기존 분기 유지 |
| 결과의 돌아가기 버튼 | 기존 시작 화면 복귀 |
| 캡처 종료·앱 종료 오류 | 활성 MediaProjection 없음, 점검 중 crash 로그 없음 |

전체 로컬 테스트 **108개**와 기존 PB-05 에뮬레이터 테스트 **5개**가 통과했다. 이 중 로컬 인증 테스트는 29개다. PB-09 커밋은 기존 플로팅·캡처·사진 선택·검사·결과 및 데이터 처리 코드를 변경하지 않았고, 공통 진입점에는 로그인 버튼과 인증 Activity 등록을 추가했다. 점검 범위에서 오류를 재현하지 못해 해당 기능 코드는 수정하지 않았다.

오버레이 권한은 점검 후 이전 `default` 상태로 복원하고 플로팅 서비스를 종료했다. 소프트 키보드 표시 설정은 유지한다. 전화 발신·공유 전송·실제 분석 API·다른 Android 버전과 실기기는 이번 확인 범위에 포함하지 않았다.

## 재실행

PowerShell에서 저장소 루트 기준으로 실행한다. 에뮬레이터는 미리 켜 둔다.

```powershell
$env:JAVA_HOME = 'D:\Android\Jdk\temurin-17'
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest --tests 'com.dundueni.app.feature.auth.*' --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dundueni.app.feature.auth.AuthFlowQaTest' --console=plain
```

10월 17일 재점검 명령:

```powershell
$env:JAVA_HOME = 'D:\Android\Jdk\temurin-17'
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dundueni.app.feature.auth.AuthFlowQaTest,com.dundueni.app.feature.auth.AuthSessionQaTest,com.dundueni.app.feature.auth.LoginLifecycleQaTest,com.dundueni.app.feature.analysisresult.AnalysisResultScreenTest' '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true' --console=plain
```

`AuthSessionQaTest`는 테스트 전 세션을 보관하고 종료 시 유효한 이전 세션을 복원한다. 네트워크에 전송하지 않는 QA 토큰만 생성한다.

에뮬레이터 테스트는 테스트 종료 후 앱을 제거할 수 있다. 다시 확인하려면 Android Studio에서 app debug를 실행한다.

키보드 테스트는 API 30 이상에서 실행하며, Gboard의 화면 키보드 표시를 켜야 한다. 키보드 애니메이션이 끝난 뒤 입력칸·버튼 전체 높이가 표시되고 IME 상단을 넘지 않는지 검사한다.

## 캡처

발표에는 [정리된 캡처 12장](presentation/README.md)을 사용한다. 각 PNG에 `UI 미리보기 · 샘플 인증`과 `실제 API 미연결`을 표시했다. 아래 `captures/` 목록은 수정 전후 진단 기록이며 발표용 캡처와 구분한다.

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
