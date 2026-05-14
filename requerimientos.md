Especificación Maestra: Microservicio de Almacenamiento "StoneStorage"

## 1. Visión General
El sistema es un Gateway de Almacenamiento Multitenant diseñado para centralizar la gestión de archivos de múltiples aplicaciones. Cada aplicación se identifica mediante una API Key y tiene asignado un espacio de trabajo aislado. El sistema es agnóstico a la infraestructura física mediante una capa de abstracción.

## 2. Arquitectura de Software (Desacoplamiento)
### Interfaz Core: StorageProvider
Para garantizar que el sistema no esté acoplado, toda operación de entrada/salida de archivos debe pasar por esta interfaz:

```java
public interface StorageProvider {
    void save(InputStream file, String fullPath);
    InputStream load(String fullPath);
    List<FileNode> listContents(String path);
    void delete(String fullPath); // Implementa Soft Delete opcionalmente
    long getFolderSize(String path);    
}
```
## 3. Modelo de Datos Robusto (SQL)
### Tabla: storage_clients
| Campo         | Tipo          | Descripción                                     |
| ------------- | ------------- | ----------------------------------------------- |
| `id`          | `UUID`        | Identificador único (PK).                       |
| `app_name`    | `VARCHAR`     | Nombre del proyecto.                            |
| `api_key`     | `VARCHAR`     | Token de acceso (`UNIQUE INDEX`).               |
| `base_dir`    | `VARCHAR`     | Carpeta raíz física (Ej: `news_app_folder`).    |
| `quota_bytes` | `BIGINT`      | Límite de espacio (Ej: `1073741824` para 1 GB). |
| `used_bytes`  | `BIGINT`      | Espacio consumido actualmente.                  |
| `is_active`   | `BOOLEAN`     | Control de acceso administrativo.               |
| `created_at`  | `TIMESTAMPTZ` | Fecha de registro (UTC).                        |


### Tabla: file_metadata
Registra la información de cada archivo para permitir búsquedas y auditoría.

| Campo           | Tipo          | Descripción                                   |
| --------------- | ------------- | --------------------------------------------- |
| `id`            | `UUID`        | PK.                                           |
| `client_id`     | `UUID`        | FK hacia `storage_clients`.                   |
| `original_name` | `VARCHAR`     | Nombre que el usuario subió.                  |
| `system_name`   | `VARCHAR`     | Nombre único en disco (`UUID + Ext`).         |
| `relative_path` | `VARCHAR`     | Ruta dentro de la app (Ej: `/fotos/perfil/`). |
| `checksum`      | `VARCHAR`     | Hash SHA-256 para integridad.                 |
| `size_bytes`    | `BIGINT`      | Tamaño del archivo.                           |
| `deleted_at`    | `TIMESTAMPTZ` | Si no es nulo, el archivo está en “papelera”. |


### 4. Requerimientos Funcionales (RF)
RF1: Seguridad y Validación (Interceptor)
Validación de Key: Verificar X-API-KEY y estado is_active.

Seguridad de Rutas: El sistema debe normalizar y sanitizar los paths para prevenir Path Traversal. Ningún cliente puede subir o listar archivos fuera de su base_dir.

RF2: Carga con Control de Cuota e Integridad
Proceso:

Verificar que size_bytes del nuevo archivo + used_bytes < quota_bytes.

Calcular checksum (SHA-256) del flujo de datos.

Guardar vía StorageProvider.

Actualizar used_bytes en storage_clients.

RF3: Navegación "Google Drive Style"
Endpoint: GET /list?path=/subfolder

Salida: Lista de objetos con metadatos:

name, type (FILE/FOLDER), size, lastModified.

Si es imagen, incluir una thumbnail_url.

RF4: Generación de Miniaturas (Thumbnails)
El servicio debe ser capaz de redimensionar imágenes dinámicamente mediante parámetros (ej: ?w=200&h=200) para no sobrecargar el frontend con archivos pesados.

RF5: Registro de Clientes
Endpoint: POST /api/v1/client/register

Entrada:

app_name (requerido): Nombre de la aplicación.

quota_gb (opcional): Cuota en GB. Por defecto: 20 GB.

Proceso:

Validar que app_name no exista previamente.

Generar api_key automáticamente (UUID).

Construir base_dir en PascalCase + "_folder" (Ej: "news app" → "NewsApp_folder").

Convertir quota_gb a bytes para almacenamiento.

Respuesta: Datos del cliente registrado incluyendo api_key y base_dir.

### 5. Requerimientos No Funcionales (RNF)
Independencia de Infraestructura: El sistema debe poder cambiar entre LocalStorageProvider y S3StorageProvider mediante la propiedad storage.type en el archivo de configuración.

Borrado Lógico (Soft Delete): Al eliminar un archivo, este se mueve a una carpeta oculta o se marca en BD para permitir recuperación durante 30 días.

Rendimiento: Implementar caché para las consultas de API-KEY y límites de cuota para evitar latencia en la base de datos.

Consistencia: Todas las fechas se manejan en TIMESTAMPTZ para soportar múltiples zonas horarias.

### 6. Lógica de Presentación de Datos (Friendly Format)
Para que el sistema sea simple pero robusto, se implementará la siguiente lógica de conversión en la capa de servicios:
Escritura (Admin): Entrada "2GB" $\rightarrow$ Lógica de conversión $\rightarrow$ Guardado 2147483648 en BD.Lectura (User): Lectura 2147483648 de BD $\rightarrow$ Lógica de formato $\rightarrow$ Respuesta API "quota": "2 GB".

### 7. Flujo Lógico de una Operación de Carga
Cliente: Envía Archivo + Path (/noticias/fotos) + API Key.

Gateway:

Valida la Key y que la App esté activa.

Verifica que la App tenga espacio disponible (Cuota).

Sanitiza la ruta: /storage/news_app_folder/noticias/fotos/archivo.jpg.

StorageProvider: Ejecuta la escritura física (en Disco o Nube).

Base de Datos: Registra el archivo con su Checksum y actualiza el contador de espacio usado de la App.

Respuesta: Devuelve un JSON con el ID del archivo y la URL de acceso.
