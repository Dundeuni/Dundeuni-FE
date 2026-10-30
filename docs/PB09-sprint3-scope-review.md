# PB-09 Sprint 3 이슈 #17 범위 검토

기준: 2026-10-30, `feature/17-auth-ui-sprint3`, 작업 시작 커밋 `8e74e96`.
이슈: https://github.com/Dundeuni/Dundeuni-FE/issues/17

## 판단

비밀번호 재설정 UI는 로그인 진입부터 이메일 인증, 새 비밀번호 입력, 샘플 완료 안내, 로그인 복귀까지 구현했다. 사용자도 에뮬레이터에서 동작을 확인했다. 이슈 #17 전체 완료를 뜻하지는 않는다.

## 이슈 작업 내용 대조

| 작업 내용 | 상태 | 근거 및 남은 내용 |
| --- | --- | --- |
| 최종 인증 정책·디자인과 기존 UI 대조 | PNG 대조 완료·정책 확인 남음 | 로그인 디자인 정보와 10월 30일 첨부 PNG 11장을 대조했다. PB09-auth-design-review.md에 반영 내역과 샘플 모순을 정리했다. 서버 정책 확정은 별도다. |
| 기존 로그인·회원가입 디자인 변경 반영 | 제공 자료 기준 완료 | 로그인 배치, 이메일 글자 크기, 공통 헤더, 가입 정보 아이콘, 가입 완료 배지·정렬·설정 아이콘·여백을 반영했다. |
| 공통 테마·버튼 적용 | 재설정 범위 완료 | Pretendard, TermsColors, 헤더, 입력 필드, 검증 칩, 버튼을 재사용했다. 앱 전체 공통 컴포넌트 정리는 이번 변경에 포함하지 않았다. |
| 재설정 이메일 인증 UI | UI 완료 | 기존 인증 UI를 재설정 모드로 재사용했다. 인증 목적·API 계약의 확정은 남았다. |
| 새 비밀번호·확인·보기 전환 | 완료 | 입력 필드, 일치 확인, 보기·가리기를 구현했다. |
| 필수·형식·비밀번호 규칙 검증 | 기존 규칙 기준 완료 | 이메일 형식, 8~20자, 영문·숫자 조합, 확인값 일치를 검증한다. 특수문자는 강도 표시에 사용한다. BE 정책 확정 후 조정한다. |
| 번호 타이머·재발송·만료·오류 | 재사용 및 연결 완료 | 기존 인증 ViewModel과 샘플 Gateway를 사용한다. 실제 서버의 재발송 제한과 만료 정책은 미확정이다. |
| 제출 중 표시·중복 제출 방지 | 완료 | ViewModel과 버튼 상태로 처리한다. 실패·만료·중복 요청은 단위 테스트로 확인했다. |
| 로그인 진입·뒤로가기 | 완료 | 실제 로그인과 Debug 미리보기에서 연결했다. 비밀번호 단계에서 돌아가면 인증과 입력을 초기화한다. |
| 완료 안내·로그인 복귀 | 샘플 범위 완료 | 완료 전용 디자인이 없어 AlertDialog를 사용한다. 실제 변경이 없다는 문구를 표시한다. |
| API 연결 입력값·상태·이벤트 분리 | 코드·문서 준비 완료 | PasswordResetGateway, ViewModel, UI를 분리했다. 담당자 전달 및 계약 합의 완료는 확인하지 않았다. |
| Mock 성공·실패·제출 중·화면 이동 | 완료 | 단위 테스트, API 36 기기 테스트와 사용자 확인을 수행했다. |

## 다음 순서

1. 첨부 PNG 기준 수정 화면을 사용자가 확인한다. 원본 폰트·SVG까지 일치시켜야 하는 항목은 원본 에셋을 확인한다.
2. 비밀번호 규칙, 인증 유효시간, 재발송 제한, 완료 화면을 담당자와 확인한다. 소셜 로그인 지원 여부도 확정한다.
3. 확인된 UI 변경만 반영하고 영향을 받는 인증 테스트를 실행한다.
4. API 연결 문서를 No.162 담당자에게 전달한다. 실제 발송·인증·변경과 세션 정책은 API 연결 후 검증한다.
5. 코드 리뷰와 dev 병합을 진행한다. 위 미완료 항목을 처리하거나 이슈 범위를 합의하기 전에는 #17 전체 완료로 표시하지 않는다.

## 변경 검토 및 검증 범위

- 현재 코드 검토에서 추가 수정이 필요한 결함은 찾지 못했다. 공통 함수의 기본값은 기존 회원가입 동작을 유지한다.
- Debug 샘플 구현은 debug 소스에만 둔다. Release에서는 API 미연결 안내를 표시하며 샘플 번호를 인증하지 않는다.
- 비밀번호와 재설정 인증값은 ViewModel 메모리에만 둔다. Intent, 저장 상태, 설정 파일, 로그에 저장하지 않는다.
- 기존 로그인 세션을 생성하거나 변경하는 코드를 추가하지 않았다.
- 직전 검증: 단위 테스트 147개, API 36 기기 테스트 12개 통과, Debug·Release 빌드 통과. 이번 범위 검토에서는 제품 코드를 바꾸지 않아 테스트를 반복하지 않았다.
- 기기 테스트는 재설정 3개와 기존 인증 9개다. PB-02·PB-05 전체 회귀, 실기기, 실제 API 연동을 검증한 결과로 확대하지 않는다.
- `git diff --check` 통과. 진단 캡처와 `docs/migrated-fe-assets-20261021/`, `docs/qa/PB09/captures/`는 커밋 대상에서 제외한다.

## 커밋 구분안

1. `feat: PB-09 인증 UI 보완 및 비밀번호 재설정 구현`
   - AndroidManifest.xml, EmailVerificationScreen.kt, EmailVerificationUiState.kt, EmailVerificationViewModel.kt, SignUpCredentialsScreen.kt, SignUpTermsScreen.kt.
   - PasswordResetActivity.kt, PasswordResetScreen.kt, PasswordResetViewModel.kt, debug/PreviewPasswordResetGateway.kt.
   - auth_reset_*.png 6개와 auth_plan_badge.xml, auth_settings.xml. 공통 인증 UI와 재설정 화면이 함께 사용하는 에셋도 이 커밋에 포함한다.
2. `fix: PB-09 로그인 배치 및 샘플 인증 진입 경로 수정`
   - LoginActivity.kt, LoginRoute.kt, LoginScreen.kt, debug/LoginPreviewActivity.kt.
3. `test: PB-09 비밀번호 재설정 및 기존 인증 흐름 검증`
   - PasswordResetViewModelTest.kt, PasswordResetFlowQaTest.kt, AuthFlowQaTest.kt, AuthPresentationCaptureTest.kt, SignUpCredentialsScreenTest.kt.
4. `docs: PB-09 재설정 API 연결 지점 및 구현 범위 정리`
   - PB09-password-reset.md, PB09-sprint3-scope-review.md, PB09-auth-design-review.md.

각 커밋 전에 스테이징 목록을 확인한다. 경로 전체를 추가하는 `git add .`는 사용하지 않는다.

기능과 디자인 수정이 같은 공통 UI 파일에 섞여 있어 위 목록은 파일 전체를 한 번씩만 포함하도록 확정했다. 테스트 선택자 갱신은 3번 커밋에 들어가므로 전체 QA는 3번까지 적용한 상태를 기준으로 한다. 들여쓰기만 변경된 app/build.gradle.kts와 진단 캡처·기존 자료 폴더는 제외한다.
