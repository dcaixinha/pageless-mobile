package live.pageless.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import live.pageless.mobile.data.repository.PlaybackTeardown
import live.pageless.mobile.playback.PlayerConnection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    /**
     * Lets the data layer end playback through an abstraction it owns, rather
     * than depending on the playback package directly — `PlayerConnection`
     * already depends on the repositories.
     */
    @Binds
    @Singleton
    abstract fun bindPlaybackTeardown(connection: PlayerConnection): PlaybackTeardown
}
