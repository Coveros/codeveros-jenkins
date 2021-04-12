#!/usr/bin/env groovy

def call(Map config) {
  properties([disableConcurrentBuilds()])

  config = utils.getConfig() + config
  config.branchName = utils.sanitizeBranchName()

  podTemplates.allTemplates {
    node(POD_LABEL) {
      stage('Checkout Code') {
        checkoutCode config
        config.commitHash = sh(returnStdout: true, script: 'git rev-parse HEAD').trim().take(7)
        currentBuild.displayName = "#${BUILD_ID}-${config.commitHash}"
      }

      dir(config.servicePath) {
        stage('Install NPM Dependencies') {
          container('nodejs') {
            sh 'npm ci --quiet'
          }
        }
        stage('Static Test and Analysis') {
          parallel(
            'Lint': {
              container('nodejs') {
                sh 'npm run checkstyle:ci'
              }
            },
            'Dependency Check': {
              dependencyCheck()
            },
            'Unit Test': {
              unitTestNode 'test-ci'
            }
          )
        }
        sonarQubeScan config
        config.dockerImage = buildDockerImage config
      }

      def helmChart = buildHelmChart config
      if (helmChart != null) {
        config.depOverrides = [helmChart]
        deployCodeveros config
      }
    }
  }
}
