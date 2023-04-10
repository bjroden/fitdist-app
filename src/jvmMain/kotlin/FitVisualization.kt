
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import estimations.DistributionType

@Composable
@Preview
fun FitVisualization(
    theoreticalProbabilities: Map<DistributionType, DoubleArray>,
    theoreticalData: Map<DistributionType, DoubleArray>,
    observedProbabilities: Map<DistributionType, DoubleArray>,
    observedData: DoubleArray
) {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("P-P Plot", "Q-Q Plot", "Histogram")
    Box {
        Column {
            TabRow(selectedTabIndex = state) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            when (state) {
                0 -> showIf(theoreticalProbabilities.isNotEmpty() && observedProbabilities.isNotEmpty()) {
                    PPPlot(theoreticalProbabilities, observedProbabilities)
                }
                1 -> showIf (theoreticalData.isNotEmpty() && observedData.isNotEmpty()) {
                    QQPlot(theoreticalData, observedData)
                }
                2 -> showIf(theoreticalData.isNotEmpty() && observedData.isNotEmpty()) {
                    Histogram(theoreticalData, observedData)
                }
            }
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "Text tab ${state + 1} selected",
            )
        }
    }
}

@Composable
fun showIf(cond: Boolean, plot: @Composable () -> Unit) {
    if (cond) {
        plot()
    } else {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = "No results to display",
                modifier = Modifier.align(Alignment.Center),
                fontStyle = FontStyle.Italic
            )
        }
    }
}