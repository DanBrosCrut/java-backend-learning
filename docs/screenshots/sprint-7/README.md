# Скриншоты Sprint 7

> Исторический артефакт Sprint 7. Эндпоинты /api/cart* удалены в Sprint 8 (backend стал stateless). Скриншоты сохранены как доказательство спринта.

Сюда складываем скриншоты запросов из Postman для отчёта по Sprint 7.
Имена файлов фиксированные — по ним отчёт ссылается на изображения.

| Файл | Запрос | Что должно быть видно |
|---|---|---|
| `01-products-miss.png` | `GET /api/products?size=1&invalidate=true` | URL, статус `200`, заголовок `X-Cache: MISS`, тело ответа |
| `02-products-hit.png` | `GET /api/products?size=1` | URL, статус `200`, заголовок `X-Cache: HIT` |
| `03-products-delta-502.png` | `GET /api/products?size=1` после 5 минут | URL, статус `502`, тело `MOYSKLAD_ERROR` |
| `04-cart-empty.png` | `GET /api/cart` | пустая корзина, `totalQuantity: 0` |
| `05-cart-add.png` | `POST /api/cart/items` | тело запроса, статус `201`, тело ответа |
| `06-cart-get.png` | `GET /api/cart` | позиция в корзине и `totalQuantity` |
| `07-orders-invalid.png` | `POST /api/orders/validate` (количество больше остатка) | `valid: false`, ошибка и альтернативы |
| `08-cart-delete.png` | `DELETE /api/cart/items/{productId}` | статус `204` |

Порт 8080 — ветка `feature/sprint-7-delta-cache`, порт 8081 — `feature/sprint-7-user-flow`.
