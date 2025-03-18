// This is a declarative pipeline script that will checkout the code from the repository
#!/usr/bin/env groovy
pipeline    {
    agent any
    stages {
        stage('checkout') {
            steps {
                script {
                    checkout scm
                }
            }
        }
    }
}
