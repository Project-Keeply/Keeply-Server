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
   - `443/tcp`
4. API 도메인의 DNS `A` 레코드를 고정 IP로 연결
   - 예: `api.keeply.example.com -> {LIGHTSAIL_STATIC_IP}`

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

API_DOMAIN=api.keeply.example.com
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
https://{API_DOMAIN}/swagger-ui/index.html
```

HTTPS 인증서는 Caddy가 `API_DOMAIN` 기준으로 Let's Encrypt에서 자동 발급하고 갱신합니다. 발급 조건은 다음과 같습니다.

- `API_DOMAIN` DNS `A` 레코드가 Lightsail 고정 IP를 바라봐야 함
- Lightsail 방화벽에서 `80/tcp`, `443/tcp`가 열려 있어야 함
- 같은 서버에서 다른 프로세스가 `80`, `443` 포트를 사용하지 않아야 함

API 컨테이너의 `8080` 포트는 외부에 직접 노출하지 않고, Caddy가 내부 Docker 네트워크에서 `app:8080`으로 프록시합니다.

프론트 환경변수에는 아래 값을 등록합니다.

```env
VITE_API_BASE_URL=https://{API_DOMAIN}
VITE_KAKAO_REDIRECT_URI=https://keeply-work.vercel.app/login/callback
```

카카오 개발자 콘솔에도 같은 Redirect URI를 등록해야 합니다.

## 4. 재배포

```bash
cd Keeply-Server
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build
```

## 5. 종료

```bash
docker compose -f docker-compose.prod.yml down
```

MySQL 데이터는 `mysql-data` Docker volume에 남습니다. 데이터까지 삭제하려면 아래 명령을 사용합니다.

```bash
docker compose -f docker-compose.prod.yml down -v
```
