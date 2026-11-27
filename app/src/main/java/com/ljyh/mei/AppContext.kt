package com.ljyh.mei

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import androidx.datastore.preferences.core.emptyPreferences
import com.ljyh.mei.constants.DefaultImageCacheLimitMb
import com.ljyh.mei.mcp.McpService
import com.ljyh.mei.constants.ImageCacheLimitMbKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.log.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import com.ljyh.mei.source.script.ScriptPluginManager
import timber.log.Timber

@HiltAndroidApp
class AppContext : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var sourcePlugins: ScriptPluginManager
    @Inject lateinit var sourceArtwork: com.ljyh.mei.source.script.SourceArtworkStore
    @Inject lateinit var sourceUpdates: com.ljyh.mei.source.update.SourceUpdateManager
    @Inject internal lateinit var mcpService: McpService
    private val sourceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var imageCacheLimitMb = DefaultImageCacheLimitMb
    private val imageCacheSettings by lazy {
        sourceScope.async(start = CoroutineStart.LAZY) {
            val preferences = dataStore.data.catch { error ->
                if (error !is IOException) throw error
                emit(emptyPreferences())
            }.first()
            imageCacheLimitMb = preferences[ImageCacheLimitMbKey] ?: DefaultImageCacheLimitMb
        }
    }
    override fun onCreate() {
        // The isolated JavaScript worker must not initialize Hilt, accounts or app storage.
        // Hilt injects from its Application.onCreate(), so this guard precedes super.
        if (android.os.Process.isIsolated()) return
        instance = this
        super.onCreate()
        // Timber owns its trees for the lifetime of this process, across Activity recreation.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        Timber.plant(FileLoggingTree(applicationContext))
        imageCacheSettings.start()
        com.ljyh.mei.source.script.SourceArtworkRegistry.attach(sourceArtwork)
        sourceScope.launch { sourcePlugins.loadInstalled() }
        sourceUpdates.checkOnStartup()
        mcpService.start()
    }

    companion object {
        @JvmStatic
        lateinit var instance: AppContext

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val headerInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
                .build()
            chain.proceed(newRequest)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.1)
                    .build()
            }
            .diskCache {
                newDiskCache()
            }
            .components {
                add(coil3.intercept.Interceptor { chain ->
                    imageCacheSettings.await()
                    chain.proceed()
                })
                add(com.ljyh.mei.source.script.SourceArtworkRegistry.interceptor)
                add(OkHttpNetworkFetcherFactory(okHttpClient))
                add(AnimatedImageDecoder.Factory())
            }
            .build()
    }

    internal suspend fun hasCachedImage(url: String): Boolean = withContext(Dispatchers.IO) {
        imageCacheSettings.await()
        try {
            SingletonImageLoader.get(this@AppContext).diskCache
                ?.openSnapshot(url)?.use { true } ?: false
        } catch (_: IOException) {
            false
        }
    }

    internal suspend fun clearImageCache() {
        imageCacheSettings.await()
        SingletonImageLoader.get(this).let { loader ->
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }

    private fun newDiskCache(): DiskCache = DiskCache.Builder()
        .directory(cacheDir.resolve("image_cache"))
        .maxSizeBytes(imageCacheLimitMb.coerceIn(50, 2048) * 1024L * 1024L)
        .build()
}

