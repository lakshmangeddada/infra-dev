#Create a stage to checkout the code from the git repository
#Create build stage to create instances in AWS account.
#Create clerar stage to clean up the workspace.
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
