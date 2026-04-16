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

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget tzdata

COPY --from=builder /app/target/management-system-1.0.0.jar /app/app.jar

RUN addgroup -S app && adduser -S -G app app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
