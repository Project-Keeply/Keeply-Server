# Server Coding Convention

  ## Package Naming
  - Lowercase, singular form (`user`, `auth`, `post`)
  - Follow domain-centric structure

  ## Class Naming
  - Suffix required based on role

  | Role | Example |
  |---|---|
  | Controller | `UserController` |
  | Service (interface) | `UserService` |
  | Service (impl) | `UserServiceImpl` |
  | Repository | `UserRepository` |
  | Entity | `User` |
  | DTO (request) | `CreateUserRequest`, `UpdateUserRequest` |
  | DTO (response) | `UserResponse` |
  | Exception | `UserNotFoundException` |

  ## Variables
  - Never use `var` — always declare types explicitly
  - Boolean variables must be prefixed with `is` (e.g. `isActive`, `isDeleted`)
  - Constants: UPPER_SNAKE_CASE (e.g. `MAX_RETRY_COUNT`)
  - Variable names must be meaningful (long is OK)

  ## Methods
  - Name format: verb + noun
    - `get` — retrieve a single item
    - `getList` — retrieve a list
    - `create` — create a new resource
    - `update` — modify an existing resource
    - `delete` — remove a resource
    - `check` — validate logic
    - `convert` — transform input to another form
    - Utility methods returning boolean: prefix with `has` (e.g. `hasEmail`)

  ## Collections
  - Prefer `List`, `Map`, `Set` over arrays
  - Use `stream()` for iteration (avoid `for` loops)
  - Copy collections with `new ArrayList<>(original)`

  ## Types
  - Always use DTOs for data transfer between layers (never expose Entity directly)
  - Define Service as interface, then provide implementation class

  ## REST Path Variables
  - Path variables for group-scoped resources MUST be named `groupId`
    - Examples: `/groups/{groupId}/notices`, `/groups/{groupId}/work-logs`, `/groups/{groupId}/expiry-items`
  - `GroupAccessAspect` (`@GroupMemberOnly` / `@GroupOwnerOnly`) scans parameters by name — do not use alternative names such as `targetGroupId`
  - Controller parameter styles allowed: `@PathVariable Long groupId` (value omitted) or `@PathVariable("groupId") Long groupId`