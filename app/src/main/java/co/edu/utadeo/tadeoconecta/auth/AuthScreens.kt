package co.edu.utadeo.tadeoconecta.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.utadeo.tadeoconecta.MainAppShell
import co.edu.utadeo.tadeoconecta.R
import androidx.compose.ui.draw.shadow
import co.edu.utadeo.tadeoconecta.catalog.FacultyCareerPicker
import co.edu.utadeo.tadeoconecta.catalog.FacultyCareerSubjectPicker
import co.edu.utadeo.tadeoconecta.ui.theme.*

// Borradores de los campos de formulario, separados del AuthUiState del ViewModel.
// Viven en AuthApp (que nunca sale de composición mientras dura la sesión de auth),
// así que sobreviven cuando el flujo pasa por Loading/Error y vuelve a la misma
// pantalla: el usuario no tiene que volver a escribir todo tras un error recuperable.
// Solo se reinician cuando formResetSignal cambia (es decir, al cerrar sesión).
private class AuthFormDrafts {
    var loginUser by mutableStateOf("")
    var loginPass by mutableStateOf("")
    var signUpName by mutableStateOf("")
    var signUpUser by mutableStateOf("")
    var signUpPass by mutableStateOf("")
    var signUpConfirm by mutableStateOf("")
    var signUpTerms by mutableStateOf(false)
    var verifyCode by mutableStateOf("")
    var professorCode by mutableStateOf("")
    var forgotUser by mutableStateOf("")
    var recoveryCode by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newPasswordConfirm by mutableStateOf("")
}

@Composable
fun AuthApp(vm: AuthViewModel = viewModel()) {
    val draft = remember(vm.formResetSignal) { AuthFormDrafts() }
    // Una vez una contraseña ya se usó con éxito, se limpia del borrador por higiene;
    // esto no afecta la retención de datos ante errores recuperables (ver arriba).
    LaunchedEffect(vm.state.screen) {
        when (vm.state.screen) {
            AuthScreen.Destination -> draft.loginPass = ""
            AuthScreen.Verify -> { draft.signUpPass = ""; draft.signUpConfirm = "" }
            AuthScreen.PasswordUpdated -> { draft.newPassword = ""; draft.newPasswordConfirm = ""; draft.recoveryCode = "" }
            else -> {}
        }
    }
    // Revisión general: no había NINGÚN BackHandler en toda la app — el
    // botón/gesto de volver del sistema no cerraba nada, directamente
    // salía de la app completa (comportamiento por defecto de Android sin
    // fragment back stack). Cada pantalla ya tiene su propia flechita que
    // llama vm.back() (ver AuthTopBar más abajo); esto solo hace que el
    // botón físico/gesto de volver haga lo mismo. Login/Startup se dejan
    // fuera a propósito (ahí SÍ se espera que volver salga de la app,
    // igual que cualquier pantalla raíz), y Destination también: una vez
    // autenticado, el back ya es responsabilidad de MainAppShell/sus
    // propios shells, no de este historial de pantallas de auth.
    BackHandler(enabled = vm.state.screen != AuthScreen.Login && vm.state.screen != AuthScreen.Startup && vm.state.screen != AuthScreen.Destination) {
        vm.back()
    }
    Box(Modifier.fillMaxSize().background(GlassBackdropBrush)) {
        GlassBlobs()
        when (vm.state.screen) {
            AuthScreen.Startup -> LoadingScreen(vm)
            AuthScreen.Login -> LoginScreen(vm, draft)
            AuthScreen.SignUp -> SignUpScreen(vm, draft)
            AuthScreen.Verify -> VerifyScreen(vm, draft)
            AuthScreen.StudentCareers -> StudentCareersScreen(vm)
            AuthScreen.ProfessorCode -> ProfessorCodeScreen(vm, draft)
            AuthScreen.ProfessorSubjects -> ProfessorSubjectsScreen(vm)
            AuthScreen.Forgot -> ForgotScreen(vm, draft)
            AuthScreen.RecoveryCode -> RecoveryCodeScreen(vm, draft)
            AuthScreen.NewPassword -> NewPasswordScreen(vm, draft)
            AuthScreen.PasswordUpdated -> SystemStateScreen("Contraseña actualizada", "Ya puedes iniciar sesión con tu nueva contraseña.", R.drawable.cabito_success, "Iniciar sesión") { vm.go(AuthScreen.Login) }
            AuthScreen.Loading -> LoadingScreen(vm)
            AuthScreen.Error -> ErrorScreen(vm)
            AuthScreen.ErrorLibrary -> ErrorLibrary(vm)
            AuthScreen.Destination -> MainAppShell(vm.state.destinationRole ?: UserRole.Student) { vm.logout() }
        }
    }
}

// Transparente a propósito: el fondo con manchas de color (GlassBackdropBrush
// + GlassBlobs) ya lo pinta AuthApp detrás de todas las pantallas; si esta
// Column tuviera su propio color sólido, lo taparía.
@Composable
private fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 26.dp),
        content = content
    )
}

// Revisión general — "Reenviar código" se podía tocar sin límite, y cada
// toque es una llamada real a Supabase Auth (que tiene su propio
// rate-limit interno, y fallaba ahí sin ningún mensaje claro en esta
// pantalla). Cooldown puramente local — no necesita nada del ViewModel,
// solo evita los taps repetidos mientras el correo todavía no llega.
@Composable
private fun ColumnScope.ResendCodeButton(onResend: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(0) }
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft -= 1
        }
    }
    TextButton(
        onClick = { onResend(); secondsLeft = 30 },
        enabled = secondsLeft == 0,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(if (secondsLeft > 0) "Reenviar en ${secondsLeft}s" else "Reenviar código")
    }
}

// Mismo tratamiento que HeaderBrand en MainShell.kt: el logo va dentro de su
// propia pastilla de vidrio en vez de flotar como un rectángulo blanco suelto
// sobre el fondo con manchas de color — así Acceso/Registro se ve con el
// mismo lenguaje visual que el resto de la app.
@Composable
private fun Brand() {
    Box(
        Modifier.height(42.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.utadeo_logo),
            contentDescription = "Universidad de Bogotá Jorge Tadeo Lozano",
            modifier = Modifier.width(128.dp).height(27.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AuthTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).glassPill(RoundedCornerShape(13.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TadeoInk, modifier = Modifier.size(19.dp))
        }
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun Cabito(res: Int, width: Int = 132, height: Int = width) {
    Image(painterResource(res), "Cabito", Modifier.width(width.dp).height(height.dp), contentScale = ContentScale.Fit)
}

// Botón en pastilla con degradado azul→aqua, igual que ProfilePrimary en
// MainShell.kt — el mismo lenguaje visual en Acceso y dentro de la app.
@Composable
private fun Primary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp)
            .shadow(10.dp, shape, ambientColor = TadeoBlue.copy(alpha = 0.25f), spotColor = TadeoBlue.copy(alpha = 0.25f))
            .clip(shape)
            .background(if (enabled) Brush.horizontalGradient(listOf(TadeoBlue, TadeoAqua)) else Brush.horizontalGradient(listOf(Color(0xFFB9C3D1), Color(0xFFB9C3D1)))),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White, disabledContainerColor = Color.Transparent)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Secondary(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.fillMaxWidth().height(50.dp).glassPill(shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TadeoBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InlineError(text: String) { Text(text, color = Color(0xFFBD4136), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }

@Composable
private fun InstitutionalUser(value: String, onValue: (String) -> Unit, label: String = "Correo institucional") {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TadeoInk)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(48.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
                modifier = Modifier.weight(1f),
                decorationBox = { inner -> if (value.isEmpty()) Text("nombre.apellido", color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
            )
            Text("@utadeo.edu.co", fontSize = 11.sp, color = TadeoUTBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RequirementChip(label: String, met: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 1.dp)) {
        Text(
            if (met) "✓" else "•",
            color = if (met) TadeoSuccess else Color(0xFFB8C0CC),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(13.dp)
        )
        Text(
            label,
            color = if (met) TadeoSuccess else TadeoMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun PasswordRequirements(password: String) {
    if (password.isEmpty()) return
    val allMet = PasswordPolicy.rules.all { it.test(password) }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        if (allMet) {
            RequirementChip("Contraseña con todos los requisitos", met = true)
        } else {
            PasswordPolicy.rules.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { rule ->
                        RequirementChip(rule.label, met = rule.test(password), modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Password(value: String, onValue: (String) -> Unit, label: String = "Contraseña", placeholder: String = "••••••••", showRequirements: Boolean = false) {
    // Revisión general — a pedido de Juan: el "ojito" para mostrar/ocultar
    // la contraseña mientras se escribe. visible es estado local de ESTE
    // campo (no del formulario) a propósito: cada campo de contraseña se
    // revela de forma independiente.
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TadeoInk)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
                modifier = Modifier.fillMaxWidth().padding(end = 26.dp),
                decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
            )
            Icon(
                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                tint = Color(0xFF9AA3B0),
                modifier = Modifier.align(Alignment.CenterEnd).size(19.dp).clickable { visible = !visible }
            )
        }
        if (showRequirements) PasswordRequirements(value)
    }
}

@Composable
private fun HeroLogin() {
    val heroShape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().height(154.dp)
            .shadow(14.dp, heroShape, ambientColor = TadeoBlue.copy(alpha = 0.18f), spotColor = TadeoBlue.copy(alpha = 0.18f))
            .clip(heroShape)
            .background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFEAF5FF), Color(0xFFDCF6F3))))
            .border(1.dp, Color.White.copy(alpha = 0.8f), heroShape)
    ) {
        Row(Modifier.fillMaxSize().padding(start = 17.dp, top = 16.dp, end = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TADEO CONECTA", color = TadeoAqua, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.7.sp)
                Text("Tu universidad, más cerca.", color = TadeoInk, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 4.dp))
                Text("Conoce a tus profesores y toma decisiones con información útil.", color = Color(0xFF43526B), fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Image(painterResource(R.drawable.cabito_welcome), "Cabito", Modifier.width(104.dp).height(120.dp).align(Alignment.Bottom), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun LoginScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Brand(); Spacer(Modifier.height(16.dp)); HeroLogin(); Spacer(Modifier.height(17.dp))
        InstitutionalUser(draft.loginUser, { draft.loginUser = it; inline = null }); Spacer(Modifier.height(13.dp))
        Password(draft.loginPass, { draft.loginPass = it; inline = null }); inline?.let { InlineError(it) }; Spacer(Modifier.height(13.dp))
        Primary("Iniciar sesión") { inline = vm.login(draft.loginUser, draft.loginPass) }
        TextButton(onClick = { vm.go(AuthScreen.Forgot) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("¿Olvidaste tu contraseña?", fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = TadeoBorder); Text("¿Primera vez aquí?", Modifier.padding(horizontal = 10.dp), fontSize = 10.sp, color = TadeoMuted); HorizontalDivider(Modifier.weight(1f), color = TadeoBorder)
        }
        Spacer(Modifier.height(10.dp)); Secondary("Crear cuenta institucional") { vm.go(AuthScreen.SignUp) }
        Spacer(Modifier.height(8.dp)); Text("Solo usuarios con correo institucional @utadeo.edu.co.", fontSize = 10.sp, color = TadeoMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuideCard(image: Int, title: String, body: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).glassPanel(RoundedCornerShape(20.dp), tint = Color(0xFFEDF5FF))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cabito(image, 54, 54); Spacer(Modifier.width(11.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) }
        }
    }
}

@Composable
private fun RoleSelector(role: UserRole, onRole: (UserRole) -> Unit) {
    Row(Modifier.fillMaxWidth().glassPill(RoundedCornerShape(14.dp)).padding(4.dp)) {
        RoleButton("Estudiante", role == UserRole.Student, Modifier.weight(1f)) { onRole(UserRole.Student) }
        RoleButton("Profesor", role == UserRole.Professor, Modifier.weight(1f)) { onRole(UserRole.Professor) }
    }
}

@Composable
private fun RoleButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(11.dp))) {
        Text(text, color = if (selected) TadeoBlue else TadeoMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SignUpScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Crear cuenta") { vm.back() }
        GuideCard(R.drawable.cabito_point, "Primero validemos tu vínculo con UTadeo.", "Selecciona tu rol y usa exclusivamente tu correo institucional.")
        if (vm.state.role == UserRole.Professor) {
            Box(Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(14.dp), tint = TadeoSoft)) {
                Column(Modifier.padding(12.dp)) { Text("Perfil docente después del registro", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Las asignaturas, grupos y datos académicos se completarán o sincronizarán en tu perfil docente; no se solicitan para crear la contraseña.", color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) }
            }
        }
        RoleSelector(vm.state.role, vm::setRole)
        Spacer(Modifier.height(13.dp)); Text("Nombre completo", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
        BasicTextField(value = draft.signUpName, onValueChange = { draft.signUpName = it; inline = null }, singleLine = true, modifier = Modifier.fillMaxWidth().height(48.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk), decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { if (draft.signUpName.isEmpty()) Text("Karen Gamboa", color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() } })
        Spacer(Modifier.height(13.dp)); InstitutionalUser(draft.signUpUser, { draft.signUpUser = it; inline = null }, "Usuario institucional")
        Spacer(Modifier.height(13.dp)); Password(draft.signUpPass, { draft.signUpPass = it; inline = null }, "Contraseña", "Mínimo 8 caracteres", showRequirements = true)
        Spacer(Modifier.height(13.dp)); Password(draft.signUpConfirm, { draft.signUpConfirm = it; inline = null }, "Confirmar contraseña", "Repite tu contraseña")
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 10.dp)) { Checkbox(draft.signUpTerms, { draft.signUpTerms = it; inline = null }); Text("Acepto términos, tratamiento de datos y política de privacidad.", fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
        inline?.let { InlineError(it) }; Spacer(Modifier.height(8.dp)); Primary("Validar correo y continuar") { inline = vm.signUp(draft.signUpName, draft.signUpUser, draft.signUpPass, draft.signUpConfirm, draft.signUpTerms) }
    }
}

@Composable
private fun OtpInput(code: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = code,
        onValueChange = { raw -> onChange(raw.filter(Char::isDigit).take(6)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { i ->
                        Box(Modifier.weight(1f).height(48.dp).glassPill(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(code.getOrNull(i)?.toString() ?: "", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Box(Modifier.matchParentSize().background(Color.Transparent)) { inner() }
            }
        }
    )
}

@Composable
private fun VerifyScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }; var resent by remember { mutableStateOf(false) }
    ScrollPage {
        AuthTopBar("Verificar correo") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Cabito(R.drawable.cabito_search, 132, 132)
            Text("Revisa tu correo institucional", fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Enviamos un código de 6 dígitos a tu cuenta @utadeo.edu.co.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(20.dp)); OtpInput(draft.verifyCode) { draft.verifyCode = it; inline = null }
        inline?.let { InlineError(it) }; if (resent) Text("Código reenviado.", color = TadeoSuccess, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp)); Primary("Confirmar código") { inline = vm.verify(draft.verifyCode) }
        ResendCodeButton { vm.resendCode(); resent = true }
        Text("El código se valida directamente con Supabase Auth.", color = TadeoMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}


@Composable
private fun ProfessorCodeScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Validación docente") { vm.back() }
        GuideCard(
            R.drawable.cabito_point,
            "Verifiquemos tu perfil docente.",
            "Ingresa el código institucional entregado exclusivamente a profesores UTadeo."
        )
        Text("Código docente", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = draft.professorCode,
            onValueChange = { draft.professorCode = it.uppercase(); inline = null },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .glassPill(RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (draft.professorCode.isEmpty()) Text("Código institucional", color = Color(0xFF9AA3B0), fontSize = 14.sp)
                    inner()
                }
            }
        )
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(18.dp))
        Primary("Validar código docente") { inline = vm.validateProfessorCode(draft.professorCode) }
        Text(
            "La validación ocurre en Supabase; la app nunca decide por sí sola quién es profesor.",
            color = TadeoMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

@Composable
private fun ProfessorSubjectsScreen(vm: AuthViewModel) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Tus asignaturas") { vm.back() }
        GuideCard(
            R.drawable.cabito_encourage,
            "Completa tu perfil académico.",
            "Elige la facultad y la carrera donde dictas clase, y marca tus materias ahí. Puedes dictar en más de una carrera — repite el proceso las veces que necesites. Podrás modificarlas después desde tu perfil."
        )
        if (vm.state.subjects.isEmpty()) {
            Box(Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(16.dp), tint = Color(0xFFFFF7DB))) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aún no hay asignaturas cargadas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Agrega registros activos en public.subjects dentro de Supabase y vuelve a esta pantalla.",
                        color = TadeoMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Text("Selecciona una o más", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            FacultyCareerSubjectPicker(
                faculties = vm.state.faculties,
                careers = vm.state.careers,
                subjects = vm.state.subjects,
                subjectCareerLinks = vm.state.subjectCareerLinks,
                selectedSubjectIds = vm.state.selectedSubjectIds,
                onToggleSubject = { vm.toggleSubject(it); inline = null }
            )
        }
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(10.dp))
        Primary("Guardar y entrar al panel docente", enabled = vm.state.subjects.isNotEmpty()) {
            inline = vm.saveProfessorSubjects()
        }
    }
}

@Composable
private fun StudentCareersScreen(vm: AuthViewModel) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Tu carrera") { vm.back() }
        GuideCard(
            R.drawable.cabito_encourage,
            "Cuéntanos qué estudias.",
            "Elige tu facultad y tu carrera. Si tienes doble titulación, puedes marcar más de una."
        )
        if (vm.state.faculties.isEmpty()) {
            Box(Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(16.dp), tint = Color(0xFFFFF7DB))) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aún no hay carreras cargadas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Agrega el catálogo de facultades y carreras en Supabase y vuelve a esta pantalla.",
                        color = TadeoMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Text("Selecciona una o más", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            FacultyCareerPicker(
                faculties = vm.state.faculties,
                careers = vm.state.careers,
                selectedCareerIds = vm.state.selectedCareerIds,
                onToggleCareer = { vm.toggleCareer(it); inline = null }
            )
        }
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(10.dp))
        Primary("Guardar y entrar a Tadeo Conecta", enabled = vm.state.faculties.isNotEmpty()) {
            inline = vm.saveStudentCareers()
        }
    }
}

@Composable
private fun ForgotScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Recuperar contraseña") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Cabito(R.drawable.cabito_point, 132, 132) }
        Text("Recupera tu acceso", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
        Text("Te enviaremos instrucciones a tu correo institucional.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(22.dp)); InstitutionalUser(draft.forgotUser, { draft.forgotUser = it; inline = null }); inline?.let { InlineError(it) }; Spacer(Modifier.height(18.dp)); Primary("Enviar instrucciones") { inline = vm.forgot(draft.forgotUser) }
    }
}

@Composable
private fun RecoveryCodeScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }; var resent by remember { mutableStateOf(false) }
    ScrollPage {
        AuthTopBar("Código de recuperación") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Cabito(R.drawable.cabito_search, 132, 132)
            Text("Ingresa el código que enviamos", fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Revisa tu correo institucional @utadeo.edu.co para continuar con la recuperación.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(20.dp)); OtpInput(draft.recoveryCode) { draft.recoveryCode = it; inline = null }
        inline?.let { InlineError(it) }; if (resent) Text("Código reenviado.", color = TadeoSuccess, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp)); Primary("Confirmar código") { inline = vm.verifyRecoveryCode(draft.recoveryCode) }
        ResendCodeButton { vm.resendRecoveryCode(); resent = true }
        Text("El código se valida directamente con Supabase Auth.", color = TadeoMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

@Composable
private fun NewPasswordScreen(vm: AuthViewModel, draft: AuthFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Nueva contraseña") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Cabito(R.drawable.cabito_point, 132, 132) }
        Text("Crea tu nueva contraseña", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
        Text("Debe ser distinta y cumplir los requisitos de seguridad institucionales.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(18.dp))
        Password(draft.newPassword, { draft.newPassword = it; inline = null }, "Nueva contraseña", "Mínimo 8 caracteres", showRequirements = true)
        Spacer(Modifier.height(13.dp))
        Password(draft.newPasswordConfirm, { draft.newPasswordConfirm = it; inline = null }, "Confirmar contraseña", "Repite tu nueva contraseña")
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(18.dp))
        Primary("Actualizar contraseña") { inline = vm.updateRecoveredPassword(draft.newPassword, draft.newPasswordConfirm) }
    }
}

@Composable
private fun LoadingScreen(vm: AuthViewModel) {
    SystemStateLayout {
        Cabito(R.drawable.cabito_loading, 210, 210)
        Text(vm.state.loadingMessage, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Estamos preparando tu experiencia.", color = TadeoMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        GlassSpinner(size = 32.dp, strokeWidth = 3.dp)
    }
}

@Composable
private fun SystemStateLayout(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp)) {
        Brand()
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
    }
}

@Composable
private fun SystemStateScreen(title: String, message: String, image: Int, button: String, onClick: () -> Unit) {
    SystemStateLayout {
        Cabito(image, 210, 210); Text(title, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center); Text(message, color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 285.dp).padding(top = 6.dp)); Spacer(Modifier.height(12.dp)); Box(Modifier.widthIn(max = 280.dp)) { Primary(button, onClick = onClick) }
    }
}

@Composable
private fun ErrorScreen(vm: AuthViewModel) {
    val e = vm.state.error ?: AuthError.InvalidCredentials
    val img = when (e) { AuthError.Offline -> R.drawable.cabito_offline; AuthError.Server -> R.drawable.cabito_error; AuthError.Timeout, AuthError.RateLimit, AuthError.AccountLocked -> R.drawable.cabito_think; else -> R.drawable.cabito_confused }
    val offerRecovery = e == AuthError.RateLimit || e == AuthError.AccountLocked
    SystemStateLayout {
        Cabito(img, 190, 190)
        Text(e.title, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(e.message, color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 285.dp).padding(top = 6.dp))
        Spacer(Modifier.height(16.dp)); Box(Modifier.widthIn(max = 280.dp)) { Primary(if (offerRecovery) "Recuperar contraseña" else "Intentar de nuevo") { if (offerRecovery) vm.go(AuthScreen.Forgot) else vm.retryFromError() } }
    }
}

@Composable
private fun ErrorLibrary(vm: AuthViewModel) {
    val access = listOf(AuthError.InvalidCredentials, AuthError.AccountLocked, AuthError.RateLimit, AuthError.InvalidInstitutional, AuthError.Duplicate, AuthError.WeakPassword, AuthError.PasswordMismatch, AuthError.InvalidCode, AuthError.ExpiredCode)
    val network = listOf(AuthError.Offline, AuthError.Timeout, AuthError.Server)
    ScrollPage {
        AuthTopBar("Estados y errores") { vm.back() }
        Text("FLUJO COMPLETO", color = TadeoAqua, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.7.sp, modifier = Modifier.padding(top = 14.dp))
        Text("Errores por módulo", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
        Text("Cada escenario es navegable y muestra mensaje, recuperación y retorno. Sirve para revisar también los caminos negativos, no solo el happy path.", color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp)
        ErrorGroup("Acceso y cuenta", access, vm)
        ErrorGroup("Conectividad y sistema", network, vm)
    }
}

@Composable
private fun ErrorGroup(title: String, errors: List<AuthError>, vm: AuthViewModel) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp).glassPanel(RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 5.dp)) {
            Text(title, color = TadeoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
            errors.forEach { e ->
                HorizontalDivider(color = Color(0xFFF0F2F5))
                Row(Modifier.fillMaxWidth().clickable { vm.showError(e) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(e.title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(e.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp) }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TadeoMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
