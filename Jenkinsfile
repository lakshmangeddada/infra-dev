pipeline {
    agent any
    // environment{
    //     SSH = credentials("SSH")
    //}
    stages {
        stage ('one'){
            steps{
                echo "one"
                sh 'env > /tmp/env'
            }
        }
        stage ('two'){
            steps{
                echo "two"
            }
        }
    }
    post {
        failure {
            echo 'I will always say Hello again!'
        }
    }
}