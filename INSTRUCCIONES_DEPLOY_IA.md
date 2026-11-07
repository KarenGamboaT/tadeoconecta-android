# Cómo activar las recomendaciones de mejora con IA (puntos 1+2)

Esto tiene 3 pasos. Los dos primeros SOLO los puedes hacer tú — una llave de
API nunca debe pasar por un tercero, ni siquiera por mí. El tercero te lo
puedo hacer yo por ti si quieres (dime y lo hago desde tu navegador), o lo
haces tú con estos mismos pasos.

## 1. Consigue una API key de Gemini (gratis)

1. Entra a <https://aistudio.google.com/apikey> con tu cuenta de Google.
2. Clic en "Create API key" (o "Crear clave de API").
3. Copia la clave que te da (empieza por `AIza...`). Guárdala en algún lado
   temporal — la vas a pegar en el paso 2.

La capa gratuita de Gemini (modelo `gemini-2.0-flash`, el que usa la
función) tiene un límite generoso de peticiones gratis por día — de sobra
para que cada profesor genere recomendaciones de vez en cuando. Si algún día
se pasa ese límite, Gemini simplemente devuelve un error y la app muestra
"No pudimos generar recomendaciones con IA en este momento" (no se cae nada
más de la app).

## 2. Guarda esa llave como secreto del proyecto en Supabase

1. Entra a tu proyecto en <https://supabase.com/dashboard> → **Project
   Settings** → **Edge Functions** → pestaña **Secrets**.
2. Agrega un secreto nuevo:
   - Nombre: `GEMINI_API_KEY`
   - Valor: la clave que copiaste en el paso 1.
3. Guarda.

`SUPABASE_URL` y `SUPABASE_ANON_KEY` ya existen como variables automáticas
dentro de cualquier Edge Function del proyecto — no hace falta crearlas.

## 3. Crea y publica la función

No necesitas instalar la CLI de Supabase para esto — se puede hacer
completo desde el navegador:

1. En el Dashboard, ve a **Edge Functions** → **Deploy a new function** →
   **Via editor** (o "Create a new function" si te aparece así).
2. Nómbrala exactamente: `generate-improvement-recommendations` (la app ya
   la llama por ese nombre — ver `RatingRepositoryImpl.generateImprovementRecommendations()`).
3. Borra el código de ejemplo que trae el editor y pega todo el contenido
   de `supabase_edge_function_generate_improvement_recommendations.ts`
   (está al lado de este archivo).
4. Clic en **Deploy**.

Y ya — en cuanto la app vuelva a compilar con los cambios que te mandé,
desde "Mejorar" en el panel del profesor va a aparecer el botón "Generar
recomendaciones con IA".

### Si prefieres que lo haga yo

Dime y entro a tu Supabase desde tu navegador (como ya hice para los GRANT
y las constraints) y hago el paso 3 completo por ti — pego el código y le
doy Deploy. Lo único que no voy a hacer en ningún caso es escribir yo mismo
tu API key en el campo del secreto (paso 2): esa parte la tienes que pegar
tú, aunque sea estando yo ahí guiándote.

### Para probarlo

Una vez desplegada, puedes probarla directo desde el Dashboard (**Edge
Functions** → la función → **Invoke**) o esperar a correr la app — si algo
falla, el error que devuelve la función (lo vas a ver en los logs de la
función en el Dashboard) dice exactamente qué pasó (falta el secreto, RPC
sin permiso, Gemini con error, etc.).
