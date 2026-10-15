# Mapa Urbano — aplicación Android

Aplicación ciudadana de Mapa Urbano, desarrollada con Kotlin y Jetpack Compose. Es uno de los tres repositorios del sistema:

- backend común: Ktor + API REST;
- frontend web: Angular;
- frontend móvil: este repositorio.

La marca visible en la aplicación es **MuniReport**.

## Alcance implementado

- Mapa interactivo OpenFreeMap/MapLibre y vista de lista alimentados por los reportes públicos del backend.
- Búsqueda y filtros combinables por estado y categoría.
- Detalle público sin exponer la identidad del vecino.
- Formulario conectado al backend, con borrador, ubicación manual o GPS; la foto continúa simulada.
- Acceso obligatorio mediante registro o inicio de sesión por DNI antes de entrar a la aplicación.
- Creación de reportes exclusivamente vinculados a una cuenta y autorizados mediante token bearer.
- Confirmación, perfil, desactivación y seguimiento de avances desde Mis reportes.
- Estados de carga, vacío, error y validación.
- Navegación inferior de tres destinos: Mapa, Reportar y Mis reportes.
- Interfaz edge-to-edge con barra de navegación del sistema oculta de forma automática.

La app consume la API Ktor de `MapaUrbano-backend-ktor`. El mapa usa MapLibre Native con el estilo Liberty de OpenFreeMap y permite gestos, selección manual y centrado mediante el GPS del dispositivo. La cámara y el envío de fotografías reales continúan pendientes.

## Datos de prueba

- Para crear una cuenta se requiere nombre, DNI argentino de 7 u 8 dígitos y contraseña de al menos 8 caracteres.
- El DNI puede escribirse con o sin puntos; se envía como texto para conservar posibles ceros iniciales.
- Para crear un reporte es obligatorio iniciar sesión; el frontend no expone creación ni seguimiento anónimos.

## Requisitos

- JDK 17;
- Android SDK 36;
- Android Studio compatible con AGP 9, o Android CLI;
- dispositivo Android 8.0 (API 26) o superior.

## Conexión con el backend

La URL se define con la propiedad `MAPA_URBANO_API_BASE_URL`. Para un teléfono físico conectado a la misma red que la computadora, agregala en `local.properties` (archivo local, ignorado por Git):

```properties
MAPA_URBANO_API_BASE_URL=http://IP_DE_LA_COMPUTADORA:8080/api/v1/
```

El valor predeterminado es `http://10.0.2.2:8080/api/v1/`, útil solamente para un emulador. El backend debe estar escuchando en `0.0.0.0:8080` y el teléfono debe poder acceder a ese puerto.

## Verificación

```bash
./scripts/validate.sh
```

La validación ejecuta pruebas unitarias, Android Lint, generación del APK debug y reporte JaCoCo. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

La interfaz también fue recorrida en un Redmi Note 13 Pro 5G físico. El emulador no formó parte de esta validación, por decisión del equipo.

## Estructura

```text
app/src/main/java/com/mapaurbano/mobile/
├── MainActivity.kt
├── MapaUrbanoApp.kt
├── core/
│   ├── data/          # Estado y repositorios remoto/falso
│   ├── designsystem/  # Tema, color, tipografía y tokens
│   ├── model/         # Modelos de presentación
│   ├── network/       # Cliente Ktor y contrato de la API
│   └── navigation/    # Destinos tipados
└── feature/
    ├── auth/
    ├── common/
    ├── map/
    ├── myreports/
    └── report/
```

La dirección de dependencias se mantiene como `UI → ViewModel → Repository`. La aplicación usa `RemoteReportRepository`; `FakeReportRepository` queda reservado para pruebas y vistas sin red.

## Estado de la integración REST

Ya están conectados:

- `GET /categories`;
- `GET /reports` y `GET /reports/{id}`;
- `POST /reports` únicamente con token bearer y modalidad `ACCOUNT`;
- registro, login, perfil, logout y desactivación bajo `/users`;
- `GET /users/me/reports` para Mis reportes.

El envío real de imágenes y el detalle privado de un reporte continúan pendientes del backend. El detalle técnico y la matriz de endpoints están en [`docs/backend-integration.md`](docs/backend-integration.md).

La estrategia y los comandos individuales están en [`docs/testing.md`](docs/testing.md).

Los problemas detectados durante las pruebas en dispositivo y sus responsables están registrados en [`docs/proximas-correcciones.md`](docs/proximas-correcciones.md).
