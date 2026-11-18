package uz.beko404.track14.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Firebase entry points. Nothing else in the app calls `getInstance()`. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    /** Must match REGION in firebase/functions/src/options.ts. */
    private const val FUNCTIONS_REGION = "europe-central2"

    @Provides @Singleton
    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun functions(): FirebaseFunctions = FirebaseFunctions.getInstance(FUNCTIONS_REGION)
}
