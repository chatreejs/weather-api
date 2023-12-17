FROM gradle:7.6.0-jdk17 AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle .. /home/gradle/src
RUN gradle build -x test

FROM eclipse-temurin:17-jdk
EXPOSE 8080

COPY --from=build /home/gradle/src/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]