# Instrucciones del repositorio

- La raíz Git debe ser únicamente esta carpeta.
- No versionar `local.properties`, secretos, APK ni contenido de `build/`.
- Mantener la dirección de dependencias `UI → ViewModel → Repository`.
- Usar fakes para las pruebas; ninguna prueba debe depender del backend real.
- Antes de entregar cambios, ejecutar `./scripts/validate.sh`.
- La estrategia completa está documentada en `docs/testing.md`.
