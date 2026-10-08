
# Dundeuni Android FE Convention

## 1. Git Workflow

```text
Issue 생성
→ dev에서 작업 브랜치 생성
→ 개발 / Commit
→ dev로 PR
→ Review
→ Squash Merge
```

- `main`, `dev`에 직접 Push하지 않는다.
- 일반 작업은 `dev` 기준으로 진행한다.
- 배포/데모 시 `dev → main`으로 병합한다.

---

## 2. Branch

```text
feature/<issue>-작업명
fix/<issue>-작업명
refactor/<issue>-작업명
docs/<issue>-작업명
chore/<issue>-작업명
```

예:

```text
feature/12-media-projection
fix/18-image-upload-error
```

- `<issue>`는 GitHub Issue 번호를 사용한다.
- 작업명은 영어 소문자와 `-`를 사용한다.

---

## 3. Commit / PR 제목

```text
<type>: <작업 내용>
```

```text
feat      기능 추가
fix       버그 수정
refactor  구조 개선
docs      문서
test      테스트
chore     설정 / 환경
```

예:

```text
feat: MediaProjection 화면 캡처 구현
fix: 이미지 선택 오류 수정
```

- 한 Commit에는 하나의 작업 의도만 담는다.
- PR 제목도 같은 형식을 사용한다.

---

## 4. Pull Request

PR에는 최소한 다음을 작성한다.

- 변경 내용
- 관련 Issue (`close #번호`)
- 테스트 / 확인 결과
- BE / AI 연동 영향
- 리뷰어가 확인할 부분

규칙:

- 1명 이상 Review 후 Merge
- Squash Merge 사용
- 작성자가 자기 PR을 바로 Merge하지 않는 것을 원칙으로 함

---

## 5. Naming

### Kotlin 파일 / Class
`PascalCase`

```text
HomeScreen.kt
AnalysisViewModel.kt
MediaProjectionManager.kt
AnalysisRepository.kt
```

### 함수 / 변수
`camelCase`

```text
startCapture()
selectedImage
analysisResult
```

### Package / Folder
`lowercase`

```text
feature
data
ui
navigation
```

### 상수
`UPPER_SNAKE_CASE`

```text
MAX_IMAGE_SIZE
REQUEST_TIMEOUT
```

파일 이름은 가능하면 역할이 드러나게 작성한다.

```text
AnalysisScreen.kt
AnalysisViewModel.kt
AnalysisRepository.kt
AnalysisApi.kt
AnalysisRequest.kt
AnalysisResponse.kt
```

`Utils.kt`, `Common.kt`, `Temp.kt`처럼 역할이 불명확한 이름은 가급적 피한다.

---

## 6. BE / AI 연동

다음 항목이 바뀌면 담당자와 공유 후 반영한다.

- API Endpoint / Method
- Request / Response
- 데이터 타입
- Multipart Field
- 이미지 포맷 / 최대 파일 크기
- Error Response
- 인증 방식

```text
FE 내부 구현 변경 → FE에서 결정
API 계약 변경 → BE와 협의
AI 입출력 변경 → AI 담당자와 협의
```

---

## 7. 보안 / 상태 표시

Commit 금지:

- API Key / Token / Password
- 개인정보
- 사용자 원본 검사 데이터

상태 라벨:

```text
🚧 blocked      진행 막힘
👀 need review  리뷰 필요
🔥 P0 / ⭐ P1 / 🌱 P2
```

---

## 핵심 요약

```text
Issue → Branch → Commit → PR → Review → Squash Merge

Branch
feature/<issue>-작업명

Commit / PR
feat: 작업 내용

Naming
Class/File = PascalCase
함수/변수 = camelCase
Package = lowercase
상수 = UPPER_SNAKE_CASE

API 계약 변경은 BE / AI와 반드시 공유
```

### Package / Folder

- Package명은 lowercase를 사용한다.
- 코드는 역할에 맞는 Package에 배치한다.
- 새로운 기능은 가능한 `feature/<기능명>` 단위로 분리한다.
- 공용 UI는 `ui/component`에 둔다.
- 임시 테스트 코드는 실제 서비스 코드와 분리한다.

자세한 프로젝트 구조는 `README.md` 참고.