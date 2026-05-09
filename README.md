# BaiTapLon_LTNC_Nhom9

## Client-Server authentication flow

This project now has a simple client-server flow for login/register.

### Backend
Run `AuthServer.main` in:
`com.nhom9.auction.baitaplon_ltnc_nhom9.server.AuthServer`

It exposes:
- `POST /api/auth/login`
- `POST /api/auth/register`

Default host/port: `http://localhost:8080`

Backend DB settings can be configured by JVM properties or env vars:
- `auction.db.host` / `AUCTION_DB_HOST`
- `auction.db.port` / `AUCTION_DB_PORT`
- `auction.db.name` / `AUCTION_DB_NAME`
- `auction.db.user` / `AUCTION_DB_USER`
- `auction.db.password` / `AUCTION_DB_PASSWORD`

The backend auto-creates table `users` and seeds default accounts if table is empty.

### JavaFX client
Run the app as before (`HelloApplication`).
`LoginController` now calls backend APIs instead of querying DB directly.

Default backend URL in client is `http://localhost:8080`.
Override with JVM option:
`-Dauction.server.baseUrl=http://<host>:<port>`
