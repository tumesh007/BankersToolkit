package kpt.core.data.visit

import kpt.core.model.visit.VisitPhoto

actual suspend fun persistPhotoToStorage(
    photo: VisitPhoto,
): VisitPhoto {
    // Non-Android: no persistent storage needed, return as-is
    return photo
}