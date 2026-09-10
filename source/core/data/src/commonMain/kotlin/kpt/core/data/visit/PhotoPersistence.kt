package kpt.core.data.visit

import kpt.core.model.visit.VisitPhoto

/**
 * Persists a captured photo from a temporary/cache location to permanent storage.
 * Returns the updated VisitPhoto with [renderedPath] pointing to the persistent copy.
 * If the photo is already in persistent storage or cannot be copied, returns the original.
 */
expect suspend fun persistPhotoToStorage(
    photo: VisitPhoto,
): VisitPhoto