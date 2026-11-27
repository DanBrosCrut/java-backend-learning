"""MCP server for the MoySklad storefront API.

This is a standalone application: it holds no business logic and calls the
Spring Boot REST API over HTTP (http://localhost:8080/api/...).

The backend is stateless — the AI agent keeps the cart in its own context and
passes the full list of positions to validate_order/create_order.
"""

import os
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

API_BASE_URL = os.getenv("MOYSKLAD_API_URL", "http://localhost:8080")
TIMEOUT_SECONDS = 30.0

mcp = FastMCP("moysklad-storefront")


def _call(method: str, path: str, **kwargs) -> Any:
    """Calls the storefront REST API and returns the parsed JSON body."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=TIMEOUT_SECONDS) as client:
            response = client.request(method, path, **kwargs)
            response.raise_for_status()
            return response.json() if response.content else None
    except httpx.HTTPStatusError as exc:
        error = _error_payload(exc.response)
        raise RuntimeError(f"{error['code']}: {error['message']}") from exc
    except httpx.RequestError as exc:
        raise RuntimeError(f"Storefront API is unreachable at {API_BASE_URL}: {exc}") from exc


def _error_payload(response: httpx.Response) -> dict[str, str]:
    """Extracts the structured ErrorResponse from a failed API call."""
    try:
        body = response.json()
        return {
            "code": body.get("code", "HTTP_ERROR"),
            "message": body.get("message", response.text),
        }
    except ValueError:
        return {"code": "HTTP_ERROR", "message": f"HTTP {response.status_code}: {response.text}"}


@mcp.tool()
def get_catalog(
        price_type: str | None = None,
        groups: str | None = None,
        q: str | None = None,
        fields: str | None = None,
        in_stock_only: bool = False,
        sort_by: str | None = None,
        order: str = "asc",
        page: int = 0,
        size: int = 20,
) -> dict:
    """Returns one page of the product catalog with prices and current stock.

    Mirrors GET /api/products. Empty values keep the MoySklad defaults.
    """
    params: dict[str, Any] = {"inStockOnly": in_stock_only, "order": order, "page": page, "size": size}
    for key, value in (
            ("priceType", price_type), ("groups", groups), ("q", q),
            ("fields", fields), ("sortBy", sort_by)):
        if value:
            params[key] = value
    return _call("GET", "/api/products", params=params)


@mcp.tool()
def get_config() -> dict:
    """Returns the available price types, groups and fields of the showcase.

    Mirrors GET /api/products/config.
    """
    return _call("GET", "/api/products/config")


@mcp.tool()
def check_availability(positions: list[dict]) -> dict:
    """Checks whether the given positions are covered by the current stock.

    Mirrors POST /api/availability. Each position is {"id": "<productId>", "quantity": N}.
    The response is {"available": bool, "missing": [...]}.
    """
    return _call("POST", "/api/availability", json={"positions": positions})


@mcp.tool()
def validate_order(positions: list[dict]) -> dict:
    """Validates an order before creation; no order is created.

    Mirrors POST /api/orders/validate. Each position is
    {"productId": "<uuid>", "quantity": N}. When the stock is short the response
    contains structured errors with requested/available quantities and up to
    three in-stock alternatives.
    """
    return _call("POST", "/api/orders/validate", json={"positions": positions})


@mcp.tool()
def create_order(positions: list[dict]) -> dict:
    """Creates a customer order in MoySklad from the given positions.

    Mirrors POST /api/orders. Each position is {"productId": "<uuid>", "quantity": N}.
    Side effect: a real order is created in MoySklad. When any position is not
    covered by the current stock, the API returns 409 ORDER_VALIDATION_FAILED
    and no order is created.
    """
    return _call("POST", "/api/orders", json={"positions": positions})


if __name__ == "__main__":
    mcp.run()
