def call(Map config = [:]) {
  podTemplates.mavenTemplate {
    podTemplates.helmTemplate {
      podTemplates.kubectlTemplate {
        podTemplates.owaspZapTemplate {
          node(POD_LABEL) {
            deployCodeveros config
          }
        }
      }
    }
  }
}