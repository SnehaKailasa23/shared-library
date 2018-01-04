def call(def Sonar_project_name) {
	sh """ curl "http://10.240.17.12:9000/sonar/api/resources?resource=${Sonar_project_name}&metrics=bugs,vulnerabilities,code_smells,duplicated_blocks" > output.json """
}
