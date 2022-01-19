FROM openjdk:11

WORKDIR /

# TODO pass the app name and version
ADD ./target/notion-backup-1.0-SNAPSHOT.jar /notion-backup.jar

ENTRYPOINT ["java", "-jar", "notion-backup.jar"]
