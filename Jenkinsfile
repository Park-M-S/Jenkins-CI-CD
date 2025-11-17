pipeline {
    agent any
    
    environment {
        // Docker 이미지 설정
        IMAGE_NAME = 'hospital-backend'
        IMAGE_TAG = "${BUILD_NUMBER}"
        
        // EC2 배포 환경 (Jenkins Credentials에서 가져오기)
        EC2_HOST = credentials('EC2_HOST')
        EC2_USER = credentials('EC2_USER')
        
        // 데이터베이스 설정
        DB_ROOT_PASSWORD = credentials('DB_ROOT_PASSWORD')
        DB_PASSWORD = credentials('DB_PASSWORD')
        DB_URL = credentials('DB_URL')
        DB_USERNAME = credentials('DB_USERNAME')
        
        // 모니터링 설정
        GRAFANA_ADMIN_PASSWORD = credentials('GRAFANA_ADMIN_PASSWORD')
        
        // 병원/약국 API 키
        HOSPITAL_MAIN_API_KEY = credentials('HOSPITAL_MAIN_API_KEY')
        HOSPITAL_DETAIL_API_KEY = credentials('HOSPITAL_DETAIL_API_KEY')
        HOSPITAL_MEDICAL_SUBJECT_API_KEY = credentials('HOSPITAL_MEDICAL_SUBJECT_API_KEY')
        HOSPITAL_PRODOC_API_KEY = credentials('HOSPITAL_PRODOC_API_KEY')
        HOSPITAL_PHARMACY_API_KEY = credentials('HOSPITAL_PHARMACY_API_KEY')
        HOSPITAL_EMERGENCY_API_KEY = credentials('HOSPITAL_EMERGENCY_API_KEY')
        API_ADMIN_KEY = credentials('API_ADMIN_KEY')
        
        // 병원/약국 API Base URL
        HOSPITAL_MAIN_API_BASE_URL = credentials('HOSPITAL_MAIN_API_BASE_URL')
        HOSPITAL_DETAIL_API_BASE_URL = credentials('HOSPITAL_DETAIL_API_BASE_URL')
        HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL = credentials('HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL')
        HOSPITAL_PRODOC_API_BASE_URL = credentials('HOSPITAL_PRODOC_API_BASE_URL')
        HOSPITAL_PHARMACY_API_BASE_URL = credentials('HOSPITAL_PHARMACY_API_BASE_URL')
        HOSPITAL_EMERGENCY_API_BASE_URL = credentials('HOSPITAL_EMERGENCY_API_BASE_URL')
        
        // YouTube API 설정
        YOUTUBE_API_KEY = credentials('YOUTUBE_API_KEY')
        YOUTUBE_API_BASE_URL = credentials('YOUTUBE_API_BASE_URL')
        YOUTUBE_API_TRUSTED_CHANNELS = credentials('YOUTUBE_API_TRUSTED_CHANNELS')
        
        // Gemini API 설정
        GEMINI_API_KEY = credentials('GEMINI_API_KEY')
        GEMINI_API_URL = credentials('GEMINI_API_URL')
        GEMINI_API_MODEL = credentials('GEMINI_API_MODEL')
        
        // Chatbot 설정
        CHATBOT_SYSTEM_PROMPT_FILE = credentials('CHATBOT_SYSTEM_PROMPT_FILE')
    }
    
    stages {
        stage('소스코드 체크아웃') {
            steps {
                echo '🔍 소스코드 체크아웃 중...'
                checkout scm
            }
        }
        
        stage('API Properties 파일 생성') {
            steps {
                script {
                    echo '📝 API Properties 파일 생성 중...'
                    sh """
                        cat > hospital_main/src/main/resources/api.properties << 'EOF'
# Hospital API Keys
hospital.main.api.key=${HOSPITAL_MAIN_API_KEY}
hospital.detail.api.key=${HOSPITAL_DETAIL_API_KEY}
hospital.medical.subject.api.key=${HOSPITAL_MEDICAL_SUBJECT_API_KEY}
hospital.prodoc.api.key=${HOSPITAL_PRODOC_API_KEY}
hospital.pharmacy.api.key=${HOSPITAL_PHARMACY_API_KEY}
hospital.emergency.api.key=${HOSPITAL_EMERGENCY_API_KEY}
api.admin.key=${API_ADMIN_KEY}

# Hospital API Base URLs
hospital.main.api.base-url=${HOSPITAL_MAIN_API_BASE_URL}
hospital.detail.api.base-url=${HOSPITAL_DETAIL_API_BASE_URL}
hospital.medical.subject.api.base-url=${HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL}
hospital.prodoc.api.base-url=${HOSPITAL_PRODOC_API_BASE_URL}
hospital.pharmacy.api.base-url=${HOSPITAL_PHARMACY_API_BASE_URL}
hospital.emergency.api.base-url=${HOSPITAL_EMERGENCY_API_BASE_URL}

# YouTube API
youtube.api.key=${YOUTUBE_API_KEY}
youtube.api.base-url=${YOUTUBE_API_BASE_URL}
youtube.api.trusted-channels=${YOUTUBE_API_TRUSTED_CHANNELS}

# Gemini API
gemini.api.key=${GEMINI_API_KEY}
gemini.api.url=${GEMINI_API_URL}
gemini.api.model=${GEMINI_API_MODEL}

# Chatbot
chatbot.system-prompt-file=${CHATBOT_SYSTEM_PROMPT_FILE}
EOF
                    """
                    echo '✅ API Properties 파일 생성 완료'
                }
            }
        }
        
        stage('백엔드 Docker 이미지 빌드') {
            steps {
                script {
                    echo '🔨 백엔드 Docker 이미지 빌드 중...'
                    dir('hospital_main') {
                        sh """
                            docker build --no-cache -t ${IMAGE_NAME}:${IMAGE_TAG} .
                            docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                        """
                    }
                    echo '✅ 백엔드 이미지 생성 완료'
                }
            }
        }
        
        stage('Docker 이미지 압축') {
            steps {
                script {
                    echo '💾 Docker 이미지 압축 중...'
                    sh """
                        docker save ${IMAGE_NAME}:latest | gzip > backend.tar.gz
                        ls -lh backend.tar.gz
                    """
                    echo '✅ 이미지 압축 완료'
                }
            }
        }
        
        stage('배포 파일 EC2로 전송') {
            steps {
                script {
                    echo '📦 배포 파일들을 EC2 서버로 전송 중...'
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh """
                            scp -o StrictHostKeyChecking=no \
                                backend.tar.gz \
                                docker-compose.prod.yml \
                                deploy.sh \
                                ${EC2_USER}@${EC2_HOST}:/home/ec2-user/
                        """
                    }
                    echo '✅ 파일 전송 완료'
                }
            }
        }
        
        stage('EC2 서버 배포') {
            steps {
                script {
                    echo '🚀 EC2 서버에 배포 시작...'
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no \
                                ${EC2_USER}@${EC2_HOST} '
                            
                            echo "🚀 병원 프로젝트 DuckDNS + 모니터링 배포 시작..."
                            
                            # Docker 이미지 로드
                            echo "📦 Docker 이미지 로드 중..."
                            docker load < /home/ec2-user/backend.tar.gz
                            
                            # 필요한 디렉토리 생성
                            sudo mkdir -p /opt/hospital/config/duckdns
                            sudo mkdir -p /opt/hospital/config/prometheus
                            sudo mkdir -p /opt/hospital/monitoring/prometheus/config
                            sudo mkdir -p /opt/hospital/monitoring/prometheus/data
                            sudo mkdir -p /opt/hospital/monitoring/grafana/data
                            sudo mkdir -p /opt/hospital/monitoring/grafana/provisioning/dashboards
                            sudo mkdir -p /opt/hospital/monitoring/grafana/provisioning/datasources
                            sudo chown -R ec2-user:ec2-user /opt/hospital/
                            
                            # .env 파일 생성
                            cat > .env << EOF
ENVIRONMENT=production
IMAGE_TAG=latest

# 데이터베이스 설정
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
DB_PASSWORD=${DB_PASSWORD}
DB_PORT=3500

# 백엔드 설정
BACKEND_HOST=hospital-backend
BACKEND_PORT=8888

# 모니터링 설정
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}

# 병원/약국 API 키 설정
HOSPITAL_MAIN_API_KEY=${HOSPITAL_MAIN_API_KEY}
HOSPITAL_DETAIL_API_KEY=${HOSPITAL_DETAIL_API_KEY}
HOSPITAL_MEDICAL_SUBJECT_API_KEY=${HOSPITAL_MEDICAL_SUBJECT_API_KEY}
HOSPITAL_PRODOC_API_KEY=${HOSPITAL_PRODOC_API_KEY}
HOSPITAL_PHARMACY_API_KEY=${HOSPITAL_PHARMACY_API_KEY}
HOSPITAL_EMERGENCY_API_KEY=${HOSPITAL_EMERGENCY_API_KEY}
API_ADMIN_KEY=${API_ADMIN_KEY}

# 병원/약국 API Base URL 설정
HOSPITAL_MAIN_API_BASE_URL=${HOSPITAL_MAIN_API_BASE_URL}
HOSPITAL_DETAIL_API_BASE_URL=${HOSPITAL_DETAIL_API_BASE_URL}
HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL=${HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL}
HOSPITAL_PRODOC_API_BASE_URL=${HOSPITAL_PRODOC_API_BASE_URL}
HOSPITAL_PHARMACY_API_BASE_URL=${HOSPITAL_PHARMACY_API_BASE_URL}
HOSPITAL_EMERGENCY_API_BASE_URL=${HOSPITAL_EMERGENCY_API_BASE_URL}

# 데이터베이스 설정
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# YouTube API 설정
YOUTUBE_API_KEY=${YOUTUBE_API_KEY}
YOUTUBE_API_BASE_URL=${YOUTUBE_API_BASE_URL}
YOUTUBE_API_TRUSTED_CHANNELS=${YOUTUBE_API_TRUSTED_CHANNELS}

# Gemini API 설정
GEMINI_API_KEY=${GEMINI_API_KEY}
GEMINI_API_URL=${GEMINI_API_URL}
GEMINI_API_MODEL=${GEMINI_API_MODEL}

# Chatbot 설정
CHATBOT_SYSTEM_PROMPT_FILE=${CHATBOT_SYSTEM_PROMPT_FILE}

EOF
                            
                            # Prometheus 설정 파일 생성
                            cat > /opt/hospital/config/prometheus/prometheus.yml << '"'"'PROMETHEUS_CONFIG'"'"'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: '"'"'hospital-production'"'"'
    environment: '"'"'prod'"'"'

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

scrape_configs:
  - job_name: '"'"'prometheus'"'"'
    static_configs:
      - targets: ['"'"'localhost:9090'"'"']
    scrape_interval: 15s

  - job_name: '"'"'hospital-backend'"'"'
    static_configs:
      - targets: ['"'"'backend:8888'"'"']
    metrics_path: '"'"'/actuator/prometheus'"'"'
    scrape_interval: 15s
    scrape_timeout: 10s

  - job_name: '"'"'node-exporter'"'"'
    static_configs:
      - targets: ['"'"'node-exporter:9100'"'"']
    scrape_interval: 15s

  - job_name: '"'"'cadvisor'"'"'
    static_configs:
      - targets: ['"'"'cadvisor:8080'"'"']
    scrape_interval: 15s
PROMETHEUS_CONFIG
                            
                            # 모니터링 스택용 Prometheus 설정
                            cat > /opt/hospital/monitoring/prometheus/config/prometheus.yml << '"'"'PROMETHEUS_CONFIG'"'"'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: '"'"'hospital-production'"'"'
    environment: '"'"'prod'"'"'

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

scrape_configs:
  - job_name: '"'"'prometheus'"'"'
    static_configs:
      - targets: ['"'"'localhost:9090'"'"']
    scrape_interval: 15s

  - job_name: '"'"'hospital-backend'"'"'
    static_configs:
      - targets: ['"'"'hospital-backend:8888'"'"']
    metrics_path: '"'"'/actuator/prometheus'"'"'
    scrape_interval: 15s
    scrape_timeout: 10s

  - job_name: '"'"'node-exporter'"'"'
    static_configs:
      - targets: ['"'"'node-exporter:9100'"'"']
    scrape_interval: 15s

  - job_name: '"'"'cadvisor'"'"'
    static_configs:
      - targets: ['"'"'cadvisor:8080'"'"']
    scrape_interval: 15s
PROMETHEUS_CONFIG
                            
                            # Prometheus 알림 규칙 생성
                            cat > /opt/hospital/monitoring/prometheus/config/alert_rules.yml << '"'"'ALERT_RULES'"'"'
groups:
  - name: hospital_backend_alerts
    rules:
      - alert: BackendDown
        expr: up{job="hospital-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Hospital Backend is down"
          description: "Hospital Backend has been down for more than 1 minute"

      - alert: HighCPUUsage
        expr: system_cpu_usage > 0.8
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
          description: "CPU usage is above 80% for more than 2 minutes"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM memory usage is above 80% for more than 2 minutes"

  - name: infrastructure_alerts
    rules:
      - alert: NodeDown
        expr: up{job="node-exporter"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Node Exporter is down"
          description: "Node Exporter has been down for more than 1 minute"

      - alert: DiskSpaceHigh
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Disk space usage high"
          description: "Disk space usage is above 90%"
ALERT_RULES
                            
                            # Grafana 데이터소스 설정
                            cat > /opt/hospital/monitoring/grafana/provisioning/datasources/prometheus.yml << '"'"'DATASOURCE_CONFIG'"'"'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
DATASOURCE_CONFIG
                            
                            # Grafana 대시보드 프로비저닝 설정
                            cat > /opt/hospital/monitoring/grafana/provisioning/dashboards/dashboard.yml << '"'"'DASHBOARD_CONFIG'"'"'
apiVersion: 1

providers:
  - name: '"'"'default'"'"'
    orgId: 1
    folder: '"'"''"'"'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
DASHBOARD_CONFIG
                            
                            # 환경변수 확인
                            echo "📋 배포 환경 설정:"
                            echo "  환경: production"
                            echo "  DuckDNS 도메인: ${DUCKDNS_DOMAIN}"
                            echo "  DuckDNS 서브도메인: ${DUCKDNS_SUBDOMAIN}"
                            echo "  백엔드 포트: 8888"
                            echo "  DB 포트: 3500"
                            echo "  프로메테우스 포트: 9090"
                            echo "  그라파나 포트: 3000"
                            
                            # 배포 스크립트 실행 권한 부여
                            chmod +x /home/ec2-user/deploy.sh
                            
                            # 배포 실행
                            echo "▶️ 배포 스크립트 실행..."
                            /home/ec2-user/deploy.sh
                            
                            # 모니터링 스택 설치
                            echo "📊 모니터링 스택 설치 중..."
                            
                            # Docker 네트워크가 없다면 생성
                            docker network ls | grep hospital-network || docker network create hospital-network
                            
                            # 기존 모니터링 컨테이너 정리
                            docker stop cadvisor node-exporter prometheus grafana 2>/dev/null || true
                            docker rm cadvisor node-exporter prometheus grafana 2>/dev/null || true
                            
                            # cAdvisor 실행
                            docker run -d \
                              --name cadvisor \
                              --restart unless-stopped \
                              --network hospital-network \
                              -p 8080:8080 \
                              -v /:/rootfs:ro \
                              -v /var/run:/var/run:rw \
                              -v /sys:/sys:ro \
                              -v /var/lib/docker/:/var/lib/docker:ro \
                              --privileged \
                              --device /dev/kmsg \
                              gcr.io/cadvisor/cadvisor:latest || echo "⚠️ cAdvisor 시작 실패 (계속 진행)"
                            
                            # Node Exporter 실행
                            docker run -d \
                              --name node-exporter \
                              --restart unless-stopped \
                              --network hospital-network \
                              -p 9100:9100 \
                              -v /proc:/host/proc:ro \
                              -v /sys:/host/sys:ro \
                              -v /:/rootfs:ro \
                              --pid host \
                              prom/node-exporter:latest \
                              --path.procfs=/host/proc \
                              --path.rootfs=/rootfs \
                              --path.sysfs=/host/sys \
                              --collector.filesystem.mount-points-exclude='"'"'^/(sys|proc|dev|host|etc)($$|/)'"'"'
                            
                            # Prometheus 실행
                            docker run -d \
                              --name prometheus \
                              --restart unless-stopped \
                              --network hospital-network \
                              -p 9090:9090 \
                              -v /opt/hospital/monitoring/prometheus/config:/etc/prometheus \
                              -v /opt/hospital/monitoring/prometheus/data:/prometheus \
                              --user "$(id -u):$(id -g)" \
                              prom/prometheus:latest \
                              --config.file=/etc/prometheus/prometheus.yml \
                              --storage.tsdb.path=/prometheus \
                              --web.console.libraries=/etc/prometheus/console_libraries \
                              --web.console.templates=/etc/prometheus/consoles \
                              --storage.tsdb.retention.time=200h \
                              --web.enable-lifecycle \
                              --web.enable-admin-api
                            
                            # Grafana 실행
                            docker run -d \
                              --name grafana \
                              --restart unless-stopped \
                              --network hospital-network \
                              -p 3000:3000 \
                              -v /opt/hospital/monitoring/grafana/data:/var/lib/grafana \
                              -v /opt/hospital/monitoring/grafana/provisioning:/etc/grafana/provisioning \
                              -e GF_SECURITY_ADMIN_USER=admin \
                              -e GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD} \
                              -e GF_INSTALL_PLUGINS=grafana-piechart-panel,grafana-worldmap-panel,grafana-clock-panel \
                              -e GF_USERS_ALLOW_SIGN_UP=false \
                              --user "$(id -u):$(id -g)" \
                              grafana/grafana:latest
                            
                            sleep 15
                            
                            echo "✅ 모니터링 스택 설치 완료!"
                            
                            # 임시 파일 정리
                            echo "🧹 임시 파일 정리..."
                            rm -f /home/ec2-user/*.tar.gz
                            
                            echo "✅ 배포 완료!"
                        '
                        """
                    }
                    echo '✅ 배포 완료'
                }
            }
        }
        
        stage('서비스 헬스체크') {
            steps {
                script {
                    echo '🏥 서비스 헬스체크 시작...'
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no \
                                ${EC2_USER}@${EC2_HOST} '
                            
                            echo "🏥 서비스 헬스체크 시작..."
                            
                            # 컨테이너 상태 확인
                            echo "📊 메인 애플리케이션 컨테이너 상태:"
                            docker-compose -f /home/ec2-user/docker-compose.prod.yml ps
                            
                            echo ""
                            echo "📊 모니터링 컨테이너 상태:"
                            docker ps --filter name=prometheus --filter name=grafana --filter name=node-exporter --filter name=cadvisor
                            
                            # 백엔드 API 확인
                            TARGET_URL="http://${EC2_HOST}:8888"
                            echo "🔍 백엔드 API 응답 테스트 (\$TARGET_URL)..."
                            
                            BACKEND_OK=false
                            for i in {1..12}; do
                                echo "  시도 \$i/12..."
                                
                                if curl -f -s --connect-timeout 10 "\$TARGET_URL/api/proDoc/status" > /dev/null 2>&1; then
                                    echo "✅ 백엔드 API: 정상"
                                    BACKEND_OK=true
                                    break
                                else
                                    echo "⏳ 백엔드 시작 대기 중..."
                                    sleep 10
                                fi
                            done
                            
                            # Prometheus 헬스체크
                            PROMETHEUS_URL="http://${EC2_HOST}:9090"
                            echo "🔍 프로메테우스 헬스체크 (\$PROMETHEUS_URL)..."
                            
                            for i in {1..3}; do
                                if curl -f -s --connect-timeout 10 "\$PROMETHEUS_URL/-/healthy" > /dev/null 2>&1; then
                                    echo "✅ 프로메테우스: 정상"
                                    break
                                else
                                    echo "⏳ 프로메테우스 시작 대기 중..."
                                    sleep 10
                                fi
                            done
                            
                            # Grafana 헬스체크
                            GRAFANA_URL="http://${EC2_HOST}:3000"
                            echo "🔍 그라파나 헬스체크 (\$GRAFANA_URL)..."
                            
                            for i in {1..3}; do
                                if curl -f -s --connect-timeout 10 "\$GRAFANA_URL/api/health" > /dev/null 2>&1; then
                                    echo "✅ 그라파나: 정상"
                                    break
                                else
                                    echo "⏳ 그라파나 시작 대기 중..."
                                    sleep 10
                                fi
                            done
                            
                            # 데이터베이스 확인
                            if docker ps | grep hospital-mariadb > /dev/null; then
                                echo "✅ 데이터베이스: 정상"
                            else
                                echo "⚠️ 데이터베이스: 확인 필요"
                            fi
                            
                            echo ""
                            echo "🎉 =========================================="
                            echo "    백엔드 배포 및 헬스체크 완료!"
                            echo "==========================================="
                            echo ""
                            echo "📍 접속 정보:"
                            echo "  🔧 백엔드 API: http://${EC2_HOST}:8888"
                            echo "  📊 프로메테우스: http://${EC2_HOST}:9090"
                            echo "  📈 그라파나: http://${EC2_HOST}:3000"
                            echo "  🖥️ Node Exporter: http://${EC2_HOST}:9100"
                            echo "  📦 cAdvisor: http://${EC2_HOST}:8080"
                        '
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ 배포가 성공적으로 완료되었습니다!'
        }
        failure {
            echo '❌ 배포 실패!'
            script {
                sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no \
                            ${EC2_USER}@${EC2_HOST} '
                        
                        echo "❌ 배포 실패! 롤백 시도..."
                        
                        # 메인 애플리케이션 컨테이너 중지
                        docker-compose -f /home/ec2-user/docker-compose.prod.yml down || true
                        
                        # 모니터링 컨테이너 중지
                        docker stop prometheus grafana node-exporter cadvisor 2>/dev/null || true
                        docker rm prometheus grafana node-exporter cadvisor 2>/dev/null || true
                        
                        # 최근 로그 확인
                        echo "📝 최근 로그:"
                        docker-compose -f /home/ec2-user/docker-compose.prod.yml logs --tail=50 || echo "로그 확인 불가"
                        
                        # 임시 파일 정리
                        rm -f /home/ec2-user/*.tar.gz
                        
                        echo "🔄 이전 버전으로 롤백하거나 수동으로 문제를 해결하세요."
                    '
                    """
                }
            }
        }
        always {
            echo '🧹 빌드 환경 정리 중...'
            sh 'rm -f backend.tar.gz || true'
        }
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 156bcab (first commit)
