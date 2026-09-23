# GitHub 라벨 가이드

라벨 정의는 [labels.yml](./labels.yml)에서 관리합니다. 이슈에는 `작업 유형 + 도메인 + 우선순위` 조합을 권장합니다.

예시:

```text
✨ feature + 🔎 Analysis + 🔥 P0 + 📡 API
🐛 fix + 🔐 Auth/User
🛠 chore + ⚙️ Common
```

PR에는 리뷰 요청 시 `👀 need review`, 외부 결정이나 협의가 필요해 진행할 수 없을 때 `🚧 blocked`를 추가합니다.

`labels.yml`은 GitHub에 자동 반영되지 않습니다. 저장소 설정에서 수동으로 만들거나 label sync 도구를 사용해 적용합니다.
