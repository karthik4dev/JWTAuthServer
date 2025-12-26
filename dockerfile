FROM eclipse-temurin:21
RUN mkdir /opt/app
WORKDIR /opt/app/.
COPY ./build/libs/AuthServer-1.1.jar .
EXPOSE 9000
CMD ["java", "-jar", "/opt/app/AuthServer-1.1.jar"]