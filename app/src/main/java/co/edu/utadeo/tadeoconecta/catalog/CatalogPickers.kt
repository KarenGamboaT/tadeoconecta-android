package co.edu.utadeo.tadeoconecta.catalog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.domain.model.SubjectCareerLink
import co.edu.utadeo.tadeoconecta.ui.theme.TadeoBlue
import co.edu.utadeo.tadeoconecta.ui.theme.TadeoInk
import co.edu.utadeo.tadeoconecta.ui.theme.TadeoMuted
import co.edu.utadeo.tadeoconecta.ui.theme.glassPanel
import co.edu.utadeo.tadeoconecta.ui.theme.glassPill
import java.text.Normalizer

// Selector reutilizable facultad → carrera (→ materia, en el de profesores)
// usado en tres lugares: el registro docente y el registro de estudiante
// (AuthScreens.kt), y la edición de materias desde el perfil docente
// (MainShell.kt). Un profesor puede dictar en más de una carrera y un
// estudiante puede tener más de una (doble titulación), así que ambos
// pickers dejan marcar varias facultades/carreras a la vez — el set de
// seleccionadas no se limpia al colapsar una sección o cambiar de carrera.

// Revisión general: "fisica"/"ingenieria" (sin tilde, lo más común al
// escribir rápido en un celular) no encontraba "Física"/"Ingeniería" —
// contains(ignoreCase = true) solo ignora mayúsculas/minúsculas, nunca
// tildes. Quita los acentos de los dos lados de la comparación antes de
// buscar, sin tocar lo que se MUESTRA (el nombre real sigue con tilde).
private fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

@Composable
fun CatalogSearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp).glassPill()) {
        Row(Modifier.heightIn(min = 46.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (query.isEmpty()) Text(placeholder, color = TadeoMuted, fontSize = 13.sp); inner() }
                )
            }
            if (query.isNotEmpty()) {
                Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = TadeoMuted, modifier = Modifier.size(15.dp).clickable { onQueryChange("") })
            }
        }
    }
}

@Composable
private fun ExpandRow(title: String, subtitle: String? = null, expanded: Boolean, indent: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(start = (indent * 14).dp, bottom = 8.dp)
            .glassPanel(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                subtitle?.let { Text(it, color = TadeoMuted, fontSize = 10.5.sp, modifier = Modifier.padding(top = 1.dp)) }
            }
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SubjectCheckRow(subject: Subject, checked: Boolean, indent: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(start = (indent * 14).dp, bottom = 8.dp)
            .glassPanel(RoundedCornerShape(14.dp))
            .then(if (checked) Modifier.border(1.5.dp, TadeoBlue, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onClick() })
            Column(Modifier.padding(start = 4.dp)) {
                Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                subject.code?.takeIf { it.isNotBlank() }?.let { Text(it, color = TadeoMuted, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun CareerCheckRow(career: Career, checked: Boolean, indent: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(start = (indent * 14).dp, bottom = 8.dp)
            .glassPanel(RoundedCornerShape(14.dp))
            .then(if (checked) Modifier.border(1.5.dp, TadeoBlue, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onClick() })
            Text(career.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// Profesores: facultad → carrera → materias, checkbox a nivel de materia. La
// búsqueda filtra sobre TODAS las materias activas (no solo las ya
// amarradas a una carrera), para que una materia nueva siga siendo
// seleccionable aunque Juan no la haya vinculado todavía a ninguna carrera.
@Composable
fun FacultyCareerSubjectPicker(
    faculties: List<Faculty>,
    careers: List<Career>,
    subjects: List<Subject>,
    subjectCareerLinks: List<SubjectCareerLink>,
    selectedSubjectIds: Set<String>,
    onToggleSubject: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var expandedFaculties by remember { mutableStateOf(setOf<String>()) }
    var expandedCareers by remember { mutableStateOf(setOf<String>()) }

    val careersByFaculty = remember(careers) { careers.groupBy { it.faculty_id } }
    val subjectIdsByCareer = remember(subjectCareerLinks) {
        subjectCareerLinks.groupBy({ it.career_id }, { it.subject_id })
    }
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }

    Column(Modifier.fillMaxWidth()) {
        CatalogSearchBar(query, { query = it }, "Buscar materia por nombre…")

        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            val normalizedQuery = normalizeForSearch(trimmed)
            val matches = subjects.filter { normalizeForSearch(it.name).contains(normalizedQuery) }.sortedBy { it.name }
            if (matches.isEmpty()) {
                Text("No encontramos materias con ese nombre.", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                matches.forEach { subject ->
                    SubjectCheckRow(subject, subject.id in selectedSubjectIds, indent = 0) { onToggleSubject(subject.id) }
                }
            }
        } else if (faculties.isEmpty()) {
            Text("No hay facultades cargadas todavía.", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            faculties.forEach { faculty ->
                val facultyExpanded = faculty.id in expandedFaculties
                ExpandRow(faculty.name, expanded = facultyExpanded, indent = 0) {
                    expandedFaculties = if (facultyExpanded) expandedFaculties - faculty.id else expandedFaculties + faculty.id
                }
                if (facultyExpanded) {
                    careersByFaculty[faculty.id].orEmpty().forEach { career ->
                        val careerExpanded = career.id in expandedCareers
                        val subjectCount = subjectIdsByCareer[career.id]?.size ?: 0
                        ExpandRow(
                            career.name,
                            subtitle = "$subjectCount materia${if (subjectCount == 1) "" else "s"}",
                            expanded = careerExpanded,
                            indent = 1
                        ) {
                            expandedCareers = if (careerExpanded) expandedCareers - career.id else expandedCareers + career.id
                        }
                        if (careerExpanded) {
                            val careerSubjects = subjectIdsByCareer[career.id].orEmpty()
                                .mapNotNull { subjectsById[it] }
                                .sortedBy { it.name }
                            if (careerSubjects.isEmpty()) {
                                Text(
                                    "Sin materias cargadas en esta carrera todavía.",
                                    color = TadeoMuted,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(start = 28.dp, bottom = 8.dp)
                                )
                            } else {
                                careerSubjects.forEach { subject ->
                                    SubjectCheckRow(subject, subject.id in selectedSubjectIds, indent = 2) { onToggleSubject(subject.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Estudiantes: facultad → carrera, checkbox a nivel de carrera (sin
// materias). Permite más de una carrera por estudiante (doble titulación).
@Composable
fun FacultyCareerPicker(
    faculties: List<Faculty>,
    careers: List<Career>,
    selectedCareerIds: Set<String>,
    onToggleCareer: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var expandedFaculties by remember { mutableStateOf(setOf<String>()) }

    val careersByFaculty = remember(careers) { careers.groupBy { it.faculty_id } }

    Column(Modifier.fillMaxWidth()) {
        CatalogSearchBar(query, { query = it }, "Buscar carrera por nombre…")

        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            val normalizedQuery = normalizeForSearch(trimmed)
            val matches = careers.filter { normalizeForSearch(it.name).contains(normalizedQuery) }.sortedBy { it.name }
            if (matches.isEmpty()) {
                Text("No encontramos carreras con ese nombre.", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                matches.forEach { career ->
                    CareerCheckRow(career, career.id in selectedCareerIds, indent = 0) { onToggleCareer(career.id) }
                }
            }
        } else if (faculties.isEmpty()) {
            Text("No hay facultades cargadas todavía.", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            faculties.forEach { faculty ->
                val facultyExpanded = faculty.id in expandedFaculties
                ExpandRow(faculty.name, expanded = facultyExpanded, indent = 0) {
                    expandedFaculties = if (facultyExpanded) expandedFaculties - faculty.id else expandedFaculties + faculty.id
                }
                if (facultyExpanded) {
                    careersByFaculty[faculty.id].orEmpty().forEach { career ->
                        CareerCheckRow(career, career.id in selectedCareerIds, indent = 1) { onToggleCareer(career.id) }
                    }
                }
            }
        }
    }
}
