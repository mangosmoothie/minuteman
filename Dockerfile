FROM openjdk:8-alpine

COPY target/uberjar/minuteman.jar /minuteman/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/minuteman/app.jar"]
