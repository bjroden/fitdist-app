
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

@Composable
@Preview
fun FitVisualization(
    qqData: Map<String, Any?>,
    ppData: Map<String, Any?>,
    histogramTheoretical: Map<String, Any?>,
    histogramEmpirical: Map<String, Any?>
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
                0 -> showIf(qqData.isNotEmpty()) {
                    PPPlot(ppData)
                }
                1 -> showIf (qqData.isNotEmpty()) {
                    QQPlot(qqData)
                }
                2 -> showIf(histogramTheoretical.isNotEmpty() && histogramEmpirical.isNotEmpty()) {
                    Histogram(histogramTheoretical, histogramEmpirical)
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