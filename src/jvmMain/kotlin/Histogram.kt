import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.batik.DefaultPlotPanelBatik
import org.jetbrains.letsPlot.geom.geomDensity
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import javax.swing.BoxLayout
import javax.swing.JPanel

@Preview
@Composable
fun Histogram(
    histogramTheoretical: Map<String, Any?>,
    histogramEmpirical: Map<String, Any?>
) {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(histPlot(histogramTheoretical, histogramEmpirical))
            }
        }
    )
}

fun histPlot(
    histogramTheoretical: Map<String, Any?>,
    histogramEmpirical: Map<String, Any?>
): JPanel {
    // TODO: See if there is a better way of doing this
    val dummy = mapOf<String, Any?>(
        "cond" to emptyList<String>(),
        "data" to emptyList<Number>()
    )

    val plot =
        letsPlot(dummy) { x = "data"; color = "cond" } +
                ggsize(500, 250) +
                geomHistogram(data = histogramEmpirical, binWidth=0.5, color="black", fill="white") { y = "..density.." } +
                geomDensity(data = histogramTheoretical, alpha=0.2)
    val rawSpec = plot.toSpec()
    val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

    return DefaultPlotPanelBatik(
        processedSpec = processedSpec,
        preserveAspectRatio = true,
        preferredSizeFromPlot = false,
        repaintDelay = 10,
    ) { messages ->
        for (message in messages) {
            println("[Example App] $message")
        }
    }
}