def call(Map config) {
  container('docker') {
    def commitTag = config.isMasterBranch ? config.commitHash : "${config.branchName}-${config.commitHash}"

    try {
      stage('Build Docker Image') {
        def image = docker.build("${config.repository}:${commitTag}", "${config.dockerfilePath}")
        docker.withRegistry("http://${config.registry}", config.registryCredentialId) {
          image.push()

          if (config.pushLatestTag) {
            echo 'Pushing latest tag'
            image.push('latest')
          } else {
            echo 'Skipping latest tag'
          }

          if (config.pushBranchTag) {
            echo 'Pushing branch tag'
            image.push(config.branchName)
          } else {
            echo 'Skipping branch tag'
          }

          if (config.tag) {
            echo "Pushing configured tag: ${config.tag}"
            image.push(config.tag)
          }

          // todo: push version tag, using version from Jenkinsfile or a docker.yaml file. Or using the version from package.json
        }
      }
    } catch(err) {
      echo 'Error building and pushing Docker image'
      throw err
    } finally {
      echo 'Delete created Docker images from local registry'
      deleteImage("${config.repository}:${commitTag}")
      deleteImage("${config.registry}/${config.repository}:${commitTag}")
      deleteImage("${config.registry}/${config.repository}:latest")
      deleteImage("${config.registry}/${config.repository}:${config.branchName}")

      if (config.tag) {
        deleteImage("${config.registry}/${config.repository}:${config.tag}")
      }
    }
    return [name: "${config.registry}/${config.repository}", tag: commitTag]
  }
}

def deleteImage(imageName) {
  def imageId = sh(returnStdout: true, script: "docker images -q ${imageName}")
  if (imageId) {
    sh "docker rmi ${imageName} | true"
  }
}