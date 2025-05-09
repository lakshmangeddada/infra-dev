#!/bin/env groovy

def call(body) {
    //evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def branch = env.BRANCH_NAME
    def mergebranch = "main"
    def live = "no"
    def yes = "yes"
    def no = "no"
    def SUCCESS = "SUCCESS"

    if(body!=null && body['mergebranch']!=null){
        mergebranch = body['mergebranch']
    }
    config['mergebranch'] = mergebranch

    if(body!=null && body['live']!=null){
        live = body['live']
    }
    config['live'] = live


    pipeline {

        options {
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }
        agent any 

    stages {
        stage('verify-account') {
            steps {
                script {
                    if(body==null || body['account']==null) || body['account']==""{
                        println "************************"
                        println "Jenkinsfile does not contain 'account' number, Add it to the Jenkisfile"
                        println "************************"
                        throw new Exception("Jenkinsfile does not contain 'account' number, Add it to the Jenkisfile")
                    }
                }
            }
        }
        stage('checkout') {
            steps {
                script {
                    checkout scm
                    env.BUILD_AWS_ACCOUNT = sh(script: "curl -s -S 'http://169.254.169.254/latest/dynamic/instance-identity/document/' | jq -r '.account'", returnStdout: true)
                    env.JENKINS_FQDN = sh(script: 'echo ${BUILD_URL/https:\\/\\/} | cut -d "/" -f1', returnStdout: true).trim()
                    env.CODE_AUTHOR = sh(script: "git log -1 --no-merges --format='%ae' ${GIT_COMMIT}", returnStdout: true).trim()
                    env.CODE_MERGED = sh(script: "git log -1 --format='%ae' ${GIT_COMMIT}", returnStdout: true).trim()
                    println "************************"
                    println "BRANCH_NAME  : " + env.BRANCH_NAME
                    println "GIT_COMMIT   : " + env.GIT_COMMIT
                    println "BUILD_AWS_ACCOUNT : " + env.BUILD_AWS_ACCOUNT
                    println "JENKINS_FQDN : " + env.JENKINS_FQDN
                    println "CODE_AUTHOR  : " + env.CODE_AUTHOR
                    println "CODE_MERGED  : " + env.CODE_MERGED
                    println "GIT_URL      : " + env.GIT_URL
                    println "************************"
                }
            }
        }
        stage('verify') {
            when{
                expression { return env.GIT_COMMIT}
            }
            steps{
                sh '''
                    git diff-tree --no-commit-id --name-only -r ${GIT_COMMIT}
                '''
            }
        }  
        stage('codereview') {
            steps {
                script {
                    if(!env.BRANCH_NAME.equals(mergebranch)){
                        println "************************"
                        println "Running template validation"
                        println "************************"
                        validatecf(config)
                        println "************************"
                        println "calling pullrequest"
                        println "************************"
                        pullRequestProcess(config)
                        }
                    }
                }
            }      
        stage('validate') {
            steps {
                script {
                    if(!env.BRANCH_NAME.equals(mergebranch) && 'yes'.equals(live)){
                        println "************************"
                        println "Running PR validation"
                        println "************************"
                        validatePR(config)
                    }
                }
            }
        }

        stage('process') {
            steps {
                script {
                    if(!env.BRANCH_NAME.equals(mergebranch)){
                        println "************************"
                        println "calling merge request"
                        println "************************"
                        mergeRequest(config)
                    }
                }
            }
        }

        stage('end') {
            steps {
                echo "************************"
                echo "Return from Job - completed"
                echo "************************"
            }
        }
    }
    post {
        always {
            script {
                if (env.BRANCH_NAME.equals(mergebranch)) {
                    env.GIT_COMMITTER_EMAIl = sh (
                        script: "git --no-pager show -s --format='%ae'",
                        returnStdout: true
                    ).trim()

                    emailext mimeType: 'text/html',
                        body: "${currentBuild.currentResult}: Job '${env.JOB_NAME} build ${env.BUILD_NUMBER} <br/>more info at: ${env.BUILD_URL}",
                        recipientProviders: [
                            [$class: 'DevelopersRecipientProvider']
                            [$class: 'RequesterRecipientProvider'],
                            [$class: 'CulpritsRecipientProvider']
                        ],
                        subject: "Jenkins build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                        to: "${env.CODE_AUTHOR},${env.CODE_MERGED}"
                    build wait: false, job: 'build-log-updater',
                        parameters: [string(name: 'INVOKER_JOB_URL', value: env.JOB_URL)
                        , string(name: 'INVOKER_JOB_NAME', value: env.JOB_NAME)
                        , string(name: 'INVOKER_BUILD_NUMBER', value: env.BUILD_NUMBER)
                        , string(name: 'INVOKER_BUILD_URL', value: env.BUILD_URL)]
                }
            }
            cleanWs()
        }
    }
    }
}
