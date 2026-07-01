---
name: create-issue
description: 사용자 인터뷰를 통해 이슈 내용을 채우고 gh CLI로 GitHub 이슈를 생성하는 스킬. "이슈 만들어줘", "GitHub 이슈 생성", "이슈 올려야 해", "create-issue" 등의 요청에 반드시 이 스킬을 사용한다.
---

# create-issue 스킬

사용자 인터뷰를 통해 이슈 내용을 채우고, gh CLI로 GitHub 이슈를 생성한다.

---

## Step 1: 전제 조건 확인

### 1.1 gh CLI 설치 확인
```bash
gh --version
```
- 설치 안 되어 있으면 중단:
  "GitHub CLI(gh)가 설치되어 있지 않습니다.
   아래 명령어로 설치해주세요:

   Mac:   brew install gh
   Windows: winget install GitHub.cli

   설치 직후에는 PATH가 반영되도록 터미널을 닫았다가 다시 열거나, 새 터미널 세션에서 진행해주세요.
   이후 `gh auth login` 으로 GitHub 로그인을 해주세요."

### 1.2 gh CLI 인증 확인
```bash
gh auth status
```
- 인증 안 되어 있으면 중단: "`gh auth login`으로 먼저 인증해주세요."

### 1.3 현재 브랜치 확인
```bash
git rev-parse --abbrev-ref HEAD
```
- `develop` 또는 `main` 브랜치가 아니면 경고:
  "현재 브랜치가 `{CURRENT_BRANCH}`입니다. 이슈는 보통 `develop`에서 생성합니다. 계속 진행할까요?"
  - 사용자가 거부하면 중단

---

## Step 2: 이슈 제목 입력

사용자에게 이슈 제목을 요청한다.

"이슈 제목을 입력해주세요. (예: `카카오 OAuth 로그인 API 구현`)"

- 입력받은 값을 `ISSUE_TITLE`로 저장
- 제목 앞에 `[Task] ` 프리픽스를 자동으로 붙인다 → `[Task] {ISSUE_TITLE}`
- 단, 사용자가 이미 `[`로 시작하는 제목을 입력했으면 프리픽스를 붙이지 않는다

---

## Step 3: 인터뷰

각 섹션을 순서대로 질문한다. 입력 없이 넘어가면 `-` 로 채운다.

### 3.1 Tasks (할 일 목록)
"이슈에서 완료해야 할 작업을 입력해주세요.
여러 개면 쉼표(,) 또는 줄바꿈으로 구분해주세요.
없으면 엔터로 건너뛰세요."

- 입력값을 `TASKS` 배열로 파싱
- 각 항목을 `- [ ] {항목}` 체크박스 형식으로 변환

### 3.2 Description (설명)
"이슈의 목적이나 이유를 설명해주세요.
없으면 엔터로 건너뛰세요."

- 입력받은 값을 `DESCRIPTION`으로 저장

### 3.3 ETC (기타)
"추가 참고 사항이나 스크린샷 URL이 있으면 입력해주세요.
없으면 엔터로 건너뛰세요."

- 입력받은 값을 `ETC`로 저장

---

## Step 4: 이슈 본문 생성

아래 템플릿을 채워서 `/tmp/issue-body.md`에 저장한다.

```markdown
## ✅ Tasks (To-Do)
{TASKS 항목들 — 체크박스 형식}

## 📝 Description
{DESCRIPTION}

## 📎 ETC
{ETC}
```

- 입력값이 없는 섹션은 `-` 로 채운다
- 본문은 항상 한국어로 작성한다

---

## Step 5: 프리뷰 확인

생성할 이슈 내용을 사용자에게 보여주고 승인을 받는다.

```
---
📋 이슈 프리뷰

제목: {최종 ISSUE_TITLE}

본문:
{생성된 이슈 본문 전체}
---

이 내용으로 이슈를 생성할까요? (승인 / 수정 요청)
```

- **승인** → Step 6으로
- **수정 요청** → 사용자 피드백 반영 후 Step 4부터 다시 실행

---

## Step 6: 이슈 생성

```bash
gh issue create \
  --title "{최종 ISSUE_TITLE}" \
  --body-file /tmp/issue-body.md
```

- 성공 시: "✅ 이슈 생성됨: {URL}" 출력
- 실패 시: 에러 메시지 표시 후 수동 생성 안내

---

## 중요 규칙

1. **이슈 본문은 항상 한국어로 작성**
2. **반드시 사용자 프리뷰 승인 후에만 이슈를 생성한다**
3. **이슈 생성 외의 작업(커밋, 브랜치 생성 등)은 절대 하지 않는다**
