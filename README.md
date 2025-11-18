# 🚀 Jenkins CI/CD 파이프라인 가이드

## 📋 목차
- [개요](#-개요)
- [파이프라인 구조](#-파이프라인-구조)
- [배포 프로세스](#-배포-프로세스)
- [주요 설정](#-주요-설정)
- [사용 방법](#-사용-방법)

---

## 🎯 개요

이 프로젝트는 **Jenkins**를 활용한 자동화된 CI/CD 파이프라인을 통해 병원 관리 시스템을 AWS EC2에 배포합니다.

### 기술 스택
- **CI/CD**: Jenkins
- **컨테이너**: Docker, Docker Compose
- **배포 환경**: AWS EC2
- **모니터링**: Prometheus, Grafana
- **백엔드**: Spring Framework (Java 21)

---

## 🏗 파이프라인 구조

### 전체 흐름도

```
📦 소스코드 체크아웃
    ↓
🔧 API Properties 생성
    ↓
🏗️ Docker 이미지 빌드
    ↓
📦 이미지 압축 및 패키징
    ↓
📤 EC2로 파일 전송
    ↓
🚀 EC2에서 배포 실행
    ↓
🏥 헬스체크
```

---

## 🔄 배포 프로세스

### 1️⃣ 빌드 단계

```groovy
// 1. 소스코드 가져오기
checkout scm

// 2. API 설정 파일 생성
- api.properties (API 키, URL 등)

// 3. Docker 이미지 빌드
docker build -t hospital-backend:latest

// 4. 이미지 압축
docker save hospital-backend:latest | gzip > backend.tar.gz
```

### 2️⃣ 설정 파일 생성

파이프라인에서 자동으로 생성되는 설정 파일들:

| 파일명 | 용도 |
|--------|------|
| `env.prod` | 환경 변수 설정 |
| `prometheus_core.yml` | 메트릭 수집 설정 |
| `grafana_datasources.yml` | 대시보드 데이터소스 |
| `alert_rules.yml` | 알림 규칙 |

### 3️⃣ EC2 배포

```bash
# 1. 패키지 압축
tar -czf deploy_pkg.tar.gz backend.tar.gz env.prod *.yml

# 2. EC2로 전송
scp deploy_pkg.tar.gz ec2-user@EC2_HOST:/home/ec2-user/

# 3. 배포 스크립트 실행
ssh ec2-user@EC2_HOST './deploy.sh'
```

### 4️⃣ 서비스 시작

EC2에서 실행되는 작업:

```bash
# Docker 이미지 로드
docker load < backend.tar.gz

# 기존 컨테이너 중지
docker-compose down

# 새 컨테이너 시작
docker-compose -f docker-compose.prod.yml up -d

# 모니터링 스택 시작
- Prometheus (메트릭 수집)
- Grafana (시각화)
- cAdvisor (컨테이너 모니터링)
- Node Exporter (시스템 모니터링)
```

---

## ⚙️ 주요 설정

### Jenkins Credentials

파이프라인에서 사용하는 인증 정보:

```
🔐 필수 Credentials
├── EC2_HOST              # EC2 서버 주소
├── EC2_USER              # EC2 사용자명
├── EC2_PRIVATE_KEY       # SSH 키
├── DB_ROOT_PASSWORD      # DB 루트 비밀번호
├── DB_PASSWORD           # DB 사용자 비밀번호
├── HOSPITAL_*_API_KEY    # 각종 API 키들
└── GRAFANA_ADMIN_PASSWORD # 그라파나 관리자 비밀번호
```

### 환경 변수

`.env.prod` 파일에 설정되는 주요 변수:

```bash
# 데이터베이스
DB_URL=jdbc:mariadb://mariadb:3500/hospital_api_db
DB_USERNAME=hospitaluser

# 백엔드
BACKEND_PORT=8888

# 모니터링
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
```

---

## 🚀 사용 방법

### 1. Jenkins 설정

1. Jenkins에 필요한 Credentials 등록
2. Pipeline 프로젝트 생성
3. Jenkinsfile 경로 설정

### 2. 수동 배포 트리거

```bash
# Jenkins UI에서 "Build Now" 클릭
# 또는 Git push 시 자동 실행 (Webhook 설정 시)
```

### 3. 배포 확인

```bash
# 헬스체크 엔드포인트 확인
curl http://EC2_HOST:8888/api/proDoc/status

# 모니터링 대시보드 접속
http://EC2_HOST:3000  # Grafana
http://EC2_HOST:9090  # Prometheus
```

---

## 📊 모니터링

### 접속 정보

| 서비스 | 포트 | URL |
|--------|------|-----|
| 백엔드 API | 8888 | http://EC2_HOST:8888 |
| Prometheus | 9090 | http://EC2_HOST:9090 |
| Grafana | 3000 | http://EC2_HOST:3000 |

### 주요 메트릭

- **JVM 메모리** 사용량
- **API 응답 시간**
- **에러율**
- **컨테이너 리소스** 사용량

---

## 🛠 문제 해결

### 배포 실패 시

```bash
# 1. Jenkins 콘솔 로그 확인
# 2. EC2 서버 접속하여 로그 확인
docker-compose logs -f backend

# 3. 컨테이너 상태 확인
docker-compose ps
```

### 주요 이슈 해결

| 문제 | 해결 방법 |
|------|----------|
| 디스크 공간 부족 | `docker system prune -a` |
| 포트 충돌 | 기존 컨테이너 중지 |
| 권한 오류 | `/opt/hospital/` 디렉토리 권한 확인 |

---

## 📝 참고 사항

- 배포는 **새벽 시간대** 권장 (사용자 영향 최소화)
- **자동 롤백** 기능은 미구현 (수동 대응 필요)
- 모니터링 데이터는 **200시간** 보관
- 배포 후 반드시 **헬스체크** 확인

---

## 🔗 관련 문서

- [Docker Compose 설정](docker-compose.prod.yml)
- [배포 스크립트](deploy.sh)
- [API 연동 구조](API연동%20구조.md)
