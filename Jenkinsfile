// This is a declarative pipeline script that will checkout the code from the repository
// #!/usr/bin/env groovy

pipeline {
  agent any

  environment {
    AWS_REGION = 'us-east-1'
    STACK_NAME = 'ec2-dev'
    TEMPLATE_FILE = 'ec2-instances/templates/dev-template.yml'
    PARAMS_FILE = 'ec2-instances/parameters/dev-parameter.yml'
  }

  stages {
    stage('Checkout Code') {
      steps {
        checkout scm
        sh 'ls -la'
      }
    }

    stage('Validate Template') {
    steps {
        script {
        echo "🔍 Validating CloudFormation template..."

        def validationOutput = sh(
            script: """
            aws cloudformation validate-template \
                --template-body file://${TEMPLATE_FILE}
            """,
            returnStdout: true
        ).trim()

        echo "✅ Template validation successful. Response:"
        echo "${validationOutput}"
        }
    }
    }


    stage('Create or Update Stack') {
      steps {
        script {
          // Check if stack exists
          def stackExists = sh(
            script: "aws cloudformation describe-stacks --stack-name ${STACK_NAME}",
            returnStatus: true
          ) == 0

          def deployCommand = """
            aws cloudformation create-stack \
            --stack-name ec2-dev \
            --template-body file://ec2-instances/templates/dev-template.yml \
            --parameters \
                ParameterKey=Env,ParameterValue=dev \
                ParameterKey=EC2Type,ParameterValue=t2.micro \
                ParameterKey=Name,ParameterValue=managed-node \
                ParameterKey=SecurityGroup,ParameterValue=launch-wizard-2 \
                ParameterKey=ImageId,ParameterValue=ami-05f08ad7b78afd8cd \
                ParameterKey=SubnetID,ParameterValue=subnet-0b7baf6955e26c759 \
            --capabilities CAPABILITY_NAMED_IAM \
            --region us-east-1
          """

          sh deployCommand
        }
      }
    }

    stage('Wait for Stack Completion') {
      steps {
        script {
          sh """
            aws cloudformation wait stack-${stackExists ? 'update' : 'create'}-complete \
              --stack-name ${STACK_NAME} \
              --region ${AWS_REGION}
          """
        }
      }
    }
  }

  post {
    success {
      echo "✅ Infrastructure created/updated successfully!"
    }
    failure {
      echo "❌ Something went wrong with CloudFormation deployment."
    }
  }
}

