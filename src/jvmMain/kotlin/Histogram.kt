import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import estimations.DistributionType
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
    histogramTheoretical: Map<DistributionType, DoubleArray>,
    histogramEmpirical: DoubleArray
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
    theoretical: Map<DistributionType, DoubleArray>,
    empirical: DoubleArray
): JPanel {
    // TODO: See if there is a better way of doing this
    val dummy = mapOf<String, Any?>(
        "cond" to emptyList<String>(),
        "data" to emptyList<Number>()
    )

    val empiricalMap = mapOf<String, Any>(
        "cond" to List(empirical.size) { "Empirical" },
        "data" to empirical.toList()
    )

    val theoreticalMap = mapOf<String, Any>(
        "cond" to theoretical.flatMap { (distType, values) ->
            List(values.size) { distType.name }
        },
        "data" to theoretical.flatMap { (_, values) ->
            values.toList()
        },
    )

    val plot =
        letsPlot(dummy) { x = "data"; color = "cond" } +
                ggsize(500, 250) +
                geomHistogram(data = empiricalMap, binWidth=0.5, color="black", fill="white") { y = "..density.." } +
                geomDensity(data = theoreticalMap, alpha=0.2)
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