package uz.beko404.track14.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.Session

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val user = Session.SignedIn("u1", "Aziz", "aziz@example.com", null)
    private lateinit var auth: FakeAuth
    private lateinit var profiles: FakeProfiles
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        auth = FakeAuth()
        profiles = FakeProfiles()
        viewModel = AuthViewModel(auth, profiles)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** [AuthViewModel.state] is a WhileSubscribed flow: collect once to start it. */
    private suspend fun current() = viewModel.state.first { it.session != null || it.error != null || it.isSigningIn }

    @Test
    fun `successful sign in creates the profile`() = runTest {
        viewModel.signIn { DomainResult.Success("token") }

        assertEquals("token", auth.lastToken)
        assertEquals(listOf(user), profiles.created)
    }

    @Test
    fun `closing the account picker shows no error`() = runTest {
        viewModel.signIn { DomainResult.Failure(DomainError.Cancelled) }

        assertNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isSigningIn)
        assertEquals(emptyList<Session.SignedIn>(), profiles.created)
    }

    @Test
    fun `missing google account is reported`() = runTest {
        viewModel.signIn { DomainResult.Failure(DomainError.NoGoogleAccount) }
        current()

        assertEquals(DomainError.NoGoogleAccount, viewModel.state.value.error)
    }

    @Test
    fun `firebase failure is reported and profile is not created`() = runTest {
        auth.result = DomainResult.Failure(DomainError.Network)

        viewModel.signIn { DomainResult.Success("token") }
        current()

        assertEquals(DomainError.Network, viewModel.state.value.error)
        assertEquals(emptyList<Session.SignedIn>(), profiles.created)
    }

    private inner class FakeAuth : AuthRepository {
        val sessionFlow = MutableStateFlow<Session>(Session.SignedOut)
        var lastToken: String? = null
        var result: DomainResult<Session.SignedIn> = DomainResult.Success(user)

        override val session: Flow<Session> = sessionFlow

        override suspend fun deleteAccount(): DomainResult<Unit> = DomainResult.Success(Unit)

        override suspend fun signInWithGoogle(idToken: String): DomainResult<Session.SignedIn> {
            lastToken = idToken
            return result
        }

        override suspend fun signOut() {
            sessionFlow.value = Session.SignedOut
        }
    }

    private class FakeProfiles : ProfileRepository {
        val created = mutableListOf<Session.SignedIn>()

        override suspend fun ensureProfile(user: Session.SignedIn): DomainResult<Unit> {
            created += user
            return DomainResult.Success(Unit)
        }

        override suspend fun updateLanguage(uid: String, languageTag: String) = Unit

        override fun observeStats(uid: String): Flow<uz.beko404.track14.domain.model.TesterStats> =
            MutableStateFlow(uz.beko404.track14.domain.model.TesterStats())
    }
}
