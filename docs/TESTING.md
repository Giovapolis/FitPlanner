# Pruebas y verificación

## Comandos

```bash
cmd.exe /c "cd /d C:\Users\Giovapolis\FitPlanner && gradlew.bat :core:domain:test :core:data:testDebugUnitTest :app:lintDebug :app:assembleDebug"
```

## Cobertura actual

### Motor de dominio

- Generación con 3 días y 45 minutos.
- Bloqueo por síntomas de alarma.
- Bloqueo por recomendación médica.
- Exclusión de ejercicios por lesión de rodilla.
- Exclusión de equipamiento no disponible.
- Determinismo con la misma entrada.
- Bloqueo sin días disponibles.
- Reducción por dolor y conservación de logs.
- Aumento máximo de una serie sin cambio automático de peso.
- Etiquetas de restricción.
- Estimación de duración con trabajo, descanso y cardio.

### Datos

- Serialización de planes con `LocalDate` y logs anidados.
- Serialización de `Set<EquipmentType>`.
- Catálogo semilla con los 20 ejercicios requeridos y campos completos.
- Persistencia de onboarding en Room.
- Seed idempotente del catálogo.

### Lint

- `:app:lintDebug` sin errores.
- Quedan avisos informativos de versiones disponibles y targetSdk, intencionadamente fijados a la combinación estable.

## Pruebas manuales recomendadas en dispositivo

1. Primer arranque y onboarding completo.
2. Guardar y reanudar el asistente cerrando la app.
3. Finalizar onboarding con síntoma de alarma y comprobar bloqueo del plan.
4. Generar rutina con 2, 3 y 5 días.
5. Cambiar equipamiento y regenerar.
6. Abrir ficha, favorito, búsqueda y filtros.
7. Iniciar sesión, registrar series y usar el temporizador.
8. Sustituir un ejercicio localmente y con IA desactivada.
9. Finalizar sesión y comprobar dashboard/progreso.
10. Guardar revisión semanal con dolor y comprobar reducción.
11. Reiniciar asistente y verificar datos precargados.
12. Activar recordatorios y comprobar notificación.
13. Probar tema oscuro, fuente grande y TalkBack.
14. Configurar IA con una clave válida y comprobar que las propuestas inválidas se descartan.

## Estado del entorno

- Compilación: OK.
- Tests JVM: OK.
- Robolectric/Room: OK.
- Lint: OK.
- Emulador/dispositivo: no disponible en el entorno; los pasos manuales quedan pendientes.
