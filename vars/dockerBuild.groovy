def call(String registryCred = 'a', String registryin = 'a', String docTag = 'a', String contName = 'a', String grepo = 'a', String gbranch = 'a', String gitcred = 'a') {

pipeline {
		environment { 
		  registryCredential = "${registryCred}"
		  registry = "$registryin" 	
		  dockerTag = "${docTag}_$BUILD_NUMBER"
		  containerName = "${contName}"
		  gitRepo = "${grepo}"
		  gitBranch = "${gbranch}"
		  gitCredId = "${gitcred}"
		}
		
	agent any
	
	triggers {
			pollSCM '* * * * *'
		}

		stages {
					stage("POLL SCM"){
						steps {
							 checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "$gitRepo", credentialsId: "$gitCredId"]], branches: [[name: "refs/heads/$gitBranch"]]], poll: true
						}
					}	
					
					stage('BUILD IMAGE') { 
						 steps { 
							 sh 'pwd'	
							 sh 'ls -lrt'	
							 script { 
								 dockerImage = docker.build registry + ":" + dockerTag 
							 }
						} 
					}
					
					stage('PUSH HUB') { 
						 steps { 
							 script { 
								 docker.withRegistry( '', registryCredential ) { 
									 dockerImage.push() 
									}
							}		
						} 
					}
					
					stage('DEPLOY IMAGE') {
						steps {
						 sshagent (credentials: ['qpfb_ssh']) {
								sh "ssh -o StrictHostKeyChecking=no -l qprofiles1 34.122.133.104 deploy $registry $dockerTag $containerName"
							  }
						}
					}
			}
			  
}

}