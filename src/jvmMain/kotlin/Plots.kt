
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import estimations.DistributionType
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
    theoreticalData: Map<DistributionType, DoubleArray>,
    observedData: DoubleArray
) {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(qqPlotJPanel(theoreticalData, observedData))
            }
        }
    )
}

@Composable
@Preview
fun PPPlot(
    theoreticalProbabilities: Map<DistributionType, DoubleArray>,
    observedProbabilities: Map<DistributionType, DoubleArray>
) {
    SwingPanel(
        background = Color.White,
        modifier = Modifier.fillMaxSize(1f),
        factory = {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(ppPlotJPanel(theoreticalProbabilities, observedProbabilities))
            }
        }
    )
}

fun ppPlotJPanel(
    theoreticalProbabilities: Map<DistributionType, DoubleArray>,
    observedProbabilities: Map<DistributionType, DoubleArray>
): JPanel {
    // TODO: Verify if we should be using geomQQ2 functions for P-P plot
    //  Particularly, the line with this doesn't go straight from 0 to 1, which is the case
    //  for all other P-P plots we've seen
    val data = mapOf<String, Any>(
        "cond" to theoreticalProbabilities.flatMap { (distType, values) ->
            List(values.size) { distType.name }
        },
        "Theoretical" to theoreticalProbabilities.flatMap { (_, values) ->
            values.toList()
        },
        "Empirical" to observedProbabilities.flatMap { (_, values) ->
            values.toList()
        }
    )
    val plot = letsPlot(data) { x = "Theoretical"; y = "Empirical"; color = "cond" } + geomQQ2(size = 4, alpha = .7) +
            geomQQ2Line(size = 1, color="#000000")

    return plotPanel(plot.toSpec())
}

fun qqPlotJPanel(
    theoreticalData: Map<DistributionType, DoubleArray>,
    observedData: DoubleArray
): JPanel{
    val data = mapOf<String, Any>(
        "cond" to theoreticalData.flatMap { (distType, values) ->
            List(values.size) { distType.name }
        },
        "Theoretical" to theoreticalData.flatMap { (_, values) ->
            values.toList()
        },
        "Empirical" to List(theoreticalData.size) { observedData.toList() }.flatten()
    )

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