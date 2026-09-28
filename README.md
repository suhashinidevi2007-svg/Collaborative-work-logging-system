# Collaborative Work Logging System (CWLS)

A full-stack collaborative work logging, attendance, and employee performance tracking system built with **Spring Boot 2.7**, **Spring Data JPA**, **Supabase (PostgreSQL) / MySQL**, and an interactive web frontend.

---

## ⚡ Recommended Cloud Deployment: Render + Supabase (100% Free)

This is the most powerful combination: **Supabase** provides a free cloud PostgreSQL database, and **Render** hosts your web service for free!

### Step 1: Create a Free Database on Supabase
1. Go to [supabase.com](https://supabase.com) and create an account.
2. Click **New Project** (give it any name, e.g. `cwls-db`, and choose a secure database password).
3. Once the project is ready, go to **Project Settings** (gear icon) > **Database**.
4. Scroll to **Connection parameters** or **Connection String**:
   * Switch to the **URI** or **JDBC** tab.
   * If using **Transaction Pooler** (recommended for IPv4 cloud hosts like Render):
     * Host: `aws-0-[region].pooler.supabase.com`
     * Port: `6543`
     * Database: `postgres`
     * User: `postgres.[project-ref]`
     * JDBC URL format:
       `jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require`

### Step 2: Deploy Web Service on Render
1. Go to [render.com](https://render.com) and click **New +** > **Web Service**.
2. Connect your GitHub repository `suhashinidevi2007-svg/Collaborative-work-logging-system`.
3. Choose **Docker** as the Runtime (it will automatically use the `Dockerfile`).
4. Select the **Free** instance plan.
5. Under **Environment Variables**, add:
   * `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<SUPABASE_HOST>:<PORT>/postgres?sslmode=require`
   * `SPRING_DATASOURCE_USERNAME` = `<SUPABASE_USER>`
   * `SPRING_DATASOURCE_PASSWORD` = `<SUPABASE_PASSWORD>`
6. Click **Deploy Web Service**. Render will build and deploy your app with a public URL!

---

## 🚀 Alternative Deployments

### Option 2: Render with In-Memory Mode (Instant Zero-Config Demo)
If you just want an instant live preview on Render without setting up any database:
1. Connect your repo on Render as a Web Service (Docker).
2. Set Environment Variable: `SPRING_PROFILES_ACTIVE` = `h2`.
3. Deploy!

### Option 3: Railway (Built-in MySQL)
1. Go to [Railway.app](https://railway.app), click **New Project** > **Deploy from GitHub repo**.
2. Click **Add MySQL** database service in Railway.
3. Add `SPRING_DATASOURCE_URL` = `${{MySQL.MYSQL_URL}}`.
4. Click **Generate Domain**.

---

## 🐳 Run Locally with Docker Compose

Run the entire application along with a pre-configured MySQL instance in one command:

```bash
docker-compose up --build
```

Then visit: `http://localhost:8080`

---

## 🔑 Default Credentials

On fresh deployments, the system automatically initializes default accounts:

| Role | Username | Password | Purpose |
|------|----------|----------|---------|
| **Admin** | `admin` | `admin123` | Approving work logs, assigning points, reviewing attendance, managing projects |
| **Employee** | `employee` | `emp123` | Logging daily tasks, clocking in/out, requesting leaves |

*You can also register new accounts anytime directly from the UI.*

---

## 🛠️ Tech Stack
- **Backend**: Java 17, Spring Boot 2.7.18, Spring Security Crypto (BCrypt), Spring Data JPA
- **Database Support**: Supabase (PostgreSQL), MySQL 8.0, In-Memory H2
- **Frontend**: Single Page HTML5 / CSS3 / JavaScript (Vanilla)
- **Containerization**: Multi-stage Docker build
