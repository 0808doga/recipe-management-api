# Requirements Checklist

Comparison of the professor's `project-requirements.pdf` (9 pages) with this project.
Status values: **COMPLETE**, **PARTIAL**, **MISSING**.
"Verified" means the check was actually run on November 23, 2026 (macOS, Apple Silicon, Java 17.0.19).

Package prefix below: `com.dogasimsek.recipeapi` (shortened to `…`).

## 1. Key facts / scope

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| Java 17+ | `pom.xml` (`java.version` 17) | build verified with Java 17.0.19 | COMPLETE |
| Spring Boot 3.x | `pom.xml` (3.3.5) | – | COMPLETE |
| Spring Data JPA, Spring Security | `pom.xml`, `…/repository`, `…/config/SecurityConfig` | – | COMPLETE |
| Database: PostgreSQL/MySQL, H2 allowed for dev/testing | H2 file DB (dev), H2 in-memory (test) – `application-dev.properties`, `application-test.properties` | app start verified, tables created by Hibernate | COMPLETE (H2, as allowed) |
| OpenAPI/Swagger | springdoc 2.6.0, `…/config/OpenApiConfig` | `ApplicationIntegrationTest` | COMPLETE |
| Git repository | local Git repository with commit | – | PARTIAL – not pushed to GitHub yet (needs your account; see README §15) |
| Topic: Recipe Management API (Recipe CRUD, ingredient management, meal planning) | Recipe, Ingredient, MealPlan | all CRUD endpoints | COMPLETE |

## 2. Mandatory features

### 2.1 Authentication & Authorization

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| User registration | `AuthService.register`, `AuthController` | `POST /api/auth/register`; `AuthControllerIntegrationTest.registerCreatesUserWithHashedPassword` | COMPLETE |
| Login | `AuthService.login` | `POST /api/auth/login`; `AuthControllerIntegrationTest.loginReturnsTokens` | COMPLETE |
| BCrypt password hashing | `SecurityConfig.passwordEncoder` | test checks hash starts with `$2a$` | COMPLETE |
| JWT-based authentication (create, validate, expire) | `JwtService`, `JwtAuthenticationFilter` | `JwtServiceTest` (7 tests incl. expired / wrong key / missing secret) | COMPLETE |
| Invalid token handling | `JwtAuthenticationFilter`, `SecurityErrorHandler` | `AuthControllerIntegrationTest.requestWithInvalidTokenReturns401` | COMPLETE |
| Refresh token (recommended) | `JwtService.generateRefreshToken`, `AuthService.refreshToken` | `POST /api/auth/refresh`; `refreshTokenGivesNewAccessToken`, `accessTokenCannotBeUsedAsRefreshToken` | COMPLETE (stateless; tokens cannot be revoked before expiry – kept simple) |
| At least 2 roles | `entity/Role` (USER, ADMIN) | – | COMPLETE |
| Role-specific endpoints | `AdminController` (`/api/admin/**`) | `AdminControllerIntegrationTest` | COMPLETE |
| Authorization rules enforced | `SecurityConfig` (URL rules), `@PreAuthorize`, `RecipeService.checkCanModify`, `MealPlanService.findOwnMealPlan` | `otherUserCannotUpdateOrDeleteRecipe`, `mealPlansArePrivate`, `normalUserGets403OnAdminEndpoints` | COMPLETE |
| Clear 401 / 403 JSON errors | `SecurityErrorHandler`, `GlobalExceptionHandler` | `requestWithoutTokenReturns401`, `normalUserGets403OnAdminEndpoints` | COMPLETE |
| Local test credentials (not hardcoded for production) | `config/DataSeeder` (`@Profile("dev")`), passwords from env vars with dev fallback | manual: seeded accounts log in (verified) | COMPLETE |

### 2.2 CRUD operations

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| ≥ 3 entities with full CRUD | Recipe, Ingredient, MealPlan (+ User) | `RecipeControllerIntegrationTest`, `IngredientControllerIntegrationTest.ingredientCrudWorks`, `MealPlanControllerIntegrationTest.mealPlanCrudWorks` | COMPLETE |
| Entity relationships | `Recipe` (`@ManyToOne owner`, `@OneToMany ingredients`), `Ingredient` (`@ManyToOne recipe`), `MealPlan` (`@ManyToOne recipe`, `@ManyToOne owner`) | `RecipeRepositoryTest` | COMPLETE (One-to-Many / Many-to-One; no Many-to-Many was needed) |
| Proper cascade operations | `Recipe.ingredients` `cascade = ALL`, `orphanRemoval = true` | `savingRecipeCascadesToIngredients…`, `deletingRecipeAlsoDeletesIngredients`, `deleteRecipeAlsoDeletesItsMealPlans` | COMPLETE |
| Business logic validation | `RecipeService`, `IngredientService`, `MealPlanService`, `AdminService` | `duplicateIngredientNamesInRecipeReturns400`, `sameDateAndMealTypeTwiceReturns409`, `adminCannotChangeOwnRole` | COMPLETE |

### 2.3 DTOs and validation

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| No direct entity exposure | all controllers return records from `…/dto` | `$.password` does not exist (tests) | COMPLETE |
| Request DTOs | `RegisterRequest`, `LoginRequest`, `RefreshTokenRequest`, `RecipeRequest`, `IngredientRequest`, `MealPlanRequest`, `RoleUpdateRequest` | – | COMPLETE |
| Response DTOs | `UserResponse`, `AuthResponse`, `RecipeResponse`, `IngredientResponse`, `MealPlanResponse`, `PageResponse`, `AuditInfo`, `AdminStatsResponse`, `ErrorResponse` | – | COMPLETE |
| Bean validation annotations | `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`, `@Positive`, `@DecimalMax`, `@Pattern`, `@FutureOrPresent` | `ValidationTest`, `invalidRecipeReturnsAllValidationErrors` | COMPLETE |
| Custom validators | `@ValidUnit` / `ValidUnitValidator`, `@ValidIngredientQuantity` / `IngredientQuantityValidator` | `ValidationTest.unknownUnitIsRejected`, `fractionalPiecesAreRejected` | COMPLETE |

### 2.4 Global exception handling

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| Centralized `@RestControllerAdvice` | `…/exception/GlobalExceptionHandler` | – | COMPLETE |
| Consistent error structure | `dto/ErrorResponse` (timestamp, status, error, message, path, validationErrors) | all error tests check `$.status` / `$.message` | COMPLETE |
| 400 / 401 / 403 / 404 / 405 / 409 / 500 | handler methods + `SecurityErrorHandler` | `missingRecipeReturns404`, `unknownEndpointReturns404AndWrongMethodReturns405`, `duplicateRegistrationReturns409`, `malformedJsonReturns400`, … | COMPLETE (500 path exists; not triggered by a test because no endpoint fails on purpose) |
| Duplicate registration | `DuplicateResourceException` → 409 | `duplicateRegistrationReturns409` | COMPLETE |
| Validation error details | `validationErrors` map with field names | `invalidRegistrationReturnsValidationErrors` | COMPLETE |
| No sensitive internal details | 500 and `DataIntegrityViolationException` return general messages; details only logged | – | COMPLETE |

### 2.5 Database design

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| Normalized schema | 4 tables, no repeated data, FKs (`README.md` §5 diagram) | – | COMPLETE |
| Data types and constraints | `nullable = false`, lengths, unique `users.email`, unique `(owner_id, plan_date, meal_type)`, named FKs | – | COMPLETE |
| Indexed fields for search | `idx_recipe_title`, `idx_recipe_owner`, `idx_ingredient_name`, `idx_ingredient_recipe`, `idx_meal_plan_owner_date` | verified in generated DDL | COMPLETE |
| Sample data (optional seeder) | `config/DataSeeder` – 2 accounts, 12 recipes | verified on startup log | COMPLETE |

### 2.6 API documentation

| Requirement | Implemented in | Endpoint / test | Status |
|---|---|---|---|
| OpenAPI/Swagger integration | springdoc, `OpenApiConfig` | `/v3/api-docs` – `openApiDocumentationIsPublic` | COMPLETE |
| All endpoints documented | `@Tag`, `@Operation`, `@ApiResponse(s)` on all 5 controllers | – | COMPLETE |
| Request/response examples | `@Schema(example = …)` on all DTO fields | visible in Swagger | COMPLETE |
| Authentication requirements specified | `bearerAuth` security scheme; public endpoints marked with `@SecurityRequirements` | `components.securitySchemes.bearerAuth` checked in test | COMPLETE |
| Accessible at `/swagger-ui.html` | `springdoc.swagger-ui.path=/swagger-ui.html` | `swaggerUiIsAvailable` (302 → `/swagger-ui/index.html`, page returns 200 – verified with curl) | COMPLETE |

### 2.7 Testing

| Requirement | Implemented in | Result | Status |
|---|---|---|---|
| Unit tests for service layer | `AuthServiceTest`, `RecipeServiceTest`, `IngredientServiceTest`, `MealPlanServiceTest`, `AdminServiceTest`, `UserServiceTest` (Mockito) | 49 tests pass | COMPLETE |
| Integration tests for critical endpoints | `AuthControllerIntegrationTest`, `RecipeControllerIntegrationTest`, `IngredientControllerIntegrationTest`, `MealPlanControllerIntegrationTest`, `AdminControllerIntegrationTest`, `ApplicationIntegrationTest` | 46 tests pass | COMPLETE |
| Repository tests (optional) | `RecipeRepositoryTest` (`@DataJpaTest`) | 4 tests pass | COMPLETE |
| Other tests | `JwtServiceTest` (7), `ValidationTest` (8) | pass | COMPLETE |
| Service coverage ≥ 70% | JaCoCo | **100.0 % lines** (99.4 % instructions) | COMPLETE |
| Overall coverage ≥ 60% | JaCoCo | **86.6 % lines** (79.6 % instructions) | COMPLETE |

Total: **114 tests, 0 failures, 0 errors** (`./mvnw verify`, BUILD SUCCESS, "All coverage checks have been met").

## 3. Additional features (3 chosen)

| Feature | Sub-requirement | Implemented in | Test | Status |
|---|---|---|---|---|
| **Pagination & Sorting** | Paginated list endpoints | `GET /api/recipes`, `/search`, `/my` with `Pageable` | `listRecipesWithPaginationAndMultipleSortFields` | COMPLETE |
| | Configurable page size | `size` param, max 50 (`spring.data.web.pageable.max-page-size`) | same | COMPLETE |
| | Sorting by multiple fields | repeated `sort` params + whitelist in `RecipeService.validateSortFields` | same + `sortingByNotAllowedFieldReturns400` | COMPLETE |
| | Total count and metadata | `PageResponse` (totalElements, totalPages, page, size, first, last, sort) | same | COMPLETE |
| **Advanced Validation** | Custom validation annotations | `@ValidUnit`, `@ValidIngredientQuantity` | `ValidationTest` | COMPLETE |
| | Cross-field validation | `IngredientQuantityValidator` (unit + quantity) | `fractionalPiecesAreRejected` | COMPLETE |
| | Business rule validation | duplicate ingredient names, one meal per slot, own-role change blocked, sort whitelist | service + integration tests | COMPLETE |
| | Conditional validation logic | whole-number rule only applies when unit is piece/slice/clove | `validIngredientHasNoErrors`, `fractionalPiecesAreRejected` | COMPLETE |
| **Audit Trail** | Track creation/modification | `AuditableEntity` + `@EnableJpaAuditing` | `savingRecipeCascadesToIngredientsAndFillsAuditFields` | COMPLETE |
| | User attribution | `@CreatedBy`, `@LastModifiedBy`, `JpaAuditingConfig.auditorAware` | `adminCanUpdateAnyRecipe` (createdBy owner, lastModifiedBy admin) | COMPLETE |
| | Timestamp tracking | `@CreatedDate`, `@LastModifiedDate` | `createRecipeReturns201WithIngredientsAndAudit` | COMPLETE |
| | Change history | only the latest change is stored (no history table) | – | PARTIAL (kept simple on purpose) |
| | Audit info in responses | `AuditInfo` inside Recipe/Ingredient/MealPlan responses | integration tests check `$.audit.*` | COMPLETE |

## 4. Technical requirements

| Requirement | Implemented in | Status |
|---|---|---|
| Layered architecture Controller → Service → Repository | packages `controller`, `service`, `repository` | COMPLETE |
| Separation of concerns, proper packages | controller, service, repository, entity, dto, security, exception, validation, config | COMPLETE |
| Constructor dependency injection | all classes (no field `@Autowired` in main code) | COMPLETE |
| Meaningful names, Java conventions | – | COMPLETE |
| Comments for complex logic | short comments in services, security, entities | COMPLETE |
| No code duplication | shared helpers: `RecipeService.findRecipe/checkCanModify`, `UserService.getUserByEmail`, `ErrorResponse.of`, `AuditInfo.from` | COMPLETE |
| SOLID principles | single-purpose classes, injected abstractions (`PasswordEncoder`, repositories) | COMPLETE |
| All passwords hashed with BCrypt | `AuthService.register`, `DataSeeder` | COMPLETE |
| JWT properly validated | signature, expiration, token type (`JwtService.extractEmail`) | COMPLETE |
| Sensitive endpoints protected | `SecurityConfig` | COMPLETE |
| CORS configuration for frontend | `SecurityConfig.corsConfigurationSource` (`CORS_ALLOWED_ORIGINS`) | COMPLETE (`corsAllowsConfiguredFrontendOnly`) |
| Input sanitization | Bean Validation, trimming/lower-casing in services, parameterized JPA queries, sort whitelist | COMPLETE |
| Environment-specific configuration / profiles | `application.properties`, `application-dev.properties`, `application-test.properties` | COMPLETE |
| Sensitive data in environment variables | `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, `SEED_ADMIN_PASSWORD`, `SEED_USER_PASSWORD`, `CORS_ALLOWED_ORIGINS` | COMPLETE (no secret in the repository: without `JWT_SECRET` a random key is generated; only the documented local test-account passwords have dev-only defaults) |
| Database connection configured | dev: `jdbc:h2:file:./data/recipedb`; test: `jdbc:h2:mem:recipe-test` | COMPLETE |
| Meaningful commit messages, `.gitignore` | `.gitignore` excludes `target/`, `data/`, `.env*`, IDE files | COMPLETE |
| README with setup instructions | `README.md` | COMPLETE |

## 5. Deliverables

| Deliverable | Where | Status |
|---|---|---|
| Complete source code | `src/` | COMPLETE |
| README: description, stack, setup, endpoints, run, tests, credentials | `README.md` | COMPLETE |
| Database schema (optional) | ER diagram in `README.md` §5 (Hibernate generates the tables) | COMPLETE |
| Postman collection (optional) | not included – the demo uses Swagger UI | MISSING (optional) |
| Swagger available when running | http://localhost:8080/swagger-ui.html | COMPLETE |
| Test classes included, tests pass | `src/test/` – 114 passing | COMPLETE |
| Coverage report (optional) | `target/site/jacoco/index.html` after `./mvnw verify` | COMPLETE |
| Defense preparation | `DEFENSE_GUIDE.md` | COMPLETE |

## 6. Verification log (what was actually run)

| Check | Command / method | Result |
|---|---|---|
| Maven compilation | `./mvnw compile` | success |
| Application startup | `./mvnw spring-boot:run` | "Started RecipeManagementApplication in 2.6 seconds" |
| Database initialization | startup log + H2 file created | tables created, 2 accounts + 12 recipes seeded |
| Swagger accessibility | `curl /swagger-ui.html` → 302, `/swagger-ui/index.html` → 200, `/v3/api-docs` → 200 | OK; 5 tags in order Authentication, Recipes, Ingredients, Meal Plans, Admin |
| H2 console (dev) | `curl /h2-console/` | 200 |
| Authentication | register 201, duplicate 409, login 200, wrong password 401, refresh 200, refresh-token-as-access 401, bad token 401 | OK |
| USER / ADMIN authorization | USER → `/api/admin/stats` 403; ADMIN → 200; USER edits admin recipe 403; ADMIN edits user recipe 200; ADMIN reads user's meal plan 403 | OK |
| All CRUD entities | the complete `DEFENSE_GUIDE.md` demo was run with curl on a fresh database | every step returned the documented result |
| Validation & errors | invalid recipe → 7 field errors; past date 400; bad enum 400; bad id 400; unknown URL 404 | OK |
| CORS | preflight from `http://localhost:3000` allowed; from another origin 403 | OK |
| Unit + integration tests | `./mvnw verify` | 114 tests, 0 failures, BUILD SUCCESS |
| JaCoCo coverage | `target/site/jacoco/jacoco.csv` | service 100.0 % lines, overall 86.6 % lines |
| README accuracy | commands, URLs, credentials, sample JSON compared to real responses | OK |
| GitHub push | not done | needs the student's GitHub account and permission |
