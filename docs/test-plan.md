# API Test Plan — Sprint 8

Документ описывает все текущие эндпоинты витрины, ожидаемые HTTP-статусы, edge cases
и результаты аудита обработки ошибок и fallback'ов.

План написан для целевого состояния Sprint 8 (stateless backend, заказы). Поведения,
которые приходят из PR в ревью, помечены: SRP/ошибки — #105, заказы/stateless — #106.

## Базовые соглашения

- Базовый URL: `http://localhost:8080`, данные — реальный учебный аккаунт МойСклад.
- Ошибки — единый формат `ErrorResponse`:
  ```json
  {"code": "ORDER_VALIDATION_FAILED", "message": "..."}
  ```
- Заголовки ответа:
  - `X-Cache: HIT|MISS` — обращался ли запрос в МойСклад (комбинированный статус каталога и остатков);
  - `X-Correlation-Id` — идентификатор запроса, совпадает с логом приложения.
- Управление кешем: `?invalidate=true` или заголовок `X-Force-Refresh: true` (полный сброс каталога и остатков).
- Swagger UI: `/swagger-ui/index.html` — контракт должен совпадать с этим документом.

## Матрица кодов ошибок

| Код | HTTP | Когда |
|---|---|---|
| `VALIDATION_ERROR` | 400 | нарушена bean-валидация или тело не читается (malformed/missing JSON) |
| `INVALID_CATALOG_OPTION` | 400 | неизвестные `priceType`, `groups`, `fields`, `q`, `sortBy`, `order` |
| `INVALID_PAGINATION` | 400 | неверные `page` / `size` |
| `NOT_FOUND` | 404 | путь или статический ресурс не найден (#105) |
| `ORDER_VALIDATION_FAILED` | 409 | при создании заказа не хватает остатка (заказ не создан) |
| `INTERNAL_ERROR` | 500 | непредвиденная ошибка; клиенту — общий текст, без внутренних деталей (#105) |
| `ORDER_CONFIGURATION_ERROR` | 500 | не заданы организация/контрагент для заказов |
| `MOYSKLAD_ERROR` | 502 | Remap вернул ошибку или недоступен; в логе — статус и тело ответа Remap |

## Эндпоинты

### GET /api/products

Параметры (все необязательные): `priceType`, `groups`, `q`, `fields`, `inStockOnly`,
`sortBy` (`name|article|group|price|stock`), `order` (`asc|desc`), `page` (с нуля),
`size` (1..100), `invalidate`, заголовок `X-Force-Refresh`.

| Сценарий | Ожидание |
|---|---|
| Без параметров | `200`, первая страница, `total` — все товары |
| `?size=1` | `200`, один элемент |
| `?page=99&size=20` | `200`, `items: []`, реальный `total` |
| `?invalidate=true` | `200`, `X-Cache: MISS` (полный сброс) |
| `X-Force-Refresh: true` | `200`, `X-Cache: MISS` |
| повторный запрос в пределах TTL | `200`, `X-Cache: HIT` |
| `?sortBy=unknown` | `400 INVALID_CATALOG_OPTION` |
| `?order=sideways` | `400 INVALID_CATALOG_OPTION` |
| `?priceType=<несуществующий>` | `400 INVALID_CATALOG_OPTION` |
| `?page=-1` или `?size=0`/`101` | `400 INVALID_PAGINATION` |
| `?q=неттакоготовара` | `200`, `items: []`, `total: 0` |
| МойСклад недоступен при пустом кеше | `502 MOYSKLAD_ERROR` |

### GET /api/products/config

| Сценарий | Ожидание |
|---|---|
| Обычный запрос | `200`, доступные типы цен, группы, поля и значения по умолчанию |
| `?invalidate=true` | `200`, `X-Cache: MISS` |
| МойСклад недоступен при пустом кеше | `502 MOYSKLAD_ERROR` |

### POST /api/availability

Тело: `{"positions":[{"id":"<uuid>","quantity":N}]}`. Валидации на DTO нет — поле `id`,
количество — на совести клиента (см. edge cases).

| Сценарий | Ожидание |
|---|---|
| Остатка хватает | `200`, `{"available":true,"missing":[]}` |
| Остатка не хватает | `200`, `available:false`, `missing` с `id/requested/available` |
| Неизвестный товар | `200`, `available:false`, `available: 0` |
| `positions: []` | `200`, `available:true` (пустая проверка валидна) |
| `quantity: -1` | `200`, позиция в `missing` (защиты нет — кандидат на hardening) |
| Некорректный JSON | `400 VALIDATION_ERROR` |
| МойСклад недоступен | `502 MOYSKLAD_ERROR` |

### POST /api/orders/validate

Тело: `{"positions":[{"productId":"<uuid>","quantity":N}]}`; валидация: `@NotEmpty`,
`@NotBlank`, `@Min(1)`.

| Сценарий | Ожидание |
|---|---|
| Всё в наличии | `200`, `{"valid":true,"errors":[]}` |
| Не хватает | `200`, `valid:false`, ошибки с `productId/requested/available/message` и до 3 альтернатив |
| Дубликаты товара | `200`, количества суммируются, одна ошибка на товар |
| Неизвестный товар | `200`, `valid:false`, `available: 0`, сообщение с ID |
| `positions: []` | `400 VALIDATION_ERROR` (`positions must not be empty`) |
| `quantity: 0`/`-1` | `400 VALIDATION_ERROR` (`must be at least 1`) |
| пустой `productId` | `400 VALIDATION_ERROR` (`must not be blank`) |
| Malformed JSON | `400 VALIDATION_ERROR` |
| МойСклад недоступен | `502 MOYSKLAD_ERROR` |

### POST /api/orders

Тело как у `/validate`. Создаёт customer order в МойСклад; требует
`moysklad.orders.organization-id` и `agent-id` (`MOYSKLAD_ORDER_ORGANIZATION_ID`,
`MOYSKLAD_ORDER_AGENT_ID`).

| Сценарий | Ожидание |
|---|---|
| Всё в наличии, конфигурация задана | `201`, `{"id","name","sum"}`; заказ виден в МойСклад |
| Не хватает остатка | `409 ORDER_VALIDATION_FAILED`, заказ НЕ создан |
| Не заданы организация/контрагент | `500 ORDER_CONFIGURATION_ERROR` |
| `positions: []`, `quantity<1`, пустой `productId` | `400 VALIDATION_ERROR` |
| Товар без цены в витрине | `201`, позиция с ценой `0` |
| Remap отвечает 4xx/5xx | `502 MOYSKLAD_ERROR` (статус и тело Remap в логе) |
| МойСклад недоступен | `502 MOYSKLAD_ERROR`, заказ не создан |

Проверка «заказ не создан» при 409: сравнить счётчик заказов в МойСклад до/после
или убедиться, что `createCustomerOrder` не вызывался (юнит-тест `OrderServiceTest`).

### Прочее

| Эндпоинт | Ожидание |
|---|---|
| `GET /health` | `200 ok` |
| `GET /actuator/health` | `200`, `{"status":"UP"}` |
| `/favicon.ico`, неизвестный путь | `404 NOT_FOUND` (#105) |

## Аудит обработки ошибок и fallback'ов

### Что уже есть

1. **Upstream-ошибки — graceful 502.** `MoyskladClient` ловит `HttpStatusCodeException`,
   логирует HTTP-статус и тело ответа Remap (код/сообщение, например `1034`) и бросает
   `MoyskladApiException` → `GlobalExceptionHandler` → `502 MOYSKLAD_ERROR`.
   Клиент не получает сырой стектрейс и не получает `500` на проблемы МойСклад. ✅
2. **Fallback остатков (delta → full).** При любом сбое delta-запроса `StockCache`
   логирует `Delta fetch failed, falling back to full sync` и выполняет полный отчёт,
   который заменяет снапшот. Fallback покрывает 412/400/5xx и сетевые ошибки
   (`RuntimeException`). ✅
3. **Fallback дельты логируется.** Warning и последующий `MISS (full)` видны в логе
   с correlation ID. ✅
4. **Неуспешная загрузка не кешируется.** Если упал и полный отчёт, старый снапшот
   остаётся, следующий запрос повторяет попытку. ✅
5. **Внутренние ошибки не текут клиенту.** Общий обработчик возвращает
   `500 INTERNAL_ERROR` с фиксированным текстом, детали — только в лог (#105). ✅

### Осознанные ограничения

- **У каталога нет fallback**: при недоступности МойСклад `GET /api/products` отвечает
  `502` (показывать нечего — graceful-деградация невозможна без данных). Это
  соответствует правилу «fallback только там, где есть восстановление».
- **У клиента нет явных таймаутов/retry** (`RestClient` использует дефолты). Если нужно
  мягкое деградирование на сетевых сбоях — добавить `connectTimeout`/`readTimeout`
  (кандидат на Sprint 9, не over-engineer сейчас).
- **`POST /api/availability` без bean-валидации** (`id`, `quantity`): отрицательные
  количества дают бессмысленный `missing`. Кандидат на hardening.

### Вывод аудита

Целевое поведение соблюдается: ошибки МойСклад → `502` (не `500`), fallback дельты
присутствует и логируется, внутренние детали наружу не уходят. Недостатков,
требующих немедленного фикса в рамках MVP, не найдено.

## Как прогонять

1. Юнит/интеграционные тесты: `cd moysklad-api && .\mvnw.cmd test` — ожидание `BUILD SUCCESS`
   (в целевом состоянии спринта: 105+ тестов).
2. Ручной прогон по таблицам выше через Postman/Swagger (скрины спринтовых прогонов:
   `docs/screenshots/`).
3. Кеш-сценарий с коротким TTL:
   `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--moysklad.stock-cache.ttl=PT5S"` —
   первый запрос `MISS (full)`, через 6 секунд `MISS (delta ...)` без `412`.
4. Сценарий недоступности МойСклад: временно неверный токен → все каталоговые/заказные
   запросы должны отвечать `502 MOYSKLAD_ERROR`, а не `500`.
