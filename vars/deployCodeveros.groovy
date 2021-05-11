def call(Map config = [:]) {

  Map defaultConfig = [
    depOverrides: []
  ]

  config = defaultConfig + config

  String appUrl

  try {
    stage("Deploy to ${config.environment} Environment") {
      dir(config.codeverosChartPath) {
        overrideChartDeps config.depOverrides
        addHelmRepos() // required for helm dependency update run during production deploy
        installHelmChart config.namespace, config.releaseName
        appUrl = getAppUrl config.externalIp, config.namespace, config.releaseName
      }
    }

    stage('Post-Deployment Checks') {
      parallel(
        'Smoke Tests': {
          try {
            echo 'Perform smoke test for system viability'
//              sh "wget ${appUrl}/login &> /dev/null"
          } catch (err) {
            echo "Error found running the smoke test."
            currentBuild.result = 'FAILURE'
            throw err
          }
        },
        'Integration Tests': {
          echo 'Run integration tests'
        },
        'API Tests': {
          dir(config.apiTestsPath) {
            runApiTests appUrl, config
          }
        },
        'Regression Tests': {
          dir(config.regressionTestsPath) {
            runRegressionTests appUrl, config
          }
        },
        'Functional Tests': {
          dir(config.functionalTestsPath) {
            runFunctionalTests appUrl, config
          }
        }
      )
    }

    stage('Non-Functional Tests') {
      parallel(
        'Performance Tests': {
          runPerformanceTests()
        },
        'Load Tests': {
          echo 'load test'
        },
        'Accessibility Tests': {
          echo 'accessibility test'
        },
        'Dynamic Security Tests': {
          echo 'dynamic security test'
        },
        'Compatibility Tests': {
          echo 'compatibility test'
        }
      )
    }

    stage('Create helm release') {
      echo 'todo: Create new umbrella helm release on success'
    }

    stage('Update Umbrella Chart') {
      if (config.pushChartOverrides) {
//      pushChartFile config
        echo 'PUSHING CHART CHANGE: Disabled for now'
      } else {
        echo 'Skip pushing Chart.yaml'
      }
    }

    stage('Deploy to Production') {
      // We would normally deploy a chart that was pushed
      dir(config.codeverosChartPath) {
        installHelmChart 'prod', config.releaseName
        getAppUrl config.externalIp, 'prod', config.releaseName
      }
    }

  } catch (err) {
    echo "Error caught deploying Codeveros"
    throw err
  } finally {
    if (config.cleanupDeploy) {
      cleanupDeploy config
    }
  }
}

void pushChartFile(Map config) {
  def chartFile = 'helm/Chart.yaml'
  def changedFiles = sh(returnStdout: true, script: 'git diff --name-only').trim()
  def hasChange = changedFiles.contains(chartFile)
  if (hasChange) {
    echo 'Pushing updated Chart.yaml to git repo to track known working versions'
    def commitMsg = "[JENKINS] Updating Codeveros Helm Chart: Caused by ${JOB_NAME} #${BUILD_NUMBER}"
    def commitDetail = "Overrides:"
    config.depOverrides.each {
      commitDetail += " ${it.name}: ${it.version}"
    }

    sh "git config user.name 'Codeveros CI'"
    sh "git config user.email 'codeveros_ci@coveros.com'"
    sh "git add ${chartFile} && git commit -a -m '${commitMsg}' -m '${commitDetail}'"
    sshagent(credentials: [ config.gitCredentials ]) {
      sh "git push origin ${env.BRANCH_NAME}"
    }
  } else {
    echo 'Chart.yaml unchanged, no updates to push'
  }
}

String getAppUrl(String externalIp, String namespace, String releaseName) {
  def service = releaseName.contains("codeveros") ? "${releaseName}-proxy" : "${releaseName}-codeveros-proxy"
  def getNodePort = "kubectl -n ${namespace} get svc ${service} -o jsonpath='{.spec.ports[?(@.name==\"http\")].nodePort}'"
  def getNodeIP = "kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type==\"InternalIP\")].address }'"

  container('kubectl') {
    def nodePort = sh(returnStdout: true, script: getNodePort).trim()
    def nodeIP = externalIp ?: sh(returnStdout: true, script: getNodeIP).trim()
    def appUrl = "http://${nodeIP}:${nodePort}/"
    echo "Codeveros is externally available at ${appUrl}"
    return appUrl
  }
}

void cleanupDeploy(Map config) {
  container('helm') {
    echo "Deleting ${config.releaseName} release from ${config.namespace}"
    sh "helm uninstall -n ${config.namespace} ${config.releaseName}"
  }
  container('kubectl') {
    echo "Deleting ${config.namespace} namespace"
    sh "kubectl delete namespace ${config.namespace}"
  }
}

void overrideChartDeps(List overrides = []) {
  def chartProps = readYaml file: 'Chart.yaml'
  chartProps.dependencies = chartProps.dependencies ?: []

  echo 'Overriding chart dependencies'

  overrides.each { override ->
    def index = chartProps.dependencies.findIndexOf { dependency ->
      dependency.name == override.name
    }

    if (index > -1) {
      echo "Overriding ${override.name} dependency chart to version ${override.version} located at ${override.repository}"
      def curr = chartProps.dependencies.get(index)
      chartProps.dependencies.set(index, curr + override)
    } else {
      echo "Adding ${override.name} chart dependency at version ${override.version} located at ${override.repository}"
      chartProps.dependencies.add(override)
    }

    writeYaml file: 'Chart.yaml', data: chartProps, overwrite: true
  }
}

void runPerformanceTests() {
  container('taurus') {
    echo 'Running performance tests'
    sh 'bzt --help'
  }
}

void runApiTests(String appUrl, Map config) {
  echo 'Run API tests'
}

void runRegressionTests(String appUrl, Map config) {
  echo 'Run Regression tests'
}

void runFunctionalTests(String appUrl, Map config) {
  container('maven') {
    try {
      echo 'Running functional tests'
      // todo: build custom image. marketing demo builds it each time
      sh '''
        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
        echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list
        apt-get update -qqy
        apt-get -qqy install google-chrome-stable --allow-unauthenticated
        rm /etc/apt/sources.list.d/google-chrome.list
        rm -rf /var/lib/apt/lists/* /var/cache/apt/*
      '''

      def mavenOpts = [
        '-Dgroups=functional',
        '-Dbrowser="name=Chrome&screensize=1920x1080"',
        "-DappURL=${appUrl}",
        "-Dproxy=${config.zapUrl}",
        '-Doptions="--headless,--no-sandbox,--disable-gpu"'
      ]

      withMaven(globalMavenSettingsConfig: config.mavenConfigId) {
        sh "mvn clean verify -B ${mavenOpts.join(' ')}"
      }
    } catch (err) {
      throw err
    } finally {
      // Run in container that has curl
      sh 'mkdir -p zap-proxy'
      sh "curl -o zap-proxy/report.html ${config.zapUrl}/OTHER/core/other/htmlreport"
      junit 'target/failsafe-reports/TEST-*.xml'
      publishHTML([
        allowMissing         : false,
        alwaysLinkToLastBuild: true,
        keepAll              : true,
        reportDir            : 'target/failsafe-reports',
        reportFiles          : 'report.html',
        reportName           : 'Functional Report'
      ])
      publishHTML([
        allowMissing         : false,
        alwaysLinkToLastBuild: true,
        keepAll              : true,
        reportDir            : 'zap-proxy',
        reportFiles          : 'report.html',
        reportName           : 'ZAP Proxy Functional Report'
      ])
    }
  }
}

void installHelmChart(String namespace, String name) {
  container('kubectl') {
    echo 'Verify whether namespace already exists'
    def uid = sh(returnStdout: true, script: "kubectl get namespace ${namespace} --ignore-not-found -o jsonpath='{.metadata.uid}'").trim()
    if (uid == null || uid.length() < 1) {
      echo "Namepace ${namespace} not found. Attempting to create"
      sh "kubectl create namespace ${namespace}"
    }
  }
  container('helm') {
    echo 'Download chart dependencies and install release'
    sh "helm dependency build"
    sh "helm upgrade --install --wait -n ${namespace} ${name} ."
  }
}

void addHelmRepos() {
  container('helm') {
    def chartProps = readYaml file: 'Chart.yaml'
    def dependencies = chartProps.dependencies ?: []
    def repos = []

    dependencies.eachWithIndex { dependency, idx ->
      def repoUrl = dependency.repository ?: ''
      if (repoUrl.startsWith('http') && !repos.contains(repoUrl)) {
        repos.add(repoUrl)
        sh "helm repo add repo${idx} ${repoUrl}"
      }
    }
  }
}