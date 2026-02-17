# Voucher Management System

A comprehensive voucher management system with a Spring Boot backend and React (Vite) frontend.

## Project Structure

- **voucher-api**: Java Spring Boot application (Backend)
- **voucher-ui**: React application built with Vite and TypeScript (Frontend)

## Prerequisites

Ensure you have the following installed:
- **Java 21**: Required for the backend.
- **Maven**: For building the Java project.
- **Node.js** (v18+): Required for the frontend.
- **MongoDB**: Must be running on `localhost:27017`.
- **Redis**: Must be running on `localhost:6379`.

## Getting Started

### 1. Database Setup
Ensure your local MongoDB and Redis instances are up and running.
- **MongoDB**: Default port `27017`
- **Redis**: Default port `6379`

### 2. Running the Backend (voucher-api)
The backend runs on port `8080`.

**Using Maven:**
```bash
cd voucher-api
mvn spring-boot:run
```

**From the Root Directory:**
```bash
mvn -pl voucher-api spring-boot:run
```

Once started, the API will be available at: `http://localhost:8080`

### 3. Running the Frontend (voucher-ui)
The frontend runs on port `5173`.

**Setup and Run:**
```bash
cd voucher-ui
npm install
npm run dev
```

Open your browser and navigate to: `http://localhost:5173`

## Building the Project

To build the entire project (both backend and frontend), run the following command from the root directory:

```bash
mvn clean install
```

This will:
1. Build the backend JAR.
2. Install Node/NPM and build the frontend assets (via `frontend-maven-plugin` in `voucher-ui`).

## Configuration

- **Backend**: `voucher-api/src/main/resources/application.yml`
- **Frontend**: `voucher-ui/vite.config.ts` and `.env` (if applicable).
