#!/bin/env groovy


import hudson.model.*
import com.ge.power.gas.cloud.automation.*

def call(config) {

    def helper = new Helper()
    //evaluate the body block, and collect onfiguration into the object
    if('master'.equals(env.CHANGE_TARGET)) {
        sh '''
            echo "starting the validation for the CF templates"
        '''
        println "************************"
        helper.downloadResource("helper_functions.sh")
        if( pullrequest!=null) {
            def changeFiles = helper.getFilesFromPull(pullrequest['files'], helper.templateChageMatchers)
            def script = libraryResource 'stack_lint.sh'
            def repoName = helper.determineRepoName()
            if(reponame.startsWith("uai")){
                env.SPEC_FILE = "app_spec.json"
            } else {
                env.SPEC_FILE = "infra_spec.json"
            }
            helper.downloadResource(env.SPEC_FILE)
            helper.downloadResource("rules/PropertyTagsIncluded.py")
            helper.downloadResource("rules/PropertyTagsNotRequired.py")
            helper.downloadResource("rules/ReferLatestAMI.py")
            helper.downloadResource("rules/PatchTagRequired.py")
            def valid = true
            for (int i = 0; i < changeFiles.length; i++) {
                env.CF_TEMPLATE_DIR = changeFiles[i]
                sh "bash -c '$script' ${CF_TEMPLATE_DIR} ${BUILD_AWS_ACCOUNT} ${SPEC_FILE}"
                final reportFiles = findFiles(glob: env.CF_TEMPLATE_DIR + '/**/report_cfn_lint.txt')
                for (reportFile in reportFiles) {
                    println reportFile.toString()
                    def text1 = readFile file: reportFile.toString()
                    if(text1.contains("errors")) {
                        valid = false
                    }   
                }
            }
        }
        if(!valid) {
            try{
                pullRequest.removeLabel("invalid")
            }catch(error){
                println "No label present moving on.."
            }
        } else {
            pullRequest.addLabel("invalid")
            pullRequest.createStatus(status: 'failure',
                context: 'continous-integration/jenkins/pr-merge/cf-validation-tests',
                description: 'CFN Lint failed',
                targetUrl: "${env.JOB_URL}")
                currentBuild.result = 'FAILURE'
        } else {
            println "This PR is not for main branch, skipping CF validation"
        }
    }else {
        println "******************************"
        println "The merge request for other branches doesnot trigger CF validation"
    }
}