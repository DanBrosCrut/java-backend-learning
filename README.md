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

Базовый URL: `http://localhost:8080`.

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
| `invalidate` | `false` | Сбросить кеш ассортимента и остатков и загрузить полный отчёт заново |
| `page` | `0` | Номер страницы (с нуля) |
| `size` | `20` | Размер страницы, от 1 до 100 |

Принудительно обновить кеши можно и заголовком `X-Force-Refresh: true` — он действует так же, как `invalidate=true`.

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

#### Кеширование каталога и остатков

**Каталог** читается из МойСклад **целиком**: Remap отдаёт не больше 1000 строк за запрос, поэтому клиент листает ассортимент страницами `limit` + `offset` до последней страницы. Размер страницы задаётся свойством `moysklad.assortment-page-size` (по умолчанию `1000`). Готовый снапшот хранится в памяти и переиспользуется до инвалидации.

**Остатки** тоже хранятся в памяти, но обновляются по трём порогам:

| Время с последнего обновления | Режим | Что происходит |
|---|---|---|
| Меньше `moysklad.stock-cache.ttl` (по умолчанию 5 минут) | HIT | Отдаётся кеш, запросов в МойСклад нет |
| 5 минут и больше, последняя **полная** синхронизация была меньше 24 часов назад | Delta | `GET /report/stock/all/current?stockType=stock&changedSince=<время последнего обновления>&include=zeroLines`; изменения мёржатся в кеш |
| 24 часа и больше с последней полной синхронизации | Full | Полный отчёт без `changedSince`; кеш **заменяется** целиком |

- `changedSince` передаётся в формате `yyyy-MM-dd HH:mm:ss` в таймзоне МойСклад (`Europe/Moscow`).
- `include=zeroLines` нужен, чтобы товар, остаток которого упал до нуля, вернулся строкой `stock: 0` и обновил кеш, а не исчез из дельты.
- Пороги настраиваются свойствами `moysklad.stock-cache.ttl` (по умолчанию `PT5M`) и `moysklad.stock-cache.full-sync-interval` (по умолчанию `PT24H`).
- **Принудительный полный refresh:** `?invalidate=true` или заголовок `X-Force-Refresh: true`. Они сбрасывают кеш ассортимента и остатков и загружают полный отчёт независимо от порогов.

Заголовок ответа **`X-Cache`** показывает, обращался ли запрос в МойСклад:

- `HIT` — оба снапшота (каталог и остатки) взяты из кеша, внешних вызовов не было;
- `MISS` — хотя бы один снапшот обновлялся: загрузка каталога, полный отчёт или delta.

В логах видно, что произошло с кешем:

- `Assortment cache HIT/MISS: N row(s)`;
- `Current stock cache HIT: N product(s)`;
- `Current stock cache MISS (full): N product(s)`;
- `Current stock cache MISS (delta since <instant>): N changed product(s), M product(s) in total`;
- сброс кеша пишется отдельной строкой.

Остатки берутся из **`/report/stock/all/current`** (`stockType=stock`) — это фактический остаток на складе, а не накопительный отчёт.

```bash
# принудительно обновить оба кеша и посмотреть заголовок
curl -i "http://localhost:8080/api/products?invalidate=true&size=1"

# то же самое через заголовок
curl -i "http://localhost:8080/api/products?size=1" -H "X-Force-Refresh: true"
```

### GET /api/products/config

Возвращает доступные для аккаунта типы цен и группы товаров, список поддерживаемых полей и значения по умолчанию.
Использует тот же кеш ассортимента и тоже возвращает заголовок `X-Cache`; `?invalidate=true` сбрасывает кеш.

```bash
curl "http://localhost:8080/api/products/config"
```

### POST /api/availability

Проверяет, покрывает ли текущий остаток запрошенные позиции.
Использует тот же кеш остатков, что и каталог: в пределах 5 минут не обращается в МойСклад, затем подтягивает дельту. Принудительно обновить общий кеш можно запросом `GET /api/products?invalidate=true` или заголовком `X-Force-Refresh: true`.

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
  # размер страницы при чтении ассортимента (Remap: максимум 1000)
  assortment-page-size: 1000
  stock-cache:
    # снапшот остатков моложе этого времени отдаётся без запроса в МойСклад
    ttl: PT5M
    # после этого времени с последней полной синхронизации загружается полный отчёт
    full-sync-interval: PT24H
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
