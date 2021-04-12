def call(String testScript = 'test') {
  container('nodejs') {
    try {
      sh "npm run ${testScript}"
    } catch (err) {
      throw err
    } finally {
      junit 'reports/*.xml'

      publishHTML(
        target: [
          reportName: "Code Coverage",
          reportDir: 'reports/coverage/lcov-report',
          reportFiles: 'index.html',
          allowMissing: false,
          alwaysLinkToLastBuild: true,
          keepAll: true
        ]
      )
    }
  }
}