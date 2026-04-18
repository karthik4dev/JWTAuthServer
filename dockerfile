FROM eclipse-temurin:21.0.9_10-jre-ubi10-minimal
RUN mkdir /opt/app
WORKDIR /opt/app/.
ARG JAR_VERSION=1.2
COPY ./build/libs/AuthServer-${JAR_VERSION}.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-${JAR_VERSION}.jar"]