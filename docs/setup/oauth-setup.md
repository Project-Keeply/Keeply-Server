# Kakao OAuth2 셋업 가이드

Keeply 백엔드의 Kakao 소셜 로그인을 구동하기 위한 카카오 개발자 콘솔 등록·환경변수 세팅 절차를 정리한다.

---

## 1. 인증 흐름 (FE 경유 콜백 방식)

```
사용자 → 카카오 인가 페이지 → (code, state 발급)
       → 카카오가 FE Redirect URI 로 리다이렉트 (예: http://localhost:3000/auth/kakao/callback?code=...&state=...)
       → FE 가 code/state 를 백엔드로 POST /auth/kakao/callback (request body)
       → 백엔드가 code → kakao access token → kakao userInfo → Keeply JWT 발급
```

- 카카오에 등록하는 `Redirect URI` 는 **프론트엔드 도메인**이다.
- 백엔드는 카카오에서 직접 콜백을 받지 않는다. FE 가 받은 `code` 를 POST 로 전달한다.

---

## 2. 카카오 개발자 콘솔 설정

### 2.1 앱 등록
1. https://developers.kakao.com 접속 → 로그인
2. `내 애플리케이션` → `애플리케이션 추가하기`
3. 앱 이름: `Keeply`, 회사명: 임의

### 2.2 앱 키 확인
- `앱 설정` → `요약 정보` → `앱 키`
- `REST API 키` 값을 환경변수 `KAKAO_CLIENT_ID` 에 사용

### 2.3 카카오 로그인 활성화
- `제품 설정` → `카카오 로그인` → `활성화 설정` → **ON**

### 2.4 Redirect URI 등록
- `제품 설정` → `카카오 로그인` → `Redirect URI` 등록
- 환경별로 모두 등록:
  - dev: `http://localhost:3000/auth/kakao/callback`
  - prod: `https://<프론트엔드-운영-도메인>/auth/kakao/callback` (배포 도메인 확정 후 등록)

### 2.5 Client Secret 발급
- `제품 설정` → `카카오 로그인` → `보안` → `Client Secret 코드` → `코드 생성` → `활성화 상태` 를 **사용함** 으로 변경
- 생성된 값을 환경변수 `KAKAO_CLIENT_SECRET` 에 사용

### 2.6 동의 항목 설정
- `제품 설정` → `카카오 로그인` → `동의항목`
- 필수 동의 처리:
  - `닉네임` (profile_nickname)
  - `프로필 사진` (profile_image)
- 백엔드는 `nickname`, `profile_image_url` 을 읽어 User 엔티티에 저장한다.

---

## 3. 환경변수

| 변수 | 설명 | 예시 |
|---|---|---|
| `JWT_SECRET` | JWT 서명용 비밀키 (32바이트 이상 임의 문자열) | `openssl rand -base64 48` 로 생성 |
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 | 콘솔 `요약 정보` → `앱 키` |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret | 콘솔 `카카오 로그인` → `보안` |
| `KAKAO_REDIRECT_URI` | 카카오에 등록한 Redirect URI (FE 도메인) | dev: `http://localhost:3000/auth/kakao/callback` |
| `DB_URL` | prod 프로파일 DB JDBC URL | `jdbc:mysql://...` |
| `DB_USERNAME` | prod 프로파일 DB 사용자 | |
| `DB_PASSWORD` | prod 프로파일 DB 비밀번호 | |

> dev 프로파일의 DB 정보는 `application.yml` 에 하드코딩되어 있어 별도 환경변수 불필요하다.

---

## 4. 로컬 실행

1. 프로젝트 루트의 `.env.example` 을 복사해 `.env` 작성
   ```bash
   cp .env.example .env
   ```
2. `.env` 에 실제 값을 채운다. (`.env` 는 `.gitignore` 에 포함되어 커밋되지 않는다)
3. 환경변수를 셸에 로드한 뒤 실행
   ```bash
   set -a
   source .env
   set +a
   ./gradlew bootRun
   ```
   또는 IntelliJ Run Configuration → `Environment variables` 에 동일 값 입력.

---

## 5. 운영(prod) 환경

- prod 도메인 확정 후 카카오 콘솔에 prod Redirect URI 추가 등록 필수.
- 환경변수는 운영 인프라(예: GitHub Actions secrets, AWS Parameter Store, k8s Secret 등)로 주입한다.
- `application-prod.yml` 은 `.gitignore` 에 포함되어 있으니 운영 비밀값을 yml 로 관리할 경우 별도 인프라 채널로 배포한다.
