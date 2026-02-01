FROM eclipse-temurin:21.0.9_10-jre-ubi10-minimal
RUN mkdir /opt/app
WORKDIR /opt/app/.
COPY ./build/libs/AuthServer-1.1.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-1.1.0.1.jar"]