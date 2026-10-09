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