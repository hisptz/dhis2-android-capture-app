package org.dhis2.mobile.omrocr.domain

import kotlinx.coroutines.CancellationException
import org.dhis2.mobile.commons.domain.UseCase
import org.dhis2.mobile.omrocr.model.FormSection

class GetFormSections(
    private val repository: FormSectionsRepository,
) : UseCase<String, List<FormSection>> {
    override suspend fun invoke(input: String): Result<List<FormSection>> =
        try {
            Result.success(repository.sections(input))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
