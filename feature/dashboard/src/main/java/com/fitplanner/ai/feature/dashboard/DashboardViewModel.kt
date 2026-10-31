package com.fitplanner.ai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.domain.ProgressCalculator
import com.fitplanner.ai.core.model.DashboardData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    planRepository: PlanRepository,
    profileRepository: ProfileRepository
) : ViewModel() {
    val state: StateFlow<DashboardData> = combine(
        planRepository.activePlan,
        profileRepository.profile
    ) { plan, profile ->
        ProgressCalculator.buildDashboard(plan, profile, LocalDate.now())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardData()
    )
}
