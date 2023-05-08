
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
    allPlot: ViewModel.PlotResult,
    qqPlot: ViewModel.PlotResult,
    ppPlot: ViewModel.PlotResult,
    histogramPlot: ViewModel.PlotResult,
    cdfPlot: ViewModel.PlotResult
) {
    var tabState by remember { mutableStateOf(0) }
    val titles = listOf("All", "P-P Plot", "Q-Q Plot", "Histogram", "CDF")
    Box {
        Column {
            TabRow(selectedTabIndex = tabState) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = tabState == index,
                        onClick = { tabState = index },
                        text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            val plotRendered = when (tabState) {
                0 -> allPlot
                1 -> ppPlot
                2 -> qqPlot
                3 -> histogramPlot
                4 -> cdfPlot
                else -> ViewModel.PlotError("illegal tab")
            }
            if (plotRendered is ViewModel.PlotSuccess) {
                plotPanel(plotRendered.value)
            } else {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = "No results to display",
                        modifier = Modifier.align(Alignment.Center),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "Text tab ${tabState + 1} selected",
            )
        }
    }
}