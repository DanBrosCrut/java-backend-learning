# mcp-server — MCP-обёртка над REST API

Отдельное приложение, которое даёт AI-агентам доступ к витрине МойСклад через MCP.
Сервер **не содержит бизнес-логики**: каждый инструмент — тонкая HTTP-обёртка над
Spring Boot API (`http://localhost:8080/api/...`), контракты и ошибки совпадают с REST.

Бэкенд stateless: корзину агент ведёт в своём контексте между ходами и передаёт
список позиций только в `validate_order` / `create_order`.

## Инструменты

| Инструмент | REST-эквивалент | Описание |
|---|---|---|
| `get_catalog` | `GET /api/products` | страница каталога с ценами и остатками (фильтры, сортировка, пагинация) |
| `get_config` | `GET /api/products/config` | доступные типы цен, группы, поля |
| `check_availability` | `POST /api/availability` | проверка остатков по позициям (`{"id", "quantity"}`) |
| `validate_order` | `POST /api/orders/validate` | проверка заказа без создания (`{"productId", "quantity"}`) |
| `create_order` | `POST /api/orders` | создание заказа в МойСклад; побочный эффект — реальный заказ |

Формат позиций:

- `check_availability`: `[{"id": "<uuid>", "quantity": 2}]`;
- `validate_order` / `create_order`: `[{"productId": "<uuid>", "quantity": 2}]`.

## Требования

- Python 3.10+;
- запущенное Spring Boot приложение (`cd moysklad-api && .\mvnw.cmd spring-boot:run`) с токеном из `.env`;
- для `create_order` у API должны быть заданы `MOYSKLAD_ORDER_ORGANIZATION_ID` и `MOYSKLAD_ORDER_AGENT_ID`.

## Установка и запуск

```powershell
cd mcp-server
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python server.py
```

Сервер работает по stdio. Базовый URL API можно переопределить переменной окружения:

```powershell
$env:MOYSKLAD_API_URL = "http://localhost:8081"
python server.py
```

## Подключение клиента

Пример для клиента с поддержкой MCP (Claude Desktop и аналоги):

```json
{
  "mcpServers": {
    "moysklad-storefront": {
      "command": "python",
      "args": ["server.py"],
      "cwd": "C:/путь/к/java-backend-learning/mcp-server"
    }
  }
}
```

## Ошибки

Ошибки REST приходят в том же виде, что и у API: `code` + `message` из `ErrorResponse`
(например, `VALIDATION_ERROR`, `ORDER_VALIDATION_FAILED`, `MOYSKLAD_ERROR`).
Если API недоступен, инструмент возвращает ошибку с текстом `Storefront API is unreachable`.

## Планы

- `get_order` (`GET /api/orders/{id}`) — после реализации эндпоинта чтения заказа в Sprint 8.
