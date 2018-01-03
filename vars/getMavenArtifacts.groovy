def call(def artifactId, def version, def packaging) {
 pom = readMavenPom file: 'pom.xml'
 return "${artifactId}-${version}.${packaging}"
}
