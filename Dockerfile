# Build stage
FROM clojure:temurin-21-tools-deps as build-stage
ENV APP_DIR /opt/bvpbot
WORKDIR $APP_DIR
COPY . $APP_DIR
RUN clojure -Srepro -J-Dclojure.main.report=stderr -T:build uber

# Run stage
FROM eclipse-temurin:21-alpine
ENV APP_DIR /opt/bvpbot
ENV APP_FILE bvpbot-standalone.jar
WORKDIR $APP_DIR
COPY --from=build-stage $APP_DIR/target/$APP_FILE $APP_DIR
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -XX:NativeMemoryTracking=summary -XX:+UseContainerSupport -Dclojure.server.repl='{:port 5555 :accept clojure.core.server/repl}' -jar $APP_FILE"]
