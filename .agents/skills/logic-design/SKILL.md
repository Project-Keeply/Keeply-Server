---
name: logic-design
description: 브랜치 컨텍스트를 자동으로 파악하고, 사용자와 함께 코드 구현 계획을 수립해 카테고리별 체크리스트로 정리하는 스킬. "로직 설계해줘", "구현 계획 세워줘", "설계 좀 도와줘", "체크리스트 만들어줘", "logic-design" 등의 요청에 반드시 이 스킬을 사용한다.
---

# 로직 설계 스킬

브랜치 컨텍스트를 자동으로 파악하고, 사용자와 함께 코드 구현 계획을 수립해 체크리스트로 정리한다.

---

## Step 1: 컨텍스트 자동 파악

### 1.1 브랜치 및 이슈 정보 수집

```bash
git rev-parse --abbrev-ref HEAD
```

- 브랜치명에서 이슈 번호 추출 (형식: `feat/description/#숫자`)
- 추출 성공 시:
  ```bash
  gh issue view {ISSUE_NUMBER} --json title,body
  ```
- 추출 실패 시: `ISSUE_NUMBER=null` → 이슈 내용 없이 브랜치명만으로 컨텍스트 파악

### 1.2 관련 코드 파악

```bash
git diff develop...HEAD --name-status
```

- 이미 변경된 파일이 있으면 파악해서 컨텍스트에 포함

### 1.3 컨텍스트 요약 및 확인

파악한 내용을 아래 형식으로 사용자에게 보여준다.

```
컨텍스트 파악 완료

브랜치: {CURRENT_BRANCH}
이슈: #{ISSUE_NUMBER} — {이슈 제목}
이슈 내용: {이슈 본문 요약}
변경된 파일: {있으면 목록, 없으면 "없음"}
```

"이 내용이 맞나요? 추가로 설명할 내용이 있으면 말씀해주세요."

- 사용자가 보완 내용을 주면 컨텍스트에 반영하고 계속 진행
- 맞다고 하면 바로 Step 2로

---

## Step 2: 구현 설계 인터뷰

아래 항목들을 **자연스러운 대화 흐름** 으로 질문한다.
한 번에 다 묻지 말고, 이전 답변을 바탕으로 필요한 항목만 골라서 질문한다.

### 질문 항목 (상황에 따라 선택)

**Controller / API**
- 새로운 엔드포인트가 추가되나요? (HTTP method / URI / 요청·응답 스펙)
- 기존 Controller 중 수정이 필요한 게 있나요?
- API 응답 포맷은 기존 공통 응답 구조를 따르나요?

**Service (인터페이스 / 구현)**
- 새로 만들 Service가 있나요? (인터페이스 + Impl 분리 원칙 준수)
- 기존 Service의 시그니처가 변경되나요?
- 트랜잭션 경계(`@Transactional`) 설정이 필요한가요?

**Repository / DB**
- 신규 쿼리 메서드가 필요한가요? (JPA 메서드 이름 / `@Query` / QueryDSL)
- Entity 스키마 변경이 있나요? → Flyway 마이그레이션 필요 여부
- 연관관계 매핑(OneToMany 등)이나 지연/즉시 로딩 결정이 필요한가요?

**Entity / DTO**
- 새 Entity가 필요한가요? (컬럼 / 제약조건 / 인덱스)
- 요청 DTO(`Create...Request`, `Update...Request`) / 응답 DTO(`...Response`) 설계는?
- Entity ↔ DTO 매핑은 어디서 처리하나요? (Service 계층 원칙)

**Security / 인증**
- 인증이 필요한 엔드포인트인가요? (JWT / SecurityConfig 수정 필요 여부)
- 특정 권한/역할 체크가 필요한가요?
- Kakao OAuth 플로우에 영향이 있나요?

**예외 처리**
- 신규 Custom Exception이 필요한가요? (`...NotFoundException` 등)
- 기존 GlobalExceptionHandler에 추가할 항목이 있나요?
- 응답 에러 코드 정의가 필요한가요?

**Validation**
- Bean Validation 어노테이션이 필요한 필드가 있나요?
- 복합 검증(도메인 규칙)이 필요한 경우 어디서 처리하나요? (Controller vs Service)

**기타**
- Swagger 문서화(`@Operation`, `@Schema`)가 필요한가요?
- 외부 연동(S3, 외부 API 등)이 있나요?
- 성능/트랜잭션/N+1 등 특별히 고려할 사항이 있나요?

---

## Step 3: 구현 체크리스트 생성

인터뷰 내용을 바탕으로 **카테고리별 구현 체크리스트** 를 생성해 대화 내에 출력한다.

### 출력 형식

```
## 구현 체크리스트 — {브랜치명}

### 파일 구조
- [ ] {생성/수정할 파일 경로 및 역할}

### Controller
- [ ] {메서드명} — {HTTP method / URI / 요청·응답 요약}

### Service
- [ ] {인터페이스 / Impl} — {비즈니스 로직 요약, 트랜잭션 경계}

### Repository / DB
- [ ] {쿼리 메서드 / Entity 스키마 변경 / Flyway 마이그레이션}

### Entity / DTO
- [ ] {Entity 필드 / DTO 필드 및 매핑 방식}

### Security / 인증
- [ ] {인증·인가 정책 / SecurityConfig 수정}

### 예외 처리
- [ ] {Custom Exception / 에러 코드 / GlobalExceptionHandler 반영}

### Validation
- [ ] {검증 항목 및 처리 위치}

### Swagger
- [ ] {문서화 어노테이션 추가 대상}

### 고려사항
- [ ] {N+1, 트랜잭션, 성능, 외부 연동 등}
```

- 해당 카테고리가 없으면 섹션 생략
- 각 항목은 최대한 구체적으로 (파일 경로, 메서드 시그니처, 어노테이션 등 포함)
- 체크리스트 아래에 Claude의 구현 제안이나 주의사항이 있으면 짧게 덧붙인다

---

## 중요 규칙

1. **코드를 직접 작성하거나 수정하지 않는다** — 설계와 계획만 수립
2. **인터뷰는 대화형으로** — 필요 없는 질문은 건너뛰고, 사용자 답변을 바탕으로 자연스럽게 이어간다
3. **체크리스트는 구현 순서를 고려해 작성** — 의존성 있는 항목은 순서대로 (예: Entity → Repository → Service → Controller)
4. **서버 코딩 컨벤션 준수** — `docs/rules/coding-convention.md` 기반 (Controller/Service/ServiceImpl/Repository/Entity/DTO/Exception 네이밍, `var` 금지, `is` 접두사 등)
5. **모든 출력은 한국어로**
