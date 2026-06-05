package uz.beko404.track14.domain.repository

import uz.beko404.track14.domain.model.Track14Snapshot
import uz.beko404.track14.domain.model.TrackApp
import uz.beko404.track14.domain.model.AddOwnerAppRequest
import uz.beko404.track14.domain.model.AddOwnerAppResult

interface Track14Repository {
    val snapshot: Track14Snapshot

    fun getAppById(appId: String): TrackApp?

    fun addOwnerApp(request: AddOwnerAppRequest): AddOwnerAppResult
}
