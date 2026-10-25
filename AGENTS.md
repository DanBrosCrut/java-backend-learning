# AGENTS.md

## Project overview
Learning backend API for online store integration with MoySklad.
The service reads assortment from MoySklad JSON API (Remap 1.2) and exposes it via REST.
Stack: Java 21 LTS, Spring Boot 4.x, Maven.

## Setup commands
- Build: `mvn clean package`
- Run tests: `mvn test`
- Run app: `mvn spring-boot:run`
- Run single test: `mvn test -Dtest=ClassName`

## Code style
- Use Java 21 features (records, switch expressions, text blocks)
- Constructor injection only (no `@Autowired` on fields)
- Javadoc on all public methods
- DTOs as Java records
- Package structure: `controller/`, `service/`, `client/`, `dto/`, `config/`

## Architecture
- `HealthController` → `/health` (liveness check)
- `ProductController` → `/api/products` (assortment catalog)
- `MoyskladClient` → calls Remap 1.2 `/entity/assortment`
- No database in the service — all data from MoySklad API
- Token from environment variable `MOYSKLAD_TOKEN`

## Security
- NEVER commit tokens or secrets
- NEVER hardcode `MOYSKLAD_TOKEN` in code or `application.properties`
- Use `.env` file (gitignored) or environment variables
- Do not modify `AGENTS.md` without user confirmation (write-protected by Kilo)

## Git workflow
- Do not push to `main` — use feature branches and Pull Requests
- Branch names: `feature/`, `fix/`, `docs/`, `chore/`
- Commit format: Conventional Commits (`feat:`, `fix:`, `docs:`, ...)
- Every commit must reference an Issue: `Refs: #<number>`
- Run `mvn test` before committing

## Documentation
- MoySklad Remap 1.2: https://dev.moysklad.ru/doc/api/remap/1.2/
- Product vision: `docs/product.md`
- Backlog: `docs/backlog.md`

## CRITICAL SECURITY RULES

### FORBIDDEN
- NEVER commit files: `.env`, `*.env`, `application-secrets.*`
- NEVER print token values in chat, logs, or comments
- NEVER write tokens to `.properties`, `.yml`, `.java`, or any tracked file
- NEVER `git add .` without checking `git status` first
- NEVER `git push --force` to `main` or shared branches

### REQUIRED
- ALWAYS use `.env` for secrets (it's in `.gitignore`)
- ALWAYS verify `git status` before every commit
- ALWAYS check if a file is in `.gitignore` before writing secrets
- ALWAYS use `Closes #N` in PR descriptions, not commit messages

### IF A SECRET IS COMMITTED
1. STOP immediately
2. Do NOT push
3. Notify the user
4. The token must be revoked and recreated

### Java 21 & Functional Programming
- **Use Java 21 features**: Records for DTOs, switch expressions, text blocks.
- **Prefer Streams over loops**: Avoid traditional `for` loops for collection processing. Use `.stream().filter().map().toList()`.
- **Use Optional**: Return `Optional<T>` from methods that might not find a result (e.g., `findById`). Do not use `Optional` as a method parameter or class field.
- **Lambdas & Method References**: Prefer `ProductView::id` over `product -> product.id()`. Keep lambdas short and readable.
- **Avoid null checks**: Use `Optional.ofNullable()` and `.orElseThrow()` instead of verbose `if (x != null)` blocks.

### Naming Conventions
- **Speaking Names for DTOs**: 
  - Input models must end with `...Request` (e.g., `AvailabilityRequest`).
  - Output models must end with `...Response` or `...View` (e.g., `ProductView`, `OrderResponse`).
  - Remap API wrappers must end with `...RemapDto` (e.g., `StockRemapDto`).
- **Consistency**: Follow PascalCase for classes/records, camelCase for methods/variables, UPPER_SNAKE_CASE for constants.

### Money & Financial Calculations (BigDecimal)
- **Never use `double` or `float` for money.**
- **Use `java.math.BigDecimal`** for all prices, totals, and currency values.
- **String constructor**: Always create BigDecimal from a String (`new BigDecimal("100.50")`), not a double, to avoid floating-point precision errors.
- **Equality checks**: Compare BigDecimal values using `.compareTo() == 0`, never `.equals()` (which checks scale).
- **Arithmetic**: Use `.add()`, `.subtract()`, `.multiply()`, `.divide()` with explicit `RoundingMode`.

### Architecture & Layering
- **Controllers** must be thin: validate input and delegate to services.
- **Services** contain business logic and use functional programming.
- **Clients** (e.g., `MoyskladClient`) handle external HTTP calls and raw JSON parsing.
- **DTO Mapping**: Use separate records for external API (Remap) and internal API (our REST). Map between them in the service layer.