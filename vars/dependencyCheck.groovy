def call(Map config = [:]) {
  container('sonar-scanner') {
    def dependencyCheck = tool 'dependency-check'
    sh "${dependencyCheck}/bin/dependency-check.sh --scan package-lock.json --format ALL --out . --data /root/owasp-cve-db"
    archiveArtifacts allowEmptyArchive: true, artifacts: 'dependency-check-report.*', onlyIfSuccessful: false
    dependencyCheckPublisher pattern: 'dependency-check-report.xml'
  }
}