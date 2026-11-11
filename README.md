# Tadeo Conecta

**Plataforma móvil de evaluación docente para la Universidad de Bogotá Jorge Tadeo Lozano (UTadeo).**
Los estudiantes encuentran profesores, consultan reseñas confiables y evalúan su experiencia de forma responsable; los profesores reciben retroalimentación agregada y accionable para mejorar su práctica docente.

> Proyecto de grado · Android nativo (Kotlin + Jetpack Compose) · Backend en Supabase · Moderación y recomendaciones con IA (Gemini)

---

## Tabla de contenido

1. [Contexto y problema](#contexto-y-problema)
2. [Propuesta de valor y decisiones de diseño](#propuesta-de-valor-y-decisiones-de-diseño)
3. [Funcionalidades](#funcionalidades)
4. [Arquitectura](#arquitectura)
5. [Stack tecnológico](#stack-tecnológico)
6. [Estructura del repositorio](#estructura-del-repositorio)
7. [Puesta en marcha](#puesta-en-marcha)
8. [Base de datos (Supabase)](#base-de-datos-supabase)
9. [Funciones de IA (Edge Functions)](#funciones-de-ia-edge-functions)
10. [Seguridad y privacidad](#seguridad-y-privacidad)
11. [Accesibilidad](#accesibilidad)
12. [Pruebas con usuarios](#pruebas-con-usuarios)
13. [Documentación adicional](#documentación-adicional)
14. [Limitaciones conocidas y trabajo futuro](#limitaciones-conocidas-y-trabajo-futuro)
15. [Autores](#autores)

---

## Contexto y problema

Elegir profesor y materia es una decisión que afecta el rendimiento y la experiencia académica de cada estudiante, pero hoy esa información circula de forma informal (chats, conocidos, redes) y con tres problemas recurrentes:

- **Las calificaciones numéricas no son comparables.** "3 estrellas" significa cosas distintas para cada persona.
- **Los comentarios abiertos pueden ser injustos o dañinos.** Un solo estudiante molesto puede convertirse en la única voz pública sobre un profesor, y los insultos o ataques personales no aportan información útil.
- **Los profesores rara vez reciben retroalimentación útil.** Lo que les llega es ruido o evaluaciones tardías sin pasos concretos para mejorar.

Tadeo Conecta busca resolver esto con una evaluación **estructurada, justificada, moderada y orientada a la mejora**, en lugar de un simple "ranking de profesores".

## Propuesta de valor y decisiones de diseño

| Decisión | Por qué |
|---|---|
| **Evaluación por tarjetas, no por estrellas.** Para cada categoría el estudiante elige la frase que mejor describe su experiencia; cada tarjeta tiene un peso interno que el estudiante no ve. La estrella pública es el promedio de esos pesos. | Evita que "3 estrellas" signifique cosas distintas para cada persona y hace la calificación más descriptiva y menos emocional. |
| **6 categorías acotadas a conducta observable:** Claridad, Organización, Justicia al calificar, Disponibilidad, Trato al estudiante y Ambiente de clase. | Mide el desempeño profesional, no la simpatía ni la apariencia. |
| **Justificación obligatoria en las respuestas más críticas.** Si el estudiante elige una de las dos tarjetas más negativas debe explicar la situación concreta (con mínimo de palabras). | Un juicio negativo sin contexto no sirve ni al profesor ni a otros estudiantes. |
| **Una pregunta por pantalla (flujo tipo "wizard").** | Reduce la carga cognitiva y permite revisar cada respuesta apenas se escribe. |
| **Peso por asistencia.** El estudiante declara su porcentaje de asistencia; con menos de 40 % la evaluación se guarda pero no cuenta en el promedio ni se muestra su comentario. | Las opiniones de quienes sí asisten tienen más peso que las de quien no vio suficiente clase. |
| **Umbral mínimo de reseñas (5) antes de mostrar comentarios abiertos.** Antes de eso solo se ven promedios y datos rápidos. | Evita que una sola persona sea la voz pública sobre un profesor. |
| **Solo se muestran comentarios sobre el curso** (el tip práctico y "lo más difícil"). Las justificaciones de calificaciones bajas y el motivo de recomendación **nunca** se muestran públicamente. | Protege a las personas de juicios directos y mantiene el foco en el curso. |
| **Moderación con IA paso a paso** y no solo al final. | El estudiante se entera de un problema en el momento y no después de llenar todo el formulario. |
| **El profesor puede reportar reseñas** con un motivo fijo (insultos, acoso, discriminación, contenido falso, spam, otro); una IA revisa el reporte. | Defensa adicional para los dos comentarios que sí se muestran. |

## Funcionalidades

### Acceso e identidad institucional
- Registro e inicio de sesión con correo institucional (`usuario@utadeo.edu.co`, dominio fijo).
- Verificación de correo con **OTP de 6 dígitos**.
- Recuperación de contraseña.
- El login **nunca pregunta el rol**: se resuelve por la identidad en el servidor. Los profesores se validan con un **código docente** verificado del lado del servidor (`redeem_professor_code`).
- Estados completos de carga, éxito y error, con recuperación y reintento; los datos ingresados se conservan ante errores recuperables.

### Experiencia del estudiante
- **Inicio** y **búsqueda** de profesores por nombre y asignatura (catálogo institucional en base de datos).
- **Perfil público del profesor:** promedio, resumen por categoría y datos rápidos (carga de trabajo, base de la nota, estilo de clase, asistencia obligatoria, revisión de parciales).
- **Calificar o actualizar una evaluación** (una por estudiante y profesor) con el flujo de tarjetas descrito arriba.
- **Reseñas** con las protecciones de privacidad descritas arriba.
- **Favoritos**, **notificaciones** y **perfil** (datos personales, carreras y materias, cambio de contraseña, foto de perfil).
- **Privacidad, Accesibilidad y Ayuda y soporte** como pantallas funcionales.

### Experiencia del profesor
- **Inicio** con nota general, tendencia (si mejora o no) e indicadores clave.
- **Resultados:** evolución mensual, desempeño por categoría y retroalimentación recibida.
- **Mejorar:** categorías débiles detectadas (solo con muestra suficiente), **metas de mejora con historial** y **recomendaciones generadas con IA** a partir de la retroalimentación agregada.
- **Reportar una reseña** con motivo fijo y revisión automática.
- Notificaciones y perfil (incluye edición de las materias que dicta).

### Inteligencia artificial
1. **Moderación del texto de la evaluación** (`moderate-review-text`): se ejecuta en cada paso con caja de texto. Bloquea insultos y calificativos (incluso disfrazados), ataques a la apariencia, el origen, el género o la vida privada, discriminación, acoso, amenazas y deseos de muerte o daño. La crítica al curso nunca se bloquea. También valida que la respuesta tenga sentido frente a la pregunta.
2. **Recomendaciones de mejora para el profesor** (`generate-improvement-recommendations`): resume patrones y propone acciones concretas.
3. **Revisión de reportes** (`moderate-report`): decide si una reseña reportada debe bajarse.

Además hay una validación local sin red (`TextQuality`) que rechaza relleno mecánico: letras repetidas, teclado machacado, texto sin vocales o la misma palabra repetida.

## Arquitectura

La app sigue una arquitectura por capas con separación entre UI, dominio y datos. **La autorización real vive en Supabase (RLS y funciones de servidor), nunca en Compose.**

```
┌────────────────────────────────────────────────────────────┐
│  UI  (Jetpack Compose)                                      │
│  auth/ · search/ · professor/ · profile/ · rating/ ·        │
│  notifications/ · accessibility/ · ui/theme                 │
│            ▲ estado (StateFlow)        │ eventos            │
│  ViewModels (AuthViewModel, RatingViewModel, …)             │
├────────────────────────────────────────────────────────────┤
│  domain/                                                    │
│  model/  (Rating, Notification, ProfessorSummary, …)        │
│  repository/  (contratos / interfaces)                      │
├────────────────────────────────────────────────────────────┤
│  data/  (implementaciones de los repositorios)              │
│  auth · profile · professor · catalog · rating · …          │
│  core/network/SupabaseProvider  (cliente único)             │
└──────────────┬─────────────────────────────────────────────┘
               │ HTTPS (clave pública)
┌──────────────▼─────────────────────────────────────────────┐
│  Supabase                                                   │
│  Auth · Postgres + RLS + RPC · Storage · Edge Functions     │
└──────────────┬─────────────────────────────────────────────┘
               │ secreto GEMINI_API_KEY (solo en el servidor)
┌──────────────▼──────────────┐
│  Google Gemini API          │
└─────────────────────────────┘
```

**Principios aplicados**

- La app **nunca llama directo a Gemini**: la llave quedaría expuesta en el APK. Llama a Edge Functions que guardan la llave como secreto.
- Los modelos son serializables (`kotlinx.serialization`) y los repositorios son interfaces, lo que facilita pruebas y reemplazo de fuentes de datos.
- La moderación es **fail-open** ante errores técnicos (sin red o IA caída no se bloquea al estudiante) y **fail-closed** ante veredictos reales de la IA. El detalle técnico queda en Logcat.
- Las Edge Functions reintentan y usan un modelo de respaldo cuando Gemini responde con sobrecarga (503/429).

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin (JVM 17) |
| UI | Jetpack Compose, Material 3, Material Icons Extended |
| Estado | ViewModel + `StateFlow`, `lifecycle-runtime-compose` |
| Backend | Supabase (Auth, PostgREST, Storage, Edge Functions) con `supabase-kt` 3.5.0 |
| Red | Ktor (cliente Android) |
| Imágenes | Coil |
| IA | Google Gemini (vía Supabase Edge Functions, Deno/TypeScript) |
| Build | Android Gradle Plugin 8.9.1 · Gradle 8.11.1 |
| SDK | `minSdk 26` · `targetSdk 35` · `compileSdk 36` |

## Estructura del repositorio

```
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                          # temas, íconos y mascota "Cabito"
│       └── java/co/edu/utadeo/tadeoconecta/
│           ├── MainActivity.kt
│           ├── MainShell.kt              # navegación y pantallas principales
│           ├── accessibility/            # tamaño de texto y preferencias
│           ├── auth/                     # login, registro, OTP, recuperación
│           ├── catalog/                  # asignaturas y carreras
│           ├── core/                     # cliente Supabase y utilidades
│           ├── data/                     # implementaciones de repositorios
│           ├── domain/                   # modelos y contratos
│           ├── notifications/
│           ├── professor/                # panel del profesor
│           ├── profile/
│           ├── rating/                   # flujo de evaluación + calidad de texto
│           ├── search/
│           └── ui/theme/
├── supabase/                             # scripts SQL por módulo
├── supabase_edge_function_*.ts           # funciones de IA (moderación y recomendaciones)
├── local.properties.example              # plantilla de credenciales
├── SUPABASE_SETUP.md                     # guía de conexión con Supabase
├── INSTRUCCIONES_DEPLOY_IA.md            # guía para desplegar las funciones de IA
├── MODULE_REQUIREMENTS.md                # contrato funcional del módulo de acceso
├── UI_PARITY_LOCK.md                     # reglas del sistema visual
└── UPGRADE_API36.md                      # notas de compilación
```

## Puesta en marcha

### Requisitos

- Android Studio (versión reciente con soporte para AGP 8.9) y JDK 17.
- Un proyecto de Supabase.
- Un dispositivo o emulador con Android 8.0 (API 26) o superior.
- Una API key de Gemini (gratuita en <https://aistudio.google.com/apikey>) si quieres activar las funciones de IA.

### Pasos

1. **Clona el repositorio y ábrelo** directamente en Android Studio.
2. **Configura las credenciales.** Copia `local.properties.example` a `local.properties` en la raíz y completa:
   ```properties
   SUPABASE_URL=https://TU-PROYECTO.supabase.co
   SUPABASE_KEY=sb_publishable_TU_CLAVE
   ```
   Usa **solo** la *publishable key*. Nunca pongas `service_role`, la contraseña de la base de datos ni secretos JWT en la app. También puedes definir ambas como variables de entorno de Gradle.
3. **Prepara la base de datos** ejecutando los scripts de [`supabase/`](#base-de-datos-supabase).
4. **Configura el correo de verificación:** en Supabase → Authentication → Email Templates → *Confirm signup*, usa `{{ .Token }}` para que llegue el código de 6 dígitos.
5. **Carga el catálogo** de asignaturas en `public.subjects` y crea el hash del código docente (ver `SUPABASE_SETUP.md`).
6. **(Opcional) Activa la IA** siguiendo [Funciones de IA](#funciones-de-ia-edge-functions).
7. **Sincroniza Gradle y ejecuta** ▶ (*Sync Project with Gradle Files*, luego *Run*).

### Generar un APK para instalar en un teléfono

*Build → Generate App Bundles or APKs → Generate APKs*, eligiendo la variante `release`. El `buildType` release está configurado como no depurable y firmado con la llave de depuración, suficiente para pruebas internas (no para publicar en Play Store).

## Base de datos (Supabase)

Los scripts de `supabase/` están organizados por módulo. Ejecútalos en el **SQL Editor** de Supabase:

| Script | Contenido |
|---|---|
| `FINAL_PATCH.sql` | Permisos de escritura para que el profesor guarde sus materias. |
| `MODULO_05_V2_EVALUACIONES.sql` | Evaluaciones por tarjetas, restricciones y políticas RLS. |
| `MODULO_07_NOTIFICACIONES.sql` | Notificaciones. |
| `MODULO_08_09_PANEL_PROFESOR.sql` | Funciones y vistas del panel del profesor. |
| `MODULO_09_HISTORIAL_METAS.sql` | Metas de mejora e historial. |
| `MODULO_REVISION_PRIVACIDAD_REPORTES.sql` | Privacidad de reseñas y reportes. |

> El esquema base (tablas de perfiles, asignaturas, códigos docentes y `redeem_professor_code`) se describe en `SUPABASE_SETUP.md`. Si empiezas en un proyecto vacío, créalo primero y luego aplica los scripts por módulo.

## Funciones de IA (Edge Functions)

Las tres funciones están en la raíz del repositorio como archivos `.ts` y se despliegan con el mismo nombre que usa la app:

| Función | Archivo | Uso |
|---|---|---|
| `moderate-review-text` | `supabase_edge_function_moderate_review_text.ts` | Moderación de cada paso del formulario. |
| `generate-improvement-recommendations` | `supabase_edge_function_generate_improvement_recommendations.ts` | Recomendaciones para el profesor. |
| `moderate-report` | `supabase_edge_function_moderate_report.ts` | Revisión de reseñas reportadas. |

Despliegue resumido (detalle completo en `INSTRUCCIONES_DEPLOY_IA.md`):

1. Crea el secreto `GEMINI_API_KEY` en Supabase → Project Settings → Edge Functions → Secrets.
2. Crea cada función desde el Dashboard (*Via editor*) con el nombre exacto de la tabla y pega el contenido del archivo.
3. Despliega. Las funciones requieren el JWT del usuario autenticado, por lo que no se prueban "en frío" desde el Dashboard: pruébalas desde la app y revisa los logs de la función.

## Seguridad y privacidad

- **Sin secretos en el cliente.** El APK solo contiene la URL del proyecto y la *publishable key*; la llave de Gemini vive únicamente como secreto de Supabase.
- **Autorización en el servidor.** Row Level Security en las tablas; el cambio de rol a profesor ocurre solo mediante la RPC `redeem_professor_code`, que valida un hash del código docente. Toda cuenta nueva nace como estudiante.
- **Privacidad de las reseñas.** El modelo que usa la app para mostrar reseñas ni siquiera incluye las justificaciones de calificaciones bajas ni el motivo de recomendación.
- **Datos mínimos.** La app solicita únicamente lo necesario para el vínculo institucional y no pide asignaturas al profesor durante la creación de credenciales.
- La pantalla *Privacidad y datos* explica al usuario qué se guarda y qué se muestra.

## Accesibilidad

- Control de **tamaño de texto** a nivel de toda la aplicación (escala aplicada en un único punto sobre `LocalDensity`).
- Íconos vectoriales de Material en lugar de glifos de texto, para que se vean igual en cualquier teléfono.
- Formularios que se ajustan al teclado (`imePadding` y desplazamiento automático hacia el campo activo).

## Pruebas con usuarios

El repositorio incluye el sistema visual y los contratos funcionales de cada módulo. Las pruebas de usabilidad se realizan con estudiantes y profesores de la universidad usando un formulario estructurado (confianza en el sistema, NPS, facilidad de uso y utilidad percibida), cuyos resultados alimentan la validación del proyecto.

## Documentación adicional

- [`SUPABASE_SETUP.md`](SUPABASE_SETUP.md): conexión, flujos de registro y arquitectura de acceso.
- [`INSTRUCCIONES_DEPLOY_IA.md`](INSTRUCCIONES_DEPLOY_IA.md): despliegue paso a paso de las funciones de IA.
- [`MODULE_REQUIREMENTS.md`](MODULE_REQUIREMENTS.md): contrato funcional del módulo de acceso.
- [`UI_PARITY_LOCK.md`](UI_PARITY_LOCK.md): reglas del sistema visual (colores, radios, mascota).
- [`UPGRADE_API36.md`](UPGRADE_API36.md): configuración de compilación.

## Limitaciones conocidas y trabajo futuro

- La IA de moderación usa la capa gratuita de Gemini, sujeta a límites de cuota y a picos de sobrecarga; por eso la moderación es *fail-open* ante errores técnicos.
- No existe todavía un panel de administración: reportar una reseña la oculta de inmediato tras la revisión automática.
- Cabito (la mascota) usa ilustraciones provisionales hasta contar con el maestro vectorial institucional.
- Pendientes: suite de pruebas automatizadas, versión para iOS, integración con el catálogo oficial de la universidad y publicación en tienda.

## Autores

- **Juan David Vargas** — desarrollo y diseño del producto.
- _Agrega aquí a los demás integrantes, el programa académico, el semestre y el nombre del docente asesor._

Universidad de Bogotá Jorge Tadeo Lozano · Proyecto académico.
