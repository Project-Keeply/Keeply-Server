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
  - 그룹 종속 리소스 경로의 path variable은 반드시 `groupId`로 통일
    - 예: `/groups/{groupId}/notices`, `/groups/{groupId}/work-logs`, `/groups/{groupId}/expiry-items`
  - `GroupAccessAspect`(`@GroupMemberOnly` / `@GroupOwnerOnly`)가 이름 기준으로 파라미터를 스캔하므로 다른 이름(`targetGroupId` 등) 사용 금지
  - 컨트롤러 파라미터는 `@PathVariable Long groupId` (value 생략) 또는 `@PathVariable("groupId") Long groupId` 형태 허용