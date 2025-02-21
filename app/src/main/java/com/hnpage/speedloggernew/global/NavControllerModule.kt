package com.hnpage.speedloggernew.global

import android.content.Context
import androidx.navigation.NavController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class) // Hoặc ActivityComponent
object NavControllerModule {

    @Provides
    fun provideNavController(@ApplicationContext context: Context): NavController {
        // Bạn có thể dùng context nếu cần, hoặc đơn giản trả NavController khác.
        throw IllegalStateException("NavController must be initialized in runtime.")
    }
}
