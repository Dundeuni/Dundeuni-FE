# PB-09 인증 API 연결 준비 (2026-11-03)

## 기준과 실제 구현 범위

- 작업 브랜치: `feature/api-updates`, FE 기준: `origin/dev` `c99ee30`.
- `git fetch origin` 후 모든 로컬/원격 브랜치의 인증 트리와 HTTP 서비스 유무를 확인했다. 최신 인증 UI 트리는 `origin/feature/17-auth-ui-sprint3`와 dev가 동일하며, 세션 저장 브랜치와 이전 인증 화면 변경도 dev에 포함되어 있다. 재사용 가능한 별도 인증 HTTP 구현은 없었다.
- BE 기준: dev `b3ce5991ec60aff9c16821aa7bd99c22a68e6f24`. [인증 문서](https://github.com/Dundeuni/Dundeuni-BE/blob/b3ce5991ec60aff9c16821aa7bd99c22a68e6f24/docs/api/auth-user.md), [AccessTokenGuard](https://github.com/Dundeuni/Dundeuni-BE/blob/b3ce5991ec60aff9c16821aa7bd99c22a68e6f24/src/common/auth/access-token.guard.ts), [Prisma 스키마](https://github.com/Dundeuni/Dundeuni-BE/blob/b3ce5991ec60aff9c16821aa7bd99c22a68e6f24/prisma/schema.prisma)를 조회했다. `src/domains/auth`에는 `.gitkeep`만 있고 User 모델·로그인·가입·토큰 발급은 없다. 분석용 HS256 검증만 존재한다.
- 아래 HTTP 계약과 MockWebServer 응답은 사용자가 제공한 2026-10-08 설계안을 구현한 것이다. BE 서버에 요청하지 않았다.

## 변경 파일과 연결

| 영역 | 파일 | 구현 |
| --- | --- | --- |
| HTTP 계약 | `data/remote/api/AuthApiService.kt`, `data/remote/dto/AuthDtos.kt` | 발송·확인·가입·로그인·약관 API, 공통 응답 DTO, 경로 상수 |
| 실제 HTTP | `data/auth/HttpAuthGateway.kt`, `data/remote/network/RetrofitClient.kt` | Retrofit, 성공/오류/네트워크/계약 오류 매핑, 인증 요청 재전송·리다이렉트 금지 |
| 구성 | `data/auth/AuthDependencies.kt`, `app/build.gradle.kts` | 명시적인 HTTP 모드와 HTTPS origin 설정, 기본 Release 미연결 유지 |
| 가입 컨텍스트 | `feature/auth/AuthContracts.kt`, `SignUpFlowMemory.kt`, `SignUpTermsViewModel.kt` | 서버 약관 ID·버전, 이메일에 연결된 일회용 인증 토큰, 메모리 전용 수명 관리 |
| 기존 화면 | 로그인·약관·이메일·가입 Activity/UiState/ViewModel/Screen | 실제 구현체 주입, 약관 전문·재조회, 재발송/재확인 제한, 만료, 입력 정책 분리 |
| 세션 | `AuthSession.kt`, `AndroidEncryptedAuthSessionStore.kt`, `BearerAuthInterceptor.kt`, `MainActivity.kt` | 기존 암호화 재사용, 응답 만료와 JWT exp 중 빠른 값, 온보딩 저장/분기, 늦은 401 보호 |
| 백업 | `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Keystore에 묶인 인증 저장소의 클라우드/기기 이동 백업 제외 |

### 토큰·약관·저장 흐름

1. 실제 모드는 `GET /api/terms`에서 SERVICE/PRIVACY 문서·ID·버전을 받는다. 두 필수 문서가 없거나 불완전하면 진행을 막는다. 빈 배열 자체는 정상 응답으로 해석한다.
2. 사용자가 동의한 ID·버전을 메모리 흐름에 보관한다. 다음 Activity에는 무작위 흐름 ID만 전달한다.
3. 인증 확인 응답의 `emailVerificationToken`과 유효시각을 이메일에 연결한다. 토큰은 Intent, Bundle, SavedStateHandle, SharedPreferences에 기록하지 않는다. 프로세스 종료 후에는 처음부터 인증한다.
4. 가입 제출 시 이메일 일치·토큰 만료·동의를 다시 확인하고 정확한 `agreements` 배열을 보낸다. 성공 또는 토큰 만료 응답이면 해당 토큰을 메모리에서 해제한다.
5. 가입 성공 JWT를 기존 Android Keystore AES-GCM 저장소에 저장한다. 저장 실패 시 화면에서 저장만 재시도하며 가입 HTTP 요청은 반복하지 않는다. 저장 완료 전에는 가입 완료 화면으로 이동하지 않는다.
6. 로그인도 저장 성공 후 이동한다. `onboardingCompleted`와 토큰 만료시각은 암호화된 세션에 함께 보존한다. 신규 가입은 설계안에 온보딩 값이 없으므로 미완료로 처리한다.
7. 공개 인증 API에는 Bearer를 보내지 않는다. 보호 API는 기존 interceptor가 BACKEND 토큰만 붙이며, 실제 HTTP 401/최상위 AUTH401에서 해당 세션을 지운다. 이전 요청의 늦은 401은 새 토큰을 지우지 않는다.

### 시간과 입력 정책

- `expiresAt`, `resendAt`을 ISO UTC로 읽고 HTTP `Date`가 있으면 서버 시계를 기준으로 남은 시간을 계산한다. 없으면 로컬 UTC를 사용한다. 이후 타이머는 monotonic elapsed time을 사용해 화면 복귀 시 재계산한다.
- 재발송 간격 60초·유효기간 3분을 HTTP 구현에 하드코딩하지 않는다. 429의 `Retry-After` 초/HTTP 날짜를 지원한다. 헤더가 없으면 확정되지 않은 제한을 만들지 않고 오류와 재시도 안내를 표시한다.
- 실제 가입은 임시 정책 8~64자 비밀번호, 1~50 코드포인트 닉네임, 254자 이하 이메일을 사용한다. 로그인은 기존 계정 비밀번호를 자의적으로 제한하지 않는다.
- 기존 디자인의 영문/숫자 조합·8~20자·닉네임 10자 정책은 `PREVIEW_RESET`으로 격리해 Debug 미리보기와 명세가 없는 비밀번호 재설정에 유지한다. HTTP 닉네임 중복 오류를 만들지 않는다.

## 서버 준비 후 설정

기본 `auth.api.mode=UNCONNECTED`다. Debug의 기존 샘플 로그인/미리보기는 debug source set에 있으며, Release는 미연결 결과만 반환한다. HTTP 실패를 샘플 성공으로 바꾸지 않는다.

BE 계약과 배포 주소 확인 후 로컬 `local.properties`에 설정하고 다시 빌드한다.

```properties
auth.api.mode=HTTP
auth.api.baseUrl=https://<확정된-BE-host>/
```

Base URL은 `/api`를 포함하지 않는 HTTPS origin이다. `/api` 포함 경로는 `AuthApiPaths` 한곳에서 관리한다. 모드가 잘못되거나 HTTP 설정이 유효하지 않으면 샘플로 대체하지 않고 미연결 상태를 유지한다. 테스트는 별도 로컬 MockWebServer HTTP 주소를 생성자로 주입한다.

## BE_CONTRACT 확인 목록

- 상세 `/auth/email-codes[/verify]`와 목록 `/auth/email/code[/verify]` 중 최종 경로.
- HTTP 상태·AUTH/COMMON 코드·공통 envelope·nullable 필드의 최종 형태.
- 인증번호 길이, 발송·확인 횟수, `resendAt`, `Retry-After`, HTTP Date 제공 여부.
- 인증 완료 토큰의 이메일 결합, 유효기간, 일회용 소모 시점, 재발송 시 기존 토큰 처리.
- 비밀번호 길이·조합·공백·Unicode 규칙 및 닉네임 길이 단위, 이메일 대소문자/정규화.
- SERVICE/PRIVACY ID·버전·본문, 동의 버전 변경 시 처리. AGE와 MARKETING은 설계안에 없어 실제 요청에 임의 필드나 ID를 추가하지 않았다.
- JWT 알고리즘·issuer/audience·exp·nbf, `accessTokenExpiresAt`와 exp의 관계. FE는 서명 검증을 흉내내지 않고 BE가 검증한다. P0 Refresh Token은 없다.
- 로그인 `onboardingCompleted` 필수 여부, 가입 직후 온보딩 상태. 온보딩 완료 서버 저장 API는 PB-09 명세에 없으므로 현재 세션에서만 완료 상태를 갱신한다.
- 비밀번호 재설정 목적별 발송/확인/변경 계약. 미작성 엔드포인트를 만들지 않았다. 카카오/Google/프로필은 P1 명세 대기다.

## 검증

Mock HTTP 테스트는 요청 JSON·5개 경로, 성공·400·401·403·409·410·422·429·500·503, Retry-After, 만료·서버 시계, 잘못된 응답, 네트워크 단절·시간 초과·취소·리다이렉트, Release 기본 구성을 검증한다. 전체 HTTP 흐름 테스트는 약관 조회부터 인증 토큰 전달, 가입, 저장, 보호 요청과 401까지 연결한다.

ViewModel/UI 테스트는 입력 정책·동의·타이머·중복 요청·저장 재시도·민감 값 초기화를 확인한다. Android 계측 테스트는 기존 로그인·회원가입·재설정·회전·키보드 흐름과 실제 Keystore 저장·복원·로그아웃·온보딩 분기를 확인한다.

전체 테스트 실행 중 발견한 기존 두 테스트 문제도 테스트 코드에서만 수정했다: `BasicInspectionQuotaTest`에 설치된 Robolectric이 지원하는 SDK를 지정하고, `MockAnalysisFlowTest`는 완료 상태의 실제 수집을 기다리도록 했다. 다른 PB의 제품 동작은 변경하지 않았다.

최종 실행 결과는 아래에 기록한다.
