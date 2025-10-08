package com.glacier.survivalgames.infrastructure.manager

import com.glacier.survivalgames.application.service.GameMapService
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.response.TaskResponse
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

@InjectableComponent
class WorldManager(val service: GameMapService) {
    companion object {
        private const val DOWNLOAD_URL = "https://storage.googleapis.com/orca-buckets/worlds/"
    }

    private val queue = ArrayBlockingQueue<String>(48)
    private val worlds = ArrayBlockingQueue<String>(48)

    @PreInitialize
    fun onPreInitialize() {
        CompletableFuture.runAsync {
            val futures = service.maps.map { map ->
                CompletableFuture.supplyAsync({ downloadAndUnzip("$DOWNLOAD_URL${map.worldName}.zip", map.worldName) },
                    Executors.newFixedThreadPool(4))
            }

            //CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
            //    createWorlds()
            //}
        }
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

    private fun downloadAndUnzip(url: String, worldName: String) {
        val zipFile = File("$worldName.zip")
        val worldFile = File(worldName)

        try {
            Log.info("Downloading world from $url...")
            downloadFile(url, zipFile)
            Log.info("World downloaded successfully.")

            Log.info("Unzipping world...")
            unzip(zipFile, worldFile)
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

    private fun unzip(zipFile: File, destination: File) {
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            val root = entry?.name?.substringBefore('/') ?: ""

            while (entry != null) {
                val path = entry.name.removePrefix("$root/")
                if (path.isNotBlank()) {
                    val newFile = File(destination, path)
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

    private fun createWorlds() {
        MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
            if (queue.isNotEmpty()) {
                val name = queue.poll()
                WorldCreator(name).createWorld().apply {
                    isAutoSave = false
                    keepSpawnInMemory = false
                    time = 1000L
                    isThundering = false
                    worlds.add(name)
                }
                return@Callable TaskResponse.continueTask()
            }
            return@Callable TaskResponse.success(true)
        }, 10L, 20L)
    }
}