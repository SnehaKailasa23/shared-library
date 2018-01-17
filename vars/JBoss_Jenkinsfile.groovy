def call(Map pipelineParams) {
def rtMaven = Artifactory.newMavenBuild()
node {
		try {
			cleanWs()
			stage ('Source Code Checkout') {
				Reason = "GIT Checkout Failed"
				checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/SnehaKailasa23/Java_sample_app.git']]]
				//checkout scm
			}
			
			stage('Maven Build') {
				Reason = "Maven Build Failed"
				rtMaven.tool = 'maven'							//Defining maven tool //
				// Maven build starts here //
				def mvn_version = tool 'maven'
				withEnv( ["PATH+MAVEN=${mvn_version}/bin"]  ) {
					buildInfo = rtMaven.run pom: 'SpringMVCSecurityXML/pom.xml', goals: 'clean install -Dmaven.test.skip=true' 
				}
			}
			
			stage('Deployments') {
				sh ''' . ./Variables
				ssh -T $pipelineParams.remote_user@$pipelineParams.remote_ip "bash -s" < $pipelineParams.deploy_script '''
			}
			
			stage('Triggering QA Job') {
				build job: 'Docker_registry', wait: false
			}
		}
		catch(Exception e)
		{
			println "In catch block"
			sh 'exit 1'
		}
	}
}
