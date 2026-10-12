# Codex 저장소 작업 규칙

## GitHub 작업

- GitHub 관련 요청(Issue, Label, Branch, Commit, Push, PR, Review, Merge, Close, 릴리스/배포)을 처리하기 전에 [docs/GITHUB_WORKFLOW.md](docs/GITHUB_WORKFLOW.md)를 반드시 읽는다.
- 이어서 [docs/FEconventions.md](docs/FEconventions.md), 해당 [.github/ISSUE_TEMPLATE/](.github/ISSUE_TEMPLATE/) 파일 또는 [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md), [라벨 정의](.github/labels.yml)와 [라벨 가이드](.github/LABELS_GUIDE.md)를 확인한다. 상세 규칙을 추측하거나 별도 형식으로 대체하지 않는다.
- GitHub 관리와 기능 구현은 별도 요청으로 취급한다. 요청받은 단계만 수행하며, 기능 구현 요청만으로 Issue/PR 생성·Commit·Push·Merge·배포를 진행하지 않는다.
- 생성 전 기존 Issue/PR을 검색해 중복을 막고, 여러 요청은 의존 순서대로 처리한다. 인증 확인과 GitHub 쓰기 작업은 `gh`를 사용한다. Git Credential Manager에서 토큰을 직접 추출하지 않는다. `gh` 미설치·미인증이면 사용자에게 직접 설정 방법을 안내하고, 권한 변경이 필요하면 먼저 알린다.
- 현재 브랜치와 staged/unstaged/untracked 변경을 먼저 확인해 기존 작업을 보존한다. 일반 작업은 최신 원격 `dev` 기반 작업 브랜치에서 진행하며 `main`/`dev`에 직접 Push하지 않는다. Commit/Push 직전 변경 파일·diff·대상 브랜치를 다시 확인한다.
- Merge·Issue Close·배포는 각각 완료 조건과 사용자의 해당 승인을 확인한다. 유효한 기존 승인은 재사용하되 한 단계의 승인을 다른 단계로 확대하지 않는다. 자동 Issue 종료도 확인 대상이다.
- 기존 작업 훼손 가능성이나 위험이 있으면 실행 전에 보고한다. 규칙 충돌·불명확한 사항은 임의로 결정하지 말고 질문한다.
- 결과는 변경 파일 또는 Issue/PR 링크, 실행·검증 결과와 미완료 사항을 구분해 보고한다.

루트 `AGENTS.md`는 저장소 작업 지침의 진입점이다. 새 세션에서 적용을 확인할 수 있다. [Codex 공식 AGENTS.md 안내](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
