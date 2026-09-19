FROM eclipse-temurin:latest
RUN mkdir /opt/app
WORKDIR /opt/app/.
ARG JAR_VERSION=1.2
COPY ./build/libs/AuthServer-2.3.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-2.3.jar"]