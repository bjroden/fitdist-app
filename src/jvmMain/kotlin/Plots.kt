
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.batik.DefaultPlotPanelBatik
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import javax.swing.BoxLayout
import javax.swing.JPanel


@Composable
@Preview
fun QQPlot(
    data: Map<String, Any?>
) {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(qqPlotJPanel(data))
            }
        }
    )
}

@Composable
@Preview
fun PPPlot(
    data: Map<String, Any?>
) {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(ppPlotJPanel(data))
            }
        }
    )
}

fun ppPlotJPanel(
    data: Map<String, Any?>
): JPanel {
    // TODO: Verify if we should be using geomQQ2 functions for P-P plot
    //  Particularly, the line with this doesn't go straight from 0 to 1, which is the case
    //  for all other P-P plots we've seen
    val plot = letsPlot(data) { x = "Theoretical"; y = "Empirical"; color = "cond" } + geomQQ2(size = 4, alpha = .7) +
            geomQQ2Line(size = 1, color="#000000")

    return plotPanel(plot.toSpec())
}

fun qqPlotJPanel(
    data: Map<String, Any?>
): JPanel{
    val plot = letsPlot(data) { x = "Theoretical"; y = "Empirical"; color = "cond" } + geomQQ2(size = 4, alpha = .7) +
            geomQQ2Line(size = 1, color="#000000")

    return plotPanel(plot.toSpec())
}

fun plotPanel(rawSpec: MutableMap<String, Any>): JPanel {
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