ARG SFTWR_VERSION=1.2
FROM eclipse-temurin:21.0.9_10-jre-ubi10-minimal
RUN mkdir /opt/app
WORKDIR /opt/app/.
COPY ./build/libs/AuthServer-${SFTWR_VERSION}.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-${SFTWR_VERSION}.jar"]