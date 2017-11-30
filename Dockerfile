FROM openjdk:8u151-jre-slim
MAINTAINER Olaf Bergner <olaf.bergner@gmx.de>

ARG version
ARG managementApiPort

WORKDIR /app

COPY target/uberjar/postgres-wal-shipper-${version}-standalone.jar /app/postgres-wal-shipper-${version}-standalone.jar
RUN ln -s /app/postgres-wal-shipper-${version}-standalone.jar /app/app.jar

ENV MANAGEMENT_API_PORT ${managementApiPort}

EXPOSE ${managementApiPort}

CMD ["java", "-jar", "/app/app.jar"]
