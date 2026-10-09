# PB-05 분석 결과 · 위험 근거 · 대응 행동 화면

## 범위

Jetpack Compose로 안전·주의·위험 분석 결과 화면을 구현합니다. 분석 결과를 표시하고 전화·공유·신고·도메인 확인·간단한 로컬 기록 저장을 제공합니다. 실제 AI/BE 요청과 응답 수신은 포함하지 않습니다.

기존 시작 화면의 Greeting과 사진 선택/화면 캡처 기능은 유지합니다. 기존 공통 테마와 ProtectiveBlue, Danger 색상을 사용하며 테마나 라이브러리 의존성을 변경하지 않습니다.

## 확인 방법

1. 이 저장소를 Android Studio에서 엽니다.
2. Gradle JDK 17을 선택하고 Android SDK 경로를 설정한 뒤 동기화합니다.
3. app의 debug 빌드를 실행합니다.
4. 시작 화면의 'PB-05 분석 결과 미리보기' 버튼을 누릅니다.
5. 안전·주의·위험 샘플을 선택해 화면과 버튼을 확인합니다.

샘플 데이터와 미리보기 Activity는 src/debug에만 있고 release 빌드에는 포함되지 않습니다. 샘플 기록은 저장하지 않습니다. Compose Preview는 AnalysisResultPreviewActivity.kt에서 확인할 수 있습니다.

## 코드 구성

| 파일 | 역할 |
|---|---|
| AnalysisResult.kt | 최종 위험 단계, 독립 지표, 근거와 사용자 행동 모델 |
| AnalysisResultJsonAdapter.kt | JSON 검증과 결과 모델 변환 |
| AnalysisResultActivity.kt | Intent로 전달된 결과 JSON을 표시하는 진입점 |
| AnalysisResultScreen.kt | 안전·주의·위험 화면과 클릭 이벤트 |
| AnalysisResultRoute.kt | 전화·공유·신고·도메인 안내·기록 저장 처리 |
| DomainPreview.kt | HTTP(S) URL의 호스트 추출 |
| ResultIcons.kt | Canvas 아이콘 |
| AnalysisResultSamples.kt (debug) | 가상 결과 데이터 |
| AnalysisResultPreviewActivity.kt (debug) | 샘플 선택과 Compose Preview |
| AnalysisResultTest.kt (test) | 모델·점수·도메인 단위 테스트 |
| AnalysisResultScreenTest.kt (androidTest) | 화면·이벤트·누락 데이터 테스트 |

MainActivity에는 debug 전용 미리보기 버튼을 추가했습니다. main Manifest에는 실제 결과 Activity, debug Manifest에는 미리보기 Activity를 exported=false로 등록했습니다. 기존 권한과 Activity/Service 등록은 유지합니다.

## 상태와 대응 행동

| 단계 | 표시 | 대응 행동 |
|---|---|---|
| SAFE | 안전 안내, 분석 대상, 지표, 점검 근거, 보안 수칙 | 돌아가기, 간단한 기록 저장 |
| CAUTION | 핵심 주의 사항, 지표, 위험 근거 | 도메인 확인, 가족·지인에게 공유 |
| DANGER | 송금 중단 경고, 원문, 지표, 위험 근거 | 전화 앱, 결과 공유, 112/118 선택 |

- 최종 위험 단계는 제공된 riskLevel을 그대로 사용합니다. FE에서 점수 임계값으로 판정을 바꾸지 않습니다.
- 사기 지표와 AI 생성·변조 가능성은 독립적으로 표시합니다.
- 점수나 근거가 없으면 해당 정보가 없다고 표시합니다. 잘못된 결과 JSON은 오류 화면으로 처리합니다.
- 전화는 ACTION_DIAL로 전화 앱을 엽니다. 자동 통화하지 않으며 가족 전화번호는 아직 지정되지 않았습니다.
- 공유는 Android 공유 창을 엽니다. 원문과 URL은 공유 텍스트에 포함하지 않습니다.
- 도메인 확인은 호스트 표시만 수행합니다. 사이트 접속, 단축 링크 해제, 안전성 검사는 없습니다. 링크가 없으면 버튼을 비활성화합니다.
- 실제 결과는 SharedPreferences에 ID, 위험 단계, 저장 시각, 일부 점수를 최근 50건까지 저장합니다. 원문/URL/근거 텍스트는 저장하지 않습니다. 전체 기록 목록 화면은 없습니다.
- 프로필 버튼은 사용 불가 안내를 표시합니다. 계정 화면 연결은 별도입니다.

## 실제 결과 연결

현재 JSON 형태는 FE 연동 예시이며 서버 API 계약을 확정한 것이 아닙니다. BE/AI 응답을 AnalysisResult로 변환한 뒤 AnalysisResultRoute에 전달하거나 아래 진입점을 사용합니다.

```kotlin
startActivity(AnalysisResultActivity.createIntent(context, resultJson))
```

입력 예시는 PB05-result-example.json에 있습니다. id, riskLevel, summary는 필수이고 점수는 제공될 경우 0~100입니다. 위험 단계는 SAFE/CAUTION/DANGER, AI 가능성은 LOW/MEDIUM/HIGH/UNKNOWN입니다.

## 검증

- 기존 추적 파일의 변경은 MainActivity와 main Manifest에 한정합니다. 기존 입력 기능과 테마 파일은 변경하지 않았습니다.
- main/debug Manifest XML 구문과 Activity 등록 확인, git diff --check 통과.
- 원본 PB-05의 모델/도메인 단위 테스트 7개는 독립 JVM 실행에서 통과했습니다.
- 이 통합 프로젝트의 compileDebugKotlin/compileReleaseKotlin 통과. 원본 프로젝트에서 발생한 AAPT2 오류를 피하기 위해 리소스 처리 작업을 제외하고 Kotlin만 검증했습니다. 전체 APK 빌드 성공을 의미하지 않습니다.
- 실제 AI 연동, 기기 렌더링, 전화/공유 앱 연결, Android UI 테스트 실행은 별도 검증이 필요합니다.
