package uz.beko404.track14.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult

/** Calls a callable Function and maps its result; errors become [DomainError]s. */
internal suspend fun <T> FirebaseFunctions.callFunction(
    name: String,
    input: Map<String, Any>,
    parse: (Map<*, *>) -> T?,
): DomainResult<T> = try {
    val data = getHttpsCallable(name).call(input).await().data as? Map<*, *> ?: emptyMap<Any, Any>()
    parse(data)?.let { DomainResult.Success(it) } ?: DomainResult.Failure(DomainError.Unknown())
} catch (e: Exception) {
    DomainResult.Failure(e.toDomainError())
}
