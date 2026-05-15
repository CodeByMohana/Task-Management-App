pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'flowboard'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend With Maven') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn clean package -DskipTests'
                    } else {
                        bat 'mvn clean package -DskipTests'
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'server',
                        'api-gateway',
                        'auth-service',
                        'workspace-service',
                        'board-service',
                        'card-service',
                        'comment-service',
                        'label-service',
                        'notification-service'
                    ]

                    services.each { service ->
                        def image = "flowboard-${service}:local"
                        if (isUnix()) {
                            sh "docker build -t ${image} ./${service}"
                        } else {
                            bat "docker build -t ${image} .\\${service}"
                        }
                    }
                }
            }
        }

        stage('Deploy Locally With Docker Compose') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker compose down'
                        sh 'docker compose up -d'
                    } else {
                        bat 'docker compose down'
                        bat 'docker compose up -d'
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (isUnix()) {
                    sh 'docker compose ps'
                } else {
                    bat 'docker compose ps'
                }
            }
        }
    }
}
