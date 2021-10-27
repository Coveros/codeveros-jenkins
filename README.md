# jenkins-library

## Usage

This repository is for the shared libraries used within the Codeveros pipelines.

The following examples assume it is loaded into Jenkins as "codeveros-library"
In order to use these shared libraries (methods defined in the "vars" directory) within your "Jenkinsfile" simply add
this line to the top of your Jenkinsfile:

`library 'codeveros-library'`

```
Directory Structure:
src
  -- com.coveros.codeveros.jenkins
      -- Config.groovy # Default constant values, can be overriden with environment variables
vars
  -- runNodeSvcPipeline.groovy  # callable library methods
  -- ...
```

### Using Shared Libraries:

The shared library is designed to encapsulate all pipeline functionality needed to build and deploy a service. To use,
call the runNodeSvcPipeline method, passing in the service-specific configuration. An example Jenkinsfile is:

```groovy
library 'codeveros-library'

runNodeSvcPipeline([
  repository: 'coveros/codeveros-user-service'
])
```

#### Configuration Options

There are many configuration options that can be passed directly into the entry pipeline library methods (`runDeployPipeline`,
`runAngularPipeline`, `runDockerImagePipeline`, and `runNodeSvcPipeline`). The only one that is currently required
to be defined is `repository` (as shown in the above example). The pipeline will fail if no configuration is passed in.
Any value passed in overrides existing default Config constants and environment variables.

Each of the entry methods accepts a single Map input object. In the example above, `runNodeSvcPipeline` is being called
with a Map object with a single attribute `repository`.

#### Environment Variables

In addition to the config map input passed into the entry library methods, configuration can be defined using
environment variables. The order of precedence is:

1. Input attribute
2. Environment variable
3. Default config

The available environment variables and default config constants are identical.

Defining the environment values can happen in a number of ways. For example, you can use the `withEnv` step when
executing the pipeline:

```groovy
library 'codeveros-library'

withEnv(["DOCKER_CREDENTIALS=nexus_auth"]) {
  runNodeSvcPipeline([
    repository: 'coveros/codeveros-user-service'
  ])
}
```

Defining environment variables this way will restrict the changes to a single branch of a single service. You could also
define environment variables using the `EnvInject` Jenkins plugin to set defaults at an environment level. For example,
the following Jenkins Configuration as Code configuration defines global environment variables that will be injected into
every job:

```yaml
  jenkins:
    globalNodeProperties:
      - envVars:
          env:
            - key: "DOCKER_CREDENTIALS"
              value: "nexus_auth"
```

Other options exist, such as reading variables from a `.env` file, or specifying environment variables in the build
agent's Pod template.

#### Configuration options

| input attribute      | Env Var/Config constant    | Description                                                          | Default Value                                     |
|----------------------|----------------------------|----------------------------------------------------------------------|---------------------------------------------------|
| agentPvcName         | AGENT_PVC_NAME             | Kubernetes PersistentVolumeClaim id to persist agent cache           | `jenkins-agent`                                   |
| apiTestsPath         | API_TESTS_PATH             | API Test directory relative to workspace root                        | `tests/api`                                |
| chartPath            | CHART_PATH                 | Location of Helm Chart directory                                     | `./helm`                                          |
| cleanupDeploy        | CLEANUP_DEPLOY             | Delete the Helm chart release and namespace when testing is complete | `true`                                            |
| codeverosChartPath   | CODEVEROS_CHART_PATH       | Codeveros Umbrella Chart directory relative to workspace root        | `charts/codeveros`                                |
| dockerfilePath       | DOCKERFILE_PATH            | Location of Dockerfile                                               | `.`                                               |
| environment          | ENVIRONMENT                | Defines environment                                                  | `ephemeral`                                       |
| externalIp           | EXTERNAL_IP                | Specify external IP address for deployed Codeveros access            | K8s Node Internal IP
| functionalTestsPath  | FUNCTIONAL_TESTS_PATH      | Functional Test directory relative to workspace root                 | `tests/selenified`                                |
| gitCredentials       | GIT_CREDENTIALS            | Credentials to use for Git repositories                              | `codeveros-gitlab-ssh`                            |
| helmCredentials      | HELM_CREDENTIALS           | Credentials to use for Helm repository                               | `docker-registry-login`                           |
| helmRepoUrl          | HELM_REPO_URL              | Helm Chart repository Url                                            | `Not set`                                         |
| masterBranch         | MASTER_BRANCH              | Repo branch to use in conditional checks and merge verification      | `master`                                          |
| mavenConfigId        | MAVEN_CONFIG_ID            | Jenkins globalMavenSettingsConfig ID                                 | `globalmaven`                                     |
| namespace            | NAMESPACE                  | Kubernetes namespace in which to install the helm release            | `codeveros-[RANDOM UIID STRING]`                  |
| nexusHelm            | NEXUS_HELM                 | Boolean to signify that Nexus is used as the Helm repository         | `true`                                            |
| pushBranchTag        | PUSH_BRANCH_TAG            | Whether to push an image tagged with branch                          | `isMasterBranch` check                          |
| pushChartOverrides   | PUSH_CHART_OVERRIDES       | Whether to push update umbrella chart to Git                         | `isMasterBranch` check                          |
| pushLatestTag        | PUSH_LATEST_TAG            | Whether to push an image tagged with `latest`                        | `isMasterBranch` check                          |
| registry             | DOCKER_REGISTRY            | Docker registry to push image                                        | `Not set`                                         |
| registryCredentialId | DOCKER_CREDENTIALS         | Docker registry credential Id                                        | `docker-registry-login`                           |
| regressionTestsPath  | REGRESSION_TESTS_PATH      | Regression Test directory relative to workspace root                 | `tests/selenified`                                |
| releaseName          | RELEASE_NAME               | Defines the Helm Chart release name                                  | `codeveros`                                       |
| repository           | DOCKER_REPOSITORY          | **Required field** Docker image repository                           | `Not set`                                         |
| servicePath          | SERVICE_PATH               | Service code directory relative to workspace root                    | `./`                                              |
| tag                  | DOCKER_TAG                 | Specify additional Docker image tag to build and push                | `Not set`                                         |
| zapUrl               | ZAP_URL                    | ZAP URL to proxy test through                                        | `localhost:5000`                                  |
