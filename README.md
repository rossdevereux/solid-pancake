# Voucher Management System

A comprehensive voucher management system with a modular Spring Boot backend and a React (Vite) frontend.

## Project Structure

- **voucher-core**: Shared domain models, repositories, and core business logic.
- **voucher-admin**: Admin API for template management, batch generation (async via RabbitMQ), and statistics. Runs on port `8081`.
- **voucher-user**: User-facing API for voucher validation and redemption. Optimized for GraalVM native compilation. Runs on port `8080`.
- **voucher-ui**: React application built with Vite and TypeScript (Frontend). Runs on port `5173`.

## Prerequisites

Ensure you have the following installed:
- **Java 21**: Required for the backend modules.
- **Maven**: For building the Java projects.
- **Node.js** (v20.19+): Required for the frontend.
- **MongoDB**: Used for persistent storage of templates and vouchers.
- **Redis**: Used for high-speed voucher code caching and lookups.
- **RabbitMQ**: Required for asynchronous voucher batch generation.

## Getting Started

### 1. Infrastructure Setup
Ensure your local instances of MongoDB, Redis, and RabbitMQ are running.
- **MongoDB**: Default port `27017`
- **Redis**: Default port `6379`
- **RabbitMQ**: Default port `5672` (Management UI at `http://localhost:15672`)

### 2. Running the Backend Services

**A. Admin Service (voucher-admin):**
```bash
mvn -pl voucher-admin spring-boot:run
```
API available at: `http://localhost:8081`

**B. User Service (voucher-user):**
```bash
mvn -pl voucher-user spring-boot:run
```
API available at: `http://localhost:8080`

### 3. Running the Frontend (voucher-ui)
```bash
cd voucher-ui
npm install
npm run dev
```
Open your browser and navigate to: `http://localhost:5173`

## Building the Project

To build the entire project (all backend modules and frontend), run from the root directory:

```bash
mvn clean install
```

### Native Image Compilation (voucher-user)
To build a native executable for the user service (requires GraalVM):
```bash
mvn -pl voucher-user -Pnative native:compile
```

## Configuration

- **Core Logic**: `voucher-core/src/main/java/com/voucher/core`
- **Admin Configuration**: `voucher-admin/src/main/resources/application.yml`
- **User Configuration**: `voucher-user/src/main/resources/application.yml`
- **Frontend Configuration**: `voucher-ui/vite.config.ts`
