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
   - `8080/tcp`

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
KAKAO_REDIRECT_URI=

MYSQL_DATABASE=keeply_prod
DB_USERNAME=keeply_app
DB_PASSWORD=

AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=keeply-images
AWS_REGION=ap-northeast-2
APP_S3_ACCESS_URL_PREFIX=https://keeply-images.s3.ap-northeast-2.amazonaws.com
```

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
http://{LIGHTSAIL_STATIC_IP}:8080/swagger-ui/index.html
```

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
