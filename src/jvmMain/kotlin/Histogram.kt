import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
fun Histogram() {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(histPlot())
            }
        }
    )
}

fun histPlot(): JPanel {
    val rand = java.util.Random()
    val n = 200
    val data = mapOf<String, Any>(
        "cond" to List(n) { "A" } + List(n) { "B" },
        "rating" to List(n) { rand.nextGaussian() } + List(n) { rand.nextGaussian() * 1.5 + 1.5 },
    )

    val plot = letsPlot(data) { x = "rating" } + ggsize(500, 250) + geomHistogram(binWidth=0.5, color="black", fill="white") { y = "..density.." } + geomDensity(alpha=0.2, fill=0xFF6666)
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