package com.github.laxy.shared

sealed class InteractionResult<E, D> {
    abstract fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit)

    abstract fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD): InteractionResult<ME, MD>

    abstract fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D>

    abstract fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD>
    //    data class Success< D>(val data: D) : Result<Nothing, D>()
    //    data class Failure<E>(val error: E) : Result<E, Nothing>()
    //
    //    fun isSuccess(): Boolean = this is Success<D>
    //    fun isFailure(): Boolean = this is Failure<E>
    //
    //    fun getOrNull(): D? = (this as? Success)?.data
    //    fun errorOrNull(): E? = (this as? Failure)?.error
    //
    //    fun <R> map(transform: (D) -> R): Result<out E, out R> = when (this) {
    //        is Success -> Success(transform(data))
    //        is Failure -> this
    //    }
    //
    //    fun <R> flatMap(transform: (D) -> Result<E, R>): Result<E, out R> = when (this) {
    //        is Success -> transform(data)
    //        is Failure -> this
    //    }
    //
    //    fun <R> fold(
    //        onSuccess: (D) -> R,
    //        onFailure: (E) -> R
    //    ): R = when (this) {
    //        is Success -> onSuccess(data)
    //        is Failure -> onFailure(error)
    //    }
}

data class Failure<E, D>(val error: E) : InteractionResult<E, D>() {
    override fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit) = onFailure(error)

    override fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD) =
        Failure<ME, MD>(failure(error))

    override fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D> =
        Failure(transform(error))

    override fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD> = Failure(error)
}

data class Success<E, D>(private val data: D) : InteractionResult<E, D>() {
    override fun fold(onFailure: (E) -> Unit, onSuccess: (D) -> Unit) = onSuccess(data)

    override fun <ME, MD> map(failure: (E) -> ME, success: (D) -> MD) =
        Success<ME, MD>(success(data))

    override fun <ME> mapFailure(transform: (E) -> ME): InteractionResult<ME, D> = Success(data)

    override fun <MD> mapSuccess(transform: (D) -> MD): InteractionResult<E, MD> =
        Success(transform(data))
}
