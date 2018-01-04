def call(def rtMaven, def server, def snapshot_repo, def release_repo, def Sonar_project_name) {
   rtMaven.deployer server: server, snapshotRepo: snapshot_repo, releaseRepo: release_repo   //Deploying artifacts to this repo //
   rtMaven.deployer.deployArtifacts = false  //this will not publish artifacts soon after build succeeds //
   rtMaven.tool = 'maven'       //Defining maven tool //
   // Maven build starts here //
   //withSonarQubeEnv {
    def mvn_version = tool 'maven'
    withEnv( ["PATH+MAVEN=${mvn_version}/bin",'Sonar_Project_Name=' + "${Sonar_project_name}"]  ) {
     buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install -Dmaven.test.skip=true'
     
    }
   //}
   return buildInfo
}
