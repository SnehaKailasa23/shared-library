def call(def rtMaven, def buildInfo) {
 Reason = "Artifacts Deployment Failed"
 rtMaven.deployer.deployArtifacts buildInfo
 server.publishBuildInfo buildInfo
}
//, def server
