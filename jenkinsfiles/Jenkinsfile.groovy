pipeline {
  agent any

  environment {
    VERSION = "0.1.0"
    IMAGE_URL = "registry.chatree.dev/weather/api"
  }

  stages {
    stage('Prepare Env') {
      steps {
        script {
          def now = new Date()
          env.BUILD_DATE = now.format("yyyyMMdd")
          env.BUILD_VERSION = "${VERSION}-${BUILD_DATE}-${BUILD_NUMBER}"
        }
      }
    }

    stage('Static Code Scan') {
      agent {
        docker {
          image 'sonarsource/sonar-scanner-cli:latest'
          args '-v $PWD:/workspace -w /workspace'
        }
      }
      steps {
        withSonarQubeEnv('SonarQube Server') {
          sh 'sonar-scanner'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        sh 'docker build -f docker/Dockerfile . -t ${IMAGE_URL}:${BUILD_VERSION}'
      }
    }

    stage('Image Vulnerability Scan') {
      agent {
        docker {
          image 'aquasec/trivy:latest'
          args '-v /var/run/docker.sock:/var/run/docker.sock -v trivy_cache:/.cache --entrypoint="" -u root --privileged'
        }
      }
      steps {
        sh 'trivy image --format template --template \"@/contrib/html.tpl\" --output report.html --severity HIGH,CRITICAL --no-progress ${IMAGE_URL}:${BUILD_VERSION}'

        publishHTML target : [
          allowMissing: true,
          alwaysLinkToLastBuild: false,
          keepAll: true,
          reportDir: '.',
          reportFiles: 'report.html',
          reportName: 'Trivy Scan Report',
        ]
      }
    }

    stage('Push to registry') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'chatree-docker-registry-credential', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          sh 'docker login registry.chatree.dev -u $USERNAME -p $PASSWORD'
          sh 'docker push ${IMAGE_URL}:${BUILD_VERSION}'
        }
      }
    }

    stage('Clear Image') {
      steps {
        sh 'docker rmi ${IMAGE_URL}:${BUILD_VERSION}'
      }
    }

    stage('Deploy') {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: 'srv-dev-01-webusr-credential', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            def remote = [: ]
            remote.name = 'srv-dev-01'
            remote.host = '10.13.21.1'
            remote.allowAnyHosts = true
            remote.user = USERNAME
            remote.password = PASSWORD
            sshCommand remote: remote, command: "cd /opt/app/weather/weather-api ; sed -i \"s/^\\(TAG_VERSION=\\).*/TAG_VERSION=${BUILD_VERSION}/g\" .env"
            sshCommand remote: remote, command: "cd /opt/app/weather/weather-api ; docker compose up -d"
          }
        }
      }
    }

  }

  post {
    success {
      discordSend description: "Duration: ${currentBuild.durationString}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "${JOB_NAME} - # ${BUILD_VERSION}", footer: "${currentBuild.getBuildCauses()[0].shortDescription}",webhookURL: "https://discord.com/api/webhooks/${DISCORD_WEBHOOK_ID}/${DISCORD_WEBHOOK_TOKEN}"
    }
    failure {
      discordSend description: "Duration: ${currentBuild.durationString}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "${JOB_NAME} - # ${BUILD_VERSION}", footer: "${currentBuild.getBuildCauses()[0].shortDescription}",webhookURL: "https://discord.com/api/webhooks/${DISCORD_WEBHOOK_ID}/${DISCORD_WEBHOOK_TOKEN}"
    }
  }

}
