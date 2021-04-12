def call(Map config) {
  if (config.isMasterBranch) {
    return checkout(scm)
  }

  checkout([
    $class: 'GitSCM',
    branches: scm.branches,
    doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
    extensions: scm.extensions + [[$class: 'PreBuildMerge', options: [mergeTarget: config.masterBranch, mergeRemote: 'origin']]],
    userRemoteConfigs: scm.userRemoteConfigs
  ])
}