# Postflight Checklist

작업 완료 후(착륙 후) 아래 검증을 순서대로 실행합니다.

## 1. 코드 품질 검증 (코드 변경 시)

- `./gradlew spotlessCheck` 통과 (포맷 검사)
  - 실패 시 `./gradlew spotlessApply`로 자동 포맷 후 재검증
- `./gradlew compileJava` 통과 (컴파일 검증)
- `./gradlew test` 통과
- `./gradlew build` 성공
- 실패 시 원인 파악 후 재시도 (에러 무시 금지)

## 2. 변경 범위 검증

- `git diff --stat`로 변경 규모 확인
- 의도한 파일만 변경됐는지 확인
- 삭제된 코드에 실제 사용처가 없는지 확인

## 3. 컨벤션 준수

- 코딩 컨벤션: docs/rules/coding-convention.md
- Git 컨벤션: docs/rules/git-convention.md

## 4. 문서 영향

- 코드 구조/규칙 변경 시 docs/ 업데이트 필요 여부 확인
- 새 스킬/규칙 추가 시 AGENTS.md 라우팅 업데이트 필요 여부 확인

## 5. API 스펙 관측 (Controller / DTO 변경 시)

- Swagger(SpringDoc) UI에서 실제 스펙 확인 (`/swagger-ui.html`)
- 요청/응답 스키마, 상태 코드, 인증 요구사항 확인
- 확인 못 하면 "확인 못 함"이라고 명시 (성공 주장 금지)

## 원칙

- 검증 건너뛰기 금지
- 검증 결과는 명확히 (성공/실패/N/A 중 하나)
