package com.fitplanner.ai.core.model

import java.time.DayOfWeek

enum class UnitSystem(val label: String) {
    METRIC("kg / cm"),
    IMPERIAL("lb / in")
}

enum class Goal(val label: String) {
    MUSCLE_GAIN("Ganar masa muscular"),
    STRENGTH("Aumentar fuerza"),
    CARDIO("Mejorar resistencia cardiovascular"),
    FAT_LOSS("Perder grasa corporal"),
    MOBILITY("Mejorar movilidad y flexibilidad"),
    GENERAL_FITNESS("Mejorar condición física general"),
    STAY_ACTIVE("Mantenerme activo y saludable")
}

enum class ExperienceLevel(val label: String) {
    BEGINNER("Principiante"),
    INTERMEDIATE("Intermedio"),
    ADVANCED("Avanzado")
}

enum class ActivityLevel(val label: String) {
    SEDENTARY("Sedentaria"),
    LIGHT("Ligera"),
    MODERATE("Moderada"),
    HIGH("Alta")
}

enum class AgeRange(val label: String) {
    UNDER_18("Menos de 18"),
    AGE_18_24("18–24"),
    AGE_25_34("25–34"),
    AGE_35_44("35–44"),
    AGE_45_54("45–54"),
    AGE_55_64("55–64"),
    AGE_65_PLUS("65 o más")
}

enum class BodyArea(val label: String) {
    KNEE("Rodilla"),
    LOWER_BACK("Zona lumbar"),
    SHOULDER("Hombro"),
    WRIST("Muñeca"),
    HIP("Cadera"),
    ANKLE("Tobillo"),
    NECK("Cuello"),
    ELBOW("Codo"),
    OTHER("Otra zona")
}

enum class IntensityPreference(val label: String) {
    LOW("Suave"),
    MODERATE("Moderada"),
    HIGH("Exigente")
}

enum class SessionType(val label: String) {
    FULL_BODY("Cuerpo completo"),
    UPPER("Tren superior"),
    LOWER("Tren inferior"),
    STRENGTH("Fuerza"),
    CARDIO("Cardio"),
    MOBILITY("Movilidad"),
    REST("Descanso")
}

enum class LocationType(val label: String) {
    HOME("Casa"),
    GYM("Gimnasio"),
    BOTH("Casa y gimnasio")
}

enum class EquipmentType(val label: String, val location: LocationType) {
    BODYWEIGHT("Peso corporal", LocationType.BOTH),
    DUMBBELLS("Mancuernas", LocationType.BOTH),
    BARBELL("Barra y discos", LocationType.BOTH),
    RESISTANCE_BANDS("Bandas elásticas", LocationType.HOME),
    BENCH("Banco", LocationType.BOTH),
    PULL_UP_BAR("Barra de dominadas", LocationType.BOTH),
    KETTLEBELLS("Kettlebells", LocationType.HOME),
    EXERCISE_BIKE("Bicicleta estática", LocationType.HOME),
    TREADMILL("Cinta de correr", LocationType.HOME),
    CABLE_MACHINE("Máquinas de poleas", LocationType.GYM),
    LEG_PRESS("Prensa de piernas", LocationType.GYM),
    LEG_EXTENSION_CURL("Extensión y curl de piernas", LocationType.GYM),
    CHEST_PRESS_MACHINE("Máquina de pecho", LocationType.GYM),
    LAT_PULLDOWN("Jalón al pecho", LocationType.GYM),
    SEATED_ROW("Remo sentado", LocationType.GYM),
    SMITH_MACHINE("Smith machine", LocationType.GYM),
    RACK("Rack y barras", LocationType.GYM),
    ADJUSTABLE_BENCH("Bancos ajustables", LocationType.GYM),
    CABLES("Cables y accesorios", LocationType.GYM),
    CARDIO_MACHINES("Máquinas cardiovasculares", LocationType.GYM),
    MAT("Esterilla", LocationType.BOTH)
}

enum class MuscleGroup(val label: String) {
    QUADS("Cuádriceps"),
    GLUTES("Glúteos"),
    HAMSTRINGS("Isquiotibiales"),
    CALVES("Pantorrillas"),
    CHEST("Pecho"),
    BACK("Espalda"),
    LATS("Dorsales"),
    SHOULDERS("Hombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    CORE("Core"),
    CARDIO("Cardio"),
    FULL_BODY("Cuerpo completo")
}

enum class MovementPattern(val label: String) {
    SQUAT("Sentadilla"),
    HINGE("Bisagra de cadera"),
    HORIZONTAL_PUSH("Empuje horizontal"),
    VERTICAL_PUSH("Empuje vertical"),
    HORIZONTAL_PULL("Tracción horizontal"),
    VERTICAL_PULL("Tracción vertical"),
    LUNGE("Zancada"),
    CARRY("Acarreo"),
    CORE("Core"),
    CARDIO("Cardio"),
    MOBILITY("Movilidad")
}

enum class ExerciseType(val label: String) {
    COMPOUND("Compuesto"),
    ISOLATION("Aislamiento"),
    BODYWEIGHT("Peso corporal"),
    CARDIO("Cardio"),
    MOBILITY("Movilidad")
}

enum class Difficulty(val label: String) {
    BEGINNER("Principiante"),
    INTERMEDIATE("Intermedio"),
    ADVANCED("Avanzado")
}

enum class ExerciseStatus {
    PENDING,
    COMPLETED,
    SKIPPED,
    SUBSTITUTED
}

enum class SessionStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

enum class WeekStatus {
    PLANNED,
    ACTIVE,
    COMPLETED
}

enum class PlanStatus {
    ACTIVE,
    ARCHIVED
}

enum class Adjustment {
    MAINTAIN,
    REDUCE,
    INCREASE
}

enum class AiMode {
    OFF,
    OPENAI_COMPATIBLE
}

val DayOfWeek.shortLabel: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "Lun"
        DayOfWeek.TUESDAY -> "Mar"
        DayOfWeek.WEDNESDAY -> "Mié"
        DayOfWeek.THURSDAY -> "Jue"
        DayOfWeek.FRIDAY -> "Vie"
        DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }

val DayOfWeek.fullLabel: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miércoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sábado"
        DayOfWeek.SUNDAY -> "Domingo"
    }
