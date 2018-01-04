def call(def Docker_Reg_Name, def om_image_name, def cp_image_name, def Docker_Registry_URL, def Docker_Credentials, def image_version, def JobName) 
{
	def images = []
	images[0] = "${Docker_Reg_Name}/${om_image_name}"
	images[1] = "${Docker_Reg_Name}/${cp_image_name}"
	docker.withRegistry("${Docker_Registry_URL}", "${Docker_Credentials}") {
  		images.each { def image ->
			docker.image("${image}").push("${image_version}")
			docker.image("${image}").push("latest")
        	}
	}
	sh """docker logout""" 
}
