    pipeline {
    agent {
        label "master"
    }

    tools {
        maven "Maven 3.8.1"
        jdk 'JDK8'
    }
    environment {
         // Pipeline controls
         SKIP_MAVEN_BUILD = false;
         SKIP_SONARQUBE = true;
         SKIP_PREPARE_PUBLISH_MEDATADATA = true;
         SKIP_PREPARE_AWS = true;
         SKIP_DEPLOY = true;
         SKIP_PUBLISH_NEXUS = true;
         SKIP_TAG_SVN = true;

        // Maven
        PROFILES="-DskipTests -Prelease -Pui -Pdist -Prpm"

        // Aws
        AWS_INSTANCE_ID="i-07e27934500a38b16"

        // Svn
        SVN_PATH="https://svn.bowmanmi.tech/"
        SVN_CHECKOUT_PATH="coraline/tags/coraline-0.0.1"
        SVN_TAGS_PATH="coraline/tags"
        
        // Nexus
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "https"
        NEXUS_URL = "nexus.bowmanmi.tech"
        NEXUS_REPOSITORY_MAVEN = "maven-bowman"
        NEXUS_REPOSITORY_RPM = "yum-bowman"
        NEXUS_REPOSITORY_NPM = "npm-bowman"
        NEXUS_REPOSITORY_NPM_PUBLIC = "npm-public"
        NPM_REGISTRY="${NEXUS_PROTOCOL}://${NEXUS_URL}/repository/${NEXUS_REPOSITORY_NPM}"
        NPM_REGISTRY_PUBLIC="${NEXUS_PROTOCOL}://${NEXUS_URL}/repository/${NEXUS_REPOSITORY_NPM_PUBLIC}"
        NPM_EMAIL="test@mail.com"        

        // Credential ids
        SVN_CREDENTIALS_ID='JenkinsSvnUser';
        NEXUS_CREDENTIAL_ID = "Nexus-Credentials"
        SONARQUBE_CREDENTIAL_ID = "SonarqubeToken"
        AWS_CREDENTIAL_ID = "Nexus-Credentials"
        
        // Env empty variables. Do not touch!
        SONARQUBE_TOKEN = "";
        SVN_USERNAME = "";
        SVN_PASSWORD = "";
        NEXUS_USERNAME = "";
        NEXUS_PASSWORD = "";
        MAIN_ARTIFACTID = "";
        MAIN_VERSION = "";
        AWS_API_KEY = "";
        AWS_API_SECRET = "";
    }
            
    stages {
        stage('Preparing pipeline') {
                steps {
                    script {

                        withCredentials([string(credentialsId: SONARQUBE_CREDENTIAL_ID, variable: 'sqc')]) {
                            SONARQUBE_CREDENTIAL_ID = sqc;
                        }                    
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:SVN_CREDENTIALS_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                            SVN_USERNAME = USERNAME;
                            SVN_PASSWORD = PASSWORD;
                        }
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:NEXUS_CREDENTIAL_ID, usernameVariable: 'NUSERNAME', passwordVariable: 'NPASSWORD']]) {
                            NEXUS_USERNAME = NUSERNAME;
                            NEXUS_PASSWORD = NPASSWORD;
                        }
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:AWS_CREDENTIAL_ID, usernameVariable: 'A1', passwordVariable: 'A2']]) {
                            AWS_API_KEY = A1;
                            AWS_API_SECRET = A2;
                        }
                                        
                    }
                }
            }    
        stage('Svn Checkout') {
            steps {
                    checkout poll: false, scm: [$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[cancelProcessOnExternalsFail: false, credentialsId: SVN_CREDENTIALS_ID, depthOption: 'infinity', ignoreExternalsOption: false, local: './', remote: "${SVN_PATH}${SVN_CHECKOUT_PATH}"]], quietOperation: true, workspaceUpdater: [$class: 'UpdateWithCleanUpdater']]
            }
        }


        stage('Preparing builds') {
            when {expression { SKIP_MAVEN_BUILD.toBoolean() == false }}

            steps {
                script {
                   stage("Build profiles") {
                                echo "Building profiles...";
                                sh "cd Coraline && mvn clean package ${PROFILES}"; 
                   }
                }
            }
        }
        
      
        stage("Preparing publish metadata") {
            when {expression { SKIP_PREPARE_PUBLISH_MEDATADATA.toBoolean() == false }}
            steps {
                script {
                        echo "Creating publish metadata directory"
                        sh "mkdir jenkinsTmp";
                        for (profile in PROFILES.split(";")) {
                            // Ciclo su tutti i pom del workspace
                            pomFiles = findFiles(glob: "*/pom.xml");
                            mainPomFile = findFiles(glob: ".flattened-pom.xml");
                            echo "Generating main project file metadata";
                             for (pomFile in mainPomFile) {
                                // Determino il finalName del progetto al netto dei profili
                                pomProperties = sh script: "echo \'\${project.groupId},\${project.artifactId},\${project.name},\${project.build.finalName},\${project.version},\${project.build.directory}\' | mvn -N -q -DforceStdout help:evaluate -P${profile} -f ${pomFile}", returnStdout: true;
                                groupId = pomProperties.split(",")[0];
                                artifactId = pomProperties.split(",")[1];
                                name = pomProperties.split(",")[2];
                                finalName = pomProperties.split(",")[3];
                                version = pomProperties.split(",")[4];
                                buildDirectory = pomProperties.split(",")[5];
                                // Controllo se il file di metadati è stato già prodotto
                                metadataFile = "jenkinsTmp"+File.separator+finalName;
                                metadataFileExists = fileExists metadataFile;
                                if (!metadataFileExists) {
                                    // Controllo se la directory target esiste
                                        // Il file di metadati non esiste, procedo alla sua creazione
                                        echo "Creazione '${metadataFile}'..."
                                        jarFile = '';
                                        rpmFile = '';
                                        flattenedPomFile = sh script: "find . -maxdepth 1 -name .flattened-pom.xml -printf '%p'", returnStdout: true;
                                        writeFile file: metadataFile, text: groupId+","+artifactId+","+name+','+finalName+","+version+","+jarFile+","+rpmFile+","+flattenedPomFile;
                                }
                            }   
                            echo "Generating projects file metadata";
                            for (pomFile in pomFiles) {
                                // Determino il finalName del progetto al netto dei profili
                                pomProperties = sh script: "echo \'\${project.groupId},\${project.artifactId},\${project.name},\${project.build.finalName},\${project.version},\${project.build.directory}\' | mvn -N -q -DforceStdout help:evaluate -P${profile} -f ${pomFile}", returnStdout: true;
                                groupId = pomProperties.split(",")[0];
                                artifactId = pomProperties.split(",")[1];
                                name = pomProperties.split(",")[2];
                                finalName = pomProperties.split(",")[3];
                                version = pomProperties.split(",")[4];
                                buildDirectory = pomProperties.split(",")[5];
                                // Controllo se il file di metadati è stato già prodotto
                                metadataFile = "jenkinsTmp"+File.separator+finalName;
                                metadataFileExists = fileExists metadataFile;
                                if (!metadataFileExists) {
                                    // Controllo se la directory target esiste
                                    targetDirExists = sh(script: "test -d ${buildDirectory} && echo '1' || echo '0' ", returnStdout: true).trim();
                                    if(targetDirExists=='1'){                                            
                                        // Il file di metadati non esiste, procedo alla sua creazione
                                        echo "Creazione '${metadataFile}'..."
                                        jarFile = sh script: "find ${buildDirectory} -maxdepth 1 -name ${finalName}*.jar -printf '%p'", returnStdout: true;
                                        rpmFile = sh script: "find ${buildDirectory} -maxdepth 5 -name ${finalName}*.rpm -printf '%p'", returnStdout: true;
                                        flattenedPomFile = sh script: "find ${artifactId} -maxdepth 1 -name .flattened-pom.xml -printf '%p'", returnStdout: true;
                                        writeFile file: metadataFile, text: groupId+","+artifactId+","+name+','+finalName+","+version+","+jarFile+","+rpmFile+","+flattenedPomFile;
                                    }                                        
                                }
                            }
                                          
                        }
                    }
                }
            }

          stage("Prepare AWS Deploy") {
               when {expression { SKIP_PREPARE_AWS.toBoolean() == false }}
               steps {
                    script {
                         // Preparo file credentials AWS
                         sh "if [ ! -d ~/.aws ]; then mkdir ~/.aws && rm -rf ~/.aws/credentials;fi"
                         sh 'echo "[default]" > ~/.aws/credentials';
                         sh "echo aws_access_key_id = ${AWS_API_KEY} >> ~/.aws/credentials";
                         sh "echo aws_secret_access_key = ${AWS_API_SECRET} >> ~/.aws/credentials";
                         echo "Check Instance Status..."
                         status = sh script: "/usr/local/bin/aws ec2 describe-instance-status --instance-ids  ${AWS_INSTANCE_ID} --output=\"text\" | grep  \"STATE\" | grep -v \"STATEREASON\" | awk '{print \$3}'", returnStdout:true
                         echo "Status: ${status}";
                         if (status.equals("") || status.equals('stopped')) {
                         echo "Lancio istanza AWS ${AWS_INSTANCE_ID}"
                         sh "/usr/local/bin/aws ec2 start-instances --instance-ids  ${AWS_INSTANCE_ID}"
                         echo "Attendo che l'instanza aws sia in stato running..."
                         sh "/usr/local/bin/aws ec2 wait instance-status-ok  --instance-ids ${AWS_INSTANCE_ID}"
                         echo "Istanza AWS avviata"
                         awsPublicIp = sh script: "/usr/local/bin/aws ec2 describe-instances --instance-ids   ${AWS_INSTANCE_ID}  | grep -i \"PublicIpAddress\" | awk -F ':' '{print \$2}' | sed -r 's|\"||g' | sed -r 's| ||g' | sed -r 's|,||g'", returnStdout: true
                         echo "AWS public ip: ${awsPublicIp}"
                         echo "Fermo l'istanza AWS..."
                         sh "/usr/local/bin/aws ec2 stop-instances --instance-ids  ${AWS_INSTANCE_ID}";
                         echo "Attendo che l'instanza aws sia in stato stopped..."
                         sh "/usr/local/bin/aws ec2 wait instance-stopped  --instance-ids ${AWS_INSTANCE_ID}";
                         echo "Istanza AWS fermata correttamente";
                         } else {
                         error("Istanza AWS in stato running. InstanceId: ${AWS_INSTANCE_ID}")
                         }
                    }
               }
          } 
          stage("Deploy package") {
               when {expression { SKIP_DEPLOY.toBoolean() == false }}
               steps {
                    script {
                         commands = readFile(file: "/tmp/playbook").replaceAll('%SSH_HOST%','google.com');
                         for (cmd in commands.split("\n")) {
                         sh "${cmd}"
                         }
                    }
               }
          }           
            stage("Publishing artifacts") {
                when {expression { SKIP_PUBLISH_NEXUS.toBoolean() == false }}
                steps {
                    script {
                        metadataFiles = findFiles(glob: "jenkinsTmp/*");
                        rollbackIds = [];
                        for (mf in metadataFiles) {
                            records = readCSV file: mf.toString()
                            groupId =  records[0][0];
                            artifactId =  records[0][1];
                            name =  records[0][2];
                            finalName =   records[0][3];
                            version =   records[0][4];
                            jarPath =   records[0][5];
                            rpmPath =   records[0][6];
                            pomPath = records[0][7];

                            echo "File: ${mf.name} - groupId: ${groupId}, artifactId: ${artifactId},name: ${name}, finalName: ${finalName}, version: ${version}, jarPath: ${jarPath}, rpmPath: ${rpmPath}"
                            if (version.contains("DEVELOPMENT")) {
                                if (!jarPath.equals("")) {
                                    publishArtifactsToRepository(jarPath, NEXUS_REPOSITORY_MAVEN, name, groupId, version, "jar");

                                }
                                if (!pomPath.equals("")) {
                                    publishArtifactsToRepository(pomPath, NEXUS_REPOSITORY_MAVEN, name, groupId, version, "pom");

                                }
                                if (!rpmPath.equals("")) {
                                    publishArtifactsToRepository(rpmPath, NEXUS_REPOSITORY_RPM, name, groupId, version, "rpm");
                                }
                            } else {
                                // Controlla su repository se esiste già la versione dei files
                                try {
                                    if (!jarPath.equals("")) {
                                    nexusResults = searchOnNexus(groupId,name,version,"jar");  
                                    jsonObj = readJSON text: nexusResults, returnPojo: true
                                        if (jsonObj['items'] != null && jsonObj['items'].size() == 0) {
                                                    publishArtifactsToRepository(jarPath, NEXUS_REPOSITORY_MAVEN, name, groupId, version, "jar");
                                                    obj = readJSON text: searchOnNexus(groupId,name,version,"jar"), returnPojo: true;
                                                    if (obj['items'] != null && obj['items'].size() > 0) {
                                                        for (i in obj['items']) {
                                                            rollbackIds.add(i.id);
                                                        }
                                                    }
                                        } else {
                                            echo "${finalName} già presente nel repository.";
                                        }

                                    }
                                    if (!pomPath.equals("")) {
                                        nexusResults = searchOnNexus(groupId,name,version,"pom");  
                                        jsonObj = readJSON text: nexusResults, returnPojo: true         
                                        if (jsonObj['items'] != null && jsonObj['items'].size() == 0) {
                                            publishArtifactsToRepository(pomPath, NEXUS_REPOSITORY_MAVEN, name, groupId, version, "pom");
                                            obj = readJSON text: searchOnNexus(groupId,name,version,"pom"), returnPojo: true;
                                                if (obj['items'] != null && obj['items'].size() > 0) {
                                                    for (i in obj['items']) {
                                                        rollbackIds.add(i.id);
                                                    }
                                            }
                                        } else {
                                            echo "${finalName} già presente nel repository.";
                                        }
                                    }
                                    if (!rpmPath.equals("")) {
                                    nexusResults = searchOnNexus(groupId,name,version,"rpm");  
                                    jsonObj = readJSON text: nexusResults, returnPojo: true
                                        if (jsonObj['items'] != null && jsonObj['items'].size() == 0) {
                                                publishArtifactsToRepository(rpmPath, NEXUS_REPOSITORY_RPM, name, groupId, version, "rpm");
                                                obj = readJSON text: searchOnNexus(groupId,name,version,"rpm"), returnPojo: true;
                                                if (obj['items'] != null && obj['items'].size() > 0) {
                                                    for (i in obj['items']) {
                                                        rollbackIds.add(i.id);
                                                    }
                                                }
                                        } else {
                                            echo "${finalName} già presente nel repository.";                                        
                                        }
                                    }
                                }  catch (e) {
                                        nexusRollback( rollbackIds);
                                        error(e);
                                }     
                            }
                        }
                    }
                }
            }
            stage("Creating tag in SVN") {
            when {expression { SKIP_TAG_SVN.toBoolean() == false }}
                steps {
                    script {
                         MAIN_ARTIFACTID = sh script: "mvn help:evaluate -q -DforceStdout -Dexpression=project.artifactId ", returnStdout: true;
                         MAIN_VERSION = sh script: "mvn help:evaluate -q -DforceStdout -Dexpression=project.version ", returnStdout: true;       
                        tagExists = sh script: "svn list --no-auth-cache --username=${SVN_USERNAME} --password=${SVN_PASSWORD} ${SVN_PATH}${SVN_TAGS_PATH} | grep ${MAIN_ARTIFACTID}-${MAIN_VERSION} || true", returnStdout: true;
                        if (!tagExists.equals("")) {
                            error("Tag ${MAIN_ARTIFACTID}-${MAIN_VERSION} già esistente sul repositroy SVN");
                        }
                        else if (MAIN_VERSION.contains("RELEASE")) {
                            
                            // Crea tag
                            stage("Creating  '${MAIN_ARTIFACTID}-${MAIN_VERSION}' tag in SVN") {
                                script {
                                    echo "Creating tag ${MAIN_ARTIFACTID}-${MAIN_VERSION} on ${SVN_TAGS_PATH}..."
                                    sh script: "svn copy --no-auth-cache --username=${SVN_USERNAME} --password=${SVN_PASSWORD} ${SVN_PATH}${SVN_CHECKOUT_PATH} ${SVN_PATH}${SVN_TAGS_PATH}/${MAIN_ARTIFACTID}-${MAIN_VERSION} -m \"Tagging release ${MAIN_ARTIFACTID}-${MAIN_VERSION}\""
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    def nexusRollback(rollbackIds) {
        script {
            echo "Starting Nexus Rollback: ${rollbackIds}";
            for (id in rollbackIds) {
                r = sh script: "curl -s -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X DELETE \"http://${NEXUS_URL}/service/rest/v1/assets/${id}\" -H \"accept: application/json\"", returnStdout: true
            }
            echo "Nexus Rollback Finished. Removed ${rollbackIds.size()} items.";
        }
    }
    def searchOnNexus(groupId,name,version,type) {
        script {
            searchResult = sh script: "curl -s -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} -X GET \"${NEXUS_PROTOCOL}://${NEXUS_URL}/service/rest/v1/search/assets?sort=version&direction=desc&repository=${NEXUS_REPOSITORY_MAVEN}&maven.groupId=${groupId}&maven.artifactId=${name}&maven.baseVersion=${version}&maven.extension=${type}\" -H \"accept: application/json\"", returnStdout: true
            echo "Nexus search results: ${searchResult}";
            return searchResult;
        }

    }

    def publishArtifactsToRepository(artifactPath, nexusRepository, artifactId, groupId, version, artifactType) {
        
        nexusResult = nexusArtifactUploader(
                    nexusVersion: NEXUS_VERSION,
                    protocol: NEXUS_PROTOCOL,
                    nexusUrl: NEXUS_URL,
                    groupId: groupId,
                    version: version,
                    repository: nexusRepository,
                    credentialsId: NEXUS_CREDENTIAL_ID,
                    artifacts: [
                        [artifactId: artifactId,
                        file: artifactPath,
                        type: artifactType,
                        classifier: '']
                    ]
            );
        if (!nexusResult){
            error("Error publishing on nexus '${artifactId}'")
        }

    }