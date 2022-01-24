FROM openjdk:11

WORKDIR /

RUN mkdir /downloads
RUN chmod 755 /downloads

ADD ./target/notion-backup-1.0-SNAPSHOT.jar /notion-backup.jar

ENTRYPOINT ["java", "-jar", "notion-backup.jar"]


### Build/Run

# Build for a different platform:
#   mvn clean install && docker build --platform linux/amd64 -t jckleiner/notion-backup .
#   mvn clean install && docker build --platform linux/x86_64 -t jckleiner/notion-backup .

# Push to DockerHub
#   docker login
#   docker push jckleiner/notion-backup

# Run Locally
# docker run --rm=true --env-file=.env jckleiner/notion-backup
