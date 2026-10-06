<div align="center">

# 🛒 E-Commerce Platform Backend

**Secure, role-based e-commerce REST API with Stripe payments, built on Spring Boot**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens)
![Stripe](https://img.shields.io/badge/Payments-Stripe-635BFF?logo=stripe&logoColor=white)
![Swagger](https://img.shields.io/badge/Docs-Swagger%20UI-85EA2D?logo=swagger&logoColor=black)

</div>

---

## 📌 Overview

A backend-only e-commerce application where **customers** shop, **sellers** list products and **admins** moderate the platform. It covers the full order lifecycle: **cart → order → Stripe checkout → webhook confirmation → email**, with automatic stock handling, cancellations and refunds.

No frontend needed: every API can be tested from **Swagger UI**.

---

## ✨ Features

| | Feature |
|---|---|
| 🔐 | **JWT authentication** (stateless, 1-hour tokens) with **BCrypt** password hashing |
| 👥 | **Role-based access**: `CUSTOMER`, `SELLER`, `ADMIN` (URL rules + `@PreAuthorize`) |
| 🛍️ | **Products**: seller CRUD, category filtering (Sports, Fashion, Technology, Grocery, Snacks, Skincare) |
| 🛒 | **Cart**: add / remove items, clear cart |
| 📍 | **Addresses**: add, update, delete, set default |
| 📦 | **Orders**: place, cancel, with multi-item order support |
| 💳 | **Stripe Checkout** (INR) with **signed webhooks** for payment confirmation, expiry and refunds |
| 🔁 | **Refunds**: cancel a paid order and it is refunded through Stripe |
| 📉 | **Safe stock handling**: atomic stock reduction at order time, restored on cancel / failure |
| ⏱️ | **Scheduler**: unpaid orders auto-expire after 10 minutes and stock is released |
| ⭐ | **Reviews**: add, update, delete, search by keyword or product |
| 📧 | **Async emails** (JavaMail): registration, order confirmed, cancelled, refunded |
| 🧯 | **Global exception handling** with clean, consistent error responses |
| 📖 | **Swagger / OpenAPI** documentation |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.8, Spring MVC |
| Security | Spring Security, JWT (jjwt 0.13), BCrypt |
| Database | MySQL, Spring Data JPA (Hibernate) |
| Payments | Stripe Java SDK (Checkout + Webhooks) |
| Email | Spring Mail (JavaMail) |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Utilities | Lombok, Jakarta Validation |

---

## 🏗️ Architecture

Classic layered design: each layer has one job.

```mermaid
flowchart LR
    C[Client / Swagger UI] --> F[JWT Filter<br/>+ Security Rules]
    F --> CT[Controller]
    CT --> S[Service<br/>Business Logic]
    S --> R[Repository<br/>Spring Data JPA]
    R --> DB[(MySQL)]
    S --> ST[Stripe API]
    S --> M[Mail Server]
    ST -. webhook .-> WH[Webhook Controller]
    WH --> S
```

---

## 🔑 Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as AuthController
    participant DB as MySQL
    participant F as JwtFilter

    U->>A: POST /api/v1/auth/register/customer (or /seller)
    A->>DB: Save user (BCrypt-hashed password)
    A-->>U: Registered + welcome email
    U->>A: POST /api/v1/auth/login
    A->>DB: Verify credentials
    A-->>U: JWT (email + role, valid 1 hour)
    U->>F: Any request with Authorization: Bearer token
    F->>F: Validate token, set role in SecurityContext
    F-->>U: Access allowed or 401 / 403
```

---

## 🛒 Order & Payment Flow

```mermaid
flowchart TD
    A[Customer adds items to cart] --> B[POST /orders]
    B --> C{Address, cart and<br/>no pending payment?}
    C -- No --> X[Error response]
    C -- Yes --> D[Reduce stock atomically]
    D --> E{Stock available?}
    E -- No --> X
    E -- Yes --> F[Save order: CREATED / PENDING]
    F --> G[Create Stripe Checkout Session]
    G --> H[Return checkout URL]
    H --> I[Customer pays on Stripe]

    I -- paid --> J[Webhook: checkout.session.completed]
    J --> K[Order CONFIRMED<br/>Cart cleared<br/>Confirmation email]

    I -- not paid --> L[Webhook: checkout.session.expired<br/>or 10-min scheduler]
    L --> M[Order cancelled / FAILED<br/>Stock restored]

    K --> N[Cancel within 30 min?]
    N -- Yes --> O[Stripe refund]
    O --> P[Webhook: charge.refunded]
    P --> Q[Order REFUNDED<br/>Stock restored<br/>Refund email]
```

### Order status lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> CONFIRMED: payment success
    CREATED --> FAILED: not paid in 10 min
    CREATED --> CANCELLED: cancelled before payment
    CONFIRMED --> REFUND_INITIATED: cancelled after payment
    REFUND_INITIATED --> REFUNDED: Stripe refund done
```

---

## 🗄️ Database Design

```mermaid
erDiagram
    CUSTOMER ||--|| CART : has
    CUSTOMER ||--o{ ADDRESS : saves
    CUSTOMER ||--o{ ORDER : places
    CUSTOMER ||--o{ REVIEW : writes
    SELLER ||--o{ PRODUCT : lists
    CART ||--o{ CART_ITEM : contains
    PRODUCT ||--o{ CART_ITEM : "added as"
    PRODUCT ||--o{ ORDER_ITEM : "ordered as"
    PRODUCT ||--o{ REVIEW : receives
    ORDER ||--o{ ORDER_ITEM : contains
    ORDER }o--|| ADDRESS : "ships to"
```

---

## 🔌 API Endpoints

Base path: `/api/v1`. Full interactive docs are in Swagger UI.

### 🔓 Auth (public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register/customer` | Register as customer |
| POST | `/auth/register/seller` | Register as seller |
| POST | `/auth/login` | Login and receive JWT |

### 🛍️ Products
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/products` | Customer / Seller | List products (filter by category) |
| GET | `/products/{id}` | Customer / Seller | Product details |
| POST | `/products` | Seller | Add product |
| PUT | `/products/{id}` | Seller | Update product |
| DELETE | `/products/{id}` | Seller | Delete product |

### 🛒 Cart, 📍 Address, 📦 Orders (Customer)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/cart/items` | Add item to cart |
| GET | `/cart` | View cart |
| DELETE | `/cart/items/{productId}` | Remove an item |
| DELETE | `/cart/clear` | Clear cart |
| POST | `/address/add` | Add address |
| GET | `/address` | List addresses |
| PUT | `/address/{id}` | Update address |
| PATCH | `/address/{id}/default` | Set default address |
| DELETE | `/address/{id}` | Delete address |
| POST | `/orders` | Place order (returns Stripe checkout URL) |
| DELETE | `/orders/{id}` | Cancel order (refund if already paid) |

### ⭐ Reviews
| Method | Endpoint | Description |
|---|---|---|
| GET | `/review/search?word=` | Search reviews by keyword |
| GET | `/review/search/{productId}` | Reviews of a product |
| GET | `/review/me` | My reviews |
| POST | `/review/products/{productId}` | Add review |
| PUT | `/review/{id}` | Update review |
| DELETE | `/review/{id}` | Delete review |

### 👤 Profile & 🛡️ Admin
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET / PUT / DELETE | `/customer/me` | Customer | View, update, delete own account |
| GET | `/seller/profile` | Seller | Seller profile |
| GET | `/admin/customers`, `/sellers`, `/products` | Admin | List everything |
| DELETE | `/admin/customers/{id}`, `/sellers/{id}` | Admin | Remove a user |
| POST | `/api/payments/webhook` | Stripe | Payment events (signature verified) |

---

## 🗂️ Project Structure

```
src/main/java/com/example/demo
├── controller/        REST endpoints
├── service/           Business logic
├── repository/        JPA repositories
├── model/             JPA entities
├── dto/               Request / response objects
├── transformers/      Entity <-> DTO mapping
├── security/          JWT filter, JWT utils, auth, user details
├── stripe/            Stripe service + webhook controller
├── configuration/     Stripe config, default admin initializer
├── enums/             Role, Category, OrderStatus, PaymentStatus, Gender
├── exception/         Custom exceptions + global handler
└── Utility/           Email, validation, order cleanup scheduler
```

---

## ⚙️ Getting Started

### Prerequisites
Java 21 · Maven · MySQL · a Stripe account (test mode) · an SMTP account

### 1. Clone
```bash
git clone https://github.com/dhruvkhurana1626/ecom-platform-backend.git
cd ecom-platform-backend
```

### 2. Set environment variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PAASWORD` | MySQL connection (the password variable is spelled `DB_PAASWORD` in the properties file) |
| `jwt_secret_key` | Secret used to sign JWTs |
| `STRIPE_SECRET_KEY` | Stripe API secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL` | SMTP host, username, password |

### 3. Run
```bash
./mvnw spring-boot:run
```
The app starts on **http://localhost:8081** (prod profile).

### 4. Open Swagger UI
👉 **http://localhost:8081/swagger-ui/index.html**

### 5. Test payments locally
```bash
stripe listen --forward-to localhost:8081/api/payments/webhook
```
Copy the printed `whsec_...` value into `STRIPE_WEBHOOK_SECRET`.

---

## 🧪 Quick Test Flow

1. Register a **seller**, log in, and add a product.
2. Register a **customer**, log in, and add an address.
3. Add the product to the cart and call `POST /orders`.
4. Open the returned Stripe checkout URL and pay with test card `4242 4242 4242 4242`.
5. Check the order is `CONFIRMED` and the confirmation email arrives.

---

## 🚀 Future Improvements
- Razorpay support alongside Stripe
- Pagination and sorting on product listing
- Refresh tokens
- Docker + docker-compose setup
- Unit and integration test coverage

---

## 👤 Author

**Dhruv Khurana**
[GitHub](https://github.com/dhruvkhurana1626)
