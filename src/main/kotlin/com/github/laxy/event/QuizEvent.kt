package com.github.laxy.event

import com.github.laxy.persistence.QuizId
import kotlinx.coroutines.flow.MutableSharedFlow

object QuizEvent {
    val eventChannel = MutableSharedFlow<Pair<QuizId, String>>(extraBufferCapacity = 100)
}
