FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p out && \
    find src/main/java -name "*.java" -print0 | xargs -0 javac -d out && \
    cp -r src/main/resources/web out/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY data ./data
EXPOSE 7070
CMD ["java", "-cp", "out", "ar.edu.uade.logistica.Main"]
