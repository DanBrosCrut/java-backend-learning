# Java Backend Learning

Учебный репозиторий по программе стажировки: Java, Spring Boot, МойСклад, Agile/Scrum.

## Структура репозитория

```
java-backend-learning/
├── README.md                    ← этот файл
├── REPORT.md                    ← недельный отчёт
├── .gitignore
├── docs/                        ← конспекты и заметки
│   ├── agile-scrum.md           ← Agile, Scrum, роли, артефакты, DoD
│   ├── environment.md           ← версии Java, Maven, настройка SDKMAN
│   └── moysklad.md              ← МойСклад + онлайн-заказ
└── health-service/              ← минимальный Spring Boot сервис
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    └── src/
        └── main/
            ├── java/com/example/health_service/
            │   ├── HealthServiceApplication.java
            │   └── HealthController.java
            └── resources/
                └── application.properties
```

## Требования

- **JDK 21 LTS** (Temurin)
- **Maven 3.9+**
- **Git** (для клонирования)
- **VS Code** (опционально, но рекомендуется)

### Установка окружения (кратко)

Через SDKMAN:
```
sdk install java 21.0.2-tem
sdk install maven
```

Или напрямую с официальных сайтов:
- Java: https://adoptium.net/temurin/releases/?version=21
- Maven: https://maven.apache.org/download.cgi

## Как собрать

```
cd health-service
mvn clean package
```

После успешной сборки в папке `target/` появится `health-service-0.0.1-SNAPSHOT.jar`.

## Как запустить

### Способ 1: через Maven

```
cd health-service
mvn spring-boot:run
```

Приложение запустится на порту **8080**.

### Способ 2: через собранный JAR

```
cd health-service
mvn clean package
java -jar target/health-service-0.0.1-SNAPSHOT.jar
```

## 🔌 API

### GET /health

## Конфигурация

Файл `health-service/src/main/resources/application.properties`:

```
# Порт приложения (по умолчанию 8080)
# server.port=8081

# Уровень логирования
# logging.level.org.apache.coyote.http11=OFF
```

## Технологии

- **Spring Boot** 4.1.1
- **Java** 21 LTS (Temurin)
- **Maven** 3.9.10
- **Spring Web** (MVC, встроенный Tomcat)
- **Apache Tomcat** 11.0.24 (embedded)

## Документация, которую использовал

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Initializr](https://start.spring.io/)
- [Apache Maven Documentation](https://maven.apache.org/guides/)
- [SDKMAN Usage](https://sdkman.io/usage)
- [Apache Tomcat 11](https://tomcat.apache.org/tomcat-11.0-doc/index.html)

## 📝 Отчёт по неделе

Смотри [REPORT.md](REPORT.md).

## 🔗 Полезные ссылки

- Репозиторий: https://github.com/<твой-username>/java-backend-learning
- МойСклад: https://www.moysklad.ru/
- МойСклад API: https://dev.moysklad.ru/
- Scrum Guide: https://scrumguides.org/