
# FE Convention 

> 공통 Git / Branch / Commit 규칙은 팀 규칙을 따르고,  
> 여기서는 Android FE에서 필요한 것만. 

## 1. 이름 규칙

- 화면: `[기능명]Screen`
    - `HomeScreen`
    - `ResultScreen`
- ViewModel: `[기능명]ViewModel`
    - `ResultViewModel`
- 상태: `[기능명]UiState`
    - `ResultUiState`
- Preview: `[이름]Preview`
    - `ResultScreenPreview`
- 클릭 등 이벤트: `onXXX`
    - `onClick`, `onRetry`
- 재사용 UI는 역할이 보이게 이름 짓기
    - `RiskBadge`, `ResultCard`

## 2. 파일 규칙

- 대표 Composable 이름 = 파일 이름
    - `ResultScreen()` → `ResultScreen.kt`
- Android Resource는 `lower_snake_case`
    - `result_warning`
    - `ic_home`

## 3. UI 작성 규칙

- Composable은 UI 표시와 사용자 입력 처리에 집중
- API 호출 / 데이터 처리 / 비즈니스 로직은 ViewModel 등으로 분리
- 재사용되거나 큰 UI는 별도 Composable로 분리
- 화면에 보이는 문자열은 가능하면 `strings.xml` 사용

## 4. 기타

- Kotlin 공식 Style Guide + Android Studio Formatter 사용
- 의미 있는 이미지/아이콘에는 `contentDescription` 작성