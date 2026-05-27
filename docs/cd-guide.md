# CD Guide

이 문서는 `develop` 브랜치에 merge되면 GitHub Actions가 backend Docker 이미지를 빌드하고 AWS 서버에서 자동 배포하도록 구성하는 절차입니다.

## 1. AWS 서버 준비

서버에는 Docker와 Docker Compose plugin이 설치되어 있어야 합니다.

배포 디렉터리를 만들고 repo에서 필요한 운영 파일을 둡니다. 현재 서버에서 `docker-compose up -d --build`를 실행한 backend 디렉터리가 `DEPLOY_PATH`입니다.

현재 서버 구조가 `~/AI`, `~/Backend`라면 `DEPLOY_PATH`는 `/home/ec2-user/Backend`입니다.

서버의 `~/dealit/backend`에는 최소한 아래 파일/디렉터리가 필요합니다.

```text
.env
docker-compose.prod.yml
nginx/nginx.conf
nginx/logs/
secrets/
uploads/
```

`docker-compose.prod.yml`은 GitHub Actions가 배포 때마다 복사합니다.

## 2. 서버 .env 예시

서버의 `.env`에는 운영 secret을 직접 둡니다. 이 파일은 GitHub에 올리지 않습니다.

```dotenv
BACKEND_IMAGE=ghcr.io/OWNER/REPOSITORY:latest

DB_NAME=dealit
DB_USERNAME=dealit
DB_PASSWORD=change-me
JWT_SECRET=change-me-to-long-random-secret

APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
APP_IMAGES_PUBLIC_BASE_URL=https://api.dealit.site

JPA_DDL_AUTO=validate
NGINX_PORT=80
SERVER_PORT=8080
```

AI는 현재 서버 구성과 동일하게 `../AI` 디렉터리의 Dockerfile로 빌드합니다. 따라서 `DEPLOY_PATH`의 부모 디렉터리에 `AI` 디렉터리가 있어야 합니다.

## 3. GitHub Secrets

GitHub repository settings > Secrets and variables > Actions에 아래 secret을 등록합니다.

```text
DEPLOY_HOST       # 15.134.165.112 또는 api.dealit.site
DEPLOY_USER       # ec2-user
DEPLOY_SSH_KEY    # private key 내용 전체
DEPLOY_PORT       # 보통 22
DEPLOY_PATH       # /home/ec2-user/Backend
```

GHCR push는 기본 `GITHUB_TOKEN`을 사용하므로 별도 token이 필요 없습니다.

## 4. 최초 1회 실행

서버에서 최초 1회만 필요한 디렉터리를 만듭니다.

```bash
cd /home/ec2-user/Backend
mkdir -p nginx/logs secrets uploads
```

`nginx/nginx.conf`는 현재 repo의 파일을 서버에 복사해 둡니다.

그 다음 GitHub Actions의 `Deploy backend` workflow를 수동 실행하거나, `develop` 브랜치에 merge하면 자동 배포됩니다.

## 5. 배포 흐름

```text
develop push/merge
-> ./gradlew test
-> Docker image build
-> ghcr.io/...:latest push
-> AWS 서버에 docker-compose.prod.yml 복사
-> 서버 .env의 BACKEND_IMAGE 갱신
-> docker compose pull backend
-> docker compose up -d --build
-> /actuator/health 확인
```

## 6. 서버에서 상태 확인

```bash
cd /home/ec2-user/Backend
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
curl http://localhost:8080/actuator/health
```
