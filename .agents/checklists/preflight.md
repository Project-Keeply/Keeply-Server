# Preflight Checklist

작업 시작 전(이륙 전) 아래 항목을 순서대로 확인합니다.

## 1. 브랜치 컨텍스트

- `git rev-parse --abbrev-ref HEAD`로 현재 브랜치 확인
- feature 브랜치인지 확인 (`develop`/`main`에서 직접 작업 금지)
- 브랜치명이 `{type}/{description}/#{issue-number}` 형식인지 확인

## 2. 이슈 컨텍스트

- 브랜치명에서 이슈 번호 추출
- `gh issue view {번호}`로 이슈 내용 파악
- 이슈의 Tasks 목록 확인

## 3. 코드 컨텍스트

- `git diff develop...HEAD --name-status`로 이미 변경된 파일 확인
- 이전 작업 컨텍스트가 있으면 이어받기

## 4. 필독 문서 확인

- AGENTS.md의 "Required Reading" 섹션 재확인
- 작업 유형에 해당하는 docs/rules/* 문서 숙지

## 5. 사용자 프리뷰 및 승인

- 작업 범위와 영향받는 파일 목록을 사용자에게 프리뷰
- 명시적 승인 받기 (승인 없이 진행 금지)

## 예외

Trivial fix (오타, 세미콜론 누락 등 1파일 & 5줄 이하 수정)는 3-5단계 생략 가능.
단, 사용자에게 "trivial fix로 판단하여 바로 진행" 이라고 사전 고지할 것.
