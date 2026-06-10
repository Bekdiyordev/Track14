package uz.beko404.track14.data

import uz.beko404.track14.domain.repository.Track14Repository

object Track14RepositoryProvider {
    val current: Track14Repository = FirebaseTrack14Repository
}
