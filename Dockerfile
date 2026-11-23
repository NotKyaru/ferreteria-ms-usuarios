# Etapa 1: compila el .jar con Maven (JDK completo)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
# chmod por si el repo se clonó sin el permiso de ejecución de mvnw
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# Etapa 2: solo el .jar sobre un JRE liviano
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
ENTRYPOINT ["java", "-jar", "app.jar"]
