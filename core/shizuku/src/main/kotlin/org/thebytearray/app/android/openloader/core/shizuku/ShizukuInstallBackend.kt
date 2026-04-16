package org.thebytearray.app.android.openloader.core.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.thebytearray.app.android.openloader.core.domain.install.InstallBackend
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuInstallBackend @Inject constructor() : InstallBackend {

    override suspend fun installStagedFile(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext Result.failure(IllegalStateException("Shizuku is not running."))
            }
            if (Shizuku.getVersion() < 11) {
                return@withContext Result.failure(IllegalStateException("Shizuku v11 or newer is required."))
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(
                    SecurityException("Grant Shizuku permission to OpenLoader in Shizuku."),
                )
            }

            suspendCancellableCoroutine { cont ->
                var process: Process? = null
                cont.invokeOnCancellation {
                    runCatching {
                        process?.destroy()
                        process?.destroyForcibly()
                    }
                }
                try {
                    val size = apkFile.length()
                    val cmd = arrayOf("sh", "-c", "cmd package install --user current -S $size")
                    val proc = newShizukuProcess(cmd).also { process = it }

                    FileInputStream(apkFile).use { input ->
                        proc.outputStream.use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (!cont.isActive) throw CancellationException()
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }

                    if (!cont.isActive) throw CancellationException()

                    val stdout = readStreamFullyWithCancel(proc.inputStream) { cont.isActive }
                    if (!cont.isActive) throw CancellationException()
                    val stderr = readStreamFullyWithCancel(proc.errorStream) { cont.isActive }
                    if (!cont.isActive) throw CancellationException()
                    if (!proc.waitFor(2, TimeUnit.MINUTES)) {
                        runCatching { proc.destroyForcibly() }
                        throw CancellationException()
                    }
                    val combined = stdout + stderr
                    if (!cont.isActive) throw CancellationException()
                    val result = if (combined.contains("Success", ignoreCase = true)) {
                        Result.success("Installation successful")
                    } else {
                        Result.failure(Exception(combined.ifBlank { "Install failed" }))
                    }
                    cont.resumeWith(kotlin.Result.success(result))
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        cont.cancel(e)
                    } else {
                        cont.resumeWith(kotlin.Result.success(Result.failure(e)))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private fun readStreamFullyWithCancel(input: InputStream, isActive: () -> Boolean): String {
        val sb = StringBuilder()
        val buf = ByteArray(8192)
        while (true) {
            if (!isActive()) throw CancellationException()
            val n = input.read(buf)
            if (n == -1) break
            if (n > 0) sb.append(String(buf, 0, n, StandardCharsets.UTF_8))
        }
        return sb.toString()
    }

    private fun newShizukuProcess(cmd: Array<String>): Process {
        val clazz = Shizuku::class.java
        try {
            val m = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            m.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return m.invoke(null, cmd, null, null) as Process
        } catch (_: NoSuchMethodException) {
            val m = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                java.io.File::class.java,
            )
            m.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return m.invoke(null, cmd, null, null) as Process
        }
    }
}
