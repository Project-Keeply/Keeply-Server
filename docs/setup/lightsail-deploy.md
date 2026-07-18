# Lightsail Deploy Guide

AWS Lightsail Ubuntu 인스턴스에서 Keeply Server를 Docker Compose로 배포하는 절차입니다.

## 1. Lightsail 콘솔 설정

1. 인스턴스 생성
   - 리전: `ap-northeast-2` 서울
   - 이미지: Ubuntu 24.04 LTS
   - 플랜: 2GB RAM 이상 권장
2. 고정 IP 생성 후 인스턴스에 연결
3. 인스턴스 `네트워킹` 탭에서 포트 추가
   - `80/tcp`

현재는 API 도메인 없이 고정 IP와 HTTP로 임시 운영합니다. `443/tcp`와 DNS 설정은 [도메인 확보 후 HTTPS 전환](#4-도메인-확보-후-https-전환) 단계에서 추가합니다.

## 2. 서버 패키지 설치

브라우저 SSH로 인스턴스에 접속한 뒤 실행합니다.

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git
sudo usermod -aG docker ubuntu
```

권한 적용을 위해 SSH를 닫고 다시 접속합니다.

## 3. 프로젝트 배포

```bash
git clone https://github.com/Project-Keeply/Keeply-Server.git
cd Keeply-Server
git switch main
```

`.env.example`을 참고해 운영 환경변수를 작성합니다.

```bash
cp .env.example .env
nano .env
```

필수 값:

```env
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=replace-with-random-string-at-least-32-bytes

KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_REDIRECT_URI=https://keeply-work.vercel.app/login/callback

MYSQL_DATABASE=keeply_prod
DB_USERNAME=keeply_app
DB_PASSWORD=

AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=keeply-images
AWS_REGION=ap-northeast-2
APP_S3_ACCESS_URL_PREFIX=https://keeply-images.s3.ap-northeast-2.amazonaws.com

APP_CORS_ALLOWED_ORIGINS=https://keeply-work.vercel.app,https://keeply-client.vercel.app,http://localhost:5173
```

### S3 버킷 CORS 설정

브라우저는 presigned URL을 사용해 S3로 직접 요청하므로 API 서버와 별도로 S3 버킷에도 CORS 정책이 필요합니다. AWS CLI 인증 정보에 `s3:PutBucketCORS`, `s3:GetBucketCORS` 권한이 있는지 확인한 뒤 저장소 루트에서 실행합니다.

```bash
aws s3api put-bucket-cors \
  --bucket keeply-images \
  --cors-configuration file://infra/s3/cors.json \
  --region ap-northeast-2
```

적용된 정책을 조회합니다.

```bash
aws s3api get-bucket-cors \
  --bucket keeply-images \
  --region ap-northeast-2
```

프론트에서 발급받은 presigned URL로 preflight 응답을 확인합니다.

```bash
curl -i -X OPTIONS "${PRESIGNED_URL}" \
  -H "Origin: https://keeply-work.vercel.app" \
  -H "Access-Control-Request-Method: PUT" \
  -H "Access-Control-Request-Headers: content-type"
```

응답의 `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`에 요청한 Origin, `PUT`, `content-type`이 포함되어야 합니다.

실행:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

로그 확인:

```bash
docker logs -f keeply-server
```

Swagger 확인:

```text
http://{LIGHTSAIL_STATIC_IP}/swagger-ui/index.html
```

현재 인스턴스의 고정 IP가 `43.201.35.8`이면 다음 주소를 사용합니다.

```text
http://43.201.35.8/swagger-ui/index.html
```

API 컨테이너의 `8080` 포트는 외부에 직접 노출하지 않고, Caddy가 내부 Docker 네트워크에서 `app:8080`으로 프록시합니다.

HTTP 임시 운영에는 다음과 같은 제한이 있습니다.

- 요청과 JWT가 암호화되지 않으므로 실제 사용자 데이터로 로그인하거나 API를 호출하지 않습니다.
- HTTPS로 배포된 프론트엔드는 브라우저의 Mixed Content 정책으로 HTTP API를 호출할 수 없습니다.
- Swagger와 API 연결 확인 등 제한된 개발 검증 용도로만 사용합니다.

로컬 프론트에서 임시 서버 연결을 확인할 때만 아래 값을 사용합니다.

```env
VITE_API_BASE_URL=http://{LIGHTSAIL_STATIC_IP}
VITE_KAKAO_REDIRECT_URI=https://keeply-work.vercel.app/login/callback
```

## 4. 도메인 확보 후 HTTPS 전환

1. Lightsail 방화벽에서 `443/tcp`를 엽니다.
2. API 도메인의 DNS `A` 레코드를 고정 IP로 연결합니다.
   - 예: `api.keeply.example.com -> {LIGHTSAIL_STATIC_IP}`
3. 운영 서버 `.env`에 API 도메인을 추가합니다.

   ```env
   API_DOMAIN=api.keeply.example.com
   ```

4. `Caddyfile`의 사이트 주소를 `{$API_DOMAIN}`으로 변경합니다.
5. `docker-compose.prod.yml`의 Caddy 서비스에 `API_DOMAIN`만 전달하고 `443:443` 포트를 추가합니다.

   ```yaml
   environment:
     API_DOMAIN: ${API_DOMAIN}
   ports:
     - "80:80"
     - "443:443"
   ```

   Caddy에는 데이터베이스, JWT, AWS 자격 증명이 포함된 전체 `.env`를 주입하지 않습니다.

6. 변경한 설정으로 재배포합니다.

   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```

7. Caddy 로그에서 TLS 인증서 발급 성공 여부를 확인합니다.

   ```bash
   docker logs --tail 100 keeply-caddy
   ```

8. Swagger와 API Docs를 HTTPS 주소로 확인합니다.

   ```text
   https://{API_DOMAIN}/swagger-ui/index.html
   https://{API_DOMAIN}/v3/api-docs
   ```

9. 프론트 환경변수와 카카오 개발자 콘솔의 Redirect URI를 HTTPS 운영 도메인 기준으로 갱신합니다.

   ```env
   VITE_API_BASE_URL=https://{API_DOMAIN}
   VITE_KAKAO_REDIRECT_URI=https://keeply-work.vercel.app/login/callback
   ```

HTTPS 인증서는 Caddy가 `API_DOMAIN` 기준으로 자동 발급하고 갱신합니다. 인증서 발급 전 다음 조건을 확인합니다.

- `API_DOMAIN` DNS `A` 레코드가 Lightsail 고정 IP를 바라봐야 함
- Lightsail 방화벽에서 `80/tcp`, `443/tcp`가 열려 있어야 함
- 같은 서버에서 다른 프로세스가 `80`, `443` 포트를 사용하지 않아야 함

## 5. 재배포

```bash
cd Keeply-Server
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build
```

## 6. 종료

```bash
docker compose -f docker-compose.prod.yml down
```

MySQL 데이터는 `mysql-data` Docker volume에 남습니다. 데이터까지 삭제하려면 아래 명령을 사용합니다.

```bash
docker compose -f docker-compose.prod.yml down -v
```
