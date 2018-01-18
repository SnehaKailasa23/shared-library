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
			
			stage('Docker-Compose'){
				sh """ sudo docker-compose up -d
				sudo chmod 777 clean_up.sh
				sudo chmod 777 wait_for_robot.sh 
				./wait_for_robot.sh """
				step([$class: 'RobotPublisher',
					outputPath: "/home/robot/results",
					passThreshold: 0,
					unstableThreshold: 0,
					otherFiles: ""])
				// If Robot Framework test case fails, then the build will be failed //	
				if("${currentBuild.result}" == "FAILURE"){	
					sh ''' ./clean_up.sh
					exit 1'''
				}
			}
			
			stage('Deployments') {
				sh """ chmod 777 remote_script.sh 
				ssh -T "${pipelineParams.remote_user}"@"${pipelineParams.remote_ip}" "bash -s" < ./remote_script.sh """
			}
			
			stage('Triggering QA Job') {
				build job: 'Docker_registry', wait: false
			}
			sh './clean_up.sh'
		}
		catch(Exception e)
		{
			sh './clean_up.sh'
			println "In catch block"
			sh 'exit 1'
		}
	}
}
