# User Management Microservice

A production-ready Spring Boot CRUD microservice for user management with comprehensive features.

## Features

- **CRUD Operations**: Complete Create, Read, Update, Delete operations for users
- **Validation**: Comprehensive input validation using Hibernate Validator
- **Pagination**: Support for paginated results with sorting
- **Search**: Full-text search across user fields
- **Exception Handling**: Global exception handling with proper HTTP status codes
- **Database**: PostgreSQL with Liquibase migrations
- **Monitoring**: Spring Boot Actuator endpoints
- **Clean Architecture**: Proper separation of concerns with layered architecture

## Architecture

```
├── controller/     # REST API endpoints
├── service/        # Business logic layer
├── repository/    # Data access layer
├── entity/        # JPA entities
├── dto/           # Data Transfer Objects
├── exception/     # Custom exceptions and global handler
└── mapper/        # Entity-DTO mapping utilities
```

### System Architecture Diagram

```mermaid
graph TB
    Client[Client Application] --> API[API Gateway/Load Balancer]
    API --> UserController[UserController]
    UserController --> UserService[UserService]
    UserService --> UserRepository[UserRepository]
    UserRepository --> Database[(PostgreSQL Database)]
    
    UserController --> GlobalExceptionHandler[GlobalExceptionHandler]
    UserService --> UserMapper[UserMapper]
    
    subgraph "Spring Boot Application"
        UserController
        UserService
        UserRepository
        UserMapper
        GlobalExceptionHandler
    end
    
    subgraph "Infrastructure"
        Database
        Liquibase[Liquibase Migrations]
    end
    
    UserRepository --> Liquibase
    Liquibase --> Database
```

### Data Flow Architecture

```mermaid
graph LR
    Request[HTTP Request] --> Controller[Controller Layer]
    Controller --> Validation[Input Validation]
    Validation --> Service[Service Layer]
    Service --> Repository[Repository Layer]
    Repository --> Database[Database]
    Database --> Repository
    Repository --> Service
    Service --> Response[HTTP Response]
    
    Validation --> ErrorHandler[Error Handler]
    Service --> ErrorHandler
    Repository --> ErrorHandler
    ErrorHandler --> Response
```

## API Endpoints

### User Management
- `POST /api/v1/users` - Create a new user
- `GET /api/v1/users/{id}` - Get user by ID
- `GET /api/v1/users/username/{username}` - Get user by username
- `GET /api/v1/users/email/{email}` - Get user by email
- `GET /api/v1/users` - Get all users
- `GET /api/v1/users/paginated` - Get paginated users
- `GET /api/v1/users/search` - Search users
- `PUT /api/v1/users/{id}` - Update user
- `PATCH /api/v1/users/{id}/status` - Update user status
- `DELETE /api/v1/users/{id}` - Delete user

### Statistics
- `GET /api/v1/users/stats/total` - Get total users count
- `GET /api/v1/users/stats/status/{status}` - Get users count by status

## API Flow Diagrams

### Create User Flow

```mermaid
flowchart TD
    Start([Start]) --> Request[POST /api/v1/users]
    Request --> Validation{Validate Input}
    Validation -->|Invalid| Error400[400 Bad Request]
    Validation -->|Valid| CheckDuplicate{Check Duplicate}
    CheckDuplicate -->|Duplicate| Error409[409 Conflict]
    CheckDuplicate -->|Unique| SaveUser[Save User to DB]
    SaveUser --> Success[201 Created]
    Error400 --> End([End])
    Error409 --> End
    Success --> End
```

### Get User by ID Flow

```mermaid
flowchart TD
    Start([Start]) --> Request[GET /api/v1/users/{id}]
    Request --> FindUser{Find User in DB}
    FindUser -->|Not Found| Error404[404 Not Found]
    FindUser -->|Found| MapToDTO[Map Entity to DTO]
    MapToDTO --> Success[200 OK]
    Error404 --> End([End])
    Success --> End
```

## Sequence Diagrams

### Create User Sequence

```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant UserService
    participant UserRepository
    participant Database
    
    Client->>UserController: POST /api/v1/users
    UserController->>UserService: createUser(userRequest)
    UserService->>UserService: validateUserRequest()
    UserService->>UserRepository: findByUsername() or findByEmail()
    UserRepository->>Database: SELECT * FROM users WHERE username/email = ?
    Database-->>UserRepository: User data
    UserRepository-->>UserService: Optional<User>
    
    alt User exists
        UserService-->>UserController: throw DuplicateResourceException
        UserController-->>Client: 409 Conflict
    else User doesn't exist
        UserService->>UserRepository: save(user)
        UserRepository->>Database: INSERT INTO users VALUES (...)
        Database-->>UserRepository: Saved User
        UserRepository-->>UserService: User entity
        UserService->>UserService: mapToResponse()
        UserService-->>UserController: UserResponse
        UserController-->>Client: 201 Created + UserResponse
    end
```

### Get User by ID Sequence

```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant UserService
    participant UserRepository
    participant Database
    
    Client->>UserController: GET /api/v1/users/{id}
    UserController->>UserService: getUserById(id)
    UserService->>UserRepository: findById(id)
    UserRepository->>Database: SELECT * FROM users WHERE id = ?
    Database-->>UserRepository: User data
    UserRepository-->>UserService: Optional<User>
    
    alt User found
        UserService->>UserService: mapToResponse()
        UserService-->>UserController: UserResponse
        UserController-->>Client: 200 OK + UserResponse
    else User not found
        UserService-->>UserController: throw ResourceNotFoundException
        UserController-->>Client: 404 Not Found
    end
```

### Error Handling Sequence

```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant GlobalExceptionHandler
    participant UserService
    
    Client->>UserController: API Request
    UserController->>UserService: Business Logic
    
    alt Validation Error
        UserService-->>UserController: throw ValidationException
        UserController->>GlobalExceptionHandler: handleException()
        GlobalExceptionHandler-->>Client: 400 Bad Request + Error details
    else Resource Not Found
        UserService-->>UserController: throw ResourceNotFoundException
        UserController->>GlobalExceptionHandler: handleException()
        GlobalExceptionHandler-->>Client: 404 Not Found + Error details
    else Duplicate Resource
        UserService-->>UserController: throw DuplicateResourceException
        UserController->>GlobalExceptionHandler: handleException()
        GlobalExceptionHandler-->>Client: 409 Conflict + Error details
    else Success
        UserService-->>UserController: Success Response
        UserController-->>Client: 200 OK + Response
    end
```

## Running the Application

### Prerequisites
- Java 21+
- PostgreSQL database
- Gradle

### Database Setup
1. Create PostgreSQL database: `user_service_db`
2. Update database credentials in `application.yml`

### Run with Gradle
```bash
./gradlew bootRun
```

### Run with Docker (optional)
```bash
docker build -t user-service .
docker run -p 8081:8081 user-service
```

## Configuration

The application runs on port `8081` by default. Key configuration options in `application.yml`:

- Database connection settings
- Server port
- Logging levels
- Management endpoints

## Validation Rules

- **First Name**: 2-50 characters, required
- **Last Name**: 2-50 characters, required
- **Username**: 3-30 characters, alphanumeric + underscores, unique
- **Email**: Valid email format, unique
- **Phone**: 10-20 characters, numeric
- **Age**: Optional integer
- **Status**: ACTIVE, INACTIVE, or SUSPENDED

## Error Handling

The service provides comprehensive error handling with proper HTTP status codes:

- `400 Bad Request` - Validation errors
- `404 Not Found` - Resource not found
- `409 Conflict` - Duplicate resource
- `500 Internal Server Error` - Unexpected errors

## Monitoring

Spring Boot Actuator endpoints are available:
- `/actuator/health` - Application health
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics

## Testing

Run tests with:
```bash
./gradlew test
```

## Database Schema

### Entity Relationship Diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar first_name
        varchar last_name
        varchar username UK
        varchar email UK
        varchar phone
        integer age
        varchar status
        timestamp created_at
        timestamp updated_at
    }
    
    USERS {
        Index idx_user_email(email)
        Index idx_user_username(username)
    }
```

### Database Schema Flow

```mermaid
flowchart TD
    Start([Application Startup]) --> Liquibase[Liquibase Init]
    Liquibase --> CheckChanges{Check for Changes}
    CheckChanges -->|New Changes| ExecuteChanges[Execute ChangeSets]
    CheckChanges -->|No Changes| Ready[Database Ready]
    ExecuteChanges --> CreateTable[Create Users Table]
    CreateTable --> CreateIndexes[Create Indexes]
    CreateIndexes --> Ready
    Ready --> AppStart[Application Starts]
```

The `users` table includes:
- **Basic user information** (name, username, email, phone)
- **Optional fields** (age)
- **Status tracking** (ACTIVE, INACTIVE, SUSPENDED)
- **Audit fields** (created_at, updated_at)
- **Indexes for performance optimization**

### Data Mapping Flow

```mermaid
flowchart LR
    DTO[DTO Objects] --> Mapper[UserMapper]
    Entity[JPA Entity] --> Mapper
    Mapper --> Database[Database Table]
    
    subgraph "API Layer"
        Request[UserRequest]
        Response[UserResponse]
    end
    
    subgraph "Data Layer"
        UserEntity[User Entity]
    end
    
    Request --> DTO
    Response --> DTO
    DTO <--> Mapper
    Mapper <--> Entity
    Entity <--> Database
```
