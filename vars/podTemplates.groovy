def dockerTemplate(Closure body) {
  podTemplate(
    containers: [containerTemplate(name: 'docker', image: 'docker:19.03', command: 'cat', ttyEnabled: true)],
    volumes: [hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')]
  ) {
    body()
  }
}

def nodejsTemplate(Closure body) {
  podTemplate(
    containers: [containerTemplate(name: 'nodejs', image: 'coveros/node-puppeteer:12.16-libs', command: 'cat', ttyEnabled: true)]
  ) {
    body()
  }
}

def sonarScannerTemplate(Closure body) {
  def claimName = utils.getConfig('agentPvcName')

  podTemplate(
    containers: [
      containerTemplate(
        name: 'sonar-scanner',
        image: 'coveros/sonarscanner-js:1.0',
        command: 'cat',
        ttyEnabled: true
      )
    ],
    volumes: [
      persistentVolumeClaim(mountPath: '/root', claimName: claimName, readOnly: false)
    ]
  ) {
    body()
  }
}

def helmTemplate(Closure body) {
  podTemplate(
    containers: [
      containerTemplate(
        name: 'helm',
        image: 'alpine/helm:3.5.0',
        command: 'cat',
        ttyEnabled: true,
        envVars: [
          envVar(key: 'HOME', value: '/tmp') // Temporarily setting this so the helm-push plugin and helm repos aren't cached
        ]
      )
    ]
  ) {
    body()
  }
}

def kubectlTemplate(Closure body) {
  podTemplate(
    containers: [containerTemplate(name: 'kubectl', image: 'bitnami/kubectl:1.17.3', command: 'cat', ttyEnabled: true, runAsUser: '0')]
  ) {
    body()
  }
}

def mavenTemplate(Closure body) {
  def claimName = utils.getConfig('agentPvcName')

  podTemplate(
    containers: [containerTemplate(name: 'maven', image: 'maven:3.6.3-jdk-8', command: 'cat', ttyEnabled: true)],
    volumes: [
      persistentVolumeClaim(mountPath: '/root', claimName: claimName, readOnly: false),
      emptyDirVolume(mountPath: '/dev/shm', memory: false)
    ]
  ) {
    body()
  }
}

def owaspZapTemplate(Closure body) {
  podTemplate(
    containers: [
      containerTemplate(
        name: 'owasp-zap',
        image: 'owasp/zap2docker-stable:2.9.0',
        command: 'zap-x.sh',
        args: '-daemon -host 0.0.0.0 -port 5000 -config api.disablekey=true',
        ttyEnabled: true
      )
    ]
  ) {
    body()
  }
}

def taurusTemplate(Closure body) {
  podTemplate(
    containers: [containerTemplate(name: 'taurus', image: 'blazemeter/taurus:1.14.2', command: 'cat', ttyEnabled: true)]
  ) {
    body()
  }
}

def pythonTemplate(Closure body) {
  podTemplate(
    containers: [
      containerTemplate(
        name: 'python',
        image: 'python:latest',
        command: 'cat',
        ttyEnabled: true
      )
    ]
  )
}

def allTemplates(Closure body) {
  dockerTemplate {
    nodejsTemplate {
      sonarScannerTemplate {
        helmTemplate {
          mavenTemplate {
            kubectlTemplate {
              owaspZapTemplate {
                taurusTemplate {
                  pythonTemplate {
                    body()
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

//def dependencyCheckTemplate(Closure body) {
//  podTemplate(
//    containers: [
//      containerTemplate(
//        name: 'dependency-check',
//        image: 'owasp/dependency-check:5.3.0',
//        command: 'cat',
//        ttyEnabled: true
//      )
//    ],
//    volumes: [
//      persistentVolumeClaim(mountPath: '/root/owasp-cve-db', claimName: 'jenkins', readOnly: false)
//    ]
//  ) {
//    body()
//  }
//}