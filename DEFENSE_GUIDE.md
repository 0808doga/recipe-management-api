# Defense Guide – Recipe Management API

**Student:** Doğa Şimşek – **ID:** 34489
**Defense:** November 25, 2026 (in person)

This guide tells you exactly what to click and what to say.
Practice it 2–3 times before the defense.

---

## Part A – Before the defense (the night before)

1. Start the app once (see Step 2) and open Swagger. Check that it works.
2. Run the tests once: `./mvnw verify` (see Step 17). It must say `BUILD SUCCESS`.
3. If you want a clean demo, stop the app and delete the old database:
   ```bash
   cd ~/Desktop/recipe-management-api
   rm -rf data
   ```
   The next start creates the test accounts and 12 sample recipes again.
4. Charge your laptop. Close other programs that may use port 8080.
5. Open this file and `README.md` in a second window so you can copy the JSON examples.

---

## Part B – Step-by-step demo instructions

### Step 1 – Open Terminal on your Mac

1. Press `Cmd + Space` (Spotlight opens).
2. Type `Terminal` and press `Enter`.
3. Go to the project folder:
   ```bash
   cd ~/Desktop/recipe-management-api
   ```

### Step 2 – Start the Spring Boot application

```bash
./mvnw spring-boot:run
```

Wait 5–10 seconds. When you see this line, the app is ready:

```
Started RecipeManagementApplication in ... seconds
```

You will also see `Creating local ADMIN account` the first time.
Leave this Terminal window open. To stop the app later press `Ctrl + C`.

### Step 3 – Open Swagger

Open Safari or Chrome and go to:

**http://localhost:8080/swagger-ui.html**

You see 5 groups: **Authentication, Recipes, Ingredients, Meal Plans, Admin**.

How to use any endpoint in Swagger:
1. Click on the endpoint (it opens).
2. Click **Try it out**.
3. Change the JSON or the fields.
4. Click **Execute** (big blue button).
5. Scroll down to **Server response**: you see the status code and the JSON.

### Step 4 – Register a user

Open **Authentication → POST /api/auth/register** → Try it out. Body:

```json
{
  "name": "Doga Simsek",
  "email": "doga@example.com",
  "password": "Password123"
}
```

Execute → status **201**. Response:

```json
{ "id": 3, "name": "Doga Simsek", "email": "doga@example.com", "role": "USER" }
```

Say: *"The password is not returned. In the database it is stored as a BCrypt hash. New users always get the USER role."*

Bonus: click Execute again → **409 Conflict** "A user with email ... already exists".

### Step 5 – Log in

Open **POST /api/auth/login** → Try it out:

```json
{
  "email": "doga@example.com",
  "password": "Password123"
}
```

Execute → **200**. You get `accessToken`, `refreshToken`, `tokenType: "Bearer"`, `expiresIn: 3600`.

Bonus: wrong password → **401** "Invalid email or password".

### Step 6 – Copy the JWT

In the response, find `"accessToken": "eyJ..."`.
Select only the long text **between the quotes** (starts with `eyJ`) and press `Cmd + C`.
Do not copy the quotes.

### Step 7 – Authorize Swagger

1. Scroll to the top of the page. Click the green **Authorize** button (lock icon).
2. In the field **Value** paste the token (`Cmd + V`). Do NOT write `Bearer` – Swagger adds it.
3. Click **Authorize**, then **Close**.

Now the lock icons are closed. Every request sends `Authorization: Bearer <token>`.

To show 401: click **Authorize → Logout**, then call `GET /api/recipes` → **401**. Log in again after that.

### Step 8 – Create a recipe

**Recipes → POST /api/recipes** → Try it out:

```json
{
  "title": "Spaghetti Bolognese",
  "description": "Pasta with meat sauce",
  "instructions": "1. Cook the sauce. 2. Boil the pasta. 3. Mix everything.",
  "preparationTime": 45,
  "servings": 4,
  "ingredients": [
    { "name": "Spaghetti", "quantity": 400, "unit": "g" },
    { "name": "Minced beef", "quantity": 300, "unit": "g" }
  ]
}
```

Execute → **201 Created**. Write down the `id` (on a fresh database it is **13**).

Say: *"The ingredients are saved together with the recipe because of `cascade = CascadeType.ALL`."*

### Step 9 – View recipes

- **GET /api/recipes/{id}** → id = 13 → you see the recipe, its ingredients and the `audit` block.
- **GET /api/recipes** → all recipes, page 0, 10 per page.
- **GET /api/recipes/my** → only your own recipes.
- **GET /api/recipes/search** → keyword = `soup` → "Lentil Soup" and "Tomato Soup".

### Step 10 – Add ingredients

**Ingredients → POST /api/recipes/{recipeId}/ingredients** → recipeId = 13:

```json
{ "name": "Onion", "quantity": 1, "unit": "piece" }
```

Execute → **201**. Write down the ingredient `id`.
Then **GET /api/recipes/{recipeId}/ingredients** → recipeId = 13 → you see 3 ingredients.

### Step 11 – Create a meal plan

**Meal Plans → POST /api/meal-plans**:

```json
{
  "date": "2026-12-01",
  "mealType": "DINNER",
  "recipeId": 13
}
```

Execute → **201**. Then **GET /api/meal-plans** → your meal plans.

Execute the same POST again → **409 Conflict** "You already have a DINNER planned on 2026-12-01"
(business rule: one meal per date and meal type).

### Step 12 – Edit and delete records

**Update the recipe** – **PUT /api/recipes/{id}** → id = 13:

```json
{
  "title": "Spaghetti Bolognese Deluxe",
  "description": "Pasta with rich meat sauce",
  "instructions": "1. Cook the sauce for 1 hour. 2. Boil the pasta. 3. Mix.",
  "preparationTime": 70,
  "servings": 4
}
```

→ **200**. Note: `ingredients` was not sent, so the ingredients stay the same.
Look at `audit.updatedAt` – it changed.

**Update an ingredient** – **PUT /api/recipes/{recipeId}/ingredients/{ingredientId}**:

```json
{ "name": "Red onion", "quantity": 2, "unit": "piece" }
```

**Update the meal plan** – **PUT /api/meal-plans/{id}** (id from Step 11):

```json
{ "date": "2026-12-01", "mealType": "LUNCH", "recipeId": 13 }
```

**Delete** – in this order:
1. **DELETE /api/recipes/{recipeId}/ingredients/{ingredientId}** → **204 No Content**
2. **DELETE /api/meal-plans/{id}** → **204**
3. (optional) **DELETE /api/recipes/{id}** → **204**. Then **GET /api/recipes/13** → **404 Not Found**.

Say: *"Deleting a recipe also deletes its ingredients (cascade + orphanRemoval) and the meal plans that use it."*

### Step 13 – Demonstrate pagination

**GET /api/recipes**:
- page = `0`, size = `5`
- sort: change the first item to `preparationTime,asc`, click **Add string item**, type `title,asc`.

Execute. Show in the response:

```json
"page": 0, "size": 5, "totalElements": 13, "totalPages": 3, "first": true, "last": false,
"sort": "preparationTime: ASC,title: ASC"
```

(`totalElements` is 13 = 12 sample recipes + yours; it is 12 if you deleted your recipe in Step 12.)

Change page to `1` → next 5 recipes. Change page to `2` → `"last": true`.

Show protection: sort = `password` → **400** "Cannot sort by 'password'. Allowed sort fields: ..."

### Step 14 – Demonstrate validation errors

**POST /api/recipes** with bad data:

```json
{
  "title": "A",
  "instructions": "",
  "preparationTime": 0,
  "servings": 100,
  "ingredients": [
    { "name": "Egg", "quantity": 2.5, "unit": "piece" },
    { "name": "Water", "quantity": -1, "unit": "bucket" }
  ]
}
```

→ **400** with a list of all problems:

```json
"validationErrors": {
  "title": "Title must be between 3 and 100 characters",
  "instructions": "Instructions are required",
  "preparationTime": "Preparation time must be at least 1 minute",
  "servings": "Servings cannot be more than 50",
  "ingredients[0].quantity": "Quantity must be a whole number for countable units (piece, slice, clove)",
  "ingredients[1].quantity": "Quantity must be greater than 0",
  "ingredients[1].unit": "Unit must be one of: g, kg, ml, l, tsp, tbsp, cup, piece, pinch, slice, clove"
}
```

Say: *"`ingredients[0].quantity` comes from my custom cross-field validator `@ValidIngredientQuantity`, and the unit message comes from my custom validator `@ValidUnit`."*

More examples:
- Meal plan with a past date `"2025-01-01"` → 400 "Meal plan date cannot be in the past".
- Meal plan with `"mealType": "BRUNCH"` → 400 "Allowed values: [BREAKFAST, LUNCH, DINNER, SNACK]".
- **GET /api/recipes/{id}** with id = `9999` → 404.

### Step 15 – Demonstrate audit information

1. **GET /api/recipes/13** → look at:
   ```json
   "audit": {
     "createdAt": "2026-11-25T10:15:30",
     "createdBy": "doga@example.com",
     "updatedAt": "2026-11-25T10:20:00",
     "lastModifiedBy": "doga@example.com"
   }
   ```
2. Log in as **admin** (Step 16), then **PUT /api/recipes/1** (a recipe of the demo user) with any valid body.
   Now `createdBy` = `system` (created by the data seeder) and `lastModifiedBy` = `admin@recipes.local`.

Say: *"I did not write any code in the services to set these fields. Spring Data JPA fills them automatically because the entity extends `AuditableEntity`."*

### Step 16 – Demonstrate USER and ADMIN permissions

Test accounts (created automatically in the dev profile):

| Role | Email | Password |
|---|---|---|
| ADMIN | `admin@recipes.local` | `Admin12345` |
| USER | `user@recipes.local` | `User12345` |

**As USER** (log in as `user@recipes.local`, Authorize with the new token):
1. **GET /api/admin/stats** → **403 Forbidden** "You do not have permission to access this resource."
2. **PUT /api/recipes/8** (recipe 8 belongs to the admin) → **403** "You can only modify your own recipes".

**As ADMIN** (log in as `admin@recipes.local`, Authorize → Logout → paste the admin token):
1. **GET /api/admin/stats** → **200** with numbers.
2. **GET /api/admin/users** → list of users (no passwords).
3. **PUT /api/admin/users/{id}/role** → id = 3, body `{ "role": "ADMIN" }` → user 3 is now ADMIN.
4. **PUT /api/admin/users/{id}/role** → id = 1 (yourself) → **400** "You cannot change your own role".
5. Admin can edit any recipe (moderation), but **cannot** see other users' meal plans → 403 (meal plans are private).

### Step 17 – Run the tests

Open a **second** Terminal window (`Cmd + N` in Terminal), then:

```bash
cd ~/Desktop/recipe-management-api
./mvnw verify
```

At the end you see:

```
Tests run: 114, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

Open the coverage report:

```bash
open target/site/jacoco/index.html
```

Show: service package 100% lines, total 86.6% lines (goal was 70% and 60%).

---

## Part C – Defense script (25–35 minutes)

### 1. Introduction (1 minute)

> "Hello, my name is Doğa Şimşek, student ID 34489. My project is a Recipe Management API.
> It is a backend-only REST API built with Java 17 and Spring Boot 3.
> Users can register, log in with JWT, manage recipes and ingredients, and plan their meals.
> I chose this topic because everybody cooks, so the data is easy to understand,
> and it naturally has relationships: a user has recipes, a recipe has ingredients,
> and a meal plan connects a user, a date and a recipe."

### 2. Demonstration (12–15 minutes)

Follow Part B in this order (about 1 minute each):

| Time | What | Steps |
|---|---|---|
| 0:00 | Start app, open Swagger | 2, 3 |
| 1:00 | Register, duplicate (409), login, wrong password (401) | 4, 5 |
| 3:00 | Authorize, show 401 without token | 6, 7 |
| 4:00 | Create recipe with ingredients, view it | 8, 9 |
| 6:00 | Add ingredient, meal plan, duplicate meal plan (409) | 10, 11 |
| 8:00 | Update and delete | 12 |
| 9:30 | Pagination + sorting | 13 |
| 10:30 | Validation errors | 14 |
| 11:30 | Audit information | 15 |
| 12:30 | USER vs ADMIN | 16 |
| 14:00 | Tests and coverage | 17 |

### 3. Code walkthrough (10 minutes)

Open the project in IntelliJ IDEA or VS Code and show these files in this order:

1. **Package structure** (1 min) – "I use a layered architecture: controller → service → repository → database. Each package has one job."
2. **`entity/Recipe.java`** (1.5 min) – `@Entity`, `@ManyToOne owner`, `@OneToMany ingredients` with `cascade = ALL` and `orphanRemoval = true`, `@Index` on title. "It extends `AuditableEntity`."
3. **`entity/AuditableEntity.java`** (1 min) – `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`. Then **`config/JpaAuditingConfig.java`**: the `AuditorAware` reads the logged-in user's e-mail.
4. **`controller/RecipeController.java`** (1 min) – thin controller: `@Valid` checks the DTO, `Principal` gives the logged-in user, the work is done in the service. Show `@PageableDefault`.
5. **`service/RecipeService.java`** (1.5 min) – `checkCanModify` (owner or ADMIN), `validateSortFields` (only allowed fields), `checkNoDuplicateIngredientNames`, `saveAndFlush` for fresh audit values.
6. **`dto/RecipeRequest.java` + `validation/IngredientQuantityValidator.java`** (1 min) – annotations and my custom validator.
7. **`config/SecurityConfig.java`** (1 min) – public endpoints, `/api/admin/**` only ADMIN, stateless sessions, JWT filter, BCrypt, CORS.
8. **`security/JwtAuthenticationFilter.java` + `JwtService.java`** (1 min) – read `Authorization` header, check signature + expiration + token type, set the user in `SecurityContextHolder`.
9. **`exception/GlobalExceptionHandler.java`** (0.5 min) – one place for all errors, same JSON format.
10. **Tests** (0.5 min) – `RecipeServiceTest` (Mockito) and `RecipeControllerIntegrationTest` (MockMvc, full app).

**Challenges you can mention:**
- *Audit date in the update response was old.* Hibernate fills `updatedAt` only when it writes to the database. Solution: `saveAndFlush`.
- *Deleting a recipe that is used in a meal plan failed (foreign key).* Solution: delete the meal plans for that recipe first in `RecipeService.deleteRecipe`.
- *Sorting by any field is dangerous* (e.g. `owner.password`). Solution: a whitelist of sortable fields.
- *Security errors returned HTML/empty bodies.* Solution: `SecurityErrorHandler` writes the same JSON error format for 401 and 403.

### 4. Questions (5–10 minutes)

See Part E below.

---

## Part D – Main classes explained in easy English

| Class | What it does |
|---|---|
| `RecipeManagementApplication` | The `main` method. Starts Spring Boot and the web server on port 8080. |
| `User` | A person who uses the app. Has name, e-mail, password hash and role. |
| `Recipe` | A recipe. Belongs to one user (owner). Has many ingredients. |
| `Ingredient` | One line of a recipe, e.g. 200 g flour. Belongs to one recipe. |
| `MealPlan` | "On this date, for this meal, I cook this recipe." Belongs to one user. |
| `AuditableEntity` | Parent class with the 4 audit fields. Recipe, Ingredient and MealPlan extend it. |
| `UserRepository`, `RecipeRepository`, ... | Interfaces. Spring Data JPA writes the SQL for us from method names like `findByEmail`. |
| `AuthService` | `register` (checks duplicate e-mail, hashes password), `login` (checks password, creates tokens), `refreshToken`. |
| `RecipeService` | Create/read/update/delete recipes, search, pagination. `checkCanModify` = only owner or admin. |
| `IngredientService` | CRUD for ingredients of a recipe. Uses `RecipeService.checkCanModify`. |
| `MealPlanService` | CRUD for meal plans. `findOwnMealPlan` = only the owner can see it. `checkSlotIsFree` = no double booking. |
| `AdminService` | List users, change role, statistics. |
| `UserService` | Finds a user by e-mail (used by the other services). |
| `JwtService` | Creates tokens (`generateAccessToken`) and reads/validates them (`extractEmailFromAccessToken`). |
| `JwtAuthenticationFilter` | Runs before every request. Reads the token from the header and logs the user in for this request. |
| `CustomUserDetailsService` | Loads a user from the database for Spring Security and gives the role `ROLE_USER` or `ROLE_ADMIN`. |
| `SecurityErrorHandler` | Writes JSON for 401 (not logged in / bad token) and 403 (wrong role). |
| `SecurityConfig` | Security rules: which URL is public, which needs a token, which needs ADMIN. BCrypt and CORS. |
| `GlobalExceptionHandler` | Catches exceptions from all controllers and turns them into JSON with the right status code. |
| `ValidUnitValidator` | Custom validator: unit must be in the allowed list. |
| `IngredientQuantityValidator` | Custom validator: for piece/slice/clove the quantity must be a whole number. |
| `OpenApiConfig` | Swagger title, groups (tags) and the JWT "Authorize" button. |
| `JpaAuditingConfig` | Turns on auditing and tells Spring who the current user is. |
| `DataSeeder` | In the dev profile: creates the admin, the user and 12 sample recipes if they do not exist. |
| DTOs (`RecipeRequest`, `RecipeResponse`, ...) | Simple Java `record`s. Requests carry validation rules. Responses never contain passwords. |

### What happens when I call `POST /api/recipes`? (learn this flow!)

1. The request arrives with the header `Authorization: Bearer eyJ...`.
2. `JwtAuthenticationFilter` checks the token with `JwtService` and puts the user into the `SecurityContext`.
3. `SecurityConfig` rules: `/api/recipes` needs a logged-in user → OK.
4. Spring turns the JSON into a `RecipeRequest` and `@Valid` runs all validation annotations.
   If something is wrong → `MethodArgumentNotValidException` → `GlobalExceptionHandler` → 400.
5. `RecipeController.createRecipe` calls `RecipeService.createRecipe(request, principal.getName())`.
6. The service loads the user, creates a `Recipe` entity, adds the ingredients, checks for duplicate names.
7. `recipeRepository.save(recipe)` → Hibernate runs `INSERT` for the recipe and (cascade) for the ingredients.
   Auditing fills `createdAt`, `createdBy`, ...
8. The service converts the entity into `RecipeResponse` (DTO) and the controller returns **201 Created**.

---

## Part E – Likely professor questions and short answers

### Java and Spring Boot

**Why Java and Spring Boot?**
Java is strongly typed and widely used for backends. Spring Boot gives ready parts (web server, security, database) with little configuration, so I can focus on business logic.

**What is Spring Boot?**
A framework on top of Spring. It has auto-configuration and an embedded Tomcat server, so the app starts with one `main` method.

**What is a `record`?**
A short Java class for data. Java creates the constructor, getters, `equals` and `toString` for it. I use records for DTOs.

### REST API and CRUD

**What is a REST API?**
An API over HTTP. Each URL is a resource (`/api/recipes`), and the HTTP method says what to do: GET read, POST create, PUT update, DELETE delete. Data is sent as JSON.

**What is CRUD?**
Create, Read, Update, Delete. I have full CRUD for Recipe, Ingredient and MealPlan.

**Which status codes do you use?**
200 OK, 201 Created, 204 No Content (after delete), 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 405 Method Not Allowed, 409 Conflict, 500 Internal Server Error.

**What is the difference between 401 and 403?**
401: I don't know who you are (no token or bad token). 403: I know who you are, but you are not allowed.

### JPA, H2 and relationships

**What is JPA / Hibernate?**
JPA is the Java standard for mapping classes to database tables. Hibernate is the implementation. `@Entity` = table, field = column.

**What is H2? Why H2?**
A small Java database. The professor allows it for development and testing. In dev I use a file (`./data/recipedb`) so data survives restarts; in tests I use memory so every run is clean. Changing to PostgreSQL only needs another driver and URL.

**Explain your relationships.**
User 1 — N Recipe (`@ManyToOne owner`). Recipe 1 — N Ingredient (`@OneToMany` + `@ManyToOne recipe`). User 1 — N MealPlan, Recipe 1 — N MealPlan.

**What does `cascade = CascadeType.ALL` do?**
Operations on the recipe are also done on its ingredients: save recipe → ingredients are saved, delete recipe → ingredients are deleted.

**What is `orphanRemoval`?**
If I remove an ingredient from the recipe's list, Hibernate deletes it from the database.

**Why `FetchType.LAZY`?**
The related object is only loaded when I use it. This avoids loading too much data.

**Which constraints and indexes do you have?**
Unique e-mail, unique (owner, date, meal type) for meal plans, NOT NULL columns, foreign keys. Indexes on recipe title, ingredient name and foreign key columns to make search faster.

**What is a repository?**
An interface that extends `JpaRepository`. Spring creates the implementation. Example: `findByEmail` becomes `SELECT ... WHERE email = ?`.

### DTO and validation

**What is a DTO and why do you use it?**
Data Transfer Object. It is what the API receives or returns. I never return entities: the password hash stays hidden, I control the JSON shape, and I avoid lazy loading problems.

**How does validation work?**
Annotations on request DTOs (`@NotBlank`, `@Size`, `@Min`, `@Max`, `@Positive`, `@Email`, `@FutureOrPresent`). `@Valid` in the controller starts the check. Errors go to `GlobalExceptionHandler` → 400 with field messages.

**Show me a custom validator.**
`@ValidUnit` + `ValidUnitValidator` (allowed units) and `@ValidIngredientQuantity` + `IngredientQuantityValidator` (cross-field: whole number for piece/slice/clove).

**What is the difference between bean validation and business validation?**
Bean validation checks one request alone (format, size). Business validation needs the database or rules, e.g. "this meal slot is already used" or "duplicate ingredient name". That is done in the service.

### Exception handling

**How do you handle errors?**
`@RestControllerAdvice` class `GlobalExceptionHandler` with `@ExceptionHandler` methods. Each exception type becomes a status code. All errors use the same `ErrorResponse` JSON. Unknown errors become 500 with a general message; details are only in the log.

**Why is 401/403 handled in another class?**
Security errors happen in filters, before the controller. So `SecurityErrorHandler` (AuthenticationEntryPoint + AccessDeniedHandler) writes them, with the same JSON format.

### Security, JWT, BCrypt

**What is JWT?**
JSON Web Token. Three parts: header, payload (e-mail, role, type, expiration), signature. The server signs it with a secret key. If someone changes the token, the signature check fails.

**How does your login work?**
`AuthService.login` finds the user, `passwordEncoder.matches` checks the password against the BCrypt hash, then `JwtService` creates an access token (60 min) and a refresh token (7 days).

**How is the token checked?**
`JwtAuthenticationFilter` reads the `Authorization: Bearer` header. `JwtService` checks signature, expiration and that the type is `access`. Then the user is loaded and put into the `SecurityContext`.

**What happens with an expired token?**
`ExpiredJwtException` → the filter saves the message → 401 "JWT token has expired. Please log in again." The client can use `/api/auth/refresh`.

**Why a refresh token?**
Access tokens are short so a stolen token is useful only for a short time. The refresh token gets a new access token without typing the password again. A refresh token cannot be used as an access token (type check).

**What is BCrypt?**
A slow hashing algorithm with a random salt. The same password gives different hashes, and it is very slow to brute force. We never store or log the plain password.

**Where is the JWT secret?**
In the environment variable `JWT_SECRET`. No secret is written in the code or in the properties files. If `JWT_SECRET` is not set, `JwtService` creates a random key at startup (then tokens stop working after a restart). The app refuses to start if the secret is shorter than 32 characters.

**How do roles work?**
`Role` enum USER/ADMIN in the database. `CustomUserDetailsService` gives `ROLE_USER` / `ROLE_ADMIN`. `SecurityConfig` says `/api/admin/**` → `hasRole("ADMIN")`, and `AdminController` also has `@PreAuthorize("hasRole('ADMIN')")`.

**How do you stop a user changing another user's recipe?**
`RecipeService.checkCanModify` compares the recipe owner id with the current user id. If different and not ADMIN → `AccessDeniedException` → 403.

**Why is CSRF disabled?**
CSRF attacks use cookies that the browser sends automatically. My API uses a token in a header, not cookies, and it is stateless. So CSRF protection is not needed.

**What is CORS?**
A browser rule. Only the frontend URLs in `app.cors.allowed-origins` may call the API from JavaScript. Other origins get 403.

**What about SQL injection?**
Spring Data JPA uses parameters (`?`), never string concatenation. Sort fields are checked against a whitelist. Input is validated and trimmed.

### Architecture

**What are controller, service and repository?**
Controller: receives HTTP requests and returns responses. Service: business logic and rules. Repository: talks to the database. Each layer only talks to the next one.

**What is dependency injection?**
Spring creates the objects (beans) and gives them to the classes that need them. I use constructor injection: dependencies are `final` and passed in the constructor. This makes testing easy because I can pass mocks.

**What does `@Transactional` do?**
All database work in the method is one unit. If an exception happens, everything is rolled back. `readOnly = true` for read methods.

**Which SOLID principles did you use?**
Single responsibility (each class has one job, e.g. `JwtService` only handles tokens), dependency inversion (classes depend on injected beans, e.g. `PasswordEncoder` interface), open/closed (new validators are added as new annotations without changing old code).

### Swagger

**What is Swagger / OpenAPI?**
OpenAPI is a standard description of the API. springdoc creates it from my controllers and annotations (`@Operation`, `@ApiResponse`, `@Schema(example=...)`). Swagger UI shows it as a web page where you can try every endpoint.

### Testing

**What is a unit test?**
It tests one class alone. In `RecipeServiceTest` I use Mockito: repositories are mocks (`@Mock`), I define what they return (`when(...).thenReturn(...)`), and I check the result or `verify` calls. No database, very fast.

**What is an integration test?**
It tests many parts together. `@SpringBootTest` + `@AutoConfigureMockMvc` starts the full app with an in-memory H2. `MockMvc` sends real HTTP requests through security, validation, controller, service and database.

**How many tests and what coverage?**
114 tests, all passing. JaCoCo: service layer 100% lines, overall 86.6% lines. `./mvnw verify` fails if coverage drops below 70% (service) or 60% (overall).

**What is `@DataJpaTest`?**
Starts only the JPA part with an in-memory database to test repositories (my search query, paging, cascade delete).

### Pagination and audit

**How does pagination work?**
The controller receives a `Pageable` (page, size, sort) from the URL. The repository returns a `Page` with the data and the total count (Spring runs a `COUNT` query). I convert it to my `PageResponse` DTO. Max page size is 50.

**How do you sort by multiple fields?**
Send `sort` more than once: `sort=preparationTime,asc&sort=title,asc`. Spring creates one `Sort` with two orders → `ORDER BY preparation_time ASC, title ASC`.

**How does the audit trail work?**
`@EnableJpaAuditing` + `AuditingEntityListener`. Before insert/update, Spring sets the dates and asks my `AuditorAware` bean for the current user (e-mail from the `SecurityContext`, or `system`). The values are shown in the `audit` part of each response.

**Do you keep a full change history?**
No. I keep who/when created and who/when last changed each record. A full history (old values) would need an extra history table; I kept it simple on purpose.

### Why this topic?

**Why did you choose a Recipe Management API?**
It is one of the professor's suggested topics, it is different from my mobile projects, and it is easy to explain. It still has real backend problems: ownership of data, relationships with cascade, validation of quantities and units, private data (meal plans), search and paging.

---

## Part F – What to study before the defense (checklist)

- [ ] I can start the app and run the full demo without reading (practice 2–3 times).
- [ ] I can draw the 4 tables and their relationships on paper.
- [ ] I can explain the request flow of `POST /api/recipes` (Part D).
- [ ] I can explain how JWT login and the filter work (`AuthService`, `JwtService`, `JwtAuthenticationFilter`).
- [ ] I can explain 401 vs 403 and show both.
- [ ] I can explain cascade + orphanRemoval in `Recipe.java`.
- [ ] I can explain my two custom validators.
- [ ] I can explain how `@CreatedBy` gets the user (`JpaAuditingConfig`).
- [ ] I can explain one unit test (`RecipeServiceTest.otherUserCannotUpdateRecipe`) and one integration test (`RecipeControllerIntegrationTest.listRecipesWithPaginationAndMultipleSortFields`).
- [ ] I know the test numbers: 114 tests, service 100%, overall 86.6% line coverage.
