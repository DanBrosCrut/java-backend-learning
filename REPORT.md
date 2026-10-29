# Отчёт по проекту: Java Backend Learning

**Репозиторий:** https://github.com/DanBrosCrut/java-backend-learning

Учебный backend-сервис на Spring Boot, который читает ассортимент и остатки из МойСклад (Remap 1.2)
и отдаёт витрину каталога через REST API. Собственной базы данных нет — источником истины остаётся МойСклад.

**Стек:** Java 21 LTS (Temurin), Spring Boot 4.1.1 (Web MVC, Validation, Actuator), Maven 3.9.x,
springdoc-openapi 3.1.1, JUnit 5 + Mockito + AssertJ.

---

## Спринт 1 — Окружение, Agile/Scrum, знакомство с МойСклад API

**Цель:** подготовить воспроизводимое рабочее окружение и разобраться в предметной области.

**Ключевые результаты**

- **Окружение:** SDKMAN в Git Bash — JDK 21.0.2-tem и Maven 3.9.10; настройка `JAVA_HOME`/`PATH`,
  VS Code с Extension Pack for Java. Окружение описано в `docs/environment.md`.
- **Git/GitHub:** создан репозиторий, настроен `.gitignore` (секреты, `target/`, IDE),
  введён workflow «одна задача = одна ветка = один PR».
- **Agile/Scrum:** разобраны Waterfall и Agile, роли, Daily Scrum (15 минут), Retrospective,
  Definition of Done, ведение Product/Sprint Backlog (`docs/agile-scrum.md`).
- **МойСклад API:** изучен Remap 1.2 (JSON, структура сущностей, справочники, остатки, типы цен),
  правила формирования URL и авторизации по токену (`docs/moysklad.md`).
- **Инструменты ИИ:** освоены IRM, MCP и агентные среды (Kilo Code, Cline), принцип human-in-the-loop.

**Итог:** окружение собирается с нуля по документации, домен и контракт внешнего API понятны.

---

## Спринт 2 — Первый Spring Boot сервис и интеграция с МойСклад

**Цель:** поднять работающий REST-сервис и получить данные из реального аккаунта МойСклад.

**Ключевые результаты**

- **Проект:** Spring Initializr → Spring Boot 4.x, embedded Tomcat, порт 8080;
  проект переименован в `moysklad-api` с пакетом `com.example.moysklad.api`.
- **Health-эндпоинт:** `GET /health` → `ok` — минимальный контроль работоспособности.
- **Интеграция:** `MoyskladClient` на `RestClient` с Bearer-токеном; запрос `GET /entity/assortment`
  к Remap 1.2, базовый URL вынесен в конфигурацию.
- **Модель данных:** Remap-DTO в виде records (`RemapAssortmentResponse`, `RemapAssortmentRow`,
  `RemapSalePrice`, `RemapPriceType`) и маппинг в публичный `ProductView`;
  цена переводится из минорных единиц в `BigDecimal` с масштабом 2.
- **Секреты:** токен только в `.env` (в `.gitignore`) или в переменной окружения `MOYSKLAD_TOKEN`;
  запрет на логирование и коммит токена закреплён в `AGENTS.md`.
- **MCP → GitHub:** чтение Issues и создание PR через MCP; при ошибке авторизации MCP
  используется REST-фолбэк через git credential helper.
- **Тесты:** JUnit 5 + Mockito на сервисный слой, внешний API замокан.

**Итог:** `GET /api/products` возвращает каталог из живого аккаунта МойСклад.

---

## Спринт 3 — Остатки, проверка доступности, наблюдаемость

**Цель:** обогатить каталог остатками, добавить сценарий проверки наличия и эксплуатационные инструменты.

**Ключевые результаты**

- **Остатки:** запрос отчёта `GET /report/stock/all`, разбор в `StockReportMapper.toStockMap`,
  слияние с ассортиментом по идентификатору товара; отсутствие строки в отчёте трактуется как остаток 0.
- **Проверка доступности:** `POST /api/availability` и `AvailabilityService` — проверка списка позиций,
  ответ `{available, missing[]}` с расшифровкой нехватки (`requested` / `available`).
- **Документация API:** springdoc-openapi, `@Tag`/`@Operation`/`@Schema`, Swagger UI на `/swagger-ui/index.html`.
- **Наблюдаемость:** Spring Boot Actuator (`/actuator/health`, `/actuator/info`);
  `CorrelationIdFilter` с MDC — идентификатор запроса печатается в каждой строке лога.
- **Обработка ошибок:** `@RestControllerAdvice` + единый `ErrorResponse{code, message}`;
  коды `INVALID_CATALOG_OPTION`, `MOYSKLAD_ERROR`, `MOYSKLAD_UNAUTHORIZED`, `MOYSKLAD_FORBIDDEN`,
  статусы 400/500/502, без утечки стектрейсов клиенту.
- **Тесты:** happy path и граничные случаи (пустой список, отсутствующий товар, недостаток остатка).

**Итог:** сервис покрывает сценарий «витрина + проверка наличия» и пригоден для эксплуатации.

---

## Спринт 4 — Рефакторинг и качество кода

**Цель:** привести кодовую базу к единому стилю и подготовить её к расширению каталога.

**Ключевые результаты**

- **Конфигурация:** переход с `application.properties` на YAML и профили `dev` / `prod`
  (уровни логирования, отключение Swagger в `prod`).
- **Слоистость:** `HealthController` перенесён в пакет `controller`; закреплены пакеты
  `client` / `config` / `controller` / `dto` / `exception` / `mapper` / `service`.
- **Java 21:** records для DTO, switch-выражения, text blocks; streams вместо циклов,
  `Optional` вместо ручных проверок на `null`, method references в лямбдах.
- **Деньги:** только `BigDecimal`, создание из строки, сравнение через `compareTo`,
  явный `RoundingMode` — без `double`/`float`.
- **Именование DTO:** speaking names — `...View` для вывода, `...Request`/`...Response` для входа/выхода,
  `...RemapDto` для внешнего API.
- **Тесты:** JUnit 5 + Mockito, описательные имена методов, покрытие happy path и edge cases.
- **Технический долг (зафиксирован и устранён в Спринте 5):** после перевода исходников из UTF-16
  в UTF-8 остался рассинхрон имён файлов и record-типов (`AvailabilityCheckRequest`,
  `AvailabilityCheckResponse`, `ProductView`), из-за чего `main` не компилировался.
  Исправление включено в PR #80 — первый PR стека Sprint 5.

**Итог:** предсказуемая структура пакетов, единый стиль, конфигурация по профилям, зелёные тесты.

---

## Спринт 5 — Продвинутый каталог витрины

**Цель:** превратить простой список товаров в полноценную витрину: настройка отображения,
поиск, сортировка и постраничная выдача.

**Ключевые результаты**

- **Vision и Backlog (#75, PR #79 — слит):** `docs/vision.md` с полным сквозным сценарием
  (каталог → наличие → создание заказа → чтение заказа), переписанный `docs/backlog.md`
  с приоритетами: Sprint 5 — витрина, Sprint 6 — заказы, далее MCP/эксплуатация.
- **Опции и конфигурация (#76, PR #80):** параметры `priceType`, `groups`, `fields`, `inStockOnly`;
  эндпоинт `GET /api/products/config` отдаёт доступные типы цен, группы, поддерживаемые поля и значения
  по умолчанию. Валидация неизвестных опций → `400 INVALID_CATALOG_OPTION`.
  `ProductView.price` переведён на `BigDecimal`, ненужные поля опускаются (`@JsonInclude(NON_NULL)`).
- **Пагинация (#39, PR #81):** параметры `page` (с нуля) и `size` (1..100, по умолчанию 20);
  ответ `{items, total, page, size}`, где `total` считается после применения опций и до нарезки страниц;
  страница за пределами данных возвращает пустой `items` и реальный `total`.
  Неверные параметры → `400 INVALID_PAGINATION`.
- **Фильтрация (#77, PR #82):** параметр `q` — поиск без учёта регистра по названию и артикулу,
  длина до 100 символов; комбинируется с остальными опциями, влияет на `total`.
- **Сортировка (#40, PR #83):** параметры `sortBy` (whitelist: `name`, `article`, `group`, `price`, `stock`)
  и `order` (`asc`/`desc`); сортировка стабильная, null-безопасная, выполняется до пагинации.
  README дополнен полным описанием контракта и curl-примерами.
- **Архитектура:** `CatalogConfigService` отвечает за разбор и валидацию опций, страницы, фильтра и сортировки;
  `ProductService` — за конвейер «фильтрация → маппинг → сортировка → пагинация»;
  новые DTO: `CatalogFilter`, `PageOptions`, `PagedResponse<T>`, `SortOptions`, `SortOrder`.
- **Тесты:** 35 тестов (1 контекстный, 3 availability, 14 config, 17 product), `BUILD SUCCESS`.

**Итог:** витрина настраивается под аккаунт, ищет, сортирует и отдаёт данные страницами
с корректным общим количеством.

---

## Архитектура и принятые решения

| Решение | Обоснование |
|---|---|
| Без собственной БД | данные остаются в МойСклад, сервис — слой представления и агрегации |
| Слои `controller → service → client/mapper` | тонкие контроллеры, бизнес-логика в сервисах, внешние вызовы изолированы |
| Records для DTO | неизменяемость, минимум шаблонного кода, явный контракт |
| `BigDecimal` для цен | отсутствие ошибок плавающей точки в деньгах |
| Валидация на входе, единый `ErrorResponse` | предсказуемые 400/502 вместо стектрейсов |
| Профили `dev`/`prod` | разный уровень логирования и доступность Swagger |
| Секреты в `.env` | токен не попадает в репозиторий и логи |

**Поток запроса каталога:**
`GET /api/products` → `ProductController` (параметры) → `CatalogConfigService` (валидация опций, фильтра,
пагинации, сортировки) → `ProductService` (ассортимент + остатки → фильтры → маппинг → сортировка → страница)
→ `PagedResponse<ProductView>`.

---

## Текущий статус и ссылки на PR

**Готово и слито:** PR #78 (Sprint 4), PR #79 (Vision/Backlog, `main` = `938c1bc`).

**Открыты, ожидают ревью** — ветки образуют стек, сливать строго по порядку:

| PR | Задача | Issue | Ветка | Что добавляет |
|---|---|---|---|---|
| [#80](https://github.com/DanBrosCrut/java-backend-learning/pull/80) | Опции и конфигурация каталога | #76 | `feature/sprint-5-catalog-options` | `priceType`, `groups`, `fields`, `inStockOnly`, `/api/products/config` |
| [#81](https://github.com/DanBrosCrut/java-backend-learning/pull/81) | Пагинация | #39 | `feature/sprint-5-pagination` | `page`, `size`, `{items,total,page,size}` |
| [#82](https://github.com/DanBrosCrut/java-backend-learning/pull/82) | Фильтрация | #77 | `feature/sprint-5-filtering` | `q` по названию и артикулу |
| [#83](https://github.com/DanBrosCrut/java-backend-learning/pull/83) | Сортировка + README | #40 | `feature/sprint-5-sorting` | `sortBy`, `order`, документация API |

**Порядок слияния:** #80 → #81 → #82 → #83. Каждый следующий PR основан на предыдущем,
поэтому до слияния первого PR в diff видны изменения предыдущих.

> Важно: `main` на момент подготовки отчёта не компилируется из-за упомянутого
> в Спринте 4 рассинхрона имён файлов и record-типов. Исправление входит в PR #80;
> после его слияния сборка и тесты на `main` зелёные (35/35).

**Метрики**

- Тесты: **35** (`./mvnw clean test`), 0 падений.
- Эндпоинты: `GET /health`, `GET /api/products`, `GET /api/products/config`, `POST /api/availability`.
- Качество: единый формат ошибок, Swagger для всех эндпоинтов, Javadoc на публичных методах,
  constructor injection, отсутствие секретов в коде.

**Следующий шаг:** Спринт 6 — заказы: создание заказа в МойСклад и чтение заказа по идентификатору,
затем MCP-интеграция и эксплуатационные задачи из `docs/backlog.md`.
