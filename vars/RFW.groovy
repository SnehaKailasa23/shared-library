
def call(def robot_result_folder, def rtMaven, def server, def jar_name) {
    def Reason = "RFW stage Failed"
sh "jarfile_name=${jar_name} /usr/local/bin/docker-compose up -d"
    sh "sudo chmod 777 wait_for_robot.sh "
                sh './wait_for_robot.sh'
    step([$class: 'RobotPublisher',
     outputPath: "/home/robot/${robot_result_folder}",
     passThreshold: 0,
     unstableThreshold: 0,
     otherFiles: ""])
    // If Robot Framework test case fails, then the build will be failed // 
    if("${currentBuild.result}" == "FAILURE")
      { 
                         sh ''' ./clean_up.sh
                         echo "after cleanup"
       exit 1'''
      } 
    sh "ls /home/robot/${robot_result_folder}"
    sh "cp /home/robot/${robot_result_folder}/ ."
    // If it is a GitHub PR job, then this part doesn't execute //      
    return Reason
}
