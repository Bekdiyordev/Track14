package uz.beko404.track14.admin.core

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    /** Must match REGION in firebase/functions/src/options.ts. */
    private const val FUNCTIONS_REGION = "europe-central2"

    @Provides @Singleton fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    @Provides @Singleton fun functions(): FirebaseFunctions = FirebaseFunctions.getInstance(FUNCTIONS_REGION)
}
