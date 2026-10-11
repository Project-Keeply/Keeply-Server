# 쿠키 기반 인증 토큰 계약 (#64)

## 문서 상태와 범위

이 문서는 [서버 이슈 #64](https://github.com/Project-Keeply/Keeply-Server/issues/64)의 API 계약과 수용 기준이다. 단계 1–12의 코드 구현과 root 에이전트의 최종 검토·전체 빌드를 완료했다. 회원 탈퇴의 잠금 순서와 실제 HTTP·H2 DB·OpenAPI 통합 검증은 아래 실행 결과로 확인한다. 구현 여부와 검증 완료 여부를 구분하며, MySQL 동시성·마이그레이션·실제 브라우저 연동은 실행 결과가 있어야 완료로 표시한다.

- 작업 브랜치: `fix/refresh-token-rotation/#64` (`develop` 기준).
- #63의 카카오 Origin별 redirect URI 변경은 별도 브랜치의 커밋 `6cf4e1a`에 보존되어 있으며, 이 작업에 포함하지 않는다. 해당 로그인 연동 변경이 필요한 환경은 별도로 통합 여부를 확인한다.
- 사용자당 Refresh 세션 하나와 DB의 SHA-256 토큰 해시 저장을 유지한다.
- Refresh Token의 전달 경로를 JSON에서 HttpOnly 쿠키로 전환하고, 회전의 고유성·원자성과 로그아웃 경합을 처리한다.
- Access Token의 기존 인증 방식과 유효 기간 설정을 유지한다. Refresh 세션 폐기는 이미 발급된 Access Token을 즉시 무효화하지 않으며, Access Token은 기존 정책에 따라 만료까지 유효하다.

## API 계약

모든 아래 POST 요청에는 서버가 허용한 정확한 `Origin`과 브라우저의 credentials 설정이 필요하다. JSON 응답은 기존 `ApiResponse` 형식을 유지한다.

| API | 요청 | 인증 조건 | 성공 처리 |
|---|---|---|---|
| `POST /auth/kakao/callback` | JSON `{ "code": "카카오 인가 코드" }` | Access Token 불필요 | 새 로그인 세션 생성, Access Token JSON 반환, Refresh 쿠키 발급 |
| `POST /auth/reissue` | body 없음, Refresh 쿠키 | Access Token 불필요 | 현재 Refresh Token 검증·소비, Access Token JSON 반환, Refresh 쿠키 회전 |
| `POST /auth/logout` | body 없음, Refresh 쿠키 | Access Token 불필요; 만료된 Access Token도 처리 방해하지 않음 | 해당 Refresh 세션 폐기, Refresh 쿠키 만료, `data: null` 반환 |

로그인과 재발급 성공 응답:

```json
{
  "success": true,
  "data": {
    "accessToken": "..."
  },
  "message": null
}
```

로그아웃 성공 응답:

```json
{
  "success": true,
  "data": null,
  "message": null
}
```

- Refresh Token은 성공 JSON에서 제외하고 `Set-Cookie`로만 전달한다. 클라이언트는 Refresh Token을 JavaScript로 읽거나 요청 body에 넣지 않는다.
- `/auth/reissue`와 `/auth/logout`은 누락·만료·변조된 Authorization 헤더가 있어도 쿠키 인증 흐름을 실행할 수 있어야 한다. Access JWT 필터와 경로 허용 설정을 함께 확인한다.
- 기존 `POST /auth/refresh`와 `ReissueRequest`는 제거한다. 새 경로를 사용하는 클라이언트와 배포 순서를 맞추며, 기존 JSON 기반 재발급을 위한 호환 API는 제공하지 않는 목표다.
- 외부 DTO `LoginResponse`에는 `accessToken`만 둔다. 서비스 내부의 `TokenIssueResult`는 Access Token, Refresh Token과 만료 시각을 전달하며 API 응답으로 직렬화하지 않는다.

## 쿠키 계약

| 항목 | 설정 |
|---|---|
| 이름 | `keeply_refresh_token` |
| HttpOnly | `true` |
| Path | `/auth` |
| Domain | 생략; API 호스트에만 적용 |
| Max-Age | 발급된 Refresh Token의 남은 수명 기준 |
| 로컬 HTTP 개발 | `SameSite=Lax`, `Secure=false` |
| 운영 교차 사이트 HTTPS | `SameSite=None`, `Secure=true` |

쿠키 설정은 환경별 설정으로 분리하고 하나의 쿠키 컴포넌트에서 생성·교체·삭제한다. 삭제는 같은 이름·Path·Domain scope를 사용하고 `Max-Age=0`으로 처리한다. `SameSite`와 `Secure`도 발급 설정을 재사용한다.

환경 변수 `APP_AUTH_COOKIE_SECURE`와 `APP_AUTH_COOKIE_SAME_SITE`는 각각 `app.auth-cookie.secure`, `app.auth-cookie.same-site`에 매핑한다. dev 기본값은 `false`/`Lax`, prod 기본값은 `true`/`None`이다. 다른 프로필의 컴포넌트 기본값은 `false`/`Lax`이다. SameSite는 `Lax`, `Strict`, `None`만 허용하며 `None`일 때 Secure가 반드시 `true`여야 한다. 잘못된 설정은 서버 시작 시 검증 오류로 거부한다.

쿠키 수명은 주입된 `Clock`의 현재 시각부터 발급 결과의 만료 시각까지 남은 시간을 초 단위로 내림하여 계산한다. 남은 시간이 1초 미만이면 사용할 수 있는 쿠키를 발급하지 않고 오류로 처리한다. 토큰 수명이 쿠키 때문에 연장되지 않게 한다.

실제 운영 프런트엔드/API 주소와 배치 관계는 아직 확인되지 않았다. 운영 주소를 임의로 정하지 않으며, 도메인과 HTTPS 확인 후 SameSite·Secure·Origin 허용 목록을 확정한다. 로컬 API 기준은 `http://localhost:8080`이며 클라이언트 Origin은 실제 개발 주소를 설정한다.

- 로그인 성공과 재발급 성공에만 새 Refresh 쿠키를 발급한다.
- 명시적 로그아웃 성공에서 쿠키를 삭제한다. 쿠키 누락·무효에 따른 멱등 성공도 동일하게 삭제한다.
- 재발급 실패, Origin 거부, 서버 오류 등의 실패 응답에는 쿠키 발급·교체·삭제를 하지 않는다. 늦게 도착한 실패 응답이 다른 요청의 최신 쿠키를 지우지 않게 한다.
- 로그아웃 DB 처리가 실패하면 성공으로 숨기지 않고 5xx로 응답한다. 이 실패에서도 쿠키를 삭제하지 않는다.

## 토큰과 세션 모델

Refresh JWT에는 서명된 사용자 ID(`sub`), `type=refresh`, 발급·만료 시각, 발급별 무작위 `jti`, 로그인 세션을 식별하는 `sessionId`를 포함한다.

- `jti`는 Refresh Token을 발급할 때마다 새로 만든다. 동일 사용자·동일 시각 연속 발급에서도 토큰 문자열이 달라져 이전 토큰이 실제로 소비된다.
- `sessionId`는 로그인마다 새로 만들고 같은 로그인 세션의 재발급에서 유지한다. DB Refresh 레코드에도 같은 값을 저장한다.
- DB에는 Refresh Token 원문을 저장하지 않는다. 사용자별 최신 토큰 SHA-256 해시·만료 시각·세션 식별자를 저장한다.
- 새 로그인은 기존 Refresh 세션을 교체한다. 이전 로그인 세션의 토큰으로 최신 로그인 세션을 로그아웃할 수 없다.
- 주입된 `Clock`을 JWT 발급·검증, DB 만료 시각, 쿠키 남은 수명 계산에 사용한다. 동일 발급 결과의 만료 시각을 전달하여 각 계층이 별도로 만료를 계산하는 차이를 줄인다.

## 잠금과 회전 처리

기존 사용자 로그인·재발급·로그아웃·회원 탈퇴는 같은 사용자 행에 `PESSIMISTIC_WRITE` 잠금을 사용한다. 동일 사용자 상태 변경은 **사용자 행 → Refresh 레코드** 순서로 접근한다. 신규 사용자 생성 시에는 사용자 식별자의 유일 제약과 동시 최초 로그인 처리도 확인한다.

재발급은 다음 순서로 실행한다.

1. 쿠키를 읽고 JWT 서명·만료·타입·사용자 ID·세션 claim을 검증한다.
2. 트랜잭션에서 사용자 행 잠금을 획득한다.
3. 잠금 대기로 시간이 경과할 수 있으므로 JWT 만료를 다시 확인하고 사용자 탈퇴 상태를 검사한다.
4. 저장된 Refresh 레코드 유무, DB 만료, 세션 일치와 최신 해시 일치를 검사한다.
5. 같은 `sessionId`와 새 `jti`로 Access/Refresh Token을 발급하고 최신 해시·만료 시각을 저장한다.
6. 트랜잭션 커밋 성공 후 Controller에서 새 쿠키와 Access Token 응답을 작성한다.

동일 Refresh Token으로 동시 재발급하면 한 요청만 성공한다. 나머지는 잠금 후 최신 해시 불일치를 확인해 `REFRESH_TOKEN_MISMATCH`로 거부한다. 거부가 최신 세션을 폐기하거나 쿠키를 삭제하지 않는다. 이 정책은 정상 경합과 악의적 재사용을 응답만으로 구분하지 않는다.

잠금 시간 초과나 DB 장애를 토큰 무효로 바꾸지 않는다. 트랜잭션 실패 시 새 쿠키를 내보내지 않으며 기존 서버 오류 응답 정책을 따른다.

## 로그아웃과 회원 탈퇴

유효한 Refresh JWT를 제출한 로그아웃은 사용자 잠금 후 현재 DB의 `sessionId`를 비교한다. 최신 토큰 해시 일치까지 요구하지 않으므로 **같은 로그인 세션에서 이미 회전된 이전 토큰**도 그 세션을 폐기할 수 있다. JWT 서명·타입·claim·만료 검증을 통과하지 못한 토큰은 세션 폐기 권한을 갖지 않는다.

- 쿠키 누락, 무효·만료 토큰, 존재하지 않는 세션 또는 다른 로그인으로 교체된 세션은 멱등 성공으로 처리하고 요청 브라우저의 쿠키만 만료한다.
- 다른 `sessionId`를 가진 최신 로그인 세션의 DB 레코드는 삭제하지 않는다.
- 회원 탈퇴도 사용자 잠금 후 Refresh 세션을 폐기하고 사용자 상태를 변경한다. 탈퇴 상태의 사용자는 재발급할 수 없다.
- 재발급이 먼저 커밋되면 뒤의 로그아웃은 같은 세션을 삭제한다. 로그아웃이 먼저 커밋되면 뒤의 재발급은 세션 부재로 거부된다.
- 응답 도착 순서가 역전되면 브라우저에 이미 폐기된 세션의 쿠키가 남을 수 있다. 쿠키만으로 DB 세션이 복구되지는 않으며, 후속 재발급은 실패한다. 응답 순서와 브라우저 쿠키 저장까지 서버 잠금이 보장하지는 않는다.

클라이언트는 로그아웃/로그인 시 세션 세대 값을 바꿔 이전 세대의 늦은 재발급 응답에서 Access Token을 다시 저장하지 않아야 한다. 이미 발급된 Access Token의 만료 정책과 브라우저 응답 경합은 별개의 사항이다.

## Origin 검증과 CORS

로그인·재발급·로그아웃의 POST 요청은 허용 목록과 정확히 일치하는 `Origin`을 요구한다. Origin 누락, 문자열 `null`, 불일치는 `AUTH_ORIGIN_NOT_ALLOWED`(403)로 거부한다. 임의 부분 문자열·접미사 매칭으로 허용하지 않는다.

- OPTIONS preflight는 Origin 검사에서 POST처럼 차단하지 않는다. CORS 정책이 허용한 Origin·메서드·헤더에 대해 처리한다.
- CORS는 정확한 Origin 목록과 credentials 허용을 유지한다. Origin 검사와 CORS는 같은 설정을 참조해 허용 기준이 어긋나지 않게 한다.
- API 호출 도구/직접 요청 테스트도 허용된 Origin 헤더를 명시한다. 운영에서 Origin 누락을 편의상 허용하는 예외는 두지 않는다.
- Refresh 쿠키를 사용하는 경로의 CSRF 방어는 위 Origin 검사로 구현한다. 쿠키 SameSite 설정만으로 교차 사이트 요청 방어가 완료된 것으로 처리하지 않는다.

## 오류 응답

`ApiResponse`에 선택적 `code`를 추가한다. 성공 응답에서는 생략할 수 있으며, 기존 클라이언트가 사용하는 `success`, `data`, `message`를 유지한다. 기존 공통/도메인 오류의 `COMMON_001`, `AUTH_001` 등 기존 코드 값은 변경하지 않는다. 아래 새 인증 계약의 code는 표의 문자열을 그대로 사용한다.

```json
{
  "success": false,
  "data": null,
  "message": "리프레시 토큰이 일치하지 않습니다.",
  "code": "REFRESH_TOKEN_MISMATCH"
}
```

| HTTP | code | 조건 |
|---|---|---|
| 401 | `REFRESH_TOKEN_MISSING` | 재발급 쿠키 누락·빈 값 |
| 401 | `REFRESH_TOKEN_EXPIRED` | 서명 검증이 가능한 JWT 또는 현재 DB Refresh 레코드 만료 |
| 401 | `REFRESH_TOKEN_INVALID` | 서명·형식·타입·필수 claim 오류, 사용자 무효·탈퇴, 세션 부재·불일치 |
| 401 | `REFRESH_TOKEN_MISMATCH` | 유효한 현재 세션의 토큰이지만 저장된 최신 해시와 불일치 |
| 403 | `AUTH_ORIGIN_NOT_ALLOWED` | 해당 인증 POST의 Origin 누락·`null`·불일치 |
| 5xx | 기존 서버 오류 코드 | DB/잠금 등 서버 처리 실패 |

로그아웃에서 무효 쿠키의 401 조건은 멱등 성공으로 처리한다. Origin 거부와 DB 장애는 멱등 성공의 대상이 아니다. 인증 검사 오류는 안정적인 code로 구분하며 클라이언트가 메시지 문자열에 의존하지 않도록 한다.

## 구현 순서

경로는 저장소 루트 기준이다. 단계별 담당자가 결과를 전달하고 root 에이전트가 검토한 후 원자 단위로 커밋한다.

| 단계 | 대상 | 결과 |
|---|---|---|
| 1 | `docs/setup/auth-token.md` | 목표 API·쿠키·회전·배포 계약 문서화 |
| 2 | `src/main/resources/db/migration/V7__add_refresh_token_session_id.sql` | 세션 식별자 컬럼 및 기존 Refresh 레코드 전환 |
| 3 | `src/main/java/com/keeply/auth/entity/RefreshToken.java` | 세션 식별자와 갱신 모델 |
| 4 | `src/main/java/com/keeply/user/repository/UserRepository.java` | 사용자 비관적 잠금 조회 |
| 5 | `src/main/java/com/keeply/auth/jwt/JwtProvider.java` | 발급별 고유값·세션 claim·타입·시간 검증 |
| 6 | `src/main/java/com/keeply/auth/service/` | Service/Impl 분리, 로그인·회전·로그아웃 트랜잭션 |
| 7 | `src/main/java/com/keeply/auth/dto/` | 외부 응답과 내부 발급 결과 분리 |
| 8 | `src/main/java/com/keeply/auth/cookie/` | 환경별 쿠키 설정·발급·삭제 공통화 |
| 9 | `src/main/java/com/keeply/auth/controller/AuthController.java` | 쿠키 API와 새 경로 적용, 구 경로 제거 |
| 10 | `src/main/java/com/keeply/common/config/SecurityConfig.java` 및 인증 Origin 검사 | 공개 인증 경로와 CSRF 방어·CORS 정합성 |
| 11 | `src/main/java/com/keeply/common/response/ApiResponse.java`, `src/main/java/com/keeply/common/exception/` | 선택적 오류 code와 새 인증 오류 응답 |
| 12 | `src/main/java/com/keeply/user/service/UserServiceImpl.java` | 회원 탈퇴와 인증의 잠금 순서 통일 |

서로 의존하는 DTO·오류 타입은 단계별 중간 컴파일 가능성과 최종 계약을 함께 검토한다. 테스트는 해당 변경과 함께 추가하고, 통합 시 `src/test/java/com/keeply/auth/` 및 회원 탈퇴 관련 테스트를 실행한다. 최종 구현 결과에 맞춰 `src/main/java/com/keeply/auth/architecture.md`도 갱신한다.

## 수용 기준과 검증

- [x] 고정된 같은 시각에 동일 사용자 Refresh Token을 반복 발급해 문자열·`jti`가 다르고 `sessionId`는 유지됨을 실제 JWT 생성으로 확인.
- [x] 새 로그인은 새로운 `sessionId`를 만들고 이전 세션의 재발급·로그아웃이 최신 로그인 세션을 변경하지 않음.
- [x] 토큰 누락·만료·서명 변조·잘못된 타입·필수 claim 오류·해시 불일치·세션 부재·탈퇴 상태를 거부.
- [x] 로그인/재발급 JSON에는 Refresh Token이 없고 쿠키의 이름·Path·HttpOnly·SameSite·Secure·수명이 계약과 일치.
- [x] 쿠키 생성/삭제 scope가 같고 모든 실패 응답에는 쿠키 변경 헤더가 없음.
- [x] Access Token 없이, 또는 만료된 Access Token을 보낸 경우에도 쿠키로 재발급/로그아웃 가능.
- [x] 로그아웃의 쿠키 누락·무효는 성공이고 DB 장애는 5xx이며 최신 세션은 DB에 맞게 처리됨.
- [x] 같은 세션의 이전 Refresh Token으로 로그아웃하면 현재 Refresh 세션도 폐기됨.
- [ ] 실제 MySQL 병렬 재발급에서 정확히 한 요청만 성공하고 패자는 명확한 401이며 성공 토큰으로 후속 재발급 가능.
- [ ] 실제 MySQL 재발급/로그아웃/로그인/탈퇴 경합에서 최종 상태가 직렬 처리 정책과 일치하고 폐기된 세션이 복구되지 않음.
- [x] JWT 만료 직전 잠금 대기 후 만료를 다시 검사함 (서비스 단위 테스트에서 잠금 획득 전후 만료를 모사; 실제 MySQL 잠금 대기 검증은 미완료).
- [x] MVC/필터 테스트에서 정확히 허용한 Origin만 성공, 누락·`null`·불일치는 403, 정상 OPTIONS preflight는 성공.
- [ ] Flyway 마이그레이션을 기존 스키마/Refresh 데이터가 있는 MySQL과 신규 DB에 적용해 최종 스키마와 세션 전환 확인.
- [x] Spotless·컴파일·테스트·빌드를 실행하고 캐시 여부와 실제 실행 범위를 보고.
- [x] 실제 생성된 OpenAPI에서 Refresh 요청 body/JSON 필드 제거·쿠키 파라미터·공개 인증 경로·선택적 응답 code 확인.
- [ ] 클라이언트 타입 재생성 및 실제 브라우저 쿠키 연동 검증.

동시성은 Mock Repository나 H2 결과만으로 보장했다고 보고하지 않는다. MySQL 실행이 불가능하면 해당 검증이 미완료임을 명시한다.

## 클라이언트 연동과 배포

클라이언트는 `withCredentials: true`, 같은 탭의 단일 재발급 Promise, 원요청 최대 1회 재시도를 적용한다. 다중 탭의 Web Locks/BroadcastChannel과 미지원 환경 대응은 클라이언트 구현·검증 범위다. 서버는 브라우저 잠금 지원 여부와 무관하게 Refresh Token 한 번 소비를 보장하며, 다중 탭에서 모든 요청이 성공한다고 보장하지 않는다.

V7 전환은 기존 Refresh 레코드를 삭제하고 `session_id` 컬럼을 추가하는 정책이다. 기존 토큰에는 세션 claim이 없으므로 배포 후 재발급은 불가하며 사용자는 재로그인해야 한다. 이미 발급된 Access Token의 남은 유효 기간은 유지되지만 만료 후 재로그인이 필요하다. Refresh 레코드 삭제는 롤백으로 복원되지 않는다.

배포 전 다음 순서로 확인한다.

1. 운영 프런트엔드/API 주소·HTTPS·사이트 관계를 확인하고 쿠키 설정과 정확한 Origin 허용 목록을 확정한다.
2. 스키마 백업 및 V7 적용 검증, 기존 Refresh 세션 종료와 재로그인 영향을 공유한다.
3. `/auth/reissue`·쿠키 로그인·쿠키 로그아웃을 사용하는 클라이언트와 서버 배포를 맞춘다. 구 클라이언트가 `/auth/refresh` 또는 로그인 JSON의 Refresh Token에 의존하는 기간을 남기지 않는다.
4. 서버의 OpenAPI 결과로 클라이언트 타입을 갱신하고 로그인 → 재발급 → 로그아웃을 실제 브라우저에서 검증한다.
5. 로그인·재발급·로그아웃의 성공/오류 상태와 DB/잠금 장애를 확인한다. 토큰 원문·쿠키·Authorization 헤더는 검증 로그에 남기지 않는다.

현재 미확정 사항은 운영 주소에 따른 쿠키/Origin 설정, 클라이언트의 Web Locks 미지원 환경 동작, 실제 배포 순서다. 확정 전 운영 준비 완료로 보고하지 않는다.

## 재현 가능한 통합 검증

`AuthTokenIntegrationTest`는 test 프로필의 H2에서 실제 Service 트랜잭션, JWT, SecurityFilterChain, Controller, OpenAPI를 검증한다. 카카오 외부 호출은 하지 않고 `processLogin`에 사용자 정보 fixture를 전달한다. H2 테스트는 MySQL 잠금 검증을 대체하지 않는다. 기존 GroupMember의 MySQL 전용 생성 컬럼 DDL은 H2에서 지원하지 않아 이 통합 테스트에만 동일 컬럼·제약의 CASE 기반 `src/test/resources/sql/auth-token-h2-group-members.sql` fixture를 적용한다. 운영 엔티티와 마이그레이션은 변경하지 않는다.

```bash
./gradlew test --tests '*UserServiceImplTest' --tests '*AuthTokenIntegrationTest'
```

`AuthTokenMySqlConcurrencyTest`는 MySQL 8의 독립 트랜잭션 병렬 요청을 검증한다. 환경 변수가 없으면 JUnit에서 명시적으로 skipped다. 환경 변수를 설정한 경우 다음 제약을 컨텍스트 시작 전 검사한다. URL·자격 증명 검증이 실패하면 연결하지 않는다. 검증 통과 후 datasource 등록·DDL 전에 읽기 전용 메타데이터 연결로 제품명 MySQL과 버전 8.x를 확인한다. MariaDB와 다른 버전은 거부하며 연결·읽기 제한 시간은 각각 3초다.

- URL은 `jdbc:mysql://localhost:<port>/keeply_auth_test_<suffix>` 또는 `127.0.0.1`만 허용한다. 쿼리 옵션은 허용하지 않는다.
- 별도로 만든 전용 테스트 DB만 사용한다. `keeply_dev`와 운영 DB는 허용하지 않는다.
- DB 계정/비밀번호는 `KEEPLY_AUTH_MYSQL_TEST_USERNAME`, `KEEPLY_AUTH_MYSQL_TEST_PASSWORD`에 명시한다.
- 읽기 전용 메타데이터 연결이 MySQL 8.x임을 확인해야 한다. MariaDB 및 MySQL 5/9 등 다른 메이저 버전은 거부한다.
- Hibernate `create-drop`이 이 전용 DB의 테이블을 생성·삭제한다. 공유 데이터가 있는 DB는 사용하지 않는다.
- Flyway는 이 테스트에서 비활성화한다. V7의 기존/신규 스키마 검증은 별도로 진행한다.

아래 명령의 DB와 자격 증명은 실행 전에 준비한다. 실제 값을 저장소나 테스트 로그에 기록하지 않는다.

```bash
export KEEPLY_AUTH_MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:3306/keeply_auth_test_rotation
export KEEPLY_AUTH_MYSQL_TEST_USERNAME=<전용_테스트_DB_계정>
export KEEPLY_AUTH_MYSQL_TEST_PASSWORD=<전용_테스트_DB_비밀번호>
./gradlew test --tests '*AuthTokenMySqlConcurrencyTest' --rerun-tasks
```

병렬 검증은 같은 토큰 두 번 소비, 회전/로그아웃, 회전/새 로그인, 회전/회원 탈퇴를 다룬다. 동시에 출발하도록 barrier를 사용하고 성공·401 code·최종 DB 상태 및 성공 토큰의 후속 사용을 확인한다. 실행 여부·테스트 수·실패·skipped 결과는 최종 검증 보고에 명시한다.

### 실제 Flyway 검증

`src/test/java/com/keeply/auth/integration/AuthTokenMySqlMigrationTest.java`는 Spring 컨텍스트 없이 실제 Flyway와 저장소의 V1~V7 SQL을 실행한다. 사전에 생성한 서로 다른 빈 MySQL 8 전용 DB 두 개가 필요하다. 위와 같은 URL·계정·제품/버전 가드를 적용하며 DB 생성·삭제·Flyway clean은 실행하지 않는다. 테이블이 있는 DB와 localhost/127.0.0.1 표현만 다른 동일 DB를 거부한다.

- 신규 DB: V1~V7 적용, Flyway validate와 성공 이력 7개, session_id 길이·NOT NULL·사용자 유일 제약·FK, 재실행 시 변경 없음.
- 기존 데이터: V1~V6 적용 후 사용자·그룹·멤버·공지·업무·유통기한 및 Refresh fixture를 삽입한다. V7 적용 후 Refresh만 삭제되고 나머지 테이블의 모든 열이 보존되는지, 새 세션 삽입과 NULL 세션 거부, 재실행 시 새 세션 보존을 확인한다.

```bash
export KEEPLY_AUTH_MYSQL_MIGRATION_NEW_URL=jdbc:mysql://127.0.0.1:3306/keeply_auth_test_migration_new
export KEEPLY_AUTH_MYSQL_MIGRATION_EXISTING_URL=jdbc:mysql://127.0.0.1:3306/keeply_auth_test_migration_existing
export KEEPLY_AUTH_MYSQL_TEST_USERNAME=<전용_테스트_DB_계정>
export KEEPLY_AUTH_MYSQL_TEST_PASSWORD=<전용_테스트_DB_비밀번호>
./gradlew test --tests '*AuthTokenMySqlMigrationTest' --rerun-tasks
```

각 URL 환경 변수가 없으면 해당 테스트는 skipped다. 실행 결과는 검증 DB에 남으므로 재검증에는 새 빈 DB를 준비한다. 검증 후 생성한 전용 DB만 정리한다. 2026-10-11 검증 재개 시 Docker 조회와 MySQL 핸드셰이크가 다시 시간 초과되어 실제 DB 테스트는 아직 실행하지 못했다. Docker 재시작은 다른 로컬 컨테이너에 영향을 줄 수 있어 사용자 확인을 요청했다.

테스트 추가 후 `./gradlew --gradle-user-home .gradle-local --offline --no-daemon spotlessCheck compileJava build`는 성공했다. 11개 task 중 4개가 실행, 7개가 UP-TO-DATE였으며 테스트 컴파일·전체 test·포맷 검사는 실제 실행했다. JUnit XML은 287개 중 281개 통과, MySQL 경합 4개와 Flyway 2개 skipped, 실패·오류 0개다. skipped 6개는 실제 MySQL 검증 성공을 의미하지 않는다.

### 단계 12 로컬 실행 결과 (2026-10-11)

다음 명령은 테스트 task가 실제 실행되어 성공했다 (`test` UP-TO-DATE 아님). `compileJava`는 UP-TO-DATE였고 테스트 소스는 다시 컴파일했다.

```bash
./gradlew --gradle-user-home .gradle-local spotlessApply test \
  --tests '*UserServiceImplTest' --tests '*AuthTokenIntegrationTest' \
  --tests '*MySqlTestDatabaseGuardTest' --tests '*AuthTokenMySqlConcurrencyTest' \
  --offline --no-daemon
```

| 검증 | 실행 결과 | 범위 |
|---|---|---|
| `UserServiceImplTest` | 8 passed | 기존 조회 2개 + 사용자 잠금·탈퇴 상태·STAFF/OWNER·DB 실패 6개 |
| `AuthTokenIntegrationTest` | 8 passed | 실제 HTTP/JWT/H2 트랜잭션, 회전·세션 교체·로그아웃·탈퇴, 생성된 OpenAPI |
| `MySqlTestDatabaseGuardTest` | 12 passed | 안전한 전용 DB URL·계정 조건, 잘못된 URL 차단 |
| `AuthTokenMySqlConcurrencyTest` | 4 skipped | DB URL 환경 변수 미설정. MySQL 경합 검증 미완료 |

총 32개 중 28개 통과, 4개 skipped, 실패 0개다. 로컬 MySQL은 127.0.0.1:3306 읽기 핸드셰이크가 3초 제한 내 완료되지 않았다. 이 결과를 MySQL 동시성이나 Flyway 마이그레이션 성공으로 해석하지 않는다. root 에이전트의 전체 검증 결과는 다음 절에 기록한다. 실제 운영 주소·브라우저 쿠키 연동·클라이언트 타입 재생성 역시 미완료다.

### root 전체 검증 결과 (2026-10-11)

root 에이전트가 12단계 변경을 리뷰하고 아래 명령을 실행하여 성공했다. 11개 task가 실제 실행되어 컴파일·Spotless check·전체 테스트·빌드 결과를 확인했다.

```bash
./gradlew --gradle-user-home .gradle-local --offline --no-daemon \
  spotlessCheck compileJava build --rerun-tasks
```

JUnit XML 집계는 281개 중 277개 통과, MySQL 4개 skipped, 실패 0개다. skipped는 검증 성공에 포함하지 않는다. 실제 MySQL 잠금·Flyway 신규/기존 스키마 검증·브라우저 연동·클라이언트 타입 재생성은 여전히 미완료다. 이후 리뷰 보완은 테스트 전용 MySQL 버전 가드에 한정하고 대상 테스트로 추가 검증한다.

### 리뷰 보완 대상 검증 (2026-10-11)

MySQL 제품명·8.x 버전 검사를 추가한 뒤 다음 대상 테스트를 실제 실행해 성공했다. 이후 root가 전체 테스트를 다시 실행한 결과는 아래 최종 검증에 기록한다.

```bash
./gradlew --gradle-user-home .gradle-local spotlessApply test \
  --tests '*MySqlTestDatabaseGuardTest' --tests '*AuthTokenIntegrationTest' \
  --tests '*AuthTokenMySqlConcurrencyTest' --offline --no-daemon
```

가드 16개, H2 통합 8개가 통과했고 MySQL 경합 4개는 skipped다. 총 28개 중 24개 통과, 4개 skipped, 실패 0개이며 `test`와 테스트 컴파일은 실제 실행됐다. 앞의 전체 실행 281개는 버전 가드 4개 추가 전 집계이므로 이 대상 실행과 합산하지 않는다. 실제 MySQL 메타데이터 연결과 경합 실행은 전용 DB 환경에서 수행해야 한다.

### 최종 검증 (2026-10-11)

리뷰 보완을 포함해 root가 `./gradlew --gradle-user-home .gradle-local --offline --no-daemon spotlessCheck compileJava build`를 실행했다. 빌드는 성공했고 11개 task 중 3개가 실행, 8개가 UP-TO-DATE였다. Spotless 검사와 전체 `test`는 실제 실행됐고 컴파일·패키징은 앞서 검증한 결과를 재사용했다. JUnit XML 집계는 **285개 중 281개 통과, MySQL 경합 4개 skipped, 실패·오류 0개**다. 생성된 OpenAPI 검증은 통과했으며 Swagger UI의 브라우저 시각 확인은 수행하지 않았다. 실제 MySQL 경합·Flyway 적용·클라이언트 타입 재생성·브라우저 쿠키 연동은 미완료다.
