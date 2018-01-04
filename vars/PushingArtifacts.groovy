def call(def rtMaven, def buildInfo, def server) {
 rtMaven.deployer.deployArtifacts buildInfo
 server.publishBuildInfo buildInfo
 }
