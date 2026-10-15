# Contrato funcional — Módulo 01 Acceso

Un módulo no se considera completo solo por tener el happy path. Debe cubrir:

**entrada → validación → carga → éxito → error → recuperación → reintento → éxito**.

Reglas bloqueadas:
- Login no pregunta si la persona es estudiante o profesor.
- El rol se resuelve por la identidad institucional.
- Registro puede solicitar rol para validar el vínculo institucional.
- El correo se construye con usuario + dominio fijo `@utadeo.edu.co`.
- No pedir asignaturas al profesor durante creación de credenciales.
- Errores deben ser visibles y recuperables, no toasts sin consecuencia.
- Los datos ingresados deben conservarse ante errores recuperables mientras la pantalla permanezca en memoria.
- Cabito es provisional y no se modifica en este módulo.
