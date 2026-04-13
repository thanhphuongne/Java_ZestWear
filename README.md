Java_ZestWear — Spring Boot + Next.js e-commerce (dev)

Quick start (development):

- Backend (dev):
  - Run from IDE or use Maven:

```powershell
cd backend
mvn spring-boot:run
```

- Frontend (dev):

```powershell
cd frontend
npm install
npm run dev
```

Run with Docker Compose (production-like):

```powershell
docker-compose build
docker-compose up
```

Configuration:
- Backend: `backend/src/main/resources/application.properties`
  - `server.port` (default 8081)
  - `app.jwt.secret` - change this in production
  - `app.jwt.expiration-minutes` - access token lifetime (minutes)

Notes:
- Auth now uses JWT access tokens (short-lived) + refresh tokens (stored server-side).
- Dev seeder creates an admin user: `admin@zestwear.test` / `admin123` (refresh token seeded).
- The frontend expects `NEXT_PUBLIC_API_URL` to point to the backend API (e.g. `http://localhost:8081/api`).

Payments (Stripe)
- This project includes a Stripe integration. Configure the following (in `application.properties` or env vars):
  - `app.stripe.secret` — your Stripe secret key (e.g. `sk_test_...`).
  - `app.stripe.webhook-secret` — webhook signing secret for `checkout.session.completed` events.
- Endpoints:
  - `POST /api/payments/create-link` — body: `{ "orderId": 123, "successUrl": "...", "cancelUrl": "..." }` returns `{ checkoutUrl }` for a Stripe Checkout session.
  - `POST /api/payments/webhook` — Stripe webhook endpoint (use Stripe CLI or configure in dashboard). The webhook will mark orders as `PAID` on successful completion.

Developer notes:
- Use the Stripe CLI to forward webhooks during local development:

```powershell
stripe listen --forward-to localhost:8081/api/payments/webhook
```

Configure `app.stripe.secret` in `backend/src/main/resources/application.properties` for simple local testing, or set the `STRIPE_API_KEY` and `STRIPE_WEBHOOK_SECRET` env vars when running in Docker/CI.

If you want, I can: run a local build, start the services, or add automated tests next.
