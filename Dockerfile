FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY libs.versions.toml settings.gradle.kts build.gradle.kts gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
COPY buildSrc /home/gradle/app/buildSrc
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle fatShadowJar --no-daemon

FROM eclipse-temurin:21 AS runtime
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/laxy-app-fat.jar /app/laxy-app.jar
ENTRYPOINT ["java","-jar","/app/laxy-app.jar"]