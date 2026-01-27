FROM opencv-tesseract-base:4.9

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod

COPY game-hub-server.jar app.jar

EXPOSE 80

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+UseG1GC", "-jar","/app/app.jar"]
