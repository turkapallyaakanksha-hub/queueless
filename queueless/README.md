# QueueLess: Smart Campus Queue & Token Management

Students take a digital token for a campus counter (canteen, stationery, college office, ID card section...)
instead of standing in line. They see their token number, who is being served, how many people are ahead
and an estimated wait. Admins call and complete tokens from a dashboard.

**Stack:** React (Vite) + Spring Boot 3 (Java 17) + MySQL 8. Data flows React -> Spring Boot REST API -> MySQL and back.

```
queueless/
  backend/    Spring Boot API (Maven)
  frontend/   React app (Vite)
  Dockerfile, docker-compose.yml   one-command run and deployment
```

## 1. Run it on your computer (VS Code)

### Install once
- Java 17 or newer (JDK) and Maven 3.9+
- Node.js 18 or newer
- MySQL 8 running locally (or use the Docker option below)
- VS Code extensions: "Extension Pack for Java" and "Spring Boot Extension Pack" (VS Code will suggest them)

### Steps
1. Open the `queueless` folder in VS Code (File > Open Folder).
2. **Database:** start MySQL. The default settings expect user `root`, password `root` on `localhost:3306`.
   The `queueless` database is created automatically. If your MySQL login is different, set it in
   `backend/src/main/resources/application.properties` or via environment variables `DB_USERNAME` and `DB_PASSWORD`.
3. **Back end** (terminal 1):
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   It starts on http://localhost:8080. Tables are created automatically and sample data is added on the first run.
4. **Front end** (terminal 2):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173. (Vite forwards `/api` calls to port 8080, so no extra setup is needed.)

Tip: Terminal > Run Task > "Run both" starts both from VS Code.

### Sample accounts (created on first run)
| Role    | Email                  | Password     |
|---------|------------------------|--------------|
| Admin   | admin@queueless.edu    | Admin@123    |
| Student | student@queueless.edu  | Student@123  |

New students can also register on the site. Admin accounts cannot be created from the public form.

### Try it
1. Register two student accounts (use two browsers or a private window).
2. Each student takes a token for the same counter. Tokens are numbered #1, #2... per counter per day.
3. Sign in as admin, open **Admin**, and press **Call next**. The student's ticket updates within a few seconds.
4. Press **Complete** when done. The next **Call next** moves to the following token.

## 2. Run everything with Docker (easiest, includes MySQL)

```bash
docker compose up --build
```
Open http://localhost:8080. This builds the React app, bundles it into the Spring Boot jar, and starts MySQL.

## 3. Deploy

The Dockerfile produces one image that serves both the React site and the API, so you only deploy one app plus a MySQL database.

**Option A: Render / Railway / Fly.io (container hosting)**
1. Push the project to GitHub.
2. Create a hosted MySQL 8 database (several providers offer one, for example Railway or Aiven; check which free tiers are currently available). Copy its host, user and password.
3. Create a web service from the repository using the root `Dockerfile`.
4. Set these environment variables:

| Variable | Meaning |
|----------|---------|
| `DB_URL` | e.g. `jdbc:mysql://HOST:3306/queueless?useSSL=true` |
| `DB_USERNAME`, `DB_PASSWORD` | database login |
| `JWT_SECRET` | long random string, 32+ characters |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first admin account (change the defaults!) |
| `APP_TIMEZONE` | your campus time zone, e.g. `Asia/Kolkata` (decides when token numbers reset each day) |
| `SEED_DEMO_DATA` | `false` for a real launch (skips the demo student and sample history) |

The platform provides `PORT` automatically.

**Option B: a VPS or college server**
Install Docker, copy the project, copy `.env.example` to `.env` with real values, then run `docker compose up -d --build`.
Put Nginx or Caddy in front for HTTPS.

**Option C: React and Spring Boot on separate hosts** (for example Netlify/Vercel + Render)
- Build the React app with `VITE_API_URL=https://your-api-host npm run build` and host `frontend/dist`.
  Because the site uses client-side routes, add a rewrite of all paths to `/index.html` on that host.
- On the API set `CORS_ORIGINS=https://your-react-site`.

Before going live: change `ADMIN_PASSWORD` and `JWT_SECRET`, serve over HTTPS, and set `SEED_DEMO_DATA=false`.

## How the main features work

- **Token numbers:** each counter restarts at #1 every day. The counter row is locked while a token is created, so two students can never get the same number. The database also enforces uniqueness.
- **People ahead:** waiting tokens with a lower number, plus the token currently being served (so #27 with #21 being served is 6 ahead, as in the brief).
- **Estimated wait:** people ahead x average minutes per person. It starts with the average you configure for the counter and switches to the real average from the last 30 days of completed tokens once there are at least 5.
- **Queue load:** Low (0-4 waiting), Moderate (5-11), High (12+). Change the thresholds in `QueueService.loadFor`.
- **Best time to visit:** counts tokens taken in each opening hour over the last 30 days and suggests the quietest hour. It appears once there are at least 20 tokens of history (the demo data provides this on day one). Opening hours come from `OPENING_HOUR` and `CLOSING_HOUR` (default 9 and 18).
- **Live updates:** the React pages refresh every few seconds, no manual reload needed.
- **Housekeeping:** tokens still waiting from a previous day are closed automatically just after midnight.

## API summary

| Method | Path | Who | Purpose |
|--------|------|-----|---------|
| POST | `/api/auth/register`, `/api/auth/login` | public | Create account, sign in (returns a JWT) |
| GET | `/api/services` | public | Counters with now serving, waiting, load, wait, best time |
| POST | `/api/tokens` | student | Take a token `{ "serviceId": 1 }` |
| GET | `/api/tokens/my` | student | My tokens and live status |
| POST | `/api/tokens/{id}/cancel` | student | Cancel my waiting token |
| GET | `/api/admin/queues` | admin | Every counter's queue |
| POST | `/api/admin/services/{id}/call-next` | admin | Finish the current token and call the next |
| POST | `/api/admin/services/{id}/complete` | admin | Mark the current token complete |
| POST | `/api/admin/tokens/{id}/cancel` | admin | Remove a token from a queue |
| POST / PUT | `/api/admin/services`, `/api/admin/services/{id}` | admin | Add, edit, pause a counter |

## Database tables

`users` (id, name, email, password hash, role), `services` (id, service_name, description, average_time, active),
`tokens` (id, token_number, user_id, service_id, status, queue_date, created_time, called_time, completed_time, duration_seconds).
Spring Boot creates and updates them automatically (`spring.jpa.hibernate.ddl-auto=update`).

## Troubleshooting

- **"Access denied for user 'root'"**: your MySQL password is not `root`. Set `DB_PASSWORD`.
- **"Cannot reach the server"** in the browser: the back end is not running on port 8080.
- **Port 8080 busy**: set `PORT=8081` for the back end and change the proxy target in `frontend/vite.config.js`.
- **Wrong "today" or odd token resets**: set `APP_TIMEZONE` to your campus time zone.
