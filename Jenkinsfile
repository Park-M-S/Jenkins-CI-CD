pipeline {
    agent any
    
    environment {
        // Docker 이미지 설정
        IMAGE_NAME = 'hospital-backend'
        IMAGE_TAG = "${BUILD_NUMBER}"
        
        // EC2 배포 환경
        EC2_HOST = credentials('EC2_HOST')
        EC2_USER = credentials('EC2_USER')
        
        // 데이터베이스 설정
        DB_ROOT_PASSWORD = credentials('DB_ROOT_PASSWORD')
        DB_PASSWORD = credentials('DB_PASSWORD')
        DB_URL = credentials('DB_URL')
        DB_USERNAME = credentials('DB_USERNAME')
        
        // 모니터링 설정
        GRAFANA_ADMIN_PASSWORD = credentials('GRAFANA_ADMIN_PASSWORD')
        
        // API 키 및 설정
        HOSPITAL_MAIN_API_KEY = credentials('HOSPITAL_MAIN_API_KEY')
        HOSPITAL_DETAIL_API_KEY = credentials('HOSPITAL_DETAIL_API_KEY')
        HOSPITAL_MEDICAL_SUBJECT_API_KEY = credentials('HOSPITAL_MEDICAL_SUBJECT_API_KEY')
        HOSPITAL_PRODOC_API_KEY = credentials('HOSPITAL_PRODOC_API_KEY')
        HOSPITAL_PHARMACY_API_KEY = credentials('HOSPITAL_PHARMACY_API_KEY')
        HOSPITAL_EMERGENCY_API_KEY = credentials('HOSPITAL_EMERGENCY_API_KEY')
        API_ADMIN_KEY = credentials('API_ADMIN_KEY')
        
        HOSPITAL_MAIN_API_BASE_URL = credentials('HOSPITAL_MAIN_API_BASE_URL')
        HOSPITAL_DETAIL_API_BASE_URL = credentials('HOSPITAL_DETAIL_API_BASE_URL')
        HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL = credentials('HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL')
        HOSPITAL_PRODOC_API_BASE_URL = credentials('HOSPITAL_PRODOC_API_BASE_URL')
        HOSPITAL_PHARMACY_API_BASE_URL = credentials('HOSPITAL_PHARMACY_API_BASE_URL')
        HOSPITAL_EMERGENCY_API_BASE_URL = credentials('HOSPITAL_EMERGENCY_API_BASE_URL')
        
        YOUTUBE_API_KEY = credentials('YOUTUBE_API_KEY')
        YOUTUBE_API_BASE_URL = credentials('YOUTUBE_API_BASE_URL')
        YOUTUBE_API_TRUSTED_CHANNELS = credentials('YOUTUBE_API_TRUSTED_CHANNELS')
        
        GEMINI_API_KEY = credentials('GEMINI_API_KEY')
        GEMINI_API_URL = credentials('GEMINI_API_URL')
        GEMINI_API_MODEL = credentials('GEMINI_API_MODEL')
        
        CHATBOT_SYSTEM_PROMPT_FILE = credentials('CHATBOT_SYSTEM_PROMPT_FILE')
    }
    
    stages {
        stage('소스코드 체크아웃') {
            steps {
                checkout scm
            }
        }
        
        stage('API Properties 생성 (빌드용)') {
            steps {
                writeFile file: 'hospital_main/src/main/resources/api.properties', text: """
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
"""
            }
        }
        
        stage('백엔드 빌드 및 압축') {
            steps {
                script {
                    dir('hospital_main') {
                        sh "docker build --no-cache -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                        sh "docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest"
                    }
                    sh "docker save ${IMAGE_NAME}:latest | gzip > backend.tar.gz"
                }
            }
        }
        
        stage('배포용 설정 파일 생성') {
            steps {
                script {
                    // 1. .env 파일 생성
                    writeFile file: 'env.prod', text: """ENVIRONMENT=production
IMAGE_TAG=latest

DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
DB_PASSWORD=${DB_PASSWORD}
DB_PORT=3500

BACKEND_HOST=hospital-backend
BACKEND_PORT=8888

PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}

HOSPITAL_MAIN_API_KEY=${HOSPITAL_MAIN_API_KEY}
HOSPITAL_DETAIL_API_KEY=${HOSPITAL_DETAIL_API_KEY}
HOSPITAL_MEDICAL_SUBJECT_API_KEY=${HOSPITAL_MEDICAL_SUBJECT_API_KEY}
HOSPITAL_PRODOC_API_KEY=${HOSPITAL_PRODOC_API_KEY}
HOSPITAL_PHARMACY_API_KEY=${HOSPITAL_PHARMACY_API_KEY}
HOSPITAL_EMERGENCY_API_KEY=${HOSPITAL_EMERGENCY_API_KEY}
API_ADMIN_KEY=${API_ADMIN_KEY}

HOSPITAL_MAIN_API_BASE_URL=${HOSPITAL_MAIN_API_BASE_URL}
HOSPITAL_DETAIL_API_BASE_URL=${HOSPITAL_DETAIL_API_BASE_URL}
HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL=${HOSPITAL_MEDICAL_SUBJECT_API_BASE_URL}
HOSPITAL_PRODOC_API_BASE_URL=${HOSPITAL_PRODOC_API_BASE_URL}
HOSPITAL_PHARMACY_API_BASE_URL=${HOSPITAL_PHARMACY_API_BASE_URL}
HOSPITAL_EMERGENCY_API_BASE_URL=${HOSPITAL_EMERGENCY_API_BASE_URL}

DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

YOUTUBE_API_KEY=${YOUTUBE_API_KEY}
YOUTUBE_API_BASE_URL=${YOUTUBE_API_BASE_URL}
YOUTUBE_API_TRUSTED_CHANNELS=${YOUTUBE_API_TRUSTED_CHANNELS}

GEMINI_API_KEY=${GEMINI_API_KEY}
GEMINI_API_URL=${GEMINI_API_URL}
GEMINI_API_MODEL=${GEMINI_API_MODEL}

CHATBOT_SYSTEM_PROMPT_FILE=${CHATBOT_SYSTEM_PROMPT_FILE}
"""

                    // 2. Prometheus Core Config
                    writeFile file: 'prometheus_core.yml', text: """global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'hospital-production'
    environment: 'prod'

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
    scrape_interval: 15s

  - job_name: 'hospital-backend'
    static_configs:
      - targets: ['backend:8888']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 15s

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    scrape_interval: 15s
"""

                    // 3. Prometheus Monitoring Stack Config
                    writeFile file: 'prometheus_monitor.yml', text: """global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'hospital-production'
    environment: 'prod'

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
    scrape_interval: 15s

  - job_name: 'hospital-backend'
    static_configs:
      - targets: ['hospital-backend:8888']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 15s

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    scrape_interval: 15s
"""

                    // 4. Alert Rules
                    writeFile file: 'alert_rules.yml', text: """groups:
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
"""

                    // 5. Grafana Datasources
                    writeFile file: 'grafana_datasources.yml', text: """apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
"""

                    // 6. Grafana Dashboards
                    writeFile file: 'grafana_dashboards.yml', text: """apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
"""
                }
            }
        }
        
        stage('파일 패키징 및 전송') {
            steps {
                script {
                    // 모든 배포 파일을 하나로 묶음
                    sh "tar -czf deploy_pkg.tar.gz backend.tar.gz env.prod *.yml deploy.sh docker-compose.prod.yml"
                    
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh "scp -o StrictHostKeyChecking=no deploy_pkg.tar.gz ${EC2_USER}@${EC2_HOST}:/home/ec2-user/"
                    }
                }
            }
        }
        
        stage('EC2 배포 실행') {
            steps {
                script {
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                            
                            echo "🚀 배포 패키지 해제 중..."
                            tar -xzf deploy_pkg.tar.gz
                            
                            # .env 파일 적용
                            mv env.prod .env
                            
                            # 모니터링 디렉토리 생성
                            sudo mkdir -p /opt/hospital/config/prometheus
                            sudo mkdir -p /opt/hospital/monitoring/prometheus/config
                            sudo mkdir -p /opt/hospital/monitoring/prometheus/data
                            sudo mkdir -p /opt/hospital/monitoring/grafana/data
                            sudo mkdir -p /opt/hospital/monitoring/grafana/provisioning/dashboards
                            sudo mkdir -p /opt/hospital/monitoring/grafana/provisioning/datasources
                            
                            # 설정 파일 이동
                            sudo mv prometheus_core.yml /opt/hospital/config/prometheus/prometheus.yml
                            sudo mv prometheus_monitor.yml /opt/hospital/monitoring/prometheus/config/prometheus.yml
                            sudo cp alert_rules.yml /opt/hospital/config/prometheus/
                            sudo mv alert_rules.yml /opt/hospital/monitoring/prometheus/config/
                            sudo mv grafana_datasources.yml /opt/hospital/monitoring/grafana/provisioning/datasources/prometheus.yml
                            sudo mv grafana_dashboards.yml /opt/hospital/monitoring/grafana/provisioning/dashboards/dashboard.yml
                            
                            sudo chown -R ec2-user:ec2-user /opt/hospital/
                            chmod +x deploy.sh
                            
                            echo "📦 Docker 이미지 로드..."
                            docker load < backend.tar.gz
                            
                            echo "▶️ 배포 스크립트 및 모니터링 스택 실행..."
                            ./deploy.sh
                            
                            # (모니터링 스택 실행 로직 추가)
                            docker network ls | grep hospital-network || docker network create hospital-network
                            
                            # 기존 컨테이너 정리
                            docker stop cadvisor node-exporter prometheus grafana 2>/dev/null || true
                            docker rm cadvisor node-exporter prometheus grafana 2>/dev/null || true
                            
                            # 모니터링 컨테이너 실행
                            docker run -d --name cadvisor --restart unless-stopped --network hospital-network -p 8080:8080 -v /:/rootfs:ro -v /var/run:/var/run:rw -v /sys:/sys:ro -v /var/lib/docker/:/var/lib/docker:ro --privileged --device /dev/kmsg gcr.io/cadvisor/cadvisor:latest
                            
                            docker run -d --name node-exporter --restart unless-stopped --network hospital-network -p 9100:9100 -v /proc:/host/proc:ro -v /sys:/host/sys:ro -v /:/rootfs:ro --pid host prom/node-exporter:latest --path.procfs=/host/proc --path.rootfs=/rootfs --path.sysfs=/host/sys --collector.filesystem.mount-points-exclude="^/(sys|proc|dev|host|etc)(\$|/)"

                            docker run -d --name prometheus --restart unless-stopped --network hospital-network -p 9090:9090 -v /opt/hospital/monitoring/prometheus/config:/etc/prometheus -v /opt/hospital/monitoring/prometheus/data:/prometheus --user "\$(id -u):\$(id -g)" prom/prometheus:latest --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/prometheus --web.console.libraries=/etc/prometheus/console_libraries --web.console.templates=/etc/prometheus/consoles --storage.tsdb.retention.time=200h --web.enable-lifecycle --web.enable-admin-api
                            
                            docker run -d --name grafana --restart unless-stopped --network hospital-network -p 3000:3000 -v /opt/hospital/monitoring/grafana/data:/var/lib/grafana -v /opt/hospital/monitoring/grafana/provisioning:/etc/grafana/provisioning -e GF_SECURITY_ADMIN_USER=admin -e GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD} -e GF_INSTALL_PLUGINS=grafana-piechart-panel,grafana-worldmap-panel,grafana-clock-panel -e GF_USERS_ALLOW_SIGN_UP=false --user "\$(id -u):\$(id -g)" grafana/grafana:latest
                            
                            # 청소
                            rm -f deploy_pkg.tar.gz backend.tar.gz env.prod *.yml
                            '
                        """
                    }
                }
            }
        }
        
        stage('헬스체크') {
            steps {
                script {
                    sshagent(credentials: ['EC2_PRIVATE_KEY']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                                echo "🏥 헬스체크 시작..."
                                sleep 10
                                curl -f -s --connect-timeout 5 http://${EC2_HOST}:8888/api/proDoc/status > /dev/null && echo "✅ 백엔드 정상" || echo "⚠️ 백엔드 확인 필요"
                                curl -f -s --connect-timeout 5 http://${EC2_HOST}:9090/-/healthy > /dev/null && echo "✅ 프로메테우스 정상" || echo "⚠️ 프로메테우스 확인 필요"
                                curl -f -s --connect-timeout 5 http://${EC2_HOST}:3000/api/health > /dev/null && echo "✅ 그라파나 정상" || echo "⚠️ 그라파나 확인 필요"
                            '
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            sh 'rm -f backend.tar.gz deploy_pkg.tar.gz *.yml env.prod || true'
        }
    }
}