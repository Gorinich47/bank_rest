# эта сбоки файла будет использоваться для сборки образа
# FROM maven:3.9.13-eclipse-temurin-25-noble используем официальный образ maven с Java 25 с Ubuntu 24.04 LTS
# AS build устанавливаем имя нашего этапа, это имя будет использоваться для дальнейшего копирования файлов
FROM maven:3.9.13-eclipse-temurin-25-noble AS build

# Создаем директорию /app внутри контейнера для нашего проекта
WORKDIR /app

# Рекомундуется сначало копировать файлы pom.xml, а потом src в контейнер перед сборкой
COPY pom.xml .
# Загружаем зависимости (для эффективного кэширования)
# RUN mvn dependency:go-offline
# Копируем src
COPY src ./src

# Запускаем команду сборки Maven
# clean - удаляем предудущие сборки, если они есть
# package - упаковываем проект в jar файл
# -DskipTests - пропускаем тесты
RUN mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Копируем собранный jar файл из этапа build в контейнер
COPY --from=build /app/target/Bankcards-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт 8088
EXPOSE 8088

# Запускаем наш jar файл
# Для запуска команды java -jar app.jar использоваться:
ENTRYPOINT ["java", "-jar", "app.jar"]

#  DuplicateStageName: Duplicate stage name "build", stage names should be unique (line 22)

# Упаковка
# docker build -t my-serv-app-calc .

# Запуск на внтреннем порту 8080 и внешнем порту 8085
# docker run -p 8085:8080 my-serv-app-calc

