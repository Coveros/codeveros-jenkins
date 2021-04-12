def call(Map config) {
  container('sonar-scanner') {
    stage('Publish Analysis Results') {
      if (!fileExists('sonar-project.properties')) {
        throw new Exception('sonar-project.properties file missing')
      }
      def sonarProps = readProperties file: 'sonar-project.properties'
      def projectKey = sonarProps['sonar.projectKey']

      if (!config.isMasterBranch) {
        projectKey = "${projectKey}_${config.branchName}"
      }

      withSonarQubeEnv() {
        sh "sonar-scanner -Dsonar.projectKey=${projectKey} \
            -Dsonar.dependencyCheck.htmlReportPath=dependency-check-report.html \
            -Dsonar.dependencyCheck.reportPath=dependency-check-report.xml \
            -Dsonar.dependencyCheck.summarize=true"
      }
    }
  }
}