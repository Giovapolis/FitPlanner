# Reglas de entrenamiento

El motor es determinista: con el mismo perfil, equipo, restricciones y fecha produce la misma selección. No usa aleatoriedad ni deja que una IA decida el plan final.

## Entrada

- Objetivo principal y secundarios.
- Nivel: principiante, intermedio, avanzado.
- Días disponibles y minutos máximos.
- Equipamiento por ubicación (casa/gimnasio).
- Lesiones y zonas declaradas.
- Ejercicios a evitar.
- Preferencias musculares.

## Selección de división

| Días | Principiante | Intermedio | Avanzado |
|---|---|---|---|
| 1 | Cuerpo completo | Cuerpo completo | Cuerpo completo |
| 2 | Cuerpo completo A/B | Cuerpo completo A/B | Tren superior/inferior |
| 3 | Cuerpo completo A/B/C | Empuje/tirón/piernas | Empuje/tirón/piernas |
| 4 | Superior/inferior x2 | Superior/inferior x2 | Superior/inferior x2 |
| 5 | Cuerpo completo + superior/inferior | Empuje/tirón/piernas + superior/inferior | Empuje/tirón/piernas + superior/inferior |
| 6 | Superior/inferior x3 | Empuje/tirón/piernas x2 | Empuje/tirón/piernas x2 |
| 7 | Hasta 6 sesiones + recuperación | Hasta 6 + recuperación | Hasta 6 + recuperación |

El objetivo de movilidad usa sesiones específicas de movilidad, core y recuperación. El objetivo cardiovascular puede añadir una sesión de acondicionamiento.

## Selección de ejercicios

Filtros obligatorios:

1. Ubicación compatible con la sesión.
2. Equipamiento requerido presente en el perfil de esa ubicación.
3. Dificultad no superior al nivel.
4. Sin intersección con etiquetas de restricción declaradas.
5. Sin coincidencia con ejercicios que el usuario pidió evitar.
6. Sin duplicados dentro de la sesión y con máximo de 2 usos semanales por ejercicio.

Puntuación determinista:

- Preferencia muscular.
- Coincidencia con el objetivo.
- Compuesto antes que aislamiento.
- Ajuste de dificultad.
- Desempate por ID.

## Volumen y prescripción

| Objetivo | Series | Repeticiones | Descanso orientativo |
|---|---|---|---|
| Fuerza | 2–5 | 4–6 | 150 s |
| Hipertrofia | 2–5 | 8–12 | 75 s |
| Pérdida de grasa | 2–4 | 12–15 | 45 s |
| Cardio | 1 (tiempo) | minutos | — |
| Movilidad | 2 | 8–10 | 30 s |
| Salud general | 2–4 | 8–12 | 60 s |

Límites:

- Principiante: base de 2 series.
- Intermedio: base de 3.
- Avanzado: base de 4.
- Compuestos: +1 serie, máximo 5.
- Volumen semanal orientativo: principiante 6–10 series/músculo, intermedio 10–16, avanzado 12–20.
- El validador avisa si se superan 20 series semanales de un músculo.
- No se programan al fallo. La intensidad se expresa con RIR.

## Duración

La duración incluye:

- Calentamiento: 4–7 min.
- Trabajo: series × (repeticiones × 3 s + descanso).
- Transiciones: 45 s por ejercicio.
- Vuelta a la calma: 2–4 min.

Si el plan excede el tiempo disponible, se eliminan accesorios y luego series, nunca el calentamiento. Si no cabe una sesión mínima, se bloquea o se convierte el día en recuperación.

## Validación

`PlanValidator` comprueba:

- Sesiones no vacías.
- Ejercicios existentes en catálogo.
- Equipo disponible.
- Restricciones declaradas.
- Duplicados.
- Duración frente al tiempo disponible.
- Volumen semanal alto.
- Más de 3 días consecutivos.

Un fallo fatal impide guardar el plan. Un aviso se muestra para revisión.

## Progresión semanal

- Dolor: reduce una serie por ejercicio (mínimo 2), añade 15 s de descanso y no aumenta cargas.
- Demasiado difícil o duración incumplida: reduce volumen o mantiene.
- Fácil, sin dolor y duración cumplida: puede añadir una serie a los dos primeros ejercicios.
- Mantener: copia la estructura.
- Nunca se aumenta el peso automáticamente. El usuario decide la carga.
- La semana anterior se conserva con sus logs.

## IA

La IA solo puede proponer slugs de ejercicios. El validador local comprueba que existan, sean compatibles con equipo y restricciones, no dupliquen la sesión y respeten el nivel. Cualquier propuesta inválida se descarta.
