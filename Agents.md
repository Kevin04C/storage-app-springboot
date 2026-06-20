# StoneStorage — Arquitectura y Guía para Agentes IA

## 1. Visión General del Proyecto

**StoneStorage** es un microservicio de almacenamiento **multitenant** diseñado como Gateway centralizado para la gestión de archivos de múltiples aplicaciones.

### Qué problema resuelve

Centraliza el almacenamiento de archivos de distintas aplicaciones en un solo servicio, con control de acceso por API Key, cuotas de espacio aisladas por tenant, y visibilidad pública/privada por archivo. Es agnóstico a la infraestructura física (local o S3) mediante una capa de abstracción.

### Tipo de sistema

- API REST reactiva (Spring WebFlux)
- Persistencia reactiva con R2DBC + PostgreSQL
- Almacenamiento de archivos en disco local o S3
- Autenticación por API Key via Spring Security Reactive

### Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 4.0.6 | Framework base |
| Spring WebFlux | - | API reactiva |
| Spring Security | - | Autenticación API Key |
| Spring Data R2DBC | - | Persistencia reactiva |
| PostgreSQL | - | Base de datos |
| R2DBC PostgreSQL | - | Driver reactivo |
| Caffeine Cache | - | Caché de API Keys y cuotas |
| Thumbnailator | 0.4.20 | Generación de thumbnails |
| Commons IO | 2.15.1 | Utilidades IO |
| SpringDoc OpenAPI | 2.8.8 | Documentación Swagger |
| Lombok | - | Reducción de boilerplate |
| Maven | - | Build |

### Propósito de cada componente importante

| Componente | Propósito |
|---|---|
| `bootstrap` | Composition root: entry point, config de frameworks, seguridad, CORS, Swagger |
| `client` | Bounded Context de clientes: registro, autenticación, cuotas |
| `storage` | Bounded Context de almacenamiento: upload, download, listado, thumbnails |
| `shared` | Shared kernel: excepciones base, value objects, DTOs de API, utilidades |

---

## 2. Arquitectura Actual

### Estilo arquitectónico

El proyecto sigue **Clean Architecture / Hexagonal (Ports & Adapters)** con separación en 3 capas por módulo:

```
Domain  ←  Application  ←  Infrastructure
  ↑            ↑                 ↑
  |    depende de interfaces    |
  └─────────────────────────────┘
       (Dependency Inversion)
```

### Capas actuales

| Capa | Responsabilidad | Prohibido |
|---|---|---|
| **Domain** | Entidades puras, value objects, interfaces de repositorio, puertos de salida, excepciones de dominio | NO depende de Spring ni frameworks |
| **Application** | Casos de uso (inbound ports), DTOs, orquestación, servicios transaccionales | NO importa de infrastructure |
| **Infrastructure** | Controllers REST, repositorios R2DBC, providers de storage, config de frameworks | NO contiene lógica de negocio |

### Responsabilidad de cada módulo

| Módulo | Responsabilidad |
|---|---|
| `client` | Gestión de clientes multitenant: registro, API Key, cuotas |
| `storage` | Gestión de archivos: subida, descarga, listado, thumbnails, visibilidad |
| `shared` | Cross-cutting: value objects (`FriendlySize`), excepciones base (`DomainException`), DTOs de API (`ApiResponse`, `ApiError`, `PaginationInfo`), utilidades (`PathSanitizer`), caché |
| `bootstrap` | Composition root: aplicación Spring Boot, security chain, CORS, OpenAPI, Jackson |

### Flujo de comunicación entre componentes

```
HTTP Request
    │
    ▼
SecurityWebFilterChain
    │ (valida API Key si ruta protegida)
    ▼
ClientContextFilter
    │ (inyecta clientId, baseDir, quota, usedBytes en request attributes)
    ▼
Controller (REST adapter)
    │ (recibe request, delega a use case, devuelve ApiResponse)
    ▼
ApplicationService (use case implementation)
    │ (orquesta: valida → procesa → persiste → responde)
    ├──→ DomainService (reglas de negocio puras)
    ├──→ Repository (interface en domain, impl en infra)
    └──→ StorageProvider (interface en domain, impl en infra)
    ▼
Infrastructure (R2DBC, filesystem, S3)
```

### Dependencias importantes entre módulos

- `storage.application` depende de `client.domain` (quota check, `QuotaExceededException`) y `shared.domain.port` (vía `ClientQuotaPort`, `PathSanitizerPort`)
- `client.infrastructure` depende de `shared.domain.port` (implementa `ClientQuotaPort`)
- `shared` no depende de ningún módulo
- `bootstrap` depende de todos los módulos (es el composition root)

---

## 3. Reglas Arquitectónicas Obligatorias

### Reglas de dependencia

```
bootstrap → todos los módulos
application → domain + shared.domain
infrastructure → application + domain + shared
domain → shared.domain (solo value objects y excepciones base)
shared.domain → nada
shared.infrastructure → shared.domain
```

### Reglas por capa

**DOMAIN:**
- NO usa anotaciones de Spring (`@Service`, `@Component`, `@Autowired`, etc.)
- NO importa nada de infrastructure
- Las entidades son POJOs puros (sin anotaciones JPA/R2DBC)
- Los repositorios y puertos de salida son interfaces
- Las excepciones de negocio extienden `DomainException`

**APPLICATION:**
- Depende SOLO de domain + shared.domain
- NO importa nada de infrastructure
- `@Service`, `@Transactional`, `@Cacheable` están permitidos (son Spring, pero son cross-cutting)
- Los DTOs de entrada/salida viven aquí (NO en domain)
- Los métodos expuestos implementan interfaces `*UseCase`

**INFRASTRUCTURE:**
- NO contiene lógica de negocio
- Los controllers son adapters REST: reciben request, delegan a use case, devuelven response
- Las entidades de persistencia (`*Entity`) son distintas de las entidades de dominio
- Los adapters de repositorio implementan interfaces de dominio y mapean Entity ↔ Domain

### Reglas de naming

- **Clases**: PascalCase
- **Métodos**: camelCase
- **Paquetes**: lowercase, separación por módulo.capa
- **Tablas**: snake_case (ej: `storage_clients`)
- **Columnas**: snake_case (ej: `api_key`)
- **DTOs**: sufijo `Request`, `Response`, `Info`
- **Use Cases**: sufijo `UseCase` (ej: `UploadFileUseCase`)
- **Servicios de aplicación**: sufijo `ApplicationService`
- **Servicios de dominio**: sufijo `DomainService`
- **Repositorios**: implementan interfaz, sufijo `R2dbcRepository`
- **Entidades de persistencia**: sufijo `Entity`

### Reglas de transaccionalidad

- `@Transactional(readOnly = true)` a nivel de clase en `ApplicationService`
- `@Transactional` (escritura) solo en métodos que mutan datos
- La lógica de dominio NUNCA maneja transacciones

### Reglas de validación

- Validaciones de entrada: Jakarta Validation (`@NotBlank`, `@Positive`, etc.) en DTOs de aplicación
- Validaciones de negocio: en servicios de dominio o entidades
- Path traversal: siempre pasar por `PathSanitizerPort`

---

## 4. Estructura del Proyecto

```
src/main/java/com/stonestorage/
├── bootstrap/
│   ├── StoneStorageApplication.java          @SpringBootApplication
│   ├── controller/
│   │   └── HealthController.java             /health, /health/ready
│   └── config/
│       ├── CorsConfig.java                   CORS global
│       ├── JacksonConfig.java                ObjectMapper bean
│       ├── OpenApiConfig.java                Swagger/OpenAPI
│       └── security/
│           ├── ApiKeyAuthenticationConverter.java    Extrae X-API-KEY header
│           ├── ApiKeyAuthenticationToken.java        Custom Authentication token
│           ├── ApiKeyReactiveAuthenticationManager.java  Valida API Key
│           ├── ClientContextFilter.java              Inyecta datos del cliente al request
│           └── SecurityConfig.java                   SecurityWebFilterChain
│
├── client/
│   ├── domain/
│   │   ├── entity/Client.java               Entidad de dominio
│   │   ├── exception/
│   │   │   ├── ClientAlreadyExistsException.java
│   │   │   ├── ClientNotFoundException.java
│   │   │   └── QuotaExceededException.java
│   │   └── repository/ClientRepository.java   Puerto de salida (interfaz)
│   ├── application/
│   │   ├── dto/
│   │   │   ├── ClientQuotaInfo.java
│   │   │   ├── ClientResponse.java
│   │   │   └── RegisterClientRequest.java
│   │   ├── port/in/
│   │   │   ├── GetClientQuotaUseCase.java
│   │   │   ├── RegisterClientUseCase.java
│   │   │   └── ValidateApiKeyUseCase.java
│   │   └── service/ClientApplicationService.java  Implementa use cases
│   └── infrastructure/
│       ├── persistence/
│       │   ├── ClientQuotaAdapter.java          Implementa ClientQuotaPort
│       │   ├── ClientR2dbcRepository.java       Implementa ClientRepository
│       │   └── entity/ClientEntity.java         Modelo de persistencia R2DBC
│       └── web/ClientController.java            REST adapter
│
├── storage/
│   ├── domain/
│   │   ├── FileVisibility.java               Enum PUBLIC / PRIVATE
│   │   ├── entity/
│   │   │   ├── FileMetadata.java             Entidad de dominio (con factory method create)
│   │   │   └── FileNode.java                 Nodo de filesystem
│   │   ├── exception/
│   │   │   ├── FileNotFoundException.java
│   │   │   └── StorageException.java
│   │   ├── port/StorageProvider.java          Puerto de salida (interfaz)
│   │   └── repository/FileMetadataRepository.java  Puerto de salida (interfaz)
│   ├── application/
│   │   ├── dto/
│   │   │   ├── FileContent.java
│   │   │   ├── FileNodeResponse.java
│   │   │   ├── UploadRequest.java
│   │   │   └── UploadResponse.java
│   │   ├── port/in/
│   │   │   ├── DeleteFileUseCase.java
│   │   │   ├── DownloadFileUseCase.java
│   │   │   ├── GenerateThumbnailUseCase.java
│   │   │   ├── ListFilesUseCase.java
│   │   │   ├── PreviewFileUseCase.java
│   │   │   └── UploadFileUseCase.java
│   │   └── service/StorageApplicationService.java  Implementa use cases
│   └── infrastructure/
│       ├── persistence/
│       │   ├── FileMetadataR2dbcRepository.java   Implementa FileMetadataRepository
│       │   └── entity/FileMetadataEntity.java     Modelo de persistencia R2DBC
│       ├── provider/
│       │   ├── LocalStorageProvider.java          Implementa StorageProvider (disco)
│       │   ├── S3StorageProvider.java             Stub (no implementado)
│       │   └── StorageProviderFactory.java        Factory + validación startup
│       └── web/
│           ├── MultipartUploadExtractor.java       Parser multipart form
│           ├── PreviewController.java              GET /f/{fileId} (público)
│           └── StorageController.java              REST adapter
│
└── shared/
    ├── domain/
    │   ├── exception/
    │   │   ├── DomainException.java               Base abstract de excepciones
    │   │   └── PathTraversalException.java
    │   ├── port/
    │   │   ├── ClientQuotaPort.java               Puerto para actualizar cuota
    │   │   └── PathSanitizerPort.java             Puerto para sanitizar rutas
    │   ├── valueobject/FriendlySize.java          Value object formateo de tamaños
    │   └── constant/                              (futuro: constantes compartidas)
    └── infrastructure/
        ├── config/CacheConfig.java                Caffeine config
        ├── util/PathSanitizer.java                Implementa PathSanitizerPort
        └── web/
            ├── ApiResponseExceptionHandler.java   WebExceptionHandler global
            └── dto/
                ├── ApiError.java                  Detalle de error
                ├── ApiResponse.java               Envoltura unificada de respuesta
                └── PaginationInfo.java            Info de paginación
```

### Flujo recomendado para agregar nuevas funcionalidades

1. **Domain**: crear/actualizar la entidad de dominio con su lógica de negocio
2. **Domain**: definir el puerto de salida (interfaz del repositorio/provider)
3. **Application**: crear el caso de uso (interfaz `*UseCase` en `port/in/`)
4. **Application**: crear DTOs de request/response
5. **Application**: implementar el caso de uso en `*ApplicationService`
6. **Infrastructure**: implementar el adaptador de repositorio
7. **Infrastructure**: crear el controller REST
8. **Config**: agregar config necesaria (security, caché, etc.)

---

## 5. Buenas Prácticas del Proyecto

### Naming

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases | PascalCase | `FileMetadata`, `StorageApplicationService` |
| Métodos | camelCase | `findByApiKey()`, `upload()` |
| Paquetes | lowercase.module.layer | `client.domain.entity` |
| Tablas | snake_case | `storage_clients`, `file_metadata` |
| Columnas | snake_case | `api_key`, `size_bytes`, `created_at` |
| Constantes | UPPER_SNAKE_CASE | `PUBLIC`, `PRIVATE` |
| Records | PascalCase | `UploadResponse`, `RegisterClientRequest` |

### Manejo de excepciones

- Toda excepción de negocio extiende `DomainException` (en `shared.domain.exception`)
- Las excepciones se lanzan desde domain o application layer
- NO se lanzan excepciones desde infrastructure (se traducen a códigos HTTP via `ApiResponseExceptionHandler`)
- El handler global (`ApiResponseExceptionHandler` con `@Order(-2)`) captura todo y devuelve `ApiResponse` con el formato estandarizado

### Validaciones

- **Input**: Jakarta Validation en DTOs (`@NotBlank`, `@Positive`, etc.)
- **Negocio**: en entidades de dominio (`Client.hasEnoughQuota()`) o domain services (`ClientDomainService.validateQuota()`)
- **Seguridad**: `PathSanitizerPort.sanitize()` previene path traversal

### Logs

- Usar Lombok `@Slf4j` en clases de infrastructure
- Loggear errores con `log.error("mensaje", exception)`
- NO loggear en domain layer
- NO loggear información sensible (API Keys, tokens)

### Manejo transaccional

- `@Transactional(readOnly = true)` a nivel de clase en `ApplicationService`
- `@Transactional` solo en métodos que escriben
- La capa de dominio NO tiene anotaciones transaccionales

### Configuración

- Toda config externalizada via `application.properties` y variables de entorno
- Las properties sensibles se leen de env vars con `${VAR}`
- El archivo `.env` carga variables localmente via `spring-dotenv`
- Caché configurable via `cache.api-key.*`, `cache.file-metadata.*`, `cache.thumbnails.*`

### Seguridad

- Autenticación via header `X-API-KEY`
- Rutas públicas: `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`, `/f/**`, `/health`, `/actuator/**`, `POST /api/v1/client/register`
- Las rutas públicas tienen `requiresAuthenticationMatcher` para evitar processamiento innecesario
- Path traversal prevenido via `PathSanitizerPort`

---

## 6. Base de Datos

### Motor

PostgreSQL, accedido via R2DBC (driver reactivo).

### Diseño actual

**Tabla `storage_clients`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PK `DEFAULT gen_random_uuid()` | ID auto-generado |
| `app_name` | VARCHAR(255) | NOT NULL | Nombre de la app |
| `api_key` | VARCHAR(255) | NOT NULL UNIQUE | Token de autenticación |
| `base_dir` | VARCHAR(500) | NOT NULL | Carpeta raíz del tenant |
| `quota_bytes` | BIGINT | NOT NULL DEFAULT 0 | Límite de espacio |
| `used_bytes` | BIGINT | NOT NULL DEFAULT 0 | Espacio usado |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | Control admin |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Fecha de registro |

**Tabla `file_metadata`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PK `DEFAULT gen_random_uuid()` | ID auto-generado |
| `client_id` | UUID | NOT NULL REFERENCES storage_clients(id) | FK al cliente |
| `original_name` | VARCHAR(500) | NOT NULL | Nombre original |
| `system_name` | VARCHAR(500) | NOT NULL | Nombre único en disco |
| `checksum` | VARCHAR(64) | NOT NULL | SHA-256 hex |
| `size_bytes` | BIGINT | NOT NULL DEFAULT 0 | Tamaño |
| `visibility` | VARCHAR(20) | NOT NULL DEFAULT 'PUBLIC' | PUBLIC/PRIVATE |
| `storage_path` | VARCHAR(2000) | NOT NULL | Ruta relativa portable |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Fecha de subida |
| `deleted_at` | TIMESTAMPTZ | NULL | Soft delete |

**Índices**
- `idx_file_metadata_client` → `client_id`
- `idx_file_metadata_storage_path` → `storage_path`
- `idx_file_metadata_deleted` → `deleted_at` WHERE `deleted_at IS NULL`

### Reglas futuras

**Crear tablas nuevas:**
- Usar el schema en `src/main/resources/db/schema.sql` como referencia
- Naming snake_case
- Siempre incluir `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- UUID como PK con `DEFAULT gen_random_uuid()`
- `TIMESTAMPTZ` para fechas (consistencia UTC)

**Crear migraciones:**
- El proyecto actualmente ejecuta `schema.sql` al iniciar (`spring.sql.init.mode=always`)
- Para producción, migrar a Flyway o Liquibase

**Índices:**
- Indexar foreign keys (`client_id`)
- Indexar columnas usadas en WHERE frecuente
- Usar partial indexes para filtros comunes (`WHERE deleted_at IS NULL`)

**Auditoría:**
- Soft delete via `deleted_at` (no borrado físico)
- Timestamps en todas las tablas (`created_at`)

---

## 7. API y Comunicación Externa

### Formato de respuesta unificado

Todas las respuestas (éxito y error) usan `ApiResponse<T>`:

```json
// Éxito
{
  "success": true,
  "message": "File uploaded successfully",
  "data": { ... },
  "error": null,
  "pagination": null
}

// Error
{
  "success": false,
  "message": "UNAUTHORIZED",
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "details": "Client not found or inactive for API key: xyz"
  }
}
```

### Endpoints existentes

**Cliente** (`/api/v1/client`)

| Método | Path | Auth | Request | Response | Descripción |
|---|---|---|---|---|---|
| `POST` | `/register` | Público | `RegisterClientRequest { appName, quotaGb? }` | `ApiResponse<ClientResponse>` | Registrar tenant |
| `GET` | `/quota` | API Key | `@RequestAttribute clientId` | `ApiResponse<ClientQuotaInfo>` | Cuota del tenant |

**Storage** (`/api/v1/storage`)

| Método | Path | Auth | Request | Response | Descripción |
|---|---|---|---|---|---|
| `POST` | `/upload` | API Key | Multipart: `file`, `path?`, `visibility?` | `ApiResponse<UploadResponse>` | Subir archivo |
| `GET` | `/download/{fileId}` | API Key | `@PathVariable fileId` | `Flux<DataBuffer>` | Descargar privado |
| `GET` | `/list` | API Key | `@RequestParam path=/` | `ApiResponse<List<FileNodeResponse>>` | Listar carpeta |
| `DELETE` | `/{fileId}` | API Key | `@PathVariable fileId` | `ApiResponse<Void>` | Soft delete |
| `GET` | `/thumbnail` | API Key | `@RequestParam path, w, h` | `ResponseEntity<byte[]>` | Thumbnail |

**Público**

| Método | Path | Request | Response | Descripción |
|---|---|---|---|---|
| `GET` | `/f/{fileId}` | `@PathVariable fileId` | `Mono<Void>` escribe response | Preview público |
| `GET` | `/health` | — | `ApiResponse<Map>` | Health check |
| `GET` | `/health/ready` | — | `ApiResponse<Map>` | Readiness |
| `GET` | `/actuator/**` | — | Actuator | Spring Boot Actuator |

### Mapeo de errores

| Excepción | HTTP Status | Error Code |
|---|---|---|
| `ClientNotFoundException` | 401 | `UNAUTHORIZED` |
| `ClientAlreadyExistsException` | 409 | `CLIENT_ALREADY_EXISTS` |
| `QuotaExceededException` | 413 | `QUOTA_EXCEEDED` |
| `FileNotFoundException` | 404 | `FILE_NOT_FOUND` |
| `StorageException` | 500 | `STORAGE_ERROR` |
| Otras `DomainException` | 400 | `DOMAIN_ERROR` |
| `ResponseStatusException` | Según status | Status name |
| Error no manejado | 500 | `INTERNAL_ERROR` |

### Cómo crear nuevos endpoints

1. Crear DTOs request/response en `application/dto/`
2. Crear interfaz `*UseCase` en `application/port/in/`
3. Implementar en `*ApplicationService`
4. Crear controller en `infrastructure/web/`
5. Si requiere seguridad: NO agregar nada (se hereda del SecurityConfig)
6. Si es público: agregar la ruta al `requiresAuthenticationMatcher` en `SecurityConfig`

---

## 8. Infraestructura

### Docker

**Dockerfile** — Multi-stage build:

```
Stage 1 (builder): maven:3.9-eclipse-temurin-21 → mvn package
Stage 2 (extractor): eclipse-temurin:21-jre → jarmode layertools extract
Stage 3 (runtime): eclipse-temurin:21-jre → JAR layers optimizados
```

- Puerto expuesto: 8080
- Healthcheck: `curl -f http://localhost:8080/actuator/health`
- JVM: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`
- Usuario no-root: `appuser`

**docker-compose.yml**

```yaml
services:
  app:
    build: .
    ports: ["8082:8080"]
    env_file: .env
    volumes:
      - /opt/storage:/app/data
    deploy:
      resources:
        limits:
          memory: 1G
    healthcheck: ...
```

### Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_R2DBC_URL` | URL de conexión PostgreSQL | — |
| `SPRING_R2DBC_USERNAME` | Usuario BD | — |
| `SPRING_R2DBC_PASSWORD` | Password BD | — |
| `STORAGE_TYPE` | `local` o `s3` | `local` |
| `STORAGE_BASE_PATH` | Ruta base de almacenamiento | `./storage` |

### Cómo levantar el proyecto

```bash
# Local (dev)
./mvnw spring-boot:run

# Docker
docker compose up --build

# Tests
./mvnw test
```

### Configuración por ambientes

Actualmente solo existe `application.properties` con valores por defecto. Para agregar ambientes:
- Crear `application-dev.properties`, `application-prod.properties`
- Activar con `spring.profiles.active=${PROFILE:dev}`

---

## 9. Manejo de Errores y Excepciones

### Estrategia actual (unificada)

Hay UN SOLO handler global que captura todas las excepciones:

**`ApiResponseExceptionHandler`** (`shared.infrastructure.web`)
- `WebExceptionHandler` con `@Order(-2)` — se ejecuta ANTES que cualquier otro handler
- Captura exceptions que escapan de controllers, security, y cualquier filtro
- Mapea tipos de exception conocidos a HTTP status + error code
- Si falla la serialización JSON, usa un fallback hardcodeado
- Loggea errores inesperados con `log.error()`

**`SecurityConfig`** maneja errores de autenticación:
- `ServerAuthenticationFailureHandler` → 401 con `ApiResponse`
- `ServerAuthenticationEntryPoint` → 401 con `ApiResponse`

### Cómo agregar una nueva excepción

1. Crear la clase en `module/domain/exception/` extendiendo `DomainException`
2. Agregar el mapping en `ApiResponseExceptionHandler` (segundo bloque `instanceof`)
3. Agregar el handler en el controller si aplica

NO crear un nuevo `@RestControllerAdvice` — todo se centraliza en `ApiResponseExceptionHandler`.

---

## 10. Patrones Utilizados

| Patrón | Dónde | Descripción |
|---|---|---|
| **Hexagonal / Ports & Adapters** | Todo el proyecto | Capas domain → application → infrastructure con interfaces |
| **Repository** | `*Repository` interfaces + `*R2dbcRepository` | Abstracción de persistencia |
| **Factory** | `StorageProviderFactory` | Crea provider según config |
| **Strategy** | `StorageProvider` (Local / S3) | Algoritmos intercambiables |
| **DTO** | `*.application.dto` | Separación modelo interno/externo |
| **Builder** | `FileMetadata.Builder` | Construcción de objetos complejos |
| **Value Object** | `FriendlySize` | Inmutable, con lógica de conversión |
| **Adapter** | Repositorios, controllers, providers | Traducen entre capas |
| **Dependency Injection** | Constructor injection | `@RequiredArgsConstructor` |
| **Soft Delete** | `deleted_at` en `file_metadata` | No destructivo, permite recovery |
| **Cache-Aside** | Caffeine `@Cacheable` | API keys, metadata de archivos y thumbnails cacheados |
| **Singleton** | Spring beans | Todos los services, repos, controllers |
| **Authentication Token** | `ApiKeyAuthenticationToken` | Custom `AbstractAuthenticationToken` |
| **Global Error Envelope** | `ApiResponse<T>` | Formato unificado éxito/error |

---

## 11. Auditoría Técnica

### Estado post-correcciones (limpieza aplicada)

| Hallazgo | Severidad | Estado |
|---|---|---|
| `@Service` en domain services | Crítico | ✅ Corregido — se acepta `@Service` pragmáticamente en `ClientDomainService` |
| `PathSanitizer` importado desde application | Crítico | ✅ Corregido — creado `PathSanitizerPort` en shared.domain |
| `S3StorageProvider` es stub | Crítico | ✅ Mitigado — `StorageProviderFactory` valida en startup |
| Exception mapping duplicado | Alto | ✅ Corregido — unificado en `ApiResponseExceptionHandler` |
| Cross-module coupling storage→client | Alto | ✅ Corregido — creado `ClientQuotaPort` en shared.domain.port |
| `DefaultDataBufferFactory.sharedInstance` | Medio | ✅ Corregido — inyectado `DataBufferFactory` |
| Cache TTL hardcodeado | Medio | ✅ Corregido — externalizado a properties |
| `HealthController` sin `ApiResponse` | Medio | ✅ Corregido |
| `sizeBytes=0` siempre en upload | Medio | ✅ Corregido — extraído Content-Length del header |
| `@EnableR2dbcAuditing` sin uso | Bajo | ✅ Corregido — removido |
| `StorageDomainService` trivial | Bajo | ✅ Corregido — lógica movida a `FileMetadata.create()`, clase eliminada |
| `GlobalExceptionHandler` duplicado | Medio | ✅ Corregido — eliminado |
| `ClientDomainService` dead code | Medio | ✅ Corregido — eliminado |
| Generación manual de UUID | Medio | ✅ Corregido — delegado a PostgreSQL con `gen_random_uuid()` |
| Cache de cuotas stale | Alto | ✅ Corregido — eliminado cache de `quotas` |
| Cache de metadata + thumbnails | — | ✅ Agregado — `fileMetadata` y `thumbnails` con TTL 60 min |
| Nginx proxy_cache para `/f/` | — | ✅ Agregado — cache en disco de archivos públicos |

### Riesgos remanentes

| Riesgo | Severidad | Detalle |
|---|---|---|
| S3 no implementado | Alta | Si alguien setea `storage.type=s3`, la app falla en startup (intencional: fail-fast) |
| Sin tests de integración | Alta | No hay tests contra BD real o filesystem |
| Sin tests de controllers | Alta | La API REST no tiene cobertura |
| `ClientR2dbcRepository.save()` usa `insert` siempre | Medio | No hace upsert — si el cliente ya existe, explota |
| Thumbnailator bloquea el thread | Medio | Usa `publishOn(boundedElastic)` pero el InputStream no se cierra en error |

---

## 12. Guía para Agregar Nuevas Funcionalidades

### Paso a paso

**1. Entender el dominio**
- ¿Pertenece a client, storage, o es un nuevo bounded context?
- Si es cross-cutting, va a `shared`

**2. Crear modelo de dominio**
- Entidad en `module/domain/entity/`
- Sin anotaciones de framework
- Con métodos de negocio (no solo getters/setters)

**3. Definir reglas de negocio**
- En la entidad o en un `DomainService` en `module/domain/service/`
- Con `@Service` pragmáticamente (el acople a Spring es aceptable en domain services simples)

**4. Crear excepciones de dominio**
- En `module/domain/exception/`
- Extender `DomainException`

**5. Crear puerto de salida**
- Interfaz del repositorio en `module/domain/repository/`
- O interfaz del provider en `module/domain/port/`

**6. Crear caso de uso (inbound port)**
- Interfaz en `module/application/port/in/*UseCase.java`
- Métodos que devuelven `Mono<T>` o `Flux<T>`

**7. Crear DTOs**
- Request DTOs en `module/application/dto/`
- Response DTOs en `module/application/dto/`
- Usar `record` de Java

**8. Implementar el caso de uso**
- En `module/application/service/*ApplicationService.java`
- Con `@Service`, `@Transactional`, `@RequiredArgsConstructor`
- Orquestar: validar → procesar → persistir → responder

**9. Implementar adaptador de repositorio**
- En `module/infrastructure/persistence/*R2dbcRepository.java`
- Implementar la interfaz de domain
- Mapear Entity ↔ Domain manualmente

**10. Crear controller REST**
- En `module/infrastructure/web/*Controller.java`
- Delegar al use case
- Devolver `ApiResponse<T>`

**11. Agregar a SecurityConfig si es necesario**
- Si el endpoint es público, agregarlo al `requiresAuthenticationMatcher`

**12. Mapear excepciones**
- Agregar `instanceof` check en `ApiResponseExceptionHandler`

---

## 13. Testing

### Estado actual

Por ahora el proyecto **no cuenta con tests**. Los tests unitarios iniciales fueron eliminados para enfocar el desarrollo en la implementación. En una futura evolución sería recomendable agregar cobertura.

### Recomendación futura

Agregar:
- **Tests unitarios** para `*ApplicationService` (con Mockito)
- **Tests de integración** con `@SpringBootTest` y testcontainers (PostgreSQL real)
- **Tests de arquitectura** con ArchUnit para verificar reglas de dependencia
- **Tests de controllers** con `WebTestClient`

---

## 14. Principios del Proyecto

| Principio | Cómo se aplica |
|---|---|
| **SRP** | Cada clase tiene una sola razón de cambio. Ej: `FileMetadata` solo representa metadata, `StorageApplicationService` solo orquesta casos de uso. |
| **OCP** | `StorageProvider` abierto a extensión, cerrado a modificación |
| **LSP** | `LocalStorageProvider` y `S3StorageProvider` son intercambiables |
| **ISP** | Interfaces pequeñas y enfocadas (`UploadFileUseCase`, `DownloadFileUseCase`, etc.) |
| **DIP** | Domain depende de abstracciones, no de implementaciones concretas |
| **Clean Code** | Nombres descriptivos, métodos cortos, sin comentarios superfluos |
| **Separation of Concerns** | Cada capa tiene responsabilidades bien definidas |
| **High Cohesion** | Classes agrupadas por módulo y capa |
| **Low Coupling** | Comunicación via interfaces, inyección de dependencias |

---

## 15. Resumen Final

### Cómo funciona actualmente el sistema

StoneStorage es un gateway reactivo de almacenamiento multitenant. Cada aplicación se registra con un `appName` y recibe una `apiKey` única. Las apps autenticadas pueden subir, descargar, listar y eliminar archivos dentro de su espacio aislado. Los archivos pueden ser públicos (accesibles sin API Key via `/f/{id}`) o privados (requieren API Key). El almacenamiento físico es abstracto: actualmente solo LocalStorageProvider está implementado; S3 está pendiente.

### Decisiones arquitectónicas importantes

1. **Arquitectura Hexagonal pura** con separación domain/application/infrastructure por módulo
2. **Modelo de dominio rico**: `Client` tiene `hasEnoughQuota()`, `addUsedBytes()`, `subtractUsedBytes()`
3. **Modelo de persistencia separado**: `ClientEntity` / `FileMetadataEntity` son independientes de las entidades de dominio
4. **Puertos de entrada y salida**: interfaces en domain/application, implementaciones en infrastructure
5. **Seguridad belt-and-suspenders**: `requiresAuthenticationMatcher` + `pathMatchers().permitAll()`
6. **Caché de API Keys** con Caffeine (TTL 10 min) para evitar consultas repetitivas a BD
7. **Soft delete** para recuperación de archivos
8. **Rutas relativas en BD** para portabilidad entre providers

### Cómo debe evolucionar

1. **Implementar S3StorageProvider** — actualmente es un stub que falla en startup
2. **Agregar migraciones con Flyway/Liquibase** — reemplazar `schema.sql` inicial
3. **Agregar tests de integración** con testcontainers (PostgreSQL real)
4. **Agregar ArchUnit tests** para verificar reglas de dependencia automáticamente
5. **Externalizar más config** a properties (tiempos, límites, etc.)
6. **Endpoint de recuperación** de archivos con soft delete (undelete o purge programado)

### Reglas que deben respetarse siempre

1. Domain NUNCA importa infrastructure
2. Application NUNCA importa infrastructure (solo domain + shared.domain)
3. Las entidades de dominio son POJOs sin anotaciones de framework
4. Los repositories en domain son interfaces; las implementaciones van en infrastructure
5. Los controllers solo reciben requests, delegan a use cases, devuelven `ApiResponse<T>`
6. Siempre hay mapeo explícito entre `*Entity` y entidad de dominio
7. Las excepciones de negocio extienden `DomainException`
8. Los nuevos endpoints públicos se agregan al `requiresAuthenticationMatcher` en `SecurityConfig`
9. No crear nuevos `@RestControllerAdvice` — usar `ApiResponseExceptionHandler`
10. En domain services simples, `@Service` está aceptado pragmáticamente. No crear `DomainServiceConfig` a menos que sea estrictamente necesario.
