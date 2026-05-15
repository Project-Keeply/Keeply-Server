# Keeply Server Guide

  ## Project Summary
  - One-line definition: Keeply is a mobile-first web service that helps
  convenience store workers manage announcements, operation logs, and
  expiration-date tracking in one place without handwritten notebooks.
  - Problems to solve:
    - Information loss during shift handovers
    - Duplicate work caused by fragmented tools for announcements, memos, and
  expiration tracking
    - Manual dependency in managing near-expiry items
  - Core values:
    - Clarity in information delivery
    - Action-oriented UX for on-site operations
    - Fast input flow like handwritten notes + digital traceability/searchability
  - Target users: Convenience store owners/managers, weekday workers, night-shift
  workers, and weekend workers

  ## Tech Stack
  - Language: Java 21
  - Framework: Spring Boot 3.5.0
  - Build Tool: Gradle (Kotlin DSL)
  - Database: MySQL 8.0 (Docker)
  - ORM: Spring Data JPA / Hibernate
  - Security: Spring Security + Kakao OAuth2 + JWT (Access/Refresh Token)
  - Validation: Spring Validation (Bean Validation)
  - Documentation: SpringDoc OpenAPI (Swagger)

  ## Coding Convention
  - Follow the coding convention in `docs/rules/coding-convention.md`
  - @docs/rules/coding-convention.md

  ## Git Convention
  - Follow the Git convention (commit & PR) in `docs/rules/git-convention.md`
  - @docs/rules/git-convention.md

## ⚠️ CRITICAL — Agent Usage Policy

> **You MUST NEVER spawn or invoke any agent without explicit user approval.**
>
> - Before using any agent, always discuss the task with the user first. Do not proceed with agent execution on your own — present the plan, explain which agent will be used and why, and wait for explicit approval.
> - If the user has already granted approval upfront, proceed with the agent work immediately. Once the work is complete, provide a clear bulleted list of everything that was done.