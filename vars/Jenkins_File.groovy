// Reading jar file name from pom.xml //
def getMavenBuildArtifactName() {
 pom = readMavenPom file: 'pom.xml'
 return "${pom.artifactId}-${pom.version}.${pom.packaging}"
}

def call() {
/****************************** Environment variables ******************************/  
def JobName	= null						// variable to get jobname  
def Sonar_project_name = null 							// varibale passed as SonarQube parameter while building the application
def robot_result_folder = null 				// variable used to store Robot Framework test results
def server = null							// Artifactory server instance declaration. 'server1' is the Server ID given to Artifactory server in Jenkins
def buildInfo = null 						// variable to store build info which is used by Artifactory
def rtMaven = Artifactory.newMavenBuild()	// creating an Artifactory Maven Build instance
def Reason = "JOB FAILED"					// variable to display the build failure reason
def lock_resource_name = null 					// variable for storing lock resource name


/****************************** Jenkinsfile execution starts here ******************************/
node {
	try {
		cleanWs()
/****************************** Git Checkout Stage ******************************/
		stage ('Checkout') {
			Reason = "GIT Checkout Failed"
			checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/TestBoga']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/SnehaKailasa23/CICD.git']]]
			//checkout scm
			}	//Checkout SCM stage ends
      	def content = readFile './.env'				// variable to store .env file contents
		Properties docker_properties = new Properties()	// creating an object for Properties class
		InputStream contents = new ByteArrayInputStream(content.getBytes());	// storing the contents
		docker_properties.load(contents)	
		contents = null
		// assigning the jarname to this variable aquired from pom.xml by below function //
		def jar_name = getMavenBuildArtifactName()

/****************************** Stage that creates lock variable and SonarQube variable ******************************/
		stage ('Reading Branch Varibles ')	{
		    Reason = " Reading Branch Varibles stage Failed"
            JobName = "${JOB_NAME}"
			lock_resource_name = "jenkins-file"
			Sonar_project_name = "jenkins-file"
			/*if("${BRANCH_NAME}".startsWith('PR-'))
			{
             	lock_resource_name = JobName.substring(0 , JobName.indexOf("/"))+"_"+"${CHANGE_TARGET}"
                Sonar_project_name = lock_resource_name + "PR"
			}
			else
			{
				 lock_resource_name = JobName.substring(0 , JobName.indexOf("/"))+"_"+"${BRANCH_NAME}"
				 Sonar_project_name = lock_resource_name
			} */
		}	// Reading branch variable stage ends
		server =  Artifactory.server docker_properties.ArtifactoryServerName
	
/****************************** Building the Application and performing SonarQube analysis ******************************/	
		stage ('Maven Build') {
			Reason = "Maven Build Failed"
			rtMaven.deployer server: server, snapshotRepo: docker_properties.snapshot_repo, releaseRepo: docker_properties.release_repo			//Deploying artifacts to this repo //
			rtMaven.deployer.deployArtifacts = false		//this will not publish artifacts soon after build succeeds	//
			rtMaven.tool = 'maven'							//Defining maven tool //
			// Maven build starts here //
			//withSonarQubeEnv {
				def mvn_version = tool 'maven'
				withEnv( ["PATH+MAVEN=${mvn_version}/bin",'Sonar_Project_Name=' + "${Sonar_project_name}"]  ) {
					buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install -Dmaven.test.skip=true' //$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.projectKey=${Sonar_Project_Name} -Dsonar.projectName=${Sonar_Project_Name}'
				}
			//}
		}	//Maven Build stage ends 

/****************************** Docker Compose and Robot Framework testing on container ******************************/
		stage ('Docker Deploy and RFW') {
			Reason = "Docker Deployment or Robot Framework Test cases Failed"
			lock(lock_resource_name) {
				// Docker Compose starts // 
				sh "jarfile_name=${jar_name} /usr/local/bin/docker-compose up -d"
				sh "sudo chmod 777 wait_for_robot.sh "
                sh './wait_for_robot.sh'
				step([$class: 'RobotPublisher',
					outputPath: "/home/robot/${docker_properties.robot_result_folder}",
					passThreshold: 0,
					unstableThreshold: 0,
					otherFiles: ""])
					println "outside"
				// If Robot Framework test case fails, then the build will be failed //	
				if("${currentBuild.result}" == "FAILURE")
					 {	
                         sh ''' ./clean_up.sh
                         echo "after cleanup"
						 exit 1'''
					 } 
				sh "cp /home/robot/${docker_properties.robot_result_folder}/* ."
				// If it is a GitHub PR job, then this part doesn't execute //					 
			stage('Pushing Artifacts')
				{
                Reason = "Pushing Artifacts stage failed"
				if(!(JobName.contains('PR-')))
				{
                    // ***** Stage for Deploying artifacts to Artifactory ***** //				
					stage ('Artifacts Deployment'){		
						Reason = "Artifacts Deployment Failed"
						rtMaven.deployer.deployArtifacts buildInfo
						server.publishBuildInfo buildInfo
					}
					// ***** Stage for Publishing Docker images ***** //							
					stage ('Publish Docker Images'){
						Reason = "Publish Docker Images Failed"								
						def images = []
						images[0] = "${docker_properties.Docker_Reg_Name}/${docker_properties.om_image_name}"
						images[1] = "${docker_properties.Docker_Reg_Name}/${docker_properties.cp_image_name}"
		 				docker.withRegistry("${docker_properties.Docker_Registry_URL}", "${docker_properties.Docker_Credentials}") {
             						images.each { def image ->
								docker.image("${image}").push("${docker_properties.image_version}")
								docker.image("${image}").push("latest")
        							}
							}
						sh """docker logout""" 
					
					}  //Docker publish stage ends here
					
					// ***** Stage for triggering CD pipeline ***** //				
					stage ('Starting QA job') {
					Reason = "Trriggering downStream Job Failed"
                    CD_Job_name = Sonar_project_name + docker_properties.CDEnvironment
		   			build job: 'Docker_Registry'//, parameters: [[$class: 'StringParameterValue', name: 'var1', value: 'var1_value']]
					} 
				}     //if loop
				}
				sh './clean_up.sh'	
			}   //lock			
		}		// Docker Deployment and RFW stage ends here //

/****************************** Stage for artifacts promotion ******************************/
		stage ('Build Promotions') {
			Reason = "Build Promotions Failed"
			def promotionConfig = [
				// Mandatory parameters
				'buildName'          : buildInfo.name,
				'buildNumber'        : buildInfo.number,
				'targetRepo'         : docker_properties.release_repo,
	 
				// Optional parameters
				'comment'            : 'PROMOTION SUCCESSFULLY COMPLETED',
				'sourceRepo'         : docker_properties.snapshot_repo,
				'status'             : 'Released',
				'includeDependencies': false,
				'copy'               : false,
				'failFast'           : true
			]
	 
			// Interactive promotion of Builds in Artifactory server from Jenkins UI //
			Artifactory.addInteractivePromotion server: server, promotionConfig: promotionConfig, displayName: "Promotions Time" //this need human interaction to promote
		} 
		
/****************************** Stage for creating reports for SonarQube Analysis ******************************/
		/*stage ('Reports creation') {
			Reason = "Reports creation Failed"
			sh """ curl "http://10.240.17.12:9000/sonar/api/resources?resource=${Sonar_project_name}&metrics=bugs,vulnerabilities,code_smells,duplicated_blocks" > output.json """
		} */
		
/****************************** Stage for sending Email Notifications when Build succeeds ******************************/	
		stage ('Email Notifications') {
           	properties([[$class: 'EnvInjectJobProperty', info: [loadFilesFromMaster: false, propertiesContent: "JobWorkSpace=${WORKSPACE}"], keepBuildVariables: true, keepJenkinsSystemVariables: true, on: true]])
			emailext (
				attachLog: true, attachmentsPattern: '*.html, output.xml', body: '''
				${SCRIPT, template="email_template_success.groovy"}''', subject: '$DEFAULT_SUBJECT', to: "${docker_properties.recipient1}, ${docker_properties.recipient2}, ${docker_properties.recipient3}") 
		}
	}
	
catch(Exception e)
	{
		sh './clean_up.sh'
		currentBuild.result = "FAILURE"
		properties([[$class: 'EnvInjectJobProperty', info: [loadFilesFromMaster: false, propertiesContent: "Reason=${Reason}"], keepBuildVariables: true, keepJenkinsSystemVariables: true, on: true]])
		emailext (
			attachLog: true, attachmentsPattern: '*.html, output.xml', body: '''${SCRIPT, template="email_template_failure.groovy"}''', subject: '$DEFAULT_SUBJECT', to: "${docker_properties.recipient1}, ${docker_properties.recipient2}, ${docker_properties.recipient3}")
		sh 'exit 1'
	}
}
}
