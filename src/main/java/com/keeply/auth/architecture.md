# Kakao OAuth2 + JWT Architecture

## 1. Scope
- Purpose: define backend authentication pipeline for Kakao OAuth2 login and JWT-based API authentication.
- Current stage: backend-first implementation without frontend dependency.

## 2. Authentication Flow (Backend Callback)
1. Client moves user to Kakao authorization endpoint.
2. Kakao redirects to backend callback: `GET /auth/kakao/callback?code=...&state=...`.
3. Backend validates `state`.
4. Backend exchanges `code` for Kakao access token.
5. Backend calls Kakao user info API and extracts `kakaoId`.
6. Backend finds user by `kakaoId`; creates user if not found.
7. Backend issues Keeply JWT tokens (`accessToken`, `refreshToken`).
8. Client uses `Authorization: Bearer <accessToken>` for protected APIs.
9. On access token expiry, client calls refresh endpoint.

## 3. Final Token Policy (Decided: 2026-05-22)
- Multi-device policy: one refresh token per user.
- Refresh reissue policy: rotation.
- Storage: MySQL.
- Storage format: refresh token hash only (no raw refresh token persistence).
- Expiration:
  - Access Token: 30 minutes
  - Refresh Token: 14 days

## 4. Endpoint Contract
- `GET /auth/kakao/callback`
  - Input: `code`, `state`
  - Output: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`
  - Failure:
    - `400`: invalid `code` or `state`
    - `401`: invalid/expired token
    - `500`: Kakao integration failure

- `POST /auth/refresh`
  - Input: refresh token
  - Behavior: validate token, verify stored hash, issue new access token and new refresh token (rotation)

- `POST /auth/logout`
  - Behavior: delete user's refresh token record

## 5. Refresh Token Lifecycle
1. Login success: generate refresh token, store hash in MySQL.
2. Refresh request:
   - verify JWT signature/expiry,
   - compare hash with stored value,
   - if valid: issue new token pair and replace stored hash,
   - if invalid/missing/expired: return `401`.
3. Logout: delete refresh token record.
4. Withdrawal: delete all refresh token records for the user and process user status.
5. Reuse/abuse detection: force re-login.

## 6. Security Requirements
- `state` verification is mandatory.
- `JWT secret` must be managed via environment variables.
- HTTPS is required in production.
- Error responses must not expose internal failure causes or provider internals.

## 7. Implementation Notes
- Kakao token is used only for identity verification in login flow.
- Keeply APIs trust only Keeply-issued JWT.
- Future extension: if multi-device sessions are required, add `deviceId` and move to per-device refresh token records.
