package com.github.laxy.shared

sealed class InteractionResult<E, D> {
    abstract fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit)

    abstract fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD): InteractionResult<ME, MD>

    abstract fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D>

    abstract suspend fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD>
}

data class Failure<E, D>(val error: E) : InteractionResult<E, D>() {
    override fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit) = onFailure(error)

    override fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD) =
        Failure<ME, MD>(failure(error))

    override fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D> =
        Failure(transform(error))

    override suspend fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD> = Failure(error)
}

data class Success<E, D>(val data: D) : InteractionResult<E, D>() {
    override fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit) = onSuccess(data)

    override fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD) =
        Success<ME, MD>(success(data))

    override fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D> = Success(data)

    override suspend fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD> =
        Success(transform(data))
}
