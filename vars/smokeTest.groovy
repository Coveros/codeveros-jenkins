def call(String appUrl="") {
    echo 'Perform smoke test for system viability'
    healthCheck = sh(script: "curl ${appUrl}/health-check", returnStatus: true)
    if (healthCheck != 0) {
        echo "Error found running the smoke test."
        currentBuild.result = 'FAILURE'
        throw err
    }
}