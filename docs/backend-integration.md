# Integración Android con el backend

Contrato aplicado el 18/10/2026: OpenAPI de `ZeroVeCC/MapaUrbano-backend-ktor`, funcionalidad de `34d219e`, documentación de `907e0c3`.

| Uso | Método y ruta bajo /api/v1 | Autorización |
|---|---|---|
| Categorías activas | GET /categories | Pública |
| Registro / login | POST /users/register · POST /users/login | DNI + contraseña |
| Perfil / baja | GET /users/me · DELETE /users/me | Bearer |
| Logout | POST /users/logout | Bearer |
| Mapa y Mis reportes | GET /users/me/reports?cursor=&limit=50 | Bearer |
| Detalle e historial | GET /users/me/reports/{id} | Bearer, solo propio |
| Fotografía | GET /users/me/reports/{id}/image | Bearer, solo propia |
| Crear reporte | POST /reports | Bearer y multipart |

Las lecturas públicas de reportes fueron retiradas. La app carga únicamente los reportes del usuario autenticado. No se mezclan resultados entre cuentas ni se reemplazan fallos por datos de demostración. `DemoContent` solo se utiliza en fakes y previews.

## Sesiones

El token se conserva en DataStore. La restauración ocurre antes de entrar al mapa. Logout, desactivación y 401 eliminan reportes, imágenes, filtros, navegación y borrador. Al cerrar sesión se borra el estado privado inmediatamente y se elimina el token guardado antes de pedir su revocación al servidor; esa solicitud tiene un límite de 10 segundos. Se conserva el borrador únicamente cuando vence la sesión durante el envío. Las respuestas tardías se descartan al cambiar o cerrar sesión. Al autenticarse comienza un estado privado nuevo.

## Lista, detalle y fotografías

`nextCursor` se conserva como texto opaco y se envía como parámetro. «Cargar más reportes» agrega elementos sin duplicar IDs; finaliza con null y permite reintentar un error de página. Si el servidor repite un cursor, se detiene la carga y se informa el problema.

Las categorías se resuelven por `categorySlug`. El detalle incorpora descripción, nombre de categoría, historial y coordenadas; se descartan coordenadas no finitas o fuera de rango. El mapa permanece visible aun sin coincidencias.

La galería usa Photo Picker; la cámara usa TakePicture y FileProvider limitado a una carpeta temporal privada. Se leen URIs con permisos temporales, sin convertirlas en rutas de almacenamiento compartido. El adjunto se mantiene en memoria, se valida MIME declarado/decodificado, tamaño máximo 5 MiB y dimensiones de 1 a 12000 píxeles por lado. Los archivos de cámara se borran después de leerlos o cancelar. Los formatos admitidos son JPEG, PNG y WebP; los archivos que superan el límite se rechazan conservando los campos del borrador.

El multipart envía `title`, `description`, `categorySlug`, `latitude`, `longitude`, `submissionMode=ACCOUNT` e `image` opcional con bytes y tipo real. La imagen del detalle se descarga con Bearer, se conserva como máximo la del detalle activo y se decodifica reducida a 1200 píxeles. No hay caché persistente de imágenes.

## Errores

400 muestra validaciones del servidor; 401 limpia sesión; 404 indica información no disponible; 409, 429 y 5xx tienen mensajes distintos. Los errores de conexión indican servidor inaccesible. Una carga fallida conserva datos únicamente de la misma sesión e informa el error; nunca simula un éxito.

## URL del backend

Configurar `MAPA_URBANO_API_BASE_URL` en `local.properties`, ignorado por Git, o mediante propiedad Gradle. La URL queda incorporada al compilar:

- Emulador: `http://10.0.2.2:8080/api/v1/` (predeterminada).
- Teléfono USB: `http://127.0.0.1:8080/api/v1/` y `adb reverse tcp:8080 tcp:8080`.
- Teléfono por Wi-Fi: `http://IP_DE_LA_COMPUTADORA:8080/api/v1/`; ambos dispositivos deben compartir una red que permita el puerto.

HTTP está habilitado solo en debug. Producción requiere HTTPS. Backend, panel y teléfono deben utilizar la misma API y la misma base de pruebas.

## Validación

`./scripts/validate.sh` ejecuta unitarias con repositorios falsos/MockEngine, Lint, APK y JaCoCo. Los tests no necesitan backend ni base de datos. La prueba HTTP real del entorno es complementaria; no demuestra por sí sola el funcionamiento del selector de fotos ni de la cámara del dispositivo.
