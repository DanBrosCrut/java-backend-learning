# МойСклад Remap 1.2 — краткий справочник

Шпаргалка по возможностям JSON API МойСклад, которые использует проект.
Полная документация: https://dev.moysklad.ru/doc/api/remap/1.2/

## База и авторизация

- Base URL: `https://api.moysklad.ru/api/remap/1.2`
- Авторизация: заголовок `Authorization: Bearer <токен>` (токен — только из `.env` / окружения).
- Цены приходят в минорных единицах (копейках) — в коде делятся на `100` и хранятся в `BigDecimal`.

## Используемые эндпоинты

| Эндпоинт | Назначение |
|---|---|
| `GET /entity/assortment` | ассортимент (товары, услуги, комплекты) |
| `GET /report/stock/all/current?stockType=stock` | текущий (физический) остаток |
| `POST /entity/customerorder` | создание заказа покупателя |

**`GET /entity/assortment`** — пагинация `limit` + `offset`, **максимум 1000 строк за запрос**;
клиент листает страницы до последней. В строке: `id`, `name`, `article`, `pathName` (группа),
`salePrices` (цены по типам), `meta` (ссылки на связанные сущности).

**`GET /report/stock/all/current?stockType=stock`** — фактический остаток на складе
(не накопительный отчёт). Дельта — параметр `changedSince` в формате `yyyy-MM-dd HH:mm:ss`
в таймзоне аккаунта (у нас `Europe/Moscow`). `changedSince` **нельзя сочетать**
с `include=zeroLines`: Remap отвечает `412` с кодом `1034`; обнулённые остатки приходят
в дельте сами.

**`POST /entity/customerorder`** — тело `organization`, `agent`, `positions`; ссылки на
сущности передаются через `meta` (`{"meta": {"href": "...", "type": "...", "metadata": {"href": "..."}}}`).
Позиция — `assortment` (meta-ссылка на товар), `quantity`, `price`.

## Формат ошибок

```json
{"errors": [{"code": 1034, "message": "..."}]}
```

Пример `1034` — несовместимое сочетание параметров (`changedSince` + `include=zeroLines`).
В сервисе такие ответы превращаются в `502 MOYSKLAD_ERROR`, статус и тело Remap пишутся в лог.
