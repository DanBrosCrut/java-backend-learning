# Java Backend Learning

Учебный репозиторий по программе стажировки: Java, Spring Boot, МойСклад, Agile/Scrum.

## Структура репозитория

```
java-backend-learning/
├── README.md                    ← запуск, env, API
├── REPORT.md                    ← отчёт по проекту (спринты 1–5)
├── AGENTS.md                    ← инструкции для AI-агента
├── docs/                        ← документация проекта
│   ├── vision.md                ← видение продукта
│   ├── backlog.md               ← Product Backlog
│   ├── product.md               ← описание продукта
│   ├── agile-scrum.md           ← Agile/Scrum
│   ├── git-workflow.md          ← Git workflow
│   ├── environment.md           ← окружение
│   ├── moysklad.md              ← МойСклад API
│   └── semver.md                ← версионирование
└── moysklad-api/                ← Spring Boot приложение
    ├── pom.xml
    ├── mvnw / mvnw.cmd          ← Maven Wrapper
    └── src/
        ├── main/java/com/example/moysklad/api/
        │   ├── MoyskladApiApplication.java
        │   ├── client/          ← MoyskladClient, Remap-DTO
        │   ├── config/          ← свойства, CorrelationIdFilter
        │   ├── controller/      ← REST-контроллеры, GlobalExceptionHandler
        │   ├── dto/             ← внутренние DTO (records)
        │   ├── exception/       ← доменные исключения
        │   ├── mapper/          ← Remap → внутренние DTO
        │   └── service/         ← бизнес-логика
        ├── main/resources/      ← application.yml, -dev.yml, -prod.yml
        └── test/java/…/service/ ← JUnit 5 + Mockito
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

```bash
cd moysklad-api
./mvnw clean package        # Windows: .\mvnw.cmd clean package
```

После успешной сборки в папке `target/` появится `moysklad-api-0.0.1-SNAPSHOT.jar`.

`clean package` заодно прогоняет тесты. Только тесты:

```bash
./mvnw clean test           # Windows: .\mvnw.cmd clean test
```

## Как запустить

Приложению нужен токен МойСклад — см. раздел «Переменные окружения».

### Способ 1: через Maven (рекомендуется)

```bash
cd moysklad-api
./mvnw spring-boot:run      # Windows: .\mvnw.cmd spring-boot:run
```

С профилем разработки (подробные логи, включён Swagger UI):

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# PowerShell:
$env:SPRING_PROFILES_ACTIVE="dev"; .\mvnw.cmd spring-boot:run
```

Приложение запустится на порту **8080**. Проверка: `curl http://localhost:8080/health` → `ok`.

### Способ 2: через собранный JAR

```bash
cd moysklad-api
./mvnw clean package
java -jar target/moysklad-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Профили

| Профиль | Назначение | Особенности |
|---|---|---|
| без профиля | базовый запуск | уровень логов по умолчанию, Swagger включён |
| `dev` | локальная разработка | `DEBUG` для приложения и Spring Web |
| `prod` | production | только `WARN`/`ERROR`, Swagger и OpenAPI отключены |

### Swagger / Actuator

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`

## 🔌 API

### GET /health

Проверка живости приложения.

```bash
curl "http://localhost:8080/health"
```

### GET /api/products

Каталог товаров из МойСклад с остатками, отфильтрованный и отсортированный под витрину.
Ответ — страница `{items, total, page, size}`, где `total` — число товаров после фильтрации (до пагинации).

Query-параметры (все необязательные):

| Параметр | Значение по умолчанию | Описание |
|---|---|---|
| `priceType` | первая цена продажи | Тип цены, например `Цена продажи` |
| `groups` | все группы | Список групп через запятую, например `Товары,Услуги` |
| `q` | — | Поиск без учёта регистра по названию и артикулу (до 100 символов) |
| `fields` | все поля | Какие поля вернуть: `name,article,group,price,stock` |
| `inStockOnly` | `false` | Скрыть товары с нулевым остатком |
| `sortBy` | порядок МойСклад | Поле сортировки: `name`, `article`, `group`, `price`, `stock` |
| `order` | `asc` | Направление сортировки: `asc` или `desc` |
| `page` | `0` | Номер страницы (с нуля) |
| `size` | `20` | Размер страницы, от 1 до 100 |

Примеры:

```bash
# вся первая страница
curl "http://localhost:8080/api/products"

# сортировка по цене: сначала самые дорогие
curl "http://localhost:8080/api/products?sortBy=price&order=desc"

# сортировка по названию + только нужные поля
curl "http://localhost:8080/api/products?sortBy=name&order=asc&fields=name,price"

# поиск по названию/артикулу вместе с фильтрами и пагинацией
curl "http://localhost:8080/api/products?q=машин&groups=Товары&inStockOnly=true&sortBy=stock&order=asc&page=0&size=10"

# страница за пределами данных: пустой items, но реальный total
curl "http://localhost:8080/api/products?page=99&size=20"
```

Особенности:

- `sortBy` применяется к значениям, которые попадают в ответ: если поле исключено через `fields`, сортировка по нему не меняет порядок.
- Сортировка выполняется до пагинации, поэтому `total` и содержимое страниц всегда согласованы.
- Некорректные `priceType`, `fields`, `q`, `sortBy`, `order` → `400 INVALID_CATALOG_OPTION`, некорректные `page`/`size` → `400 INVALID_PAGINATION`.

### GET /api/products/config

Возвращает доступные для аккаунта типы цен и группы товаров, список поддерживаемых полей и значения по умолчанию.

```bash
curl "http://localhost:8080/api/products/config"
```

### POST /api/availability

Проверяет, покрывает ли текущий остаток запрошенные позиции.

```bash
curl -X POST "http://localhost:8080/api/availability" \
  -H "Content-Type: application/json" \
  -d '{"positions":[{"id":"<uuid-товара>","quantity":1}]}'
```

Ответ: `{"available":true,"missing":[]}`; если остатка не хватает — `available: false` и список `missing` с полями `id`, `requested`, `available`.

### Формат ошибок

Все ошибки возвращаются в едином виде:

```json
{
  "code": "INVALID_CATALOG_OPTION",
  "message": "Unknown sort field 'unknown'. Allowed fields: name,article,group,price,stock"
}
```

Коды: `INVALID_CATALOG_OPTION`, `INVALID_PAGINATION` (400), `MOYSKLAD_ERROR` (502), `INTERNAL_ERROR` (500).

## Конфигурация

Основной файл — `moysklad-api/src/main/resources/application.yml`:

```yaml
spring:
  config:
    import: "optional:file:../.env[.properties]"

moysklad:
  token: ${MOYSKLAD_TOKEN}
  base-url: https://api.moysklad.ru/api/remap/1.2
```

Профили вынесены в отдельные файлы:

- `application-dev.yml` — `DEBUG`-логи приложения и Spring Web (активация: `SPRING_PROFILES_ACTIVE=dev`);
- `application-prod.yml` — только `WARN`/`ERROR`, Swagger и OpenAPI отключены.

Порт можно переопределить: `--server.port=8081` или `server.port` в конфиге.

## Технологии

- **Spring Boot** 4.x (Web MVC, Validation, Actuator)
- **Java** 21 LTS (Temurin) — records, switch-выражения, text blocks, streams
- **Maven** 3.9.x + Maven Wrapper
- **Apache Tomcat** (embedded)
- **springdoc-openapi** — Swagger UI
- **JUnit 5 + Mockito + AssertJ** — тесты сервисного слоя
- **МойСклад Remap 1.2** — источник данных (без собственной БД)

## Документация, которую использовал

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Initializr](https://start.spring.io/)
- [Apache Maven Documentation](https://maven.apache.org/guides/)
- [SDKMAN Usage](https://sdkman.io/usage)
- [Apache Tomcat 11](https://tomcat.apache.org/tomcat-11.0-doc/index.html)

## Отчёт по проекту

Кратко по спринтам и текущий статус — в [REPORT.md](REPORT.md).

## Переменные окружения

Токен МойСклад хранится **только** в `.env` в корне репозитория (рядом с `README.md`); файл добавлен в `.gitignore`.

```
MOYSKLAD_TOKEN=ваш_токен_без_кавычек
```

Почему в корне: `application.yml` подключает `optional:file:../.env[.properties]`, а приложение запускается из каталога `moysklad-api`, поэтому `../.env` — это корень репозитория.

- Токен без кавычек и пробелов: `.properties`-парсер кавычки не снимает.
- Токен берётся из МойСклад → Настройки → Пользователи → Токены.
- Альтернатива на одну сессию: переменная окружения `MOYSKLAD_TOKEN`.
- Токен нельзя коммитить, логировать и вставлять в код. Проверка: `git check-ignore -v .env`.


## Полезные ссылки

- Репозиторий: https://github.com/<твой-username>/java-backend-learning
- МойСклад: https://www.moysklad.ru/
- МойСклад API: https://dev.moysklad.ru/
- Scrum Guide: https://scrumguides.org/
