# syntax=docker/dockerfile:1
# Optional Compose additional_contexts: certs: ./cert (Zscaler CA for local Maven Central)

FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY --from=certs . /tmp/certs/
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && update-ca-certificates \
    && if [ -f /tmp/certs/ZscalerRootCA.pem ]; then \
         cp /tmp/certs/ZscalerRootCA.pem /usr/local/share/ca-certificates/ZscalerRootCA.crt \
         && update-ca-certificates \
         && (keytool -delete -alias zscaler-root-ca \
             -keystore "$JAVA_HOME/lib/security/cacerts" \
             -storepass changeit >/dev/null 2>&1 || true) \
         && printf 'yes\n' | keytool -importcert -trustcacerts \
             -alias zscaler-root-ca \
             -file /usr/local/share/ca-certificates/ZscalerRootCA.crt \
             -keystore "$JAVA_HOME/lib/security/cacerts" \
             -storepass changeit ; \
       fi \
    && rm -rf /var/lib/apt/lists/* /tmp/certs
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=certs . /tmp/certs/
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && update-ca-certificates \
    && if [ -f /tmp/certs/ZscalerRootCA.pem ]; then \
         cp /tmp/certs/ZscalerRootCA.pem /usr/local/share/ca-certificates/ZscalerRootCA.crt \
         && update-ca-certificates \
         && (keytool -delete -alias zscaler-root-ca \
             -keystore "$JAVA_HOME/lib/security/cacerts" \
             -storepass changeit >/dev/null 2>&1 || true) \
         && printf 'yes\n' | keytool -importcert -trustcacerts \
             -alias zscaler-root-ca \
             -file /usr/local/share/ca-certificates/ZscalerRootCA.crt \
             -keystore "$JAVA_HOME/lib/security/cacerts" \
             -storepass changeit ; \
       fi \
    && groupadd -r nhub && useradd -r -g nhub nhub \
    && rm -rf /var/lib/apt/lists/* /tmp/certs
COPY --from=build /workspace/target/notification-hub-*.jar /app/app.jar
USER nhub
EXPOSE 8088
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
