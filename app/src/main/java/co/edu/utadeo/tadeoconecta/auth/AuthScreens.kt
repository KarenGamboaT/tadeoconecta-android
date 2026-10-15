package co.edu.utadeo.tadeoconecta.auth

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.utadeo.tadeoconecta.MainAppShell
import co.edu.utadeo.tadeoconecta.R
import co.edu.utadeo.tadeoconecta.ui.theme.*

@Composable
fun AuthApp(vm: AuthViewModel = viewModel()) {
    Surface(Modifier.fillMaxSize(), color = TadeoCanvas) {
        when (vm.state.screen) {
            AuthScreen.Startup -> LoadingScreen(vm)
            AuthScreen.Login -> LoginScreen(vm)
            AuthScreen.SignUp -> SignUpScreen(vm)
            AuthScreen.Verify -> VerifyScreen(vm)
            AuthScreen.ProfessorCode -> ProfessorCodeScreen(vm)
            AuthScreen.ProfessorSubjects -> ProfessorSubjectsScreen(vm)
            AuthScreen.Forgot -> ForgotScreen(vm)
            AuthScreen.MailSent -> SystemStateScreen("Correo enviado", "Revisa tu bandeja de entrada para continuar.", R.drawable.cabito_success, "Volver al login") { vm.go(AuthScreen.Login) }
            AuthScreen.Loading -> LoadingScreen(vm)
            AuthScreen.Error -> ErrorScreen(vm)
            AuthScreen.ErrorLibrary -> ErrorLibrary(vm)
            AuthScreen.Destination -> MainAppShell(vm.state.destinationRole ?: UserRole.Student) { vm.logout() }
        }
    }
}

@Composable
private fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(TadeoCanvas).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 26.dp),
        content = content
    )
}

@Composable
private fun Brand() {
    Image(
        painter = painterResource(R.drawable.utadeo_logo),
        contentDescription = "Universidad de Bogotá Jorge Tadeo Lozano",
        modifier = Modifier.width(152.dp).height(33.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun AuthTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = TadeoInk, fontSize = 25.sp)
        }
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun Cabito(res: Int, width: Int = 132, height: Int = width) {
    Image(painterResource(res), "Cabito", Modifier.width(width.dp).height(height.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun Primary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TadeoBlue)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Secondary(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA9C7FF))) {
        Text(text, fontWeight = FontWeight.Bold)
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
            Modifier.fillMaxWidth().height(48.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, TadeoBorder, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp),
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
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TadeoInk)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, TadeoBorder, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
            )
        }
        if (showRequirements) PasswordRequirements(value)
    }
}

@Composable
private fun HeroLogin() {
    Box(
        Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFEAF5FF), Color(0xFFDCF6F3))))
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
private fun LoginScreen(vm: AuthViewModel) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var inline by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(TadeoCanvas).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Brand(); Spacer(Modifier.height(16.dp)); HeroLogin(); Spacer(Modifier.height(17.dp))
        InstitutionalUser(user, { user = it; inline = null }); Spacer(Modifier.height(13.dp))
        Password(pass, { pass = it; inline = null }); inline?.let { InlineError(it) }; Spacer(Modifier.height(13.dp))
        Primary("Iniciar sesión") { inline = vm.login(user, pass) }
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
    Surface(color = Color(0xFFEDF5FF), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cabito(image, 54, 54); Spacer(Modifier.width(11.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) }
        }
    }
}

@Composable
private fun RoleSelector(role: UserRole, onRole: (UserRole) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFEDF0F4), RoundedCornerShape(14.dp)).padding(4.dp)) {
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
private fun SignUpScreen(vm: AuthViewModel) {
    var name by remember { mutableStateOf("") }; var user by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var terms by remember { mutableStateOf(false) }; var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Crear cuenta") { vm.back() }
        GuideCard(R.drawable.cabito_point, "Primero validemos tu vínculo con UTadeo.", "Selecciona tu rol y usa exclusivamente tu correo institucional.")
        if (vm.state.role == UserRole.Professor) {
            Surface(color = TadeoSoft, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) { Text("Perfil docente después del registro", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Las asignaturas, grupos y datos académicos se completarán o sincronizarán en tu perfil docente; no se solicitan para crear la contraseña.", color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) }
            }
        }
        RoleSelector(vm.state.role, vm::setRole)
        Spacer(Modifier.height(13.dp)); Text("Nombre completo", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
        BasicTextField(value = name, onValueChange = { name = it; inline = null }, singleLine = true, modifier = Modifier.fillMaxWidth().height(48.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, TadeoBorder, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk), decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { if (name.isEmpty()) Text("Karen Gamboa", color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() } })
        Spacer(Modifier.height(13.dp)); InstitutionalUser(user, { user = it; inline = null }, "Usuario institucional")
        Spacer(Modifier.height(13.dp)); Password(pass, { pass = it; inline = null }, "Contraseña", "Mínimo 8 caracteres", showRequirements = true)
        Spacer(Modifier.height(13.dp)); Password(confirm, { confirm = it; inline = null }, "Confirmar contraseña", "Repite tu contraseña")
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 10.dp)) { Checkbox(terms, { terms = it; inline = null }); Text("Acepto términos, tratamiento de datos y política de privacidad.", fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
        inline?.let { InlineError(it) }; Spacer(Modifier.height(8.dp)); Primary("Validar correo y continuar") { inline = vm.signUp(name, user, pass, confirm, terms) }
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
                        Box(Modifier.weight(1f).height(48.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, TadeoBorder, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
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
private fun VerifyScreen(vm: AuthViewModel) {
    var code by remember { mutableStateOf("") }; var inline by remember { mutableStateOf<String?>(null) }; var resent by remember { mutableStateOf(false) }
    ScrollPage {
        AuthTopBar("Verificar correo") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Cabito(R.drawable.cabito_search, 132, 132)
            Text("Revisa tu correo institucional", fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Enviamos un código de 6 dígitos a tu cuenta @utadeo.edu.co.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(20.dp)); OtpInput(code) { code = it; inline = null }
        inline?.let { InlineError(it) }; if (resent) Text("Código reenviado.", color = TadeoSuccess, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp)); Primary("Confirmar código") { inline = vm.verify(code) }
        TextButton(onClick = { vm.resendCode(); resent = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Reenviar código") }
        Text("El código se valida directamente con Supabase Auth.", color = TadeoMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}


@Composable
private fun ProfessorCodeScreen(vm: AuthViewModel) {
    var code by remember { mutableStateOf("") }
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
            value = code,
            onValueChange = { code = it.uppercase(); inline = null },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.dp, TadeoBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (code.isEmpty()) Text("Código institucional", color = Color(0xFF9AA3B0), fontSize = 14.sp)
                    inner()
                }
            }
        )
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(18.dp))
        Primary("Validar código docente") { inline = vm.validateProfessorCode(code) }
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
            "Selecciona las asignaturas que dictas actualmente. Podrás modificarlas después desde tu perfil."
        )
        if (vm.state.subjects.isEmpty()) {
            Surface(
                color = Color(0xFFFFF7DB),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1DE9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
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
            vm.state.subjects.forEach { subject ->
                val checked = subject.id in vm.state.selectedSubjectIds
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) TadeoBlue else TadeoBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { vm.toggleSubject(subject.id) }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { vm.toggleSubject(subject.id) })
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            subject.code?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = TadeoMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        inline?.let { InlineError(it) }
        Spacer(Modifier.height(10.dp))
        Primary("Guardar y entrar al panel docente", enabled = vm.state.subjects.isNotEmpty()) {
            inline = vm.saveProfessorSubjects()
        }
    }
}

@Composable
private fun ForgotScreen(vm: AuthViewModel) {
    var user by remember { mutableStateOf("") }; var inline by remember { mutableStateOf<String?>(null) }
    ScrollPage {
        AuthTopBar("Recuperar contraseña") { vm.back() }
        Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Cabito(R.drawable.cabito_point, 132, 132) }
        Text("Recupera tu acceso", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
        Text("Te enviaremos instrucciones a tu correo institucional.", color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(22.dp)); InstitutionalUser(user, { user = it; inline = null }); inline?.let { InlineError(it) }; Spacer(Modifier.height(18.dp)); Primary("Enviar instrucciones") { inline = vm.forgot(user) }
    }
}

@Composable
private fun LoadingScreen(vm: AuthViewModel) {
    SystemStateLayout {
        Cabito(R.drawable.cabito_loading, 210, 210)
        Text(vm.state.loadingMessage, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Estamos preparando tu experiencia.", color = TadeoMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = TadeoBlue, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SystemStateLayout(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(TadeoCanvas).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp)) {
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
    val img = when (e) { AuthError.Offline -> R.drawable.cabito_offline; AuthError.Server -> R.drawable.cabito_error; AuthError.Timeout, AuthError.RateLimit -> R.drawable.cabito_think; else -> R.drawable.cabito_confused }
    SystemStateLayout {
        Cabito(img, 190, 190)
        Text("ESTADO DE ERROR", color = TadeoError, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.7.sp)
        Text(e.title, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(e.message, color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 285.dp).padding(top = 6.dp))
        Surface(color = Color(0xFFF6F8FB), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.widthIn(max = 300.dp).padding(top = 10.dp)) {
            Column(Modifier.padding(13.dp)) { Text("Qué puedes hacer", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(e.recovery, color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp)) }
        }
        Spacer(Modifier.height(12.dp)); Box(Modifier.widthIn(max = 280.dp)) { Primary(if (e == AuthError.RateLimit) "Recuperar contraseña" else "Intentar de nuevo") { if (e == AuthError.RateLimit) vm.go(AuthScreen.Forgot) else vm.retryFromError() } }
        TextButton(onClick = { vm.go(AuthScreen.ErrorLibrary) }) { Text("Volver a biblioteca de errores", color = TadeoBlue) }
    }
}

@Composable
private fun ErrorLibrary(vm: AuthViewModel) {
    val access = listOf(AuthError.InvalidCredentials, AuthError.RateLimit, AuthError.InvalidInstitutional, AuthError.Duplicate, AuthError.WeakPassword, AuthError.PasswordMismatch, AuthError.InvalidCode, AuthError.ExpiredCode)
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
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 5.dp)) {
            Text(title, color = TadeoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
            errors.forEach { e ->
                HorizontalDivider(color = Color(0xFFF0F2F5))
                Row(Modifier.fillMaxWidth().clickable { vm.showError(e) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(e.title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(e.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp) }
                    Text("›", color = TadeoMuted, fontSize = 20.sp)
                }
            }
        }
    }
}
