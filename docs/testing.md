# Estrategia de validación

## Objetivo

Validar que la aplicación compile, respete las comprobaciones estáticas y mantenga cubierta la lógica que no depende del framework Android.

## Comando único

Desde la raíz del repositorio:

```bash
./scripts/validate.sh
```

El script comprueba:

1. que la raíz Git sea exactamente esta carpeta y no un directorio padre;
2. que `local.properties` permanezca ignorado;
3. que no aparezcan patrones comunes de claves privadas o tokens;
4. espacios o marcadores de conflicto mediante Git;
5. pruebas unitarias;
6. Android Lint;
7. construcción del APK debug;
8. generación del informe JaCoCo.

## Capas de pruebas

| Tipo | Ubicación | Ejecución | Propósito |
|---|---|---|---|
| Unitarias | `app/src/test` | `./gradlew testDebugUnitTest` | ViewModels, flujos y repositorios usando fakes. |
| UI instrumentada | `app/src/androidTest` | `./gradlew connectedDebugAndroidTest` | Navegación e interacción Compose en dispositivo/emulador. |
| Análisis estático | Código completo | `./gradlew lintDebug` | APIs incompatibles, recursos y problemas Android. |
| Cobertura | Unitarias | `./gradlew jacocoDebugReport` | Informe HTML y XML de las pruebas locales. |

El informe de cobertura se genera en `app/build/reports/jacoco/jacocoDebugReport/html/index.html`.

## Reglas para nuevas funcionalidades

- Probar ViewModels y repositorios con fakes; no probar composables como tests unitarios.
- Agregar pruebas Compose para navegación y acciones visibles.
- Cubrir carga, éxito, vacío y error cuando una pantalla obtenga datos remotos.
- Ejecutar pruebas de permisos, cámara, ubicación y edge-to-edge en emulador o dispositivo.
- No usar el backend real en pruebas de UI; reemplazarlo mediante el repositorio.
