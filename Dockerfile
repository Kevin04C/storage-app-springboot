# Etapa 1: Construcción (Build)
FROM eclipse-temurin:21-jdk-alpine AS builder

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el wrapper de maven y los archivos de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descargar las dependencias (ideal para cachear de Docker)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src/ src/

# Construir la aplicación (omitiendo tests para acelerar el build para esta imagen)
RUN ./mvnw clean package -DskipTests

# Etapa 2: Producción (Ejecución)
FROM eclipse-temurin:21-jre-alpine

# Establecer el directorio de trabajo
WORKDIR /app

# Mejores prácticas de seguridad: no correr como root
RUN addgroup -S springg && adduser -S springu -G springg
USER springu:springg

# Copiar el .jar compilado desde la etapa "builder"
# Spring Boot Maven Plugin genera normalmente un único archivo .jar
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto por defecto de Spring Boot (WebFlux)
EXPOSE 8080

# Variable de entorno para configuraciones extra de la JVM
ENV JAVA_OPTS=""

# Comando para iniciar la aplicación (permite perfil activo por variable de entorno y pasar el JAVA_OPTS)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
