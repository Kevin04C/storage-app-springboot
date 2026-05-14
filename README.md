# StoneStorage

Microservicio de Almacenamiento Multitenant diseñado con **Clean Architecture**, principios **SOLID** y patrones de diseño modernos.

## 1. Visión General

StoneStorage es un Gateway de Almacenamiento que centraliza la gestión de archivos de múltiples aplicaciones. Cada aplicación se identifica mediante una **API Key** y tiene asignado un espacio de trabajo aislado.

El sistema es agnóstico a la infraestructura física mediante una capa de abstracción (`StorageProvider`), permitiendo cambiar entre almacenamiento local y S3 sin modificar la lógica de negocio.

## 2. Arquitectura

El proyecto sigue **Clean Architecture** con una separación clara en capas:

- **Domain**: Entidades, value objects, excepciones de dominio, servicios de dominio y puertos (interfaces).
- **Application**: Casos de uso (inbound ports), DTOs, servicios de aplicación y lógica de orquestación.
- **Infrastructure**: Implementación de repositorios, controladores REST, filtros, configuración de frameworks y adaptadores externos.

### Estructura de módulos

```
com.stonestorage
├── shared
│   ├── domain
│   │   ├── exception
│   │   └── valueobject
│   └── infrastructure
│       ├── config
│       ├── util
│       └── web
│           └── dto
├── client
│   ├── domain
│   │   ├── entity
│   │   ├── exception
│   │   ├── repository
│   │   └── service
│   ├── application
│   │   ├── dto
│   │   ├── port
│   │   │   └── in
│   │   └── service
│   └── infrastructure
│       ├── persistence
│       │   └── entity
│       └── web
│           └── interceptor
└── storage
    ├── domain
    │   ├── entity
    │   ├── exception
    │   ├── port
    │   ├── repository
    │   └── service
    ├── application
    │   ├── dto
    │   ├── port
    │   │   └── in
    │   └── service
    └── infrastructure
        ├── persistence
        │   └── entity
        ├── provider
        └── web
```

## 3. Principios Aplicados

| Principio | Aplicación |
|-----------|------------|
| **SRP** | Cada clase tiene una sola razón de cambio. Por ejemplo, `ClientDomainService` solo gestiona lógica de cliente/cuota. |
| **OCP** | `StorageProvider` es una interfaz abierta a extensión (`LocalStorageProvider`, `S3StorageProvider`). |
| **LSP** | Los proveedores de almacenamiento son intercambiables sin afectar a la capa de aplicación. |
| **ISP** | Los casos de uso (`UploadFileUseCase`, `DownloadFileUseCase`) son interfaces pequeñas y enfocadas. |
| **DIP** | La capa de dominio depende de abstracciones (repositorios, proveedores), no de implementaciones concretas. |

## 4. Patrones de Diseño

- **Factory**: `StorageProviderFactory` crea la instancia correcta según configuración.
- **Strategy**: `StorageProvider` permite cambiar la estrategia de almacenamiento en tiempo de ejecución.
- **Dependency Injection**: Spring inyecta dependencias a través de constructores.
- **Repository**: Abstracción de acceso a datos para desacoplar el dominio de la tecnología de persistencia.
- **DTO**: Separación entre modelos de dominio y contratos de API.

## 5. Requerimientos Funcionales

### RF1: Seguridad y Validación
- Autenticación por API Key mediante **Spring Security Reactive** (`SecurityWebFilterChain` + `ApiKeyAuthenticationFilter`).
- Rutas públicas (`/f/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`, `/health`, `/actuator/**`, `/api/v1/client/register`) están **excluidas del filtro de autenticación** mediante un `requiresAuthenticationMatcher` personalizado, evitando que Spring Security intente autenticar requests sin API Key en endpoints abiertos.
- Sanitización de rutas para prevenir **Path Traversal** (`PathSanitizer`).

### RF2: Carga con Control de Cuota e Integridad
- Verificación de cuota antes de almacenar.
- Cálculo de **SHA-256** para integridad del archivo.
- Formato amigable de tamaños (ej: `"2 GB"` ↔ `2147483648`).

### RF3: Navegación "Google Drive Style"
- Endpoint `GET /api/v1/storage/list?path=/subfolder`.
- Respuesta con metadatos: `name`, `type`, `size`, `lastModified`.
- Generación automática de `thumbnailUrl` para imágenes.

### RF4: Generación de Miniaturas
- Redimensionamiento dinámico con Thumbnailator.
- Parámetros: `?w=200&h=200`.

## 6. Requerimientos No Funcionales

| RNF | Implementación |
|-----|----------------|
| Independencia de infraestructura | Propiedad `storage.type=local|s3` |
| Soft Delete | Campo `deleted_at` en `file_metadata` |
| Rendimiento | Caché con Caffeine para API Keys y cuotas |
| Consistencia | Todas las fechas en `TIMESTAMPTZ` (UTC) |

## 7. Tecnologías

- Java 21
- Spring Boot 4.0.6 (WebFlux + R2DBC + Security)
- PostgreSQL (R2DBC)
- Lombok
- Caffeine Cache
- Thumbnailator
- Commons IO
- SpringDoc OpenAPI

## 8. Base de Datos

### Tablas

**storage_clients**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | UUID | PK |
| `app_name` | VARCHAR | Nombre del proyecto |
| `api_key` | VARCHAR | Token de acceso (UNIQUE) |
| `base_dir` | VARCHAR | Carpeta raíz física |
| `quota_bytes` | BIGINT | Límite de espacio |
| `used_bytes` | BIGINT | Espacio consumido |
| `is_active` | BOOLEAN | Control de acceso |
| `created_at` | TIMESTAMPTZ | Fecha de registro (UTC) |

**file_metadata**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | UUID | PK |
| `client_id` | UUID | FK a storage_clients |
| `original_name` | VARCHAR | Nombre original del archivo |
| `system_name` | VARCHAR | Nombre único en disco (ej: `foto-a1b2c3d4.jpg`) |
| `checksum` | VARCHAR | SHA-256 del contenido |
| `size_bytes` | BIGINT | Tamaño del archivo |
| `visibility` | VARCHAR | `PUBLIC` o `PRIVATE` |
| `storage_path` | VARCHAR | Ruta relativa completa (sin `storage.base-path`). Ej: `/ClientFolder/imagenes/foto-abc.jpg` |
| `deleted_at` | TIMESTAMPTZ | Soft delete |

## 9. Endpoints

### Cliente
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/client/register` | Registrar nuevo cliente (`appName`, `quotaGb` opcional) |
| `GET` | `/api/v1/client/quota` | Información de cuota del cliente autenticado |

### Almacenamiento
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/storage/upload` | Subir archivo (multipart: `file` requerido, `path` opcional default `/`, `visibility` opcional default PUBLIC) |
| `GET` | `/api/v1/storage/download/{fileId}` | Descargar archivo privado (**requiere API Key**) |
| `GET` | `/f/{fileId}` | Previsualizar archivo público (**sin API Key**) |
| `GET` | `/api/v1/storage/list?path=/` | Listar contenido de carpeta |
| `DELETE` | `/api/v1/storage/{fileId}` | Eliminar archivo (soft delete) |
| `GET` | `/api/v1/storage/thumbnail?path=...&w=200&h=200` | Generar miniatura |

## 10. Formato de Respuesta

Todas las respuestas de la API siguen un formato estandarizado de producción:

### Éxito

```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "id": "...",
    "originalName": "photo.jpg",
    "systemName": "...",
    "relativePath": "/images",
    "checksum": "...",
    "sizeBytes": 2048,
    "visibility": "PUBLIC",
    "publicUrl": "/f/...",
    "privateUrl": "/api/v1/storage/download/..."
  },
  "error": null
}
```

### Error

```json
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

### Estructura

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `success` | `boolean` | `true` si la operación fue exitosa |
| `message` | `string` | Mensaje descriptivo de la operación |
| `data` | `T` | Payload de la respuesta (varía según el endpoint) |
| `error` | `ApiError` | Información del error (`code`, `details`) |
| `pagination` | `PaginationInfo` | *(Opcional)* Solo en endpoints paginados (`page`, `size`, `totalElements`, `totalPages`) |

## 11. Manejo de Errores Global

Todas las respuestas de error —incluyendo errores de autenticación, excepciones no manejadas y fallos de Spring WebFlux— se formatean automáticamente como `ApiResponse`. Esto asegura consistencia incluso cuando el error ocurre fuera del alcance de `@RestControllerAdvice`.

### Componentes

| Componente | Propósito |
|------------|-----------|
| `ApiResponseExceptionHandler` | `WebExceptionHandler` con `@Order(-2)` que intercepta **todas** las excepciones no manejadas y las serializa como `ApiResponse` usando `ObjectMapper`. |
| `SecurityConfig` | Configura `ServerAuthenticationFailureHandler` y `ServerAuthenticationEntryPoint` personalizados para que los errores 401 de Spring Security retornen `ApiResponse` JSON en lugar de respuestas vacías. |
| `JacksonConfig` | Expone un bean `ObjectMapper` utilizado por los handlers de error para serializar respuestas. |

### Mapeo de excepciones conocidas

| Excepción | HTTP Status | Código de error |
|-----------|-------------|-----------------|
| `ClientNotFoundException` | 401 | `UNAUTHORIZED` |
| `ClientAlreadyExistsException` | 409 | `CLIENT_ALREADY_EXISTS` |
| `QuotaExceededException` | 413 | `QUOTA_EXCEEDED` |
| `FileNotFoundException` | 404 | `FILE_NOT_FOUND` |
| `StorageException` | 500 | `STORAGE_ERROR` |
| `DomainException` | 400 | `DOMAIN_ERROR` |
| `ResponseStatusException` | Según status | Nombre del status |
| Otras excepciones | 500 | `INTERNAL_ERROR` |

## 12. Visibilidad de Archivos

Cada archivo tiene un campo `visibility` (enum `FileVisibility`, default: `PUBLIC`) que determina cómo puede accederse:

| Visibilidad | Endpoint público | Endpoint privado | Uso típico |
|-------------|------------------|------------------|------------|
| `PUBLIC` | `GET /f/{fileId}` (sin API Key, con `Content-Type` automático para visualización en navegador) | `GET /api/v1/storage/download/{fileId}` (con API Key) | Imágenes de noticias, avatares, contenido público |
| `PRIVATE` | **No disponible** (404) | `GET /api/v1/storage/download/{fileId}` (con API Key, fuerza descarga) | Documentos, contratos, contenido restringido |

### Ejemplo de uso

Subir una imagen pública:
```bash
curl -X POST http://localhost:8080/api/v1/storage/upload \
  -H "X-API-KEY: tu-api-key" \
  -F "file=@foto.jpg" \
  -F "path=/imagenes/productos" \
  -F "visibility=PUBLIC"
```

Si la carpeta `/imagenes/productos` no existe, se creará automáticamente.

Respuesta:
```json
{
  "publicUrl": "/f/81a42f78-006d-4ee3-a767-3d1ce1869de2",
  "privateUrl": "/api/v1/storage/download/81a42f78-006d-4ee3-a767-3d1ce1869de2"
}
```

Usar en HTML:
```html
<img src="http://storage-host/f/81a42f78-006d-4ee3-a767-3d1ce1869de2" />
```

### Recomendación para consumidores

- Guarda el `fileId` y la `publicUrl` en tu base de datos.
- Para contenido público (noticias, blogs), usa siempre la `publicUrl`.
- Para contenido privado, usa el endpoint `/download/{fileId}` con API Key.

## 12. Configuración

```properties
spring.application.name=stonestorage

# Database
spring.r2dbc.url=r2dbc:postgresql://host:5432/storage_app
spring.r2dbc.username=your_user
spring.r2dbc.password=your_pass

# Storage provider: local | s3
storage.type=local
# Ruta absoluta donde se almacenan los archivos (montar como volumen en Docker)
# En Windows usar forward slashes: D:/storage (NO D:\storage)
storage.base-path=/data/stonestorage
```

> **Nota Windows**: Usar forward slashes (`D:/storage`) o doble backslash (`D:\\storage`) en `storage.base-path`. Un solo backslash se interpreta como carácter de escape.

### Estructura de almacenamiento

Los archivos se guardan con la siguiente estructura:

```
{storage.base-path}/
└── {clientBaseDir}/
    └── {path}/
        └── {originalName}-{uniqueId}.{extension}
```

**Ejemplo**: Si `storage.base-path=/data/stonestorage`, el cliente tiene `baseDir=app123`, y sube `foto.jpg` a `path=/imagenes/productos`:

| Ubicación | Valor |
|-----------|-------|
| **Disco** | `/data/stonestorage/app123/imagenes/productos/foto-a1b2c3d4.jpg` |
| **BD (`storage_path`)** | `/app123/imagenes/productos/foto-a1b2c3d4.jpg` |

> **Portabilidad**: En BD solo se guarda la ruta relativa (sin `storage.base-path`). Esto permite cambiar el proveedor de almacenamiento o mover archivos a otro servidor sin modificar la base de datos.

### Docker

En Docker, monta un volumen persistente para `storage.base-path`:

```yaml
volumes:
  - /ruta/en/host/storage:/data/stonestorage
```

## 13. Compilación y Ejecución

```bash
# Compilar
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Ejecutar aplicación
./mvnw spring-boot:run
```

## 14. Documentación de API (Swagger/OpenAPI)

La API expone documentación interactiva mediante **SpringDoc OpenAPI**.

### Acceso

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | UI interactiva de Swagger |
| `http://localhost:8080/v3/api-docs` | Spec JSON de OpenAPI 3 |

### Autenticación en Swagger

- Haz clic en **Authorize** y proporciona tu `X-API-KEY`.
- Los endpoints marcados con 🔒 requieren autenticación.
- `POST /api/v1/client/register` es público (no requiere API Key).

### Tags disponibles

- **Cliente** — Registro y consulta de cuotas
- **Almacenamiento** — Subida, descarga, listado, eliminación y miniaturas
- **Vista Pública** — Acceso público a archivos (`/f/{fileId}`) sin API Key

## 15. Flujo de Carga

1. Cliente envía `file` (requerido) + `path` (opcional, default `/`, ej: `/imagenes/productos`) + `visibility` (opcional, default `PUBLIC`) + API Key (`X-API-KEY`). Si las carpetas del path no existen, se crean automáticamente. El archivo se guarda con su nombre original + sufijo único (ej: `foto-a1b2c3d4.jpg`).
2. **Spring Security** valida la key y estado `is_active` vía `ApiKeyAuthenticationFilter`. Las rutas públicas (`/f/**`, `/swagger-ui/**`, `/health`, etc.) se excluyen del filtro vía `requiresAuthenticationMatcher`.
3. `ClientContextFilter` inyecta datos del cliente en el request (`clientId`, `baseDir`, `quotaBytes`, `usedBytes`).
4. Servicio verifica cuota disponible.
5. Se sanitiza la ruta destino.
6. Se calcula SHA-256 del contenido.
7. `StorageProvider` escribe el archivo físicamente.
8. Se registra metadata en `file_metadata` con visibilidad asignada.
9. Se responde con `UploadResponse` (ID, URL de acceso).

---

*Proyecto generado con Clean Architecture, SOLID y patrones de diseño.*
