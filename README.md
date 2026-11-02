# Java Backend Learning

Учебный репозиторий по программе стажировки: Java, Spring Boot, МойСклад, Agile/Scrum.

## Структура репозитория

```
java-backend-learning/
├── README.md                    ← запуск, env, структура
├── REPORT.md                    ← недельный отчёт
├── AGENTS.md                    ← инструкции для AI-агента
├── .gitignore                   ← секреты, target/, IDE
├── kilo.jsonc                   ← MCP конфиг (в .gitignore)
├── .env.example                 ← шаблон (без токена)
├── docs/
│   ├── product.md               ← видение продукта
│   ├── backlog.md               ← Product Backlog
│   ├── sprint-2.md              ← Sprint Backlog
│   ├── git-workflow.md          ← merge, rebase, cherry-pick
│   ├── semver.md                ← SemVer
│   └── ...
└── moysklad-api/                ← Spring Boot проект
    ├── pom.xml
    ├── .env                     ← в .gitignore
    └── src/main/java/com/example/moysklad/api/
        ├── MoyskladApiApplication.java
        ├── HealthController.java
        ├── client/
        │   └── MoyskladClient.java
        ├── dto/
        │   └── ProductDto.java
        └── controller/
            └── ProductController.java
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
cd moysklad-api
mvn clean package
```

После успешной сборки в папке `target/` появится `moysklad-api-0.0.1-SNAPSHOT.jar`.

## Как запустить

### Способ 1: через Maven

```
cd moysklad-api
mvn spring-boot:run
```

Приложение запустится на порту **8080**.

### Способ 2: через собранный JAR

```
cd moysklad-api
mvn clean package
java -jar target/moysklad-api-0.0.1-SNAPSHOT.jar
```

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

Файл `moysklad-api/src/main/resources/application.properties`:

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

## Отчёт по неделе

Смотри [REPORT.md](REPORT.md).

## Переменные окружения

Создай `.env` в корне `moysklad-api/`:
MOYSKLAD_TOKEN="MOYSKLADTOKEN"
Токен берётся из: МойСклад → Настройки → Пользователи → Токены.


## Полезные ссылки

- Репозиторий: https://github.com/<твой-username>/java-backend-learning
- МойСклад: https://www.moysklad.ru/
- МойСклад API: https://dev.moysklad.ru/
- Scrum Guide: https://scrumguides.org/
