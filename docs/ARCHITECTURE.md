# Arquitectura

## Capas

```
UI Compose (features)
        ↓
ViewModel + StateFlow
        ↓
Casos de uso / motor de reglas (core:domain)
        ↓
Repositorios (core:data)
        ↓
Room + DataStore + WorkManager + red opcional
```

- `core:model` no depende de Android.
- `core:domain` es Kotlin puro y testeable en JVM.
- `core:data` implementa persistencia y servicios.
- Las features no acceden a DAOs.
- `app` compone navegación y Hilt.

## Módulos

| Módulo | Responsabilidad |
|---|---|
| `:app` | `MainActivity`, `FitPlannerApplication`, NavHost, permiso de notificaciones |
| `:core:model` | Entidades de dominio, enums, snapshots de plan |
| `:core:domain` | `PlanGenerator`, `PlanValidator`, `SafetyGate`, `WeekAdjustmentEngine`, `ProgressCalculator`, `SubstitutionEngine`, `DurationEstimator` |
| `:core:data` | Room, DataStore, repositorios, catálogo semilla, Moshi, IA opcional, WorkManager |
| `:core:designsystem` | Tema, tipografía, tarjetas, botones, estados vacíos y animación de técnica |
| `:feature:onboarding` | Asistente de 5 pasos y cribado |
| `:feature:dashboard` | Resumen diario y semanal |
| `:feature:planner` | Generación, revisión y semana |
| `:feature:exercises` | Catálogo, filtros y fichas |
| `:feature:workout` | Sesión activa, series, descanso y sustituciones |
| `:feature:progress` | Cumplimiento, volumen, récords, historial y revisión |
| `:feature:profile` | Datos, salud, equipo, recordatorios e IA |

## Persistencia

Room contiene:

- `profile`, `health`, `experience`: JSON tipado para snapshots de perfil.
- `availability`: una fila por día.
- `equipment_profile`: perfiles con equipamiento serializado.
- `exercise`: metadatos para consulta y JSON completo para la ficha.
- `favorite`.
- `plan`: plan completo serializado para preservar el historial inmutable.
- `body_metric`: reservado para métricas corporales.

DataStore guarda:

- `onboarding_completed`, paso y borrador.
- preferencias de recordatorios.
- configuración de IA y consentimiento.

La API key se guarda con `EncryptedSharedPreferences` y Keystore. Si el dispositivo no soporta el almacén seguro, la app sigue funcionando sin clave.

Los planes se guardan como snapshots JSON. Esto simplifica la inmutabilidad: una semana cerrada conserva exactamente los ejercicios, series y registros que se usaron. Las entidades principales sí son tablas Room. La deuda técnica es que consultas analíticas complejas futuras requerirán normalizar sesiones y series.

## Flujo de generación

1. `PlannerViewModel` lee perfil, salud, experiencia, disponibilidad, equipo y catálogo.
2. Construye `PlanGenerationInput`.
3. `PlanGenerator` aplica `SafetyGate`, elige división, filtra ejercicios, asigna series/reps/descanso, estima duración y valida.
4. `PlanRepository` archiva el plan activo y guarda el nuevo.
5. `Dashboard` y `Planner` observan el plan activo con Flow.

## Flujo de sesión

1. `WorkoutViewModel` recibe `sessionId` por `SavedStateHandle`.
2. Marca la sesión `IN_PROGRESS`.
3. Cada serie se añade como `SetLog` dentro del snapshot del plan.
4. El temporizador de descanso vive en el ViewModel.
5. Al finalizar, la sesión pasa a `COMPLETED`.
6. `ProgressCalculator` agrega solo series registradas.

## Revisión semanal

`WeekAdjustmentEngine`:

- Copia la semana anterior y la marca `COMPLETED`.
- Crea una semana nueva con fechas desplazadas.
- Reduce series/descanso si hubo dolor o dificultad.
- Añade como máximo una serie a los dos primeros ejercicios si fue fácil y se cumplió la duración.
- Nunca cambia el peso automáticamente.
- No toca los logs de la semana cerrada.

## Recordatorios

- `ReminderWorker` comprueba si hay sesión hoy y notifica.
- `ReminderScheduler` usa trabajo periódico diario con retraso inicial según la hora elegida.
- `WorkManager` se inicializa con `HiltWorkerFactory`.
- No se usan alarmas exactas.

## IA opcional

- `AiRepository` construye un resumen anonimizado.
- `OpenAiCompatibleProvider` llama a `/chat/completions`.
- La respuesta JSON se parsea con Moshi.
- `PlanValidator.validateSuggestion` descarta slugs que no existan, no estén en el equipo, contradigan restricciones o dupliquen la sesión.
- Si la IA falla, la app mantiene las alternativas locales.

## Decisiones y deuda técnica

- Multimódulo real para separar capas y features.
- Hilt en todas las capas.
- Sin Coil ni librería de gráficos: animaciones y visualizaciones se dibujan con Compose Canvas.
- El plan en JSON prioriza inmutabilidad y velocidad de implementación.
- No hay sincronización ni backend.
- No hay tests instrumentados ejecutados por falta de dispositivo/emulador en el entorno.
