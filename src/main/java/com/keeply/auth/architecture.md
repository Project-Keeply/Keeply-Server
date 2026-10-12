# Kakao OAuth2 + JWT 아키텍처

API·쿠키·오류 코드·배포 계약의 source of truth는 [인증 토큰 계약](../../../../../../docs/setup/auth-token.md)이다. 이 문서는 실제 구현의 계층과 처리 흐름을 설명한다.

## 로그인

1. 클라이언트가 카카오 인가 흐름에서 받은 `code`를 `POST /auth/kakao/callback` JSON으로 전송한다. 서버가 현재 받는 요청 필드는 `code`이며, OAuth `state` 검증은 현재 서버 구현에 포함되어 있지 않다.
2. `AuthenticationOriginFilter`가 정확한 허용 Origin을 확인하고 `AuthServiceImpl`이 카카오 토큰·사용자 정보를 조회한다.
3. `AuthAccountServiceImpl.processLogin`이 트랜잭션에서 기존 사용자 행을 잠그고 사용자 정보를 반영한다. 신규 사용자에게는 고유 kakao_id 제약을 적용한다.
4. 로그인마다 새로운 `sessionId`, 발급마다 새로운 `jti`로 JWT를 만든다. DB에는 최신 Refresh Token의 SHA-256 해시·만료 시각·sessionId만 저장한다.
5. 트랜잭션 성공 후 Controller는 Access Token만 JSON으로 응답하고 Refresh Token은 `keeply_refresh_token` HttpOnly 쿠키로 발급한다. 내부 `TokenIssueResult`를 JSON 응답으로 노출하지 않는다.

## 재발급·로그아웃·탈퇴

- `POST /auth/reissue`: body 없이 쿠키를 사용한다. JWT 서명·만료·type·claim을 검증한 뒤 사용자 행에 비관적 쓰기 잠금을 잡는다. 잠금 후 만료를 다시 확인하고 사용자 상태·DB 만료·sessionId·최신 해시를 검사한다. 같은 sessionId와 새 jti로 회전한다. 실패에는 쿠키 변경 헤더가 없다.
- `POST /auth/logout`: Access Token 없이 쿠키의 서명된 sessionId로 세션을 폐기한다. 같은 세션의 이미 소비된 이전 토큰도 로그아웃할 수 있다. 다른 로그인 세션은 폐기하지 않는다. 쿠키 누락·무효는 멱등 성공, DB 장애는 5xx다. 성공 시 같은 scope로 쿠키를 만료한다.
- 회원 탈퇴: 사용자 행 잠금 후 OWNER 탈퇴 제한을 확인하고 그룹 멤버 탈퇴, Refresh 세션 삭제, 사용자 soft-delete를 처리한다. 인증의 사용자 → Refresh 레코드 잠금 순서를 따른다.
- 사용자당 Refresh 세션 하나다. 이전 토큰 재사용은 401로 거부하되 최신 세션을 삭제하지 않는다. 잠금은 DB 최종 상태를 직렬화하며 브라우저 응답 도착 순서까지 보장하지 않는다.
- 구 `/auth/refresh`와 body 기반 Refresh DTO는 제거했다. 성공 응답의 토큰 필드는 `accessToken` 하나다. `refreshToken`, `tokenType`, `expiresIn`은 응답 필드가 아니다.

## 인증·시간·쿠키

- 보호 API는 `Authorization: Bearer <accessToken>`을 사용한다. 쿠키 인증 POST 3개는 공개 경로이며 JWT 필터가 Authorization을 파싱하지 않는다.
- Access Token은 기존 설정 2시간, Refresh Token은 14일이다. JWT 발급·검증, DB Refresh 만료, 쿠키 남은 수명은 주입된 Clock을 기준으로 한다.
- 세션 폐기는 기존 Access Token을 즉시 무효화하지 않는다. Access Token은 기존 정책대로 만료까지 유효하다.
- 쿠키는 Path=/auth, Domain 생략, HttpOnly=true다. 로컬은 Lax/비Secure, 운영 교차 사이트 HTTPS는 None/Secure를 기본값으로 한다. 운영 주소 확인 후 환경 설정을 확정한다.
- 인증 POST의 Origin은 정확히 허용 목록과 일치해야 한다. Origin 누락·null·불일치는 403이며 OPTIONS는 CORS에서 처리한다. credentials CORS와 Origin 검사는 같은 목록을 사용한다.
- JWT secret은 환경 변수로 관리하고 토큰 원문·쿠키·Authorization을 로그에 남기지 않는다.

## 구현·검증 경계

Service/Impl이 인증 트랜잭션을 소유하고 Controller는 커밋 후 쿠키를 작성한다. 쿠키 컴포넌트가 생성·회전·삭제 scope를 통일하며 공통 ApiResponse의 선택적 code가 오류 식별을 제공한다. 실제 OpenAPI와 H2 통합 테스트, 조건부 MySQL 동시성 테스트는 `src/test/java/com/keeply/auth/integration/`에 있다. MySQL을 실행하지 못한 경우 동시성과 Flyway 적용을 검증 완료로 보고하지 않는다. V7 적용 시 기존 Refresh 레코드가 삭제되므로 재로그인이 필요하다. #63 카카오 Origin별 redirect URI 수정은 이 브랜치에 포함하지 않는다.
