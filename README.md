# Tadeo Conecta · Android v5 · Supabase

Proyecto Android nativo con Kotlin + Jetpack Compose. Mantiene las visuales bloqueadas a partir del HTML v10 y conecta el módulo de acceso a Supabase.

## Arranque rápido

1. Abre **esta carpeta** completa en Android Studio.
2. Copia tus valores al `local.properties`:
   - `SUPABASE_URL=...`
   - `SUPABASE_KEY=...`
3. Si vienes exactamente del SQL que se entregó antes, ejecuta una vez `supabase/FINAL_PATCH.sql`.
4. En Supabase, cambia el template de confirmación para mostrar `{{ .Token }}` si quieres usar el OTP visual de 6 dígitos.
5. Sync Gradle.
6. Run ▶.

Consulta `SUPABASE_SETUP.md` para el flujo completo.

## Regla visual

`UI_PARITY_LOCK.md` sigue vigente: el HTML v10 es la fuente visual canónica. Supabase reemplaza datos simulados; no autoriza rediseñar las pantallas.
