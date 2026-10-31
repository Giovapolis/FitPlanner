package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.HealthScreening

sealed class SafetyGateResult {
    data class Allowed(val warnings: List<String> = emptyList()) : SafetyGateResult()
    data class Blocked(val reason: String, val detail: String) : SafetyGateResult()
}

object SafetyGate {
    fun evaluate(health: HealthScreening): SafetyGateResult {
        if (health.hasRedFlags == true) {
            return SafetyGateResult.Blocked(
                reason = "No podemos generar una rutina automática",
                detail = "Has indicado dolor, mareos, desmayos, dolor de pecho o dificultad respiratoria inusual durante el ejercicio. Consulta con un profesional de la salud antes de entrenar."
            )
        }
        if (health.hasMedicalRecommendation == true) {
            return SafetyGateResult.Blocked(
                reason = "Hay una recomendación médica que debemos respetar",
                detail = "Para generar un plan necesitamos que un profesional valore tus restricciones. Puedes consultar el catálogo educativo mientras tanto."
            )
        }
        val warnings = mutableListOf<String>()
        if (health.prefersNotToAnswer) {
            warnings += "No respondiste al cribado de salud. El plan será conservador, pero no sustituye una valoración profesional."
        }
        if (health.hasInjury == true) {
            warnings += "Adaptaremos el plan a las limitaciones declaradas. Esto no confirma que un ejercicio sea seguro para tu lesión."
        }
        if (health.hasHealthCondition == true) {
            warnings += "Consulta con tu profesional sanitario si tu condición puede afectar al entrenamiento."
        }
        return SafetyGateResult.Allowed(warnings)
    }

    fun restrictionTags(health: HealthScreening): Set<String> {
        if (health.hasInjury != true) return emptySet()
        return health.injuryAreas.map { it.name }.toSet()
    }
}
