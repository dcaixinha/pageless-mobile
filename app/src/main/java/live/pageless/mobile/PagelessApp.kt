package live.pageless.mobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import live.pageless.mobile.data.sync.SyncScheduler
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class PagelessApp :
    Application(),
    Configuration.Provider,
    ImageLoaderFactory {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // The authenticated OkHttp client so Coil's cover requests carry the bearer
    // token and hit the correct (runtime-configured) server host.
    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodic()
    }

    // On-demand WorkManager init (see the removed default initializer in the
    // manifest) so Hilt can inject dependencies into workers.
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .okHttpClient(okHttpClient)
            .build()
}
