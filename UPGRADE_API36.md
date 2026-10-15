# Tadeo Conecta v6 — actualización de compilación

Configuración actual:
- Android Gradle Plugin: 8.9.1
- Gradle Wrapper: 8.11.1
- compileSdk: 36
- targetSdk: 35
- minSdk: 26
- JVM: 17
- Supabase BOM: 3.5.0

## Credenciales de Supabase
El ZIP fuente no contenía el archivo `local.properties` real (solo `local.properties.example`), por lo que las claves privadas/locales del computador no pueden copiarse automáticamente.

Copia tu `local.properties` actual a la raíz de esta carpeta, o agrega al `local.properties` que Android Studio genere:

```properties
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_KEY=sb_publishable_TU_CLAVE
```

No uses `service_role`.

## Primera apertura
1. Abrir esta carpeta directamente en Android Studio.
2. File > Sync Project with Gradle Files.
3. Build > Clean Project.
4. Build > Rebuild Project.
5. Ejecutar con Run.
