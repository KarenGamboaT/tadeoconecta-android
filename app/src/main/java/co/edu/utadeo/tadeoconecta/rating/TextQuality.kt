package co.edu.utadeo.tadeoconecta.rating

// Revisión general — a pedido de Juan: que el mínimo de caracteres de los
// comentarios obligatorios no se pueda "rellenar" con basura (una letra
// repetida, el teclado machacado al azar, la misma palabra copiada muchas
// veces) solo para pasar el contador.
//
// OJO con el alcance real de esto: NO es un chequeo de que el comentario
// tenga sentido de verdad ni de que diga algo coherente sobre el profesor
// — eso necesitaría un modelo de lenguaje real, y esta app no tiene ninguna
// integración de IA (ni la necesita para nada más). Alguien decidido a
// hacer trampa igual puede escribir palabras reales sin ninguna relación
// entre sí ("mesa lápiz montaña rápido verde correr") y esto lo deja pasar
// — ningún heurístico sin IA puede distinguir eso de un comentario real.
// Lo que SÍ bloquea, sin depender de nada externo ni de red, son los
// patrones de relleno MÁS comunes y mecánicos:
//   1. Muy pocas palabras reales para la longitud (una sola palabra larga,
//      o texto pegado sin espacios).
//   2. Muy pocas letras distintas para su longitud ("aaaaaaaaaa...",
//      "asdasdasdasdasd...").
//   3. Casi sin vocales (típico de pasar la mano por el teclado:
//      "qwrtpsdfgh").
//   4. Las mismas palabras repetidas una y otra vez en vez de variar
//      ("bueno bueno bueno bueno bueno").
object TextQuality {
    private const val MIN_WORDS = 5
    private const val MIN_DISTINCT_WORD_RATIO = 0.5
    // Revisión general — bug encontrado en producción: esto era una
    // PROPORCIÓN (letras distintas ÷ total de letras), y esa proporción baja
    // sola mientras el texto es más largo, porque el abecedario tiene un
    // techo fijo (~27 letras) sin importar cuán largo sea el texto — un
    // comentario real de 130 caracteres con 22 letras distintas (ya variado
    // de sobra) daba solo ~20%, por debajo del 25% que pedía esto, así que
    // un comentario real y largo quedaba rechazado por ser largo, no por
    // ser basura. Ahora es una CANTIDAD fija, no una proporción — no le
    // importa qué tan largo sea el texto, así que un comentario real nunca
    // queda penalizado por tener más caracteres.
    private const val MIN_DISTINCT_LETTERS = 10
    private const val MIN_VOWEL_RATIO = 0.15
    private val VOWELS = "aeiouáéíóúü".toSet()
    private val WORD_SPLIT = Regex("[^\\p{L}]+")

    // Revisión general — a pedido de Juan: el mínimo de cada campo ahora se
    // mide en PALABRAS, no en caracteres (ver MIN_COMMENT_WORDS y afines en
    // RatingViewModel.kt). Esta es la única definición de "palabra" en toda
    // la validación (2+ letras, igual que el chequeo de abajo) — tanto el
    // contador que ve la persona mientras escribe como lo que de verdad
    // bloquea el envío cuentan exactamente lo mismo.
    fun wordCount(text: String): Int =
        text.trim().lowercase().split(WORD_SPLIT).count { it.length >= 2 }

    fun isMeaningful(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        // 1) Suficientes palabras reales (2+ letras cada una), no una sola
        // palabra larga ni texto pegado sin espacios.
        val words = trimmed.lowercase().split(WORD_SPLIT).filter { it.length >= 2 }
        if (words.size < MIN_WORDS) return false

        // 4) No son casi todas la misma palabra repetida.
        val distinctRatio = words.distinct().size.toDouble() / words.size
        if (distinctRatio < MIN_DISTINCT_WORD_RATIO) return false

        val letters = trimmed.lowercase().filter { it.isLetter() }
        if (letters.isEmpty()) return false

        // 2) Suficiente variedad de letras (no "aaaa..." ni "asdasdasd...").
        // Cantidad absoluta, no proporción — ver el comentario de
        // MIN_DISTINCT_LETTERS arriba.
        val distinctLetters = letters.toSet().size
        if (distinctLetters < MIN_DISTINCT_LETTERS) return false

        // 3) Tiene vocales de verdad, no solo consonantes al azar.
        val vowelRatio = letters.count { it in VOWELS }.toDouble() / letters.length
        if (vowelRatio < MIN_VOWEL_RATIO) return false

        return true
    }
}
