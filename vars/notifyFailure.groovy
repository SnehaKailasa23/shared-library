def call(def Reason){
  emailext (attachLog: true, attachmentsPattern: '*.html, output.xml', body: '''${SCRIPT, template="email_template_failure.groovy"}''', subject: '$DEFAULT_SUBJECT', to: 'yerriswamy.konanki@ggktech.com')
}