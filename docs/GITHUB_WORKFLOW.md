# GitHub 운영 워크플로

GitHub 관리와 기능 구현은 별도 작업이다. 아래 흐름은 팀의 전체 절차이며, Codex는 사용자가 요청한 단계만 수행한다. 기능 구현 요청을 Issue/PR 생성·Commit·Push·Merge·배포 승인으로 해석하지 않는다.

## 1. 기준 문서

Branch Naming, Commit/PR 제목, Review/Merge 세부 규칙은 기존 컨벤션을 우선한다. 템플릿의 항목을 재작성하지 않고 실제 파일을 읽어 사용한다.

| 용도 | 참조 파일 |
| --- | --- |
| 브랜치·Commit·PR·리뷰 규칙 | [FEconventions.md](./FEconventions.md) 1~4절 |
| 프로젝트 범위·구조 | [README.md](../README.md) |
| 기능 Issue (`[Feat]`, `✨ feature`) | [feature_request.md](../.github/ISSUE_TEMPLATE/feature_request.md) |
| 버그 Issue (`[Fix]`, `🐛 fix`) | [bug_report.md](../.github/ISSUE_TEMPLATE/bug_report.md) |
| 설정·일반 문서 Issue (`[Chore]`, `🛠 chore`) | [chore.md](../.github/ISSUE_TEMPLATE/chore.md) |
| API 명세 Issue (`[Docs] API -`, `📝 docs` + `📡 API`) | [api_spec.md](../.github/ISSUE_TEMPLATE/api_spec.md) |
| DB/Prisma Issue (`[DB]`, `🗄 DB`) | [db_schema.md](../.github/ISSUE_TEMPLATE/db_schema.md) |
| 빈 Issue 허용·BE API 문서 안내 | [config.yml](../.github/ISSUE_TEMPLATE/config.yml) |
| PR 본문 | [PULL_REQUEST_TEMPLATE.md](../.github/PULL_REQUEST_TEMPLATE.md) |
| 라벨 이름·색상·설명 / 적용 방법 | [labels.yml](../.github/labels.yml), [LABELS_GUIDE.md](../.github/LABELS_GUIDE.md) |

템플릿에는 BE 공통 항목이 포함되어 있다. Android 작업에 해당하지 않는 API·Swagger·Prisma 등은 `해당 없음`과 이유를 기재하고, 수행하지 않은 검증에 체크하지 않는다. API 템플릿의 `docs/api`가 FE에 있다고 가정하지 말고 실제 대상과 `config.yml`의 BE 문서 링크를 확인한다.

## 2. 전체 흐름

```text
기존 Issue/PR 검색 → Issue 생성 또는 재사용 + Label
→ Git 상태 확인 → 원격 dev 기준 최신화 → 작업 브랜치
→ 요청받은 작업 및 검증 → add → commit → push
→ dev 대상 PR + Label → Review → 승인 확인 → Squash Merge
→ PR의 merged/closed 확인 → 완료 조건·승인 확인 후 Issue Close
→ 매주 합의한 일정에 파트별 dev 검토 → dev → main PR
→ Review·검증·승인 후 Merge → 검증된 버전 배포(별도 승인)
```

## 3. Issue와 Label

- 생성 전 저장소, 제목·본문의 핵심어, 관련 번호로 열린/닫힌 Issue와 PR을 검색한다. 같은 작업이 있으면 재사용하고, 종료된 작업과 다른 후속 범위만 새 Issue로 분리한다.
- 작업에 맞는 템플릿의 제목 접두사·본문·기본 라벨을 유지한다. 전용 템플릿이 없는 작업은 가장 가까운 템플릿을 재사용하되 분류가 불명확하면 질문한다.
- Issue에는 가이드에 따라 `작업 유형 + 도메인 + 우선순위` 조합을 권장한다. 일반 문서 작업은 `chore.md`를 사용하고 `📝 docs`를 추가할 수 있다. 우선순위·담당자를 추측하지 않는다.
- 라벨은 `labels.yml`의 이모지·공백을 포함한 정확한 이름을 사용한다. 실제 원격 라벨 목록도 확인한다. 누락 라벨을 기본 영문 라벨로 임의 대체하지 않는다.
- `labels.yml`은 자동 동기화되지 않는다. 누락 라벨 생성·동기화는 요청 범위를 확인한 별도 설정 작업으로 처리한다. 필요한 라벨이 없으면 이를 보고하고 적용 방법을 확인한다.
- PR에도 관련 작업·도메인 라벨을 지정하고, 리뷰 요청 시 `👀 need review`, 외부 결정으로 막혔을 때 `🚧 blocked`를 적용한다. 상태가 해소되면 상태 라벨을 제거한다.

## 4. 브랜치와 최신화

- `main`: 검증된 배포/데모 기준. `dev`: 개발 통합 기준. 두 브랜치에 직접 Push하지 않는다.
- 일반 작업은 최신 원격 `dev`에서 분기하고 PR base는 `dev`로 지정한다. 브랜치 이름은 컨벤션 2절을 따른다(예: `docs/123-github-workflow`). Issue 번호를 임의로 만들지 않는다.
- 먼저 `git status --short --branch`, `git diff`, `git diff --cached`, `git branch -vv`, `git remote -v`로 현재 브랜치·추적 관계·staged/unstaged/untracked 변경을 확인한다.
- 기존 변경이나 별도 작업 브랜치가 있으면 보존한다. 임의 stash·reset·clean·checkout 덮어쓰기·rebase·강제 Push를 하지 않는다. 전환/통합이 기존 작업을 훼손할 수 있으면 영향을 보고하고 보존 방법을 확인한다.

아래는 작업 트리가 깨끗하고 새 작업을 시작할 때의 예시다. 각 단계 성공을 확인한 뒤 다음 명령을 실행한다.

```powershell
git fetch origin
git switch dev
git merge --ff-only origin/dev
git rev-list --left-right --count dev...origin/dev
# 0 0인지 확인한 뒤 실제 Issue 번호를 사용한다.
git switch -c docs/123-github-workflow origin/dev
```

`dev`가 없으면 `git switch --track -c dev origin/dev`로 생성한다. fast-forward 실패, 로컬 전용 커밋, 충돌이 있으면 자동 수정하지 말고 확인한다. 기존 작업 브랜치를 계속 쓸 때도 fetch 후 `origin/dev`와 차이를 확인하고 필요한 통합 방식은 기존 작업 보존을 전제로 결정한다.

## 5. Commit과 Push

1. 변경 파일·diff·검증 결과가 요청 범위에 맞는지 확인한다. 비밀키·토큰·개인정보·사용자 원본 검사 데이터는 포함하지 않는다(컨벤션 7절).
2. 요청 범위의 파일만 명시적으로 `git add`한다. 기존 staged 변경을 임의로 포함하거나 해제하지 않는다.
3. `git diff --cached`를 재확인하고 컨벤션 3절의 `<type>: <작업 내용>` 형식으로 Commit한다. 한 Commit에는 하나의 의도만 담는다.
4. Push 직전 상태·대상 원격/브랜치·전송할 커밋을 확인한다. 작업 브랜치의 첫 Push는 `git push -u origin <작업-브랜치>`를 사용한다.

Commit/Push는 해당 단계가 요청되었을 때만 실행한다. 실패 시 원인을 보고하고 강제 Push로 우회하지 않는다.

## 6. PR·Review·Merge와 종료

- PR 생성 전 관련 Issue, 동일 head/base의 기존 PR, `origin/dev...HEAD` 변경 범위, Push 여부를 확인한다. 기존 PR이 있으면 재사용한다.
- PR 제목은 Commit과 같은 형식, 본문은 기존 PR 템플릿을 따른다. 변경 이유·내용, 실제 검증 결과와 미실행 항목, BE/AI 영향, 리뷰 포인트, 관련 Issue를 채운다.
- 컨벤션에 따라 관련 Issue는 `close #번호`로 연결한다. 단, 종료 승인 전 자동 종료가 일어날 수 있는 base/커밋 메시지는 확인하고 미완료 Issue에는 종료 키워드를 사용하지 않는다.
- 1명 이상 Review 후 Squash Merge한다. 미해결 수정 요청·충돌·필수 검사 실패가 있으면 병합하지 않는다. 작성자가 자기 PR을 바로 Merge하지 않는 원칙을 따른다.
- Merge 직전 최신 head/base, 리뷰와 검사 상태, 실제 브랜치 보호/Ruleset, 권한, 사용자의 해당 Merge 승인을 확인한다. 새 커밋이나 범위 변경으로 기존 승인이 유효하지 않으면 다시 확인한다. 관리자 우회는 하지 않는다.
- Merge 성공 시 PR은 자동으로 닫힌다. `merged`와 `closed` 상태를 확인하며 별도 Close 명령으로 병합을 대신하지 않는다. 미병합 PR의 Close는 취소·중복 등 사유와 요청을 확인한다.
- Issue는 완료 기준 충족, 필요한 검증, 관련 PR 병합 및 사용자 종료 승인을 모두 확인한 뒤 닫는다. 배포까지 완료 기준에 포함된 Issue는 배포 확인 전 닫지 않는다. 부분 완료이면 남은 범위를 기록하고 열어 둔다.
- 기본 브랜치가 `main`이면 `dev` 대상 PR 본문의 `close #번호`로는 자동 종료되지 않는다. 병합 후 Issue 상태를 조회하고, 승인된 경우 수동 종료한다. `main` 통합 PR에서는 자동 종료될 Issue도 Merge 전에 완료 조건과 종료 승인을 확인한다. [GitHub 공식 동작](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue)

## 7. 주간 dev → main 통합과 배포

정기 통합 요일·시간·시간대와 파트별 검토 담당자는 **팀 합의 필요**다. 추후 상황에 따라 유동적으로 결정한다. 문서화를 위해 확정할 필요는 없으며, 실제 주간 통합을 실행할 때 합의된 일정과 담당자를 확인한다.

1. 합의한 일정에 각 파트 담당자가 자신의 `dev` 변경, 연동 계약, 검증 결과, 미해결 Issue를 검토한다. 이 FE 저장소에서는 FE 변경과 BE/AI 연동 영향을 확인하며 다른 저장소를 임의로 변경하지 않는다.
2. 최신 원격 `dev`와 `main`의 양방향 커밋·실제 diff를 확인한다. `main`에만 있는 변경이나 Squash Merge로 생긴 이력 차이가 있으면 덮어쓰지 말고 통합 방법을 확인한다.
3. 기존 통합 PR을 먼저 찾고, 없으면 head `dev`, base `main`으로 PR을 만든다. 기존 템플릿에 포함 PR/Issue, 파트별 검토 결과, 통합 검증, 배포 대상 버전·변경 요약을 기록한다.
4. Review, 필요한 빌드·테스트·주요 흐름/연동 검증, 보호 규칙 충족 및 Merge 승인을 확인한 뒤 컨벤션에 따라 Squash Merge한다. 미검증 변경이 포함되면 배포를 보류한다.
5. 병합된 `main`의 정확한 커밋/버전을 검증하고, 배포 환경·채널·담당자·절차·복구 방법을 확인한다. **Merge 승인과 별도로 배포 승인**을 받은 뒤 검증된 산출물만 배포한다.
6. 배포 버전·커밋·검증 결과를 릴리스 기록에 남기고 배포 후 주요 동작을 확인한다. 배포가 완료 조건인 Issue의 종료 조건과 승인을 다시 확인한다.

배포 도구·환경·버전 정책은 기존 자료에서 확인되지 않았다. 구체적인 배포 요청 시 확인하며 문서 작성만으로 배포 설정을 만들지 않는다.

## 8. Codex 실행 기준

- GitHub 작업 시작 시 이 문서와 연결된 컨벤션·해당 템플릿·라벨 정의를 먼저 읽는다. 사용자가 지정한 범위가 전체 흐름보다 우선한다.
- 요청된 여러 단계는 의존 순서로 처리한다. 이미 완료된 단계는 상태를 확인해 재사용하고 Issue/PR을 중복 생성하지 않는다. 선행 단계가 막히면 의존 작업만 보류하고 독립적인 문서·초안 준비는 진행한다.
- 인증 확인과 GitHub 쓰기 작업은 GitHub CLI(`gh`)를 사용한다. 먼저 `gh --version`, `gh auth status`와 대상 저장소의 Issue/PR 생성·Label 관리 권한을 확인한다. Git Credential Manager에서 토큰을 직접 추출하거나 출력·파일 저장하지 않는다. CLI가 없거나 미인증이면 사용자가 직접 설치·로그인하도록 안내하고, 권한 부족 시 필요한 조치를 먼저 알린다. 다른 인증 수단으로 우회하지 않는다.
- Issue/PR 작성·라벨 적용·Commit/Push는 요청에 포함된 범위에서 처리한다. Merge·Issue Close·배포는 **각각** 완료 조건과 사용자의 명시적 승인을 확인한다. 세션에서 이미 받은 해당 승인이 유효하면 반복해서 묻지 않는다.
- 위험한 작업, 기존 작업 훼손 가능성, 규칙 충돌, 불명확한 대상·우선순위·일정은 임의로 결정하지 않고 근거와 영향을 설명한 뒤 질문한다.
- 완료 보고에는 변경 파일 또는 Issue/PR 링크, 수행 단계, 검증 결과, 보류 사유·후속 확인 사항을 간결하게 적는다. 실행하지 않은 일을 완료로 보고하지 않는다.

## 9. 최초 확인 기록 (2026-10-12)

이 기록은 설정의 보장이 아니며 실제 GitHub 작업 전에 다시 조회한다.

- 원격: `https://github.com/Dundeuni/Dundeuni-FE.git`. 기본 브랜치 `main`, Issues 사용 가능.
- 로컬 `main`/`dev`는 각각 같은 원격 브랜치를 추적한다. 작업 시작 시 `main`, 미커밋 변경 없음. 원격 브랜치는 `main`, `dev`, `feature/ar_screen`이며 기존 브랜치 이름은 변경하지 않았다.
- 원격 SHA와 로컬 추적 참조를 대조한 결과 `main`은 `dev`보다 8커밋 앞서 있다. 이를 자동으로 동기화하지 않았으며 다음 개발/통합 작업 전에 차이 처리가 필요하다.
- 공개 API에서 세 브랜치 모두 `protected: false`, 조회 가능한 Ruleset 목록은 비어 있었다. 팀 문서의 리뷰/직접 Push 금지 규칙이 서버에서 강제된다고 가정하지 않는다. Merge 방식 허용값·계정 권한은 비인증 응답에 없어 미확인이다.
- 원격에는 기본 영문 라벨 10개만 있고 `labels.yml`의 팀 라벨은 미반영이다. 라벨 동기화는 별도 작업이 필요하다.
- 로컬 `.github/workflows/`와 루트 `AGENTS.md`는 없었다. `gh`는 PATH에서 찾지 못해 CLI 인증은 미확인이다.
- README의 `./FEConventions.md` 링크와 실제 `docs/FEconventions.md` 경로가 다르다. 이번 범위에서는 README·템플릿·원격 설정을 수정하지 않았다.
- 최초 문서 작성 단계에서는 브랜치 전환 없이 작업 트리에 두 파일만 추가했으며, Issue/PR 생성, Commit, Push, Merge, 배포는 수행하지 않았다. 이후 작업 상태는 실제 Git 및 GitHub 이력을 기준으로 확인한다.
