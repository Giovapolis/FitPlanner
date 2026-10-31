package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.AiSettings
import com.fitplanner.ai.core.model.AiSuggestionRequest
import com.fitplanner.ai.core.model.AiSuggestionResponse

interface AiSuggestionProvider {
    suspend fun suggest(
        request: AiSuggestionRequest,
        settings: AiSettings,
        apiKey: String
    ): Result<AiSuggestionResponse>
}
