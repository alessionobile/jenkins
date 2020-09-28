#!groovy

import com.amazonaws.services.ec2.model.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsScope
import hudson.model.*
import hudson.plugins.ec2.*
import jenkins.model.*

println "init-ec2.groovy: Starting..."

// parameters
def SlaveTemplateParameters = [
  ami:                      env.JENKINS_BUILDER_AMI_ID,
  associatePublicIp:        false,
  connectBySSHProcess:      true,
  connectUsingPublicIp:     false,
  customDeviceMapping:      '',
  deleteRootOnTermination:  true,
  description:              'Jenkins Builder',
  ebsOptimized:             false,
  iamInstanceProfile:       env.JENKINS_BUILDER_INSTANCE_PROFILE_ARN ?: '',
  idleTerminationMinutes:   env.JENKINS_BUILDER_IDLE_TERMINATION_MINUTES ?: '30',
  minimumNumberOfInstances: '0'
  minimumNumberOfSpareInstances: '0'
  initScript:               env.JENKINS_BUILDER_INIT_SCRIPT ?: '',
  instanceCapStr:           env.JENKINS_BUILDER_INSTANCE_CAP,
  jvmopts:                  '',
  labelString:              'aws.ec2.jenkins.builder',
  launchTimeoutStr:         env.JENKINS_BUILDER_LAUNCH_TIMEOUT_SECONDS ?: '300',
  numExecutors:             env.JENKINS_BUILDER_NUM_EXECUTORS ?: '4',
  remoteAdmin:              env.JENKINS_BUILDER_REMOTE_ADMIN ?: 'root',
  remoteFS:                 '',
  securityGroups:           env.JENKINS_BUILDER_SECURITY_GROUPS,
  stopOnTerminate:          false,
  subnetId:                 env.JENKINS_BUILDER_SUBNET_ID,
  tags:                     new EC2Tag('Name', cloud_name),
  tmpDir:                   '',
  type:                     env.JENKINS_BUILDER_INSTANCE_TYPE ?: 't2.small',
  useDedicatedTenancy:      false,
  useEphemeralDevices:      true,
  monitoring:               true,
  t2Unlimited:              false,
  maxTotalUses:             '0'
  userData:                 env.JENKINS_BUILDER_USER_DATA ?: '',
  zone:                     env.JENKINS_BUILDER_REGION + 'a'
]

def AmazonEC2CloudParameters = [
  cloudName:      env.JENKINS_BUILDER_CLOUD_NAME,
  credentialsId:         '',
  sshKeysCredentialsId:  'jenkins-builder-key',
  instanceCapStr: env.JENKINS_BUILDER_INSTANCE_CAP,
  privateKey:     env.JENKINS_BUILDER_PRIVATE_KEY ? new String(env.JENKINS_BUILDER_PRIVATE_KEY.decodeBase64()) : '',
  region:         env.JENKINS_BUILDER_REGION,
  useInstanceProfileForCredentials: false
]
// https://github.com/jenkinsci/ec2-plugin/blob/master/src/main/java/hudson/plugins/ec2/SlaveTemplate.java#L336
SlaveTemplate slaveTemplate = new SlaveTemplate(
  SlaveTemplateParameters.ami,
  SlaveTemplateParameters.zone,
  null,
  SlaveTemplateParameters.securityGroups,
  SlaveTemplateParameters.remoteFS,
  InstanceType.fromValue(SlaveTemplateParameters.type),
  SlaveTemplateParameters.ebsOptimized,
  SlaveTemplateParameters.labelString,
  Node.Mode.NORMAL,
  SlaveTemplateParameters.description,
  SlaveTemplateParameters.initScript,
  SlaveTemplateParameters.tmpDir,
  SlaveTemplateParameters.userData,
  SlaveTemplateParameters.numExecutors,
  SlaveTemplateParameters.remoteAdmin,
  new UnixData(null, null, null, null),
  SlaveTemplateParameters.jvmopts,
  SlaveTemplateParameters.stopOnTerminate,
  SlaveTemplateParameters.subnetId,
  [SlaveTemplateParameters.tags],
  SlaveTemplateParameters.idleTerminationMinutes,
  SlaveTemplateParameters.minimumNumberOfInstances,
  SlaveTemplateParameters.minimumNumberOfSpareInstances
  SlaveTemplateParameters.instanceCapStr,
  SlaveTemplateParameters.iamInstanceProfile,
  SlaveTemplateParameters.deleteRootOnTermination,
  SlaveTemplateParameters.useEphemeralDevices,
  SlaveTemplateParameters.useDedicatedTenancy,
  SlaveTemplateParameters.launchTimeoutStr,
  SlaveTemplateParameters.associatePublicIp,
  SlaveTemplateParameters.customDeviceMapping,
  SlaveTemplateParameters.connectBySSHProcess,
  SlaveTemplateParameters.monitoring,
  SlaveTemplateParameters.t2Unlimited,
  null,
  SlaveTemplateParameters.maxTotalUses,
  SlaveTemplateParameters.connectUsingPublicIp,
  null,
  null
)

AmazonEC2Cloud amazonEC2Cloud = new AmazonEC2Cloud(
  AmazonEC2CloudParameters.cloudName,
  AmazonEC2CloudParameters.useInstanceProfileForCredentials,
  AmazonEC2CloudParameters.credentialsId,
  AmazonEC2CloudParameters.region,
  AmazonEC2CloudParameters.privateKey,
  AmazonEC2CloudParameters.sshKeysCredentialsId,
  AmazonEC2CloudParameters.instanceCapStr,
  [slaveTemplate],
  '',
  ''
)

// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()

// get credentials domain
def domain = Domain.global()

// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

privateKey = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(AmazonEC2CloudParameters.privateKey)
credentials = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  AmazonEC2CloudParameters.credentialsId,
  SlaveTemplateParameters.remoteAdmin,
  privateKey,
  "",
  "Jenkins Builder Key"
)

// add credential to store
store.addCredentials(domain, credentials)

// add cloud configuration to Jenkins
jenkins.clouds.add(amazonEC2Cloud)

// save current Jenkins state to disk
jenkins.save()
println "init-ec2.groovy: Configured EC2 cloud"
