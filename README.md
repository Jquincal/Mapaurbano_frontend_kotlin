# Mapa Urbano — aplicación Android

Aplicación ciudadana de Mapa Urbano, desarrollada con Kotlin y Jetpack Compose. Es uno de los tres repositorios del sistema:

- backend común: Ktor + API REST;
- frontend web: Angular;
- frontend móvil: este repositorio.

La marca visible en la aplicación es **MuniReport**.

## Alcance implementado

- Mapa ilustrado y vista de lista con el mismo conjunto de reportes.
- Búsqueda y filtros combinables por estado y categoría.
- Detalle público sin exponer la identidad del vecino.
- Formulario con modalidades cuenta y anónima, borrador, ubicación y foto simuladas.
- Confirmación con código opaco para reportes anónimos.
- Seguimiento por código con historial de estados.
- Inicio de sesión, registro, perfil y Mis reportes.
- Estados de carga, vacío, error y validación.
- Navegación inferior de cuatro destinos y retorno correcto desde autenticación.
- Interfaz edge-to-edge con barra de navegación del sistema oculta de forma automática.

La app usa un repositorio falso en memoria y todavía no consume el backend, un SDK de mapas, cámara ni GPS reales.

## Datos de demostración

- Correo sugerido: `valeria@correo.com`
- Contraseña: cualquier valor de al menos 4 caracteres para iniciar sesión.
- Código de seguimiento existente: `7F2K-9B1M-4X3P`

## Requisitos

- JDK 17;
- Android SDK 36;
- Android Studio compatible con AGP 9, o Android CLI;
- dispositivo Android 8.0 (API 26) o superior.

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
│   ├── data/          # Estado, contenido ficticio y repositorio
│   ├── designsystem/  # Tema, color, tipografía y tokens
│   ├── model/         # Modelos de presentación
│   └── navigation/    # Destinos tipados
└── feature/
    ├── auth/
    ├── common/
    ├── map/
    ├── myreports/
    ├── report/
    └── tracking/
```

La dirección de dependencias se mantiene como `UI → ViewModel → Repository`. `FakeReportRepository` puede reemplazarse por una implementación REST cuando el contrato del backend esté versionado.

## Integración REST pendiente

La futura implementación remota deberá conservar el prefijo `/api/v1` y contemplar, como mínimo:

- `GET /reports` y `GET /reports/{id}`;
- `POST /reports`;
- `POST /report-status`;
- registro, login, sesión y `GET /users/me/reports`.

La estrategia y los comandos individuales están en [`docs/testing.md`](docs/testing.md).
