def call(def Sonar_project_name, def CDJob)
{
  def Reason = "Triggering CD job failed"
  CD_Job_name = Sonar_project_name + CDJob
  println CD_Job_name
  build job: 'Docker_registry'//, parameters: [[$class: 'StringParameterValue', name: 'var1', value: 'var1_value']]
  return Reason
}
