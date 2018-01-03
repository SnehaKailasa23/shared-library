def call(def pom.artifactId, def pom.version, def pom.packaging) {
 pom = readMavenPom file: 'pom.xml'
 return "${pom.artifactId}-${pom.version}.${pom.packaging}"
}
