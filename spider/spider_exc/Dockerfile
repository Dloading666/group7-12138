ARG PLAYWRIGHT_BASE_IMAGE=mcr.microsoft.com/playwright/java:v1.40.0-jammy

FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

RUN printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' \
    '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
    '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>aliyunmaven</id>' \
    '      <mirrorOf>*</mirrorOf>' \
    '      <url>https://maven.aliyun.com/repository/public</url>' \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' \
    > /tmp/settings.xml

COPY pom.xml ./
RUN mvn -q -B -s /tmp/settings.xml -DskipTests -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline

COPY src ./src
RUN mvn -q -B -s /tmp/settings.xml -DskipTests -Dmaven.wagon.http.retryHandler.count=5 package

FROM ${PLAYWRIGHT_BASE_IMAGE}

WORKDIR /app

COPY --from=builder /app/target/spider-exc-1.0.0.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
