package uz.beko404.track14.domain.repository

import uz.beko404.track14.domain.model.Track14Snapshot
import uz.beko404.track14.domain.model.TrackApp
import uz.beko404.track14.domain.model.AddOwnerAppRequest
import uz.beko404.track14.domain.model.AddOwnerAppResult
import uz.beko404.track14.domain.model.DailyTestResult
import uz.beko404.track14.domain.model.JoinAppResult

interface Track14Repository {
    val snapshot: Track14Snapshot

    fun getAppById(appId: String): TrackApp?

    fun addOwnerApp(request: AddOwnerAppRequest): AddOwnerAppResult

    fun markGoogleGroupOpened(appId: String): JoinAppResult

    fun markPlayOptInOpened(appId: String): JoinAppResult

    fun joinApp(appId: String): JoinAppResult

    fun startDailyTest(membershipId: String): DailyTestResult

    fun completeEligibleDailyTests()
}
