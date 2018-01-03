def call() {
 pom = readMavenPom file: 'pom.xml'
 return "${artifactId}-${version}.${packaging}"
}
