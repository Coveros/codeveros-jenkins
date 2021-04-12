def call(Map config) {
  def chartProps

  stage('Build Helm Chart') {
    if (!fileExists("${config.chartPath}/Chart.yaml")) {
      echo 'Helm chart not found, skipping'
      return
    }

    container('helm') {
      dir(config.chartPath) {
        chartProps = readYaml file: 'Chart.yaml'
        chartProps.version = "${chartProps.version}-${config.dockerImage.tag}"
        chartProps.appVersion = config.dockerImage.tag
        writeYaml file: 'Chart.yaml', data: chartProps, overwrite: true
        def valuesProps = readYaml file: 'values.yaml'
        valuesProps.image.repository = config.dockerImage.name
        writeYaml file: 'values.yaml', data: valuesProps, overwrite: true
        sh "helm lint ."
      }

      // todo: bake this helm-push installation into custom Docker image, add push plugin
      sh '''
        apk --no-cache add git curl
        helm plugin install https://github.com/chartmuseum/helm-push
        apk del git
        rm -f /var/cache/apk/*
      '''

      withCredentials([
        usernamePassword(usernameVariable: 'REPO_USER', passwordVariable: 'REPO_PASS', credentialsId: config.helmCredentials)
      ]) {
        if (config.nexusHelm) {
          sh """
           helm dependency build ${config.chartPath}
           CHART_PACKAGE="\$(helm package ${config.chartPath} | cut -d":" -f2 | tr -d '[:space:]')"
           curl -u $REPO_USER:$REPO_PASS ${config.helmRepoUrl} --upload-file \$CHART_PACKAGE -v
          """
        } else {
          sh "helm repo add chartRepo ${config.helmRepoUrl} --username $REPO_USER --password $REPO_PASS"
          sh "helm push -d ${config.chartPath} chartRepo"
        }
      }
    }
    return [name: chartProps.name, version: chartProps.version, repository: config.helmRepoUrl]
  }
}
