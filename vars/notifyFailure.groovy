def call(def Reason){
  properties([[$class: 'EnvInjectJobProperty', info: [loadFilesFromMaster: false, propertiesContent: "Reason=${Reason}"], keepBuildVariables: true, keepJenkinsSystemVariables: true, on: true]])
  emailext (attachLog: true, attachmentsPattern: '*.html, output.xml', body: '''${SCRIPT, template="email_template_failure.groovy"}''', subject: '$DEFAULT_SUBJECT', to: 'yerriswamy.konanki@ggktech.com, sneha.kailasa@ggktech.com, sunil.boga@ggktech.com')
}
