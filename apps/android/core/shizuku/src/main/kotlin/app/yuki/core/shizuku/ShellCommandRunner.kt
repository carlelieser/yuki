package app.yuki.core.shizuku

import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal data class ShellResult(val exitCode: Int, val output: String) {
    val isSuccess: Boolean get() = exitCode == 0
}

internal interface ShellCommandRunner {
    fun run(command: List<String>, input: ((OutputStream) -> Unit)? = null): ShellResult
}

internal class ProcessShellCommandRunner : ShellCommandRunner {
    override fun run(command: List<String>, input: ((OutputStream) -> Unit)?): ShellResult = try {
        execute(command, input)
    } catch (error: IOException) {
        throw PrivilegedInstallException(failureMessage(command), error)
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        throw PrivilegedInstallException(failureMessage(command), error)
    }

    private fun execute(command: List<String>, input: ((OutputStream) -> Unit)?): ShellResult {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = drainOutput(process)

        input?.let { write -> process.outputStream.use(write) } ?: process.outputStream.close()

        val exitCode = process.waitFor()

        return ShellResult(exitCode, output.get())
    }

    private fun drainOutput(process: Process): Future<String> {
        val reader = Executors.newSingleThreadExecutor()
        val drained = reader.submit<String> {
            process.inputStream.use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
        reader.shutdown()

        return drained
    }
}

private fun failureMessage(command: List<String>): String =
    "Failed to run privileged command '${command.joinToString(" ")}'"
