FROM eclipse-temurin:21
RUN mkdir /opt/app
WORKDIR /opt/app/.
COPY ./build/libs/AuthServer-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "/opt/app/AuthServer-0.0.1-SNAPSHOT.jar"]