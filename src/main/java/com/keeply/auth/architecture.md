# Kakao OAuth2 + JWT Architecture

## 1. Scope
- Purpose: define backend authentication pipeline for Kakao OAuth2 login and JWT-based API authentication.
- Current stage: backend-first implementation without frontend dependency.

## 2. Authentication Flow (Backend Callback)
1. Client moves user to Kakao authorization endpoint.
2. Kakao redirects the client with `code` (and `state`).
3. Client sends `POST /auth/kakao/callback` with `{ "code": "...", "state": "..." }` in the request body.
4. Backend validates `state`.
5. Backend exchanges `code` for Kakao access token.
6. Backend calls Kakao user info API and extracts `kakaoId`.
7. Backend finds user by `kakaoId`; creates user if not found.
8. Backend issues Keeply JWT tokens (`accessToken`, `refreshToken`).
9. Client uses `Authorization: Bearer <accessToken>` for protected APIs.
10. On access token expiry, client calls refresh endpoint.

## 3. Final Token Policy
- Multi-device policy: one refresh token per user.
- Refresh reissue policy: rotation.
- Storage: MySQL.
- Storage format: refresh token hash only (no raw refresh token persistence).
- Expiration:
  - Access Token: 30 minutes
  - Refresh Token: 14 days

## 4. Endpoint Contract
- `POST /auth/kakao/callback`
  - Input (request body): `{ "code": "...", "state": "..." }`
  - Output: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`
  - Failure:
    - `400`: invalid `code` or `state`
    - `401`: invalid/expired token
    - `500`: Kakao integration failure

- `POST /auth/refresh`
  - Input (request body): `{ "refreshToken": "..." }`
  - Behavior: validate token, verify stored hash, issue new access token and new refresh token (rotation)

- `POST /auth/logout`
  - Behavior: delete user's refresh token record

## 5. Refresh Token Lifecycle
1. Login success: generate refresh token, store hash in MySQL.
2. Refresh request:
   - verify JWT signature/expiry,
   - compare hash with stored value,
   - if valid: issue new token pair and replace stored hash,
   - if invalid/missing/expired: return `401`.
3. Logout: delete refresh token record.
4. Withdrawal: delete all refresh token records for the user and process user status.
5. Reuse/abuse detection: force re-login.

## 6. Security Requirements
- `state` verification is mandatory.
- `JWT secret` must be managed via environment variables.
- HTTPS is required in production.
- Error responses must not expose internal failure causes or provider internals.

## 7. Implementation Notes
- Kakao token is used only for identity verification in login flow.
- Keeply APIs trust only Keeply-issued JWT.
- Future extension: if multi-device sessions are required, add `deviceId` and move to per-device refresh token records.

---

# Kakao OAuth2 + JWT 아키텍처 (KOR)

## 1. 범위
- 목적: Kakao OAuth2 로그인과 JWT 기반 API 인증 파이프라인을 백엔드 기준으로 정의한다.
- 현재 단계: 프론트엔드 연동 전, 백엔드 우선 구현 단계.

## 2. 인증 플로우 (백엔드 콜백 방식)
1. 클라이언트가 사용자를 Kakao 인가 엔드포인트로 이동시킨다.
2. Kakao가 클라이언트에게 `code`와 `state`를 전달한다.
3. 클라이언트가 `POST /auth/kakao/callback` 요청 본문으로 `{ "code": "...", "state": "..." }`를 전송한다.
4. 백엔드가 `state`를 검증한다.
5. 백엔드가 `code`로 Kakao access token을 발급받는다.
6. 백엔드가 Kakao 사용자 정보 API를 호출해 `kakaoId`를 추출한다.
7. 백엔드가 `kakaoId`로 사용자 조회 후, 없으면 신규 생성한다.
8. 백엔드가 Keeply JWT(`accessToken`, `refreshToken`)를 발급한다.
9. 클라이언트는 보호 API 호출 시 `Authorization: Bearer <accessToken>`을 사용한다.
10. Access Token 만료 시 클라이언트가 refresh 엔드포인트를 호출한다.

## 3. 최종 토큰 정책 
- 다기기 정책: 사용자당 Refresh Token 1개.
- 재발급 정책: Rotation.
- 저장소: MySQL.
- 저장 방식: Refresh Token 원문 저장 금지, 해시 저장.
- 만료 시간:
  - Access Token: 30분
  - Refresh Token: 14일

## 4. 엔드포인트 계약
- `POST /auth/kakao/callback`
  - 입력 (request body): `{ "code": "...", "state": "..." }`
  - 출력: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`
  - 실패:
    - `400`: 잘못된 `code` 또는 `state`
    - `401`: 토큰 무효/만료
    - `500`: Kakao 연동 실패

- `POST /auth/refresh`
  - 입력 (request body): `{ "refreshToken": "..." }`
  - 동작: 토큰 검증, 저장된 해시 검증 후 access/refresh 재발급(rotation)

- `POST /auth/logout`
  - 동작: 사용자 refresh token 레코드 삭제

## 5. Refresh Token 라이프사이클
1. 로그인 성공 시 refresh token 발급 후, 해시를 MySQL에 저장한다.
2. 재발급 요청 시:
   - JWT 서명/만료 검증,
   - 저장된 해시와 비교,
   - 유효하면 새 토큰 쌍 발급 후 해시 교체,
   - 무효/없음/만료면 `401` 반환.
3. 로그아웃 시 refresh token 레코드를 삭제한다.
4. 탈퇴 시 사용자 refresh token 전체 삭제 및 사용자 상태 처리.
5. 재사용/이상 징후 감지 시 강제 재로그인 처리.

## 6. 보안 요구사항
- `state` 검증은 필수.
- `JWT secret`은 환경변수로 관리.
- 운영 환경에서 HTTPS 필수.
- 에러 응답에 내부 원인/외부 제공자 상세 정보 노출 금지.

## 7. 구현 메모
- Kakao 토큰은 로그인 시 신원 확인 용도로만 사용한다.
- Keeply API 인증은 Keeply 발급 JWT만 신뢰한다.
- 추후 다기기 세션이 필요하면 `deviceId`를 도입해 단말별 refresh token 저장 구조로 확장한다.
