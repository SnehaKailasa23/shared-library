def call(def buildInfo, def release_repo, def snapshot_repo, def server)
{
	def promotionConfig = [
	// Mandatory parameters
	'buildName'          : buildInfo.name,
	'buildNumber'        : buildInfo.number,
	'targetRepo'         : release_repo,
		
	// Optional parameters
	'comment'            : 'PROMOTION SUCCESSFULLY COMPLETED',
	'sourceRepo'         : snapshot_repo,
	'status'             : 'Released',
	'includeDependencies': false,
	'copy'               : false,
	'failFast'           : true
	]
	 
	// Interactive promotion of Builds in Artifactory server from Jenkins UI //
	Artifactory.addInteractivePromotion server: server, promotionConfig: promotionConfig, displayName: "Promotions Time" //this need human interaction to promote
}
