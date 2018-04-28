pause
start https://oss.sonatype.org/#stagingRepositories
mvn clean deploy -P sonatype-oss-release 
pause