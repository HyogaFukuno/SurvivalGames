package com.glacier.survivalgames.infrastructure

import com.glacier.survivalgames.domain.service.GameMapService
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PostInitialize
import io.fairyproject.container.PreDestroy
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipInputStream

@InjectableComponent
class WorldDownloader(val mapService: GameMapService) {

    private val queue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    private val worlds = ConcurrentLinkedQueue<String>()

    companion object {
        private const val DOWNLOAD_URL = "https://storage.googleapis.com/orca-buckets/worlds/"
    }

    @PostInitialize
    fun onPostInitialize() {
        CompletableFuture.runAsync {
            mapService.maps.parallelStream().forEach {
                downloadAndUnzipAsync("$DOWNLOAD_URL${it.worldName}.zip", it.worldName)
            }
        }

        MCSchedulers.getGlobalScheduler().schedule({
            createWorld(queue.poll())
        }, 20L * 5)
    }

    @PreDestroy
    fun onPreDestroy() {
        worlds.forEach {
            Bukkit.unloadWorld(it, false)
            val worldDir = File(it)
            if (worldDir.exists()) {
                worldDir.deleteRecursively()
            }
        }
    }

    private fun downloadAndUnzipAsync(url: String, worldName: String) {
        val zipFile = File("$worldName.zip")
        val worldDir = File(worldName)

        try {
            Log.info("Downloading world from $url...")
            downloadFile(url, zipFile)
            Log.info("World downloaded successfully.")

            Log.info("Unzipping world...")
            unzip(zipFile, worldDir)
            Log.info("World unzipped successfully.")
            File("$worldName/uid.dat").delete()
            queue.add(worldName)
        } catch (e: Exception) {
            Log.error("Failed to download or unzip world.", e)
        } finally {
            zipFile.delete()
        }

    }

    private fun downloadFile(url: String, file: File) {
        URI(url).toURL().openStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun unzip(zipFile: File, destDirectory: File) {
        if (destDirectory.exists()) {
            destDirectory.deleteRecursively()
        }
        destDirectory.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            val rootDir = entry?.name?.substringBefore('/') ?: ""

            while (entry != null) {
                val path = entry.name.removePrefix("$rootDir/")
                if (path.isNotBlank()) {
                    val newFile = File(destDirectory, path)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        newFile.outputStream().use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun createWorld(name: String) {
        MCSchedulers.getGlobalScheduler().schedule {
            WorldCreator(name).createWorld().apply {
                isAutoSave = false
                keepSpawnInMemory = false
                time = 1000L
                isThundering = false
                worlds.add(this.name)
            }

            if (queue.isNotEmpty()) {
                val name: String = queue.poll()
                createWorld(name)
            }
        }
    }
}