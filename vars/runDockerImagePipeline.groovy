#!/usr/bin/env groovy

def call(Map config) {
  properties([disableConcurrentBuilds()])

  config = utils.getConfig() + config
  config.branchName = utils.sanitizeBranchName()

  podTemplates.dockerTemplate {
    node(POD_LABEL) {
      stage('Checkout Code') {
        checkout scm
        config.commitHash = sh(returnStdout: true, script: 'git rev-parse HEAD').trim().take(7)
        currentBuild.displayName = "#${BUILD_ID}-${config.commitHash}"
      }
      buildDockerImage config
    }
  }
}
