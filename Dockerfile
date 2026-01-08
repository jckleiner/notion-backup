FROM maven:3.9.10-eclipse-temurin-11

# Automatically links the repository with the container image deployed on GitHub Container Registry
LABEL org.opencontainers.image.source="https://github.com/jckleiner/notion-backup"

WORKDIR /build

# Build app
COPY . /build
RUN mvn clean install

RUN mkdir /downloads
RUN chmod 755 /downloads

WORKDIR /app
RUN cp /build/target/notion-backup-1.0-SNAPSHOT.jar notion-backup.jar

ENTRYPOINT ["java", "-jar", "notion-backup.jar"]


### Build/Run

# Build for a specific platform:
#   docker build --platform linux/amd64 --build-arg -t jckleiner/notion-backup .
#   docker build --platform linux/x86_64 --build-arg -t jckleiner/notion-backup .

# Push to DockerHub:
#   docker login
#   docker push jckleiner/notion-backup

# Run Locally:
#   docker run --rm=true --env-file=.env jckleiner/notion-backup
