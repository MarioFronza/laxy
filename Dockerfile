FROM gradle:7.0.2-jdk17 AS builder

COPY ./ /home/gradle/project

WORKDIR /home/gradle/project

RUN gradle -Dorg.gradle.daemon=false clean build --stacktrace --info

RUN mkdir -p /home/gradle/project/build/distributions/app/

RUN unzip /home/gradle/project/build/distributions/*.zip -d /home/gradle/project/build/distributions/app/

FROM openjdk:17-jre

COPY --from=builder /home/gradle/project/build/distributions/app/ /opt/app/

WORKDIR /opt/app

RUN rm -rf /var/cache/*

EXPOSE 8080

CMD ["/opt/app/laxy/bin/laxy"]