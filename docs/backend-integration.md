# Integración con MapaUrbano backend

Backend de referencia: `https://github.com/ZeroVeCC/MapaUrbano-backend-ktor.git`.

## Funciones conectadas

| Función móvil | Endpoint | Estado |
| --- | --- | --- |
| Categorías activas | `GET /api/v1/categories` | Integrada |
| Marcadores y lista del mapa | `GET /api/v1/reports` | Integrada |
| Detalle público | `GET /api/v1/reports/{id}` | Integrada |
| Registro con DNI | `POST /api/v1/users/register` | Integrada |
| Inicio de sesión con DNI | `POST /api/v1/users/login` | Integrada |
| Recuperar sesión y perfil | `GET /api/v1/users/me` | Integrada |
| Cerrar sesión | `POST /api/v1/users/logout` | Integrada |
| Desactivar cuenta | `DELETE /api/v1/users/me` | Integrada |
| Crear reporte con cuenta | `POST /api/v1/reports` + bearer | Integrada |
| Mis reportes | `GET /api/v1/users/me/reports` | Integrada (primera página) |

El frontend utiliza directamente las coordenadas geográficas del backend para ubicar marcadores en OpenFreeMap mediante MapLibre Native. La consulta pública usa el área de Mendoza (`-32.96/-68.93` a `-32.82/-68.76`) y el backend devuelve como máximo 50 elementos. Limitar el área reduce la consulta espacial, aunque el backend `cb698f8` también corrigió el listado sin límites y la decodificación persistente de coordenadas PostGIS.

## Configuración local

La propiedad `MAPA_URBANO_API_BASE_URL` se puede declarar como propiedad de Gradle o en `local.properties`:

```properties
MAPA_URBANO_API_BASE_URL=http://192.168.1.39:8080/api/v1/
```

El permiso de tráfico HTTP sin TLS está habilitado únicamente en la variante `debug`, para poder probar el servidor local. Una distribución de producción debe usar HTTPS.

## Sesión ciudadana

El acceso usa DNI y contraseña y es obligatorio antes de entrar al mapa o usar cualquier función principal. El token bearer se guarda en DataStore, se restaura al abrir la aplicación y se elimina al cerrar sesión, desactivar la cuenta o recibir una sesión inválida en una llamada protegida. Las cuentas se registran con nombre, DNI y contraseña; el correo ya no forma parte de este contrato.

El frontend no ofrece creación ni seguimiento anónimos. Cada `POST /reports` exige un token bearer, envía `submissionMode=ACCOUNT` y vincula el reporte con la sesión activa. Los avances se consultan desde `GET /users/me/reports`.

Para que esta regla sea una garantía de seguridad y no solamente una restricción de interfaz, el backend también debe rechazar cualquier `POST /reports` sin bearer válido y cualquier intento de usar `submissionMode=ANONYMOUS`. Ese cambio corresponde al equipo del backend y no se realizó desde este repositorio.

## Pendientes del contrato del backend

- `GET /users/me/reports` devuelve actualmente el identificador de categoría dentro de `categorySlug`; el cliente acepta ambos valores para mantener la compatibilidad.
- La lista de Mis reportes consume por ahora la primera página (hasta 50 elementos). Se agregará paginación cuando el cursor del backend esté validado de punta a punta.
- `GET /users/me/reports/{id}` todavía responde como no implementado; el detalle se consulta mediante la ruta pública.
- Los DTO públicos devuelven `hasImage = false` y el historial vacío en la implementación actual; el cliente ya admite ambos campos cuando el backend los complete.
- La interfaz permite seleccionar una foto de ejemplo, pero aún no conserva bytes reales para adjuntarlos al multipart.
- El cursor de la lista pública está presente en el contrato, pero la consulta del backend todavía no lo aplica; el cliente evita paginar para no repetir indefinidamente la primera página.

## Separación para pruebas

`RemoteReportRepository` y `RemoteAuthRepository` son las implementaciones usadas por la aplicación. Las pruebas unitarias inyectan repositorios falsos, por lo que son deterministas y no requieren red ni una base de datos activa.
