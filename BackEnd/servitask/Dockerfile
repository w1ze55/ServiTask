# Estágio de build
FROM maven:3.8.6-openjdk-17-slim as build

WORKDIR /app

# Copiar arquivos de configuração do Maven
COPY pom.xml .
COPY .mvn .mvn

# Copiar o código fonte
COPY src src

# Construir o aplicativo
RUN mvn clean package -DskipTests

# Estágio de produção
FROM openjdk:17-slim

WORKDIR /app

# Copiar o JAR construído
COPY --from=build /app/target/*.jar app.jar

# Expor a porta 8080
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
