FROM openjdk:11
ARG PATH_TO_JAR

# Automatically links the repository with the container image deployed on GitHub Container Registry
LABEL org.opencontainers.image.source="https://github.com/jckleiner/notion-backup"

WORKDIR /

RUN mkdir /downloads
RUN chmod 755 /downloads

ADD ${PATH_TO_JAR} /notion-backup.jar

ENTRYPOINT ["java", "-jar", "notion-backup.jar"]


### Build/Run

# Build for a specific platform:
#   mvn clean install && docker build --platform linux/amd64 --build-arg PATH_TO_JAR=./target/notion-backup-1.0-SNAPSHOT.jar -t jckleiner/notion-backup .
#   mvn clean install && docker build --platform linux/x86_64 --build-arg PATH_TO_JAR=./target/notion-backup-1.0-SNAPSHOT.jar -t jckleiner/notion-backup .

# Push to DockerHub:
#   docker login
#   docker push jckleiner/notion-backup

# Run Locally:
#   docker run --rm=true --env-file=.env jckleiner/notion-backup
