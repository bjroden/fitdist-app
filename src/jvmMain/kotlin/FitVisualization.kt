
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
@Preview
fun FitVisualization() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3 with lots of text")
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
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "Text tab ${state + 1} selected",
            )
        }
    }
}