# Supabase · conexión Android

Esta versión reemplaza la autenticación simulada por Supabase sin cambiar la referencia visual del HTML v10.

## 1. Variables de conexión

En el archivo `local.properties` del proyecto agrega:

```properties
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_KEY=sb_publishable_TU_CLAVE
```

Usa únicamente la **Publishable key**. Nunca pegues `service_role`, contraseña de base de datos ni secretos JWT en Android.

También puedes definir `SUPABASE_URL` y `SUPABASE_KEY` como variables de entorno del proceso de Gradle. `local.properties` tiene prioridad secundaria.

## 2. Parche SQL final

El esquema inicial ya crea tablas, RLS y `redeem_professor_code`. Como el SQL inicial no daba permisos de escritura a `professor_subjects`, ejecuta una vez:

`supabase/FINAL_PATCH.sql`

Después de ese parche, el profesor puede guardar sus materias desde Android.

## 3. OTP de 6 dígitos

Para conservar la pantalla de seis casillas del diseño:

Supabase Dashboard → Authentication → Email Templates → Confirm signup

Usa `{{ .Token }}` en el correo en vez de depender solamente de `{{ .ConfirmationURL }}`.

La app verifica ese código con Supabase Auth.

## 4. Catálogo de asignaturas

La app no inventa materias. Debes cargar el catálogo institucional en `public.subjects`.

Ejemplo temporal de QA:

```sql
insert into public.subjects(code, name) values
('PROG-I', 'Programación I'),
('PROG-II', 'Programación II'),
('EDD', 'Estructuras de Datos');
```

Cuando tengas el catálogo oficial, reemplaza esos datos de prueba.

## 5. Código docente

Cuando definan el código, crea su hash en Supabase; nunca lo pongas literal en Android:

```sql
insert into public.professor_registration_codes(code_hash, max_uses)
values (crypt('CODIGO-QUE-DEFINAN', gen_salt('bf')), 100);
```

## Flujo implementado

### Login
1. Usuario escribe `usuario` o `usuario@utadeo.edu.co`.
2. Supabase Auth valida email + password.
3. La app consulta `public.profiles` usando el usuario autenticado.
4. `role=student` → interfaz estudiante.
5. `role=professor` → interfaz profesor.

El Login nunca pregunta el rol.

### Registro estudiante
1. Selecciona Estudiante.
2. Nombre, usuario institucional y contraseña.
3. Supabase crea Auth user.
4. Trigger crea `profiles` con rol seguro `student`.
5. Verifica OTP.
6. La app crea/asegura `student_profiles`.
7. Entra a interfaz estudiante.

### Registro profesor
1. Selecciona Profesor.
2. Nombre, usuario institucional y contraseña.
3. Supabase crea la cuenta inicialmente como `student` por seguridad.
4. Verifica OTP.
5. Pantalla de código docente.
6. `redeem_professor_code()` valida del lado servidor y cambia el rol.
7. La app carga `subjects` y el profesor selecciona las que dicta.
8. Se guardan en `professor_subjects`.
9. Entra al panel docente.

## Arquitectura

- `core/network/SupabaseProvider.kt`: cliente Supabase.
- `domain/model`: modelos serializables y rol de aplicación.
- `domain/repository`: contratos.
- `data/auth`: Supabase Auth.
- `data/profile`: perfil y routing por rol.
- `data/professor`: código docente y materias.
- `auth/AuthViewModel.kt`: orquestación de UI; no contiene claves ni reglas de autorización.

La autorización real está en Supabase/RLS, no en Compose.
