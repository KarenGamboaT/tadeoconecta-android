package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

// Módulo 05 v2 — Calificaciones y reseñas por TARJETAS, no por estrellas.
// La idea (decisión de Juan): un número de 1 a 5 significa algo distinto
// para cada persona ("3 estrellas" no es lo mismo para todo el mundo), así
// que el estudiante nunca toca una estrella directamente. En vez de eso
// elige, para cada categoría, la tarjeta que mejor describe su experiencia
// — una frase concreta y reconocible — y esa tarjeta trae un peso fijo
// (RatingCardOption.weight) invisible para el estudiante. La estrella que
// se muestra en el perfil del profesor es el promedio de esos pesos entre
// todas las reseñas: nunca algo que alguien haya escrito directamente.

// Las 6 preguntas de tarjeta: 4 académicas + 2 sobre el profesor como
// persona (trato y ambiente) — a propósito acotadas a conducta profesional
// observable (cómo trata, cómo se siente el ambiente), nunca a apariencia
// o personalidad fuera de lo académico, que es justo lo que ha hecho que
// otras plataformas midan "qué tan simpático" en vez de "qué tan buen
// profesor".
enum class RatingCategory(val key: String, val shortLabel: String, val question: String, val isAboutPerson: Boolean = false) {
    CLARITY("clarity", "Claridad", "¿Qué tan claro es explicando los temas?"),
    ORGANIZATION("organization", "Organización", "¿Qué tan organizado es el curso?"),
    FAIRNESS("fairness", "Justicia al calificar", "¿Las notas reflejan lo que realmente aprendiste?"),
    AVAILABILITY("availability", "Disponibilidad", "¿Qué tan disponible está fuera de clase?"),
    RESPECT("respect", "Trato al estudiante", "¿Cómo trata a sus estudiantes?", isAboutPerson = true),
    ATMOSPHERE("atmosphere", "Ambiente de clase", "¿Cómo se siente el ambiente en su clase?", isAboutPerson = true)
}

// index = la tarjeta elegida (0 = mejor … 3 = peor). weight = lo que de
// verdad se promedia para mostrar la estrella; requiresNote = true en las
// DOS tarjetas más críticas de cada categoría (índices 2 y 3) — ahí la UI
// exige contar la situación puntual antes de dejar enviar la evaluación.
data class RatingCardOption(val index: Int, val text: String, val weight: Double, val requiresNote: Boolean)

object RatingCards {
    // Mismo peso en las 4 tarjetas de cualquier categoría: 5 / 4 / 2.5 / 1.
    // Deja la escala fácil de explicar y de ajustar en un solo lugar.
    private val WEIGHTS = listOf(5.0, 4.0, 2.5, 1.0)

    private fun cards(vararg texts: String): List<RatingCardOption> =
        texts.mapIndexed { i, t -> RatingCardOption(i, t, WEIGHTS[i], requiresNote = i >= texts.size - 2) }

    val byCategory: Map<RatingCategory, List<RatingCardOption>> = mapOf(
        RatingCategory.CLARITY to cards(
            "Explica de forma que entiendes todo en el momento, incluso lo difícil.",
            "Explica bien, aunque a veces toca repasar el material por tu cuenta.",
            "Depende del tema: unas clases se entienden, otras tocan aprenderlas solo.",
            "Es difícil seguirle el hilo; terminas aprendiendo casi todo por fuera."
        ),
        RatingCategory.ORGANIZATION to cards(
            "Todo clarísimo desde el inicio (temario, fechas, entregas) y lo cumple.",
            "Generalmente organizado, aunque a veces hay cambios de última hora.",
            "El curso se siente improvisado varias veces en el semestre.",
            "Nunca es claro qué sigue ni qué se va a evaluar."
        ),
        RatingCategory.FAIRNESS to cards(
            "Criterios claros desde el principio y la nota siempre corresponde.",
            "En general califica justo, aunque algún parcial se sintió injusto.",
            "Calificar se siente algo arbitrario o poco transparente.",
            "Las notas no corresponden al esfuerzo ni se explican los criterios."
        ),
        RatingCategory.AVAILABILITY to cards(
            "Responde rápido y siempre encuentra espacio para resolver dudas.",
            "Responde, aunque a veces toca insistir o esperar un poco.",
            "Cuesta encontrar espacio para hablar con él/ella fuera de clase.",
            "Prácticamente no hay forma de contactarlo fuera de clase."
        ),
        RatingCategory.RESPECT to cards(
            "Siempre respetuoso, escucha y no hay favoritismos.",
            "Buen trato en general, con algún mal día ocasional.",
            "El trato varía mucho según el estudiante o su humor.",
            "Suele ser cortante, irrespetuoso o hay favoritismos evidentes."
        ),
        RatingCategory.ATMOSPHERE to cards(
            "Se nota que le gusta enseñar y hace que valga la pena ir a clase.",
            "Buen ambiente, aunque la clase puede sentirse plana a veces.",
            "El ambiente es frío o tenso con frecuencia.",
            "El ambiente desanima a participar o a preguntar."
        )
    )

    // Qué le pide a la nota obligatoria de cada categoría — se muestra como
    // hint del campo de texto que se abre cuando se elige la peor tarjeta.
    val noteHint: Map<RatingCategory, String> = mapOf(
        RatingCategory.CLARITY to "Cuéntanos un ejemplo concreto de cuándo no se entendió la explicación.",
        RatingCategory.ORGANIZATION to "Cuéntanos qué cambió sin aviso o qué no quedó claro.",
        RatingCategory.FAIRNESS to "Cuéntanos la situación puntual en la que sentiste que la calificación fue injusta.",
        RatingCategory.AVAILABILITY to "Cuéntanos qué intentaste para contactarlo y qué pasó.",
        RatingCategory.RESPECT to "Cuéntanos qué pasó en esa situación concreta.",
        RatingCategory.ATMOSPHERE to "Cuéntanos qué hizo que te sintieras así en esa clase."
    )

    // Revisión general — a pedido de Juan: las dos tarjetas BUENAS de cada
    // categoría ahora también abren una caja de texto, pero OPCIONAL (a
    // diferencia de noteHint, que es obligatoria en las dos críticas). Un
    // hint distinto porque la pregunta es la inversa: acá se invita a contar
    // qué estuvo bien, no a justificar un problema.
    val goodNoteHint: Map<RatingCategory, String> = mapOf(
        RatingCategory.CLARITY to "¿Qué hace que sus explicaciones se entiendan tan bien? (opcional)",
        RatingCategory.ORGANIZATION to "¿Qué te gustó de cómo organiza el curso? (opcional)",
        RatingCategory.FAIRNESS to "¿Qué hace que sientas que califica justo? (opcional)",
        RatingCategory.AVAILABILITY to "¿Cómo resuelve tus dudas fuera de clase? (opcional)",
        RatingCategory.RESPECT to "¿Qué hace que el trato se sienta tan bueno? (opcional)",
        RatingCategory.ATMOSPHERE to "¿Qué hace que el ambiente de su clase sea tan bueno? (opcional)"
    )
}

// "¿Para quién lo recomendarías?" — chips de selección múltiple, solo con
// sentido cuando wouldRecommend = true.
object RecommendFor {
    const val DEEP_LEARNING = "aprender_a_fondo"
    const val EASY_GRADE = "nota_facil"
    const val NEEDS_BACKGROUND = "buena_base"
    const val NEEDS_SUPPORT = "mucho_acompanamiento"

    val labels: Map<String, String> = mapOf(
        DEEP_LEARNING to "Si quieres aprender a fondo",
        EASY_GRADE to "Si buscas una nota fácil",
        NEEDS_BACKGROUND to "Si ya tienes buena base en el tema",
        NEEDS_SUPPORT to "Si necesitas mucho acompañamiento"
    )
}

// Datos rápidos: hechos neutrales (no "buenos" ni "malos") que se muestran
// como % de reseñas que los marcaron, no como estrellas.
// Asistencia declarada por el estudiante al evaluar (Módulo 12). Son RANGOS,
// no un número exacto, para que sea fácil contestar con honestidad. El rango
// decide cuánto PESA esa reseña en los promedios del profesor: quien casi no
// fue a clase no debería mover el promedio igual que quien sí fue. La clave
// "minima" se guarda pero pesa 0: no entra en promedios ni se muestra su
// comentario. Las reseñas viejas (attendance_band null) pesan 1.0.
object AttendanceBands {
    val labels = linkedMapOf(
        "alta" to "Casi siempre (90–100 % de las clases)",
        "media" to "La mayoría (70–89 %)",
        "baja" to "Aproximadamente la mitad (40–69 %)",
        "minima" to "Pocas clases (menos del 40 %)"
    )

    fun weight(band: String?): Double = when (band) {
        "alta" -> 1.0
        "media" -> 0.75
        "baja" -> 0.4
        "minima" -> 0.0
        else -> 1.0
    }
}

object RatingFacts {
    val workloadLabels = mapOf("poca" to "Poca carga fuera de clase", "moderada" to "Carga moderada", "alta" to "Mucha carga fuera de clase")
    val gradingBasisLabels = mapOf("examenes" to "Exámenes", "trabajos" to "Trabajos y proyectos", "participacion" to "Participación en clase")
    val classStyleLabels = mapOf("teorica" to "Más teórica", "practica" to "Más práctica")
}

@Serializable
data class CategoryAnswers(
    val clarity: Int,
    val organization: Int,
    val fairness: Int,
    val availability: Int,
    val respect: Int,
    val atmosphere: Int
) {
    fun get(category: RatingCategory): Int = when (category) {
        RatingCategory.CLARITY -> clarity
        RatingCategory.ORGANIZATION -> organization
        RatingCategory.FAIRNESS -> fairness
        RatingCategory.AVAILABILITY -> availability
        RatingCategory.RESPECT -> respect
        RatingCategory.ATMOSPHERE -> atmosphere
    }

    fun with(category: RatingCategory, index: Int): CategoryAnswers = when (category) {
        RatingCategory.CLARITY -> copy(clarity = index)
        RatingCategory.ORGANIZATION -> copy(organization = index)
        RatingCategory.FAIRNESS -> copy(fairness = index)
        RatingCategory.AVAILABILITY -> copy(availability = index)
        RatingCategory.RESPECT -> copy(respect = index)
        RatingCategory.ATMOSPHERE -> copy(atmosphere = index)
    }

    companion object {
        // -1 = todavía sin responder; el formulario no deja enviar hasta que
        // las 6 categorías tengan una tarjeta elegida.
        val UNANSWERED = CategoryAnswers(-1, -1, -1, -1, -1, -1)
    }
}

@Serializable
data class CategoryNotes(
    val clarity: String? = null,
    val organization: String? = null,
    val fairness: String? = null,
    val availability: String? = null,
    val respect: String? = null,
    val atmosphere: String? = null
) {
    fun get(category: RatingCategory): String? = when (category) {
        RatingCategory.CLARITY -> clarity
        RatingCategory.ORGANIZATION -> organization
        RatingCategory.FAIRNESS -> fairness
        RatingCategory.AVAILABILITY -> availability
        RatingCategory.RESPECT -> respect
        RatingCategory.ATMOSPHERE -> atmosphere
    }

    fun with(category: RatingCategory, note: String?): CategoryNotes = when (category) {
        RatingCategory.CLARITY -> copy(clarity = note)
        RatingCategory.ORGANIZATION -> copy(organization = note)
        RatingCategory.FAIRNESS -> copy(fairness = note)
        RatingCategory.AVAILABILITY -> copy(availability = note)
        RatingCategory.RESPECT -> copy(respect = note)
        RatingCategory.ATMOSPHERE -> copy(atmosphere = note)
    }

    companion object { val EMPTY = CategoryNotes() }
}

@Serializable
data class ProfessorRatingInsert(
    val student_id: String,
    val professor_id: String,
    val category_answers: CategoryAnswers,
    val category_notes: CategoryNotes,
    val would_recommend: Boolean,
    // Ahora obligatoria SIEMPRE (sea "sí" o "no"), por eso ya no es nullable.
    val recommend_reason: String,
    val recommend_for: List<String>,
    val workload: String,
    val attendance_mandatory: Boolean,
    // Sin valor por defecto a propósito: siempre viaja en el upsert.
    val attendance_band: String,
    val grading_basis: String,
    val allows_review_of_exams: Boolean,
    val class_style: String,
    val tip_comment: String,
    val hardest_part_comment: String
)

@Serializable
data class ProfessorRatingRow(
    // Revisión general — id propio de la fila (ver
    // MODULO_REVISION_PRIVACIDAD_REPORTES.sql): ya no se expone student_id
    // en la consulta pública de reseñas (ver PUBLIC_RATING_COLUMNS en
    // RatingRepositoryImpl), así que acá ambos tienen un default — las
    // únicas dos vías que SÍ necesitan student_id (get_my_rating,
    // get_professor_feedback) siguen haciendo "select *" y lo traen igual.
    val id: String = "",
    val student_id: String = "",
    val professor_id: String,
    val category_answers: CategoryAnswers,
    val category_notes: CategoryNotes = CategoryNotes.EMPTY,
    val would_recommend: Boolean,
    val recommend_reason: String = "",
    val recommend_for: List<String> = emptyList(),
    val workload: String,
    val attendance_mandatory: Boolean,
    val attendance_band: String? = null,
    val grading_basis: String,
    val allows_review_of_exams: Boolean,
    val class_style: String,
    val tip_comment: String,
    val hardest_part_comment: String,
    val created_at: String? = null
) {
    // Promedio de las 6 tarjetas de ESTA evaluación puntual (no el promedio
    // general del profesor) — lo que se muestra junto a cada reseña.
    fun overallWeight(): Double =
        RatingCategory.entries.map { RatingCards.byCategory.getValue(it)[category_answers.get(it)].weight }.average()
}

// Una reseña lista para mostrar a OTROS ESTUDIANTES: promedio de esa
// evaluación + sus dos comentarios guiados (ambos obligatorios, así que
// siempre hay algo que mostrar). ratingId viaja sin mostrarse, solo para
// poder reportarla — a propósito YA NO es el student_id de quien la
// escribió (ver MODULO_REVISION_PRIVACIDAD_REPORTES.sql): antes sí lo era,
// y como profiles.select está abierto a cualquier authenticated (ver
// FINAL_PATCH.sql), cualquiera podía cruzar ese id con profiles y saber
// exactamente quién escribió cada comentario — justo lo que el umbral de 5
// evaluaciones (Módulo 05) existe para evitar. Ahora es el id propio de la
// fila, que no identifica a nadie por sí solo.
//
// A propósito NO incluye las notas de justificación de categorías mal
// calificadas ni el motivo de la recomendación (decisión de Juan): esos
// textos son los que más fácil se leen como un ataque directo a la
// persona, y mostrarlos sin ningún filtro a CUALQUIER estudiante —
// incluido uno que calificó mal solo porque "le cayó mal" el profesor un
// día — es el riesgo que más puede jugarle en contra a un profesor real.
// Esos dos campos siguen guardándose en professor_ratings (category_notes,
// recommend_reason) para que el profesor los pueda revisar en privado el
// día que exista esa pantalla, pero ReviewsScreen nunca los lee.
data class ProfessorReview(
    val ratingId: String,
    val overall: Double,
    val wouldRecommend: Boolean,
    val tipComment: String,
    val hardestPartComment: String
)

// Desglose agregado de una categoría: promedio en estrellas + cuántas
// reseñas cayeron en cada una de las 4 tarjetas (para mostrar, por ejemplo,
// "3 de 20 dijeron que las notas se sintieron injustas" en vez de solo un
// número).
data class CategoryBreakdown(val average: Double, val cardCounts: List<Int>)

// Módulo 08/09 — una evaluación completa tal como la ve el PROPIO profesor
// evaluado. A diferencia de ProfessorReview, SÍ incluye category_notes y
// recommend_reason: este es justo el consumidor privado para el que esos
// dos campos se siguieron guardando (ver el comentario de ProfessorReview
// más arriba). Nunca debe llegar a ninguna pantalla que no sea la del
// propio profesor autenticado — y desde esta migración eso ya no depende
// solo de que Kotlin no lo use: get_professor_feedback() en Supabase
// filtra server-side por professor_id = auth.uid(), así que ni un request
// manual a la API puede traer el feedback de otro profesor.
data class ProfessorFeedbackNote(
    val studentId: String,
    // Revisión general — id propio de la fila (professor_ratings.id), el
    // mismo que usa reportReview() para una reseña pública: deja al
    // profesor reportar feedback abusivo sin exponer quién lo escribió,
    // reutilizando report_professor_rating y su umbral de 3 reportes.
    val ratingId: String,
    val createdAt: String?,
    val overall: Double,
    val categoryAnswers: CategoryAnswers,
    // Solo las categorías donde el estudiante eligió una tarjeta CRÍTICA y
    // por lo tanto dejó una nota obligatoria (ver RatingCardOption.requiresNote).
    val criticalNotes: Map<RatingCategory, String>,
    // Revisión general — separado de criticalNotes a propósito: ahora las
    // tarjetas BUENAS también pueden traer una nota, pero OPCIONAL (ver
    // OptionalMultilineField en MainShell.kt). Antes category_notes solo
    // existía cuando la tarjeta era crítica, así que ProfessorFeedbackCard
    // pintaba todo en una caja roja de alerta — eso ya no tendría sentido
    // para un elogio, así que el profesor las ve separadas.
    val positiveNotes: Map<RatingCategory, String>,
    val wouldRecommend: Boolean,
    val recommendReason: String,
    val tipComment: String,
    val hardestPartComment: String,
    // Asistencia que declaró quien escribió esto (ver AttendanceBands) — el
    // profesor la ve junto al comentario para leerlo con ese contexto.
    val attendanceBand: String? = null
)

// Módulo 09 — el "objetivo activo" del plan de mejora, persistido en
// professor_improvement_goals (antes vivía solo en memoria en MainShell.kt
// y se perdía al cerrar la app). actionsDone siempre trae 3 valores, uno
// por cada acción sugerida en ProfessorTips.actionsByCategory[category].
data class ProfessorImprovementGoal(
    val category: RatingCategory,
    val actionsDone: List<Boolean>
) {
    companion object {
        fun blank(category: RatingCategory) = ProfessorImprovementGoal(category, listOf(false, false, false))
    }
}

// Módulo 09 — una meta ya reemplazada o quitada (ver
// professor_improvement_goal_history). "completed" es una foto de si las 3
// acciones ya estaban en true justo antes de archivarla, no algo que se
// recalcule después — así el historial distingue una meta que de verdad se
// cumplió de una que se cambió o se quitó a medio camino.
data class ProfessorGoalHistoryEntry(
    val category: RatingCategory,
    val actionsDone: List<Boolean>,
    val completed: Boolean,
    val archivedAt: String
)

// Módulo 09 — acciones concretas sugeridas según la categoría más débil del
// profesor en el periodo. Son plantillas fijas (no generadas con IA, no hay
// integración de ese tipo en el proyecto): cada categoría trae exactamente
// 3 acciones, consistente con el checklist de 3 ítems que ya existía en
// ProfessorImprove cuando era contenido de muestra.
object ProfessorTips {
    val actionsByCategory: Map<RatingCategory, List<String>> = mapOf(
        RatingCategory.CLARITY to listOf(
            "Cierra cada bloque con una pregunta de comprobación.",
            "Da un ejemplo concreto antes de pasar a la teoría.",
            "Resume las dudas frecuentes al inicio de la siguiente sesión."
        ),
        RatingCategory.ORGANIZATION to listOf(
            "Comparte el temario y las fechas clave desde la primera semana.",
            "Avisa con anticipación cualquier cambio de plan.",
            "Recuerda al inicio de cada clase qué se evaluará y cuándo."
        ),
        RatingCategory.FAIRNESS to listOf(
            "Publica los criterios de calificación antes de cada evaluación.",
            "Explica cómo se calificó cada parcial al momento de entregarlo.",
            "Habilita un espacio para resolver dudas puntuales sobre una nota."
        ),
        RatingCategory.AVAILABILITY to listOf(
            "Define y comunica un horario fijo de atención fuera de clase.",
            "Responde mensajes en un plazo máximo anunciado.",
            "Ofrece un canal alterno (correo, foro) cuando no puedas estar presencialmente."
        ),
        RatingCategory.RESPECT to listOf(
            "Escucha la pregunta completa antes de responder.",
            "Evita comparar a estudiantes entre sí frente al grupo.",
            "Pide retroalimentación anónima sobre el trato en clase."
        ),
        RatingCategory.ATMOSPHERE to listOf(
            "Abre cada clase con algo que invite a participar.",
            "Reconoce públicamente los aportes de los estudiantes.",
            "Habilita un minuto de dudas anónimas antes del cierre."
        )
    )

    // Texto corto para la tarjeta de insight ("punto por fortalecer") de
    // ProfessorHome.
    val summaryByCategory: Map<RatingCategory, String> = mapOf(
        RatingCategory.CLARITY to "Es la categoría que más recurrentemente aparece como oportunidad este periodo.",
        RatingCategory.ORGANIZATION to "Varios estudiantes sintieron el curso menos organizado de lo esperado.",
        RatingCategory.FAIRNESS to "La forma de calificar es lo que más dudas generó este periodo.",
        RatingCategory.AVAILABILITY to "La disponibilidad fuera de clase es la oportunidad más mencionada.",
        RatingCategory.RESPECT to "El trato a los estudiantes es lo que más se repite como punto a cuidar.",
        RatingCategory.ATMOSPHERE to "El ambiente de clase es la oportunidad que más aparece este periodo."
    )
}

data class ProfessorRatingSummary(
    val reviewCount: Int,
    val averageOverall: Double?,
    val categoryAverages: Map<RatingCategory, CategoryBreakdown>,
    // % de reseñas con would_recommend = true — el dato "titular" en vez
    // del promedio de estrellas (eso es lo que de verdad resiste el efecto
    // "fácil = bueno" que tienen los promedios genéricos).
    val recommendPercent: Int?,
    val recommendForCounts: Map<String, Int>,
    val workloadCounts: Map<String, Int>,
    val attendanceMandatoryCount: Int,
    val gradingBasisCounts: Map<String, Int>,
    val allowsReviewOfExamsCount: Int,
    val classStyleCounts: Map<String, Int>,
    val reviews: List<ProfessorReview>
) {
    companion object {
        val EMPTY = ProfessorRatingSummary(
            reviewCount = 0,
            averageOverall = null,
            categoryAverages = emptyMap(),
            recommendPercent = null,
            recommendForCounts = emptyMap(),
            workloadCounts = emptyMap(),
            attendanceMandatoryCount = 0,
            gradingBasisCounts = emptyMap(),
            allowsReviewOfExamsCount = 0,
            classStyleCounts = emptyMap(),
            reviews = emptyList()
        )
    }
}
