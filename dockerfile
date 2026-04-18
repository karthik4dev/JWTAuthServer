ARG version
FROM eclipse-temurin:21.0.9_10-jre-ubi10-minimal
RUN mkdir /opt/app
WORKDIR /opt/app/.
COPY ./build/libs/AuthServer-${version}.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-${version}.jar"]