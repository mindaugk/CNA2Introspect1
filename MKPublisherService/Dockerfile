FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /build

# copy sources and build a shaded (fat) jar via the maven-shade-plugin
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Recommended: override or provide configuration via env vars or mounted config.properties
EXPOSE 8080

# copy fat jar produced by maven-shade-plugin
COPY --from=build /build/target/mkpublisher-1.0.0.jar /app/mkpublisher.jar

ENTRYPOINT ["java","-jar","/app/mkpublisher.jar"]
CMD ["server","8080"]
