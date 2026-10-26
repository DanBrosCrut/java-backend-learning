# AGENTS.md

## 1. Project Overview
Learning backend API for online store integration with MoySklad.
The service reads assortment from MoySklad JSON API (Remap 1.2) and exposes it via REST.
Stack: Java 21 LTS, Spring Boot 4.x, Maven.

## 2. Setup Commands
- Build: `mvn clean package`
- Run tests: `mvn test`
- Run single test: `mvn test -Dtest=ClassName`
- Run app: `mvn spring-boot:run`

## 3. Architecture & Layering
- **Controllers**: Must be thin. Validate input and delegate to services. Use `HealthController` → `/health`, `ProductController` → `/api/products`.
- **Services**: Contain all business logic. Use functional programming.
- **Clients**: Handle external HTTP calls and raw JSON parsing (e.g., `MoyskladClient` calls Remap 1.2 `/entity/assortment`).
- **DTOs**: Use separate records for external API (Remap) and internal API (our REST). Map between them in the service layer.
- **Database**: No database in the service — all data comes from the MoySklad API.
- **Packages**: `controller/`, `service/`, `client/`, `dto/`, `config/`.

## 4. Code Style & Java 21 Features
- **Java 21**: Use Records for DTOs, switch expressions, and text blocks.
- **Constructor Injection**: Only use constructor injection. NEVER use `@Autowired` on fields.
- **Javadoc**: Required on all public methods.
- **Streams over Loops**: Avoid traditional `for` loops for collection processing. Use `.stream().filter().map().toList()`.
- **Optional**: Return `Optional<T>` from methods that might not find a result (e.g., `findById`). Do not use `Optional` as a method parameter or class field.
- **Lambdas**: Prefer method references (`ProductView::id`) over verbose lambdas. Keep lambdas short.
- **Null Checks**: Use `Optional.ofNullable()` and `.orElseThrow()` instead of verbose `if (x != null)` blocks.

## 5. Naming Conventions
- **Speaking Names for DTOs**: 
  - Input models: end with `...Request` (e.g., `AvailabilityRequest`).
  - Output models: end with `...Response` or `...View` (e.g., `ProductView`, `OrderResponse`).
  - Remap API wrappers: end with `...RemapDto` (e.g., `StockRemapDto`).
- **Consistency**: PascalCase for classes/records, camelCase for methods/variables, UPPER_SNAKE_CASE for constants.

## 6. Money & Financial Calculations (BigDecimal)
- **No Floats**: NEVER use `double` or `float` for money.
- **BigDecimal**: Use `java.math.BigDecimal` for all prices, totals, and currency values.
- **String Constructor**: Always create from String (`new BigDecimal("100.50")`) to avoid floating-point errors.
- **Equality**: Compare using `.compareTo() == 0`, never `.equals()` (which checks scale).
- **Arithmetic**: Use `.add()`, `.subtract()`, `.multiply()`, `.divide()` with explicit `RoundingMode`.

## 7. Error Handling & Logging
- **Global Exception Handler**: Use `@RestControllerAdvice` for centralized error handling. Never let a raw stack trace reach the client.
- **Error Responses**: Return a structured `ErrorResponse` record with `code` and `message` fields.
- **HTTP Statuses**: Use appropriate codes (400 for validation, 404 for not found, 409 for conflict, 502 for upstream Moysklad errors).
- **Logging**: Use SLF4J (`LoggerFactory.getLogger`). NEVER use `System.out.println`. 
- **No Sensitive Data**: Never log tokens, passwords, or raw `MOYSKLAD_TOKEN` values.

## 8. Testing Standards
- **Frameworks**: Use JUnit 5 and Mockito.
- **Mocking**: Mock external dependencies (e.g., `MoyskladClient`) using `@Mock` and `@InjectMocks`.
- **Coverage**: Always write tests for happy paths AND edge cases (e.g., empty lists, missing products, insufficient stock).
- **Naming**: Test methods must be descriptive, e.g., `shouldReturnAvailableTrue_whenStockIsSufficient()`.

## 9. API Documentation (Swagger)
- **Annotations**: Every new controller and endpoint MUST have `@Tag` (class level) and `@Operation` (method level) with a clear `summary` and `description`.
- **DTO Schemas**: Use `@Schema(description = "...")` on record fields to document their meaning for the API client.

## 10. MCP Usage (GitHub)
- **Creating PRs**: Always use the GitHub MCP tool to create Pull Requests. Do not use raw `gh` CLI or `curl` if MCP is available.
- **Checking Issues**: Use MCP to read the description and Acceptance Criteria of the Issue before starting work.
- **Review comments**: If asked to fix a review, read the PR comments via MCP.

## 11. Git Workflow (Strict)
- **One task = one branch = one PR**: NEVER commit multiple unrelated tasks into a single branch. Always create a new branch for each task from `main`.
- **Branch names**: `feature/`, `fix/`, `docs/`, `chore/`. No ambiguous names.
- **Commit format**: Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`).
- **Commit granularity**: Commits should be small and atomic (one logical change per commit).
- **Issue linking**: Every commit must reference an Issue in the body: `Refs: #<number>`.
- **Pull Requests**: PR descriptions MUST include: What was done, How to test (curl commands), and `Closes #<issue-number>` (if applicable).
- **Pre-commit**: Run `mvn test` before committing.
- **Cherry-picking**: If you accidentally committed to the wrong branch, use `git cherry-pick <hash>` to move it; never use destructive commands.

## 12. CRITICAL SECURITY RULES
### FORBIDDEN
- NEVER commit files: `.env`, `*.env`, `application-secrets.*`
- NEVER print token values in chat, logs, or comments
- NEVER write tokens to `.properties`, `.yml`, `.java`, or any tracked file
- NEVER `git add .` without checking `git status` first
- NEVER `git push --force` to `main` or shared branches
- Do not modify `AGENTS.md` without user confirmation (write-protected)

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

## 13. Documentation Links
- MoySklad Remap 1.2: https://dev.moysklad.ru/doc/api/remap/1.2/
- Product vision: `docs/product.md`
- Backlog: `docs/backlog.md`
