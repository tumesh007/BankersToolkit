package kpt.feature.visitreport.export

import java.awt.Desktop
import java.io.File

actual object ReportPublisher {

    actual fun publish(fileName: String, mimeType: String, bytes: ByteArray): String? {
        return try {
            val userHome = System.getProperty("user.home")
            val targetDir = File(userHome, "Downloads/BankersToolkit/VisitReports").apply { mkdirs() }
            val outputFile = File(targetDir, fileName)
            outputFile.writeBytes(bytes)

            // Attempt to open the generated report with the default system application
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(outputFile)
                } else {
                    ProcessBuilder("xdg-open", outputFile.absolutePath).start()
                }
            } catch (_: Exception) {
                // Ignore opening error; file is already saved successfully
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
