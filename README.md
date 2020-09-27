# Jenkins Configuration as Code

***Build, run and configure a Jenkins cluster without manual setup***

Work in progress.

## Build on local machine

```bash
you@machine:~/jenkins$ docker build -t my-jenkins .
```

## Run on local machine

```bash
you@machine:~/jenkins$ docker run -p 8080:8080 -e "JENKINS_USER=admin" -e "JENKINS_PASS=admin" my-jenkins
```

## Work in progress

- Add Configuration as Code plugin for pipelines
- Add documentation regarding the EC2 builders execution
- Add documentation regarding the AWS CodeBuild process
