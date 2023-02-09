import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor


@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
@Composable
@Preview
fun DistEntry(distName: String){

    val (checkedState, onStateChange) = remember { mutableStateOf(true) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toggleable(
                value = checkedState,
                onValueChange = { onStateChange(!checkedState) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = distName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
@Composable
@Preview
fun DistSelection() {
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        Text(
            text = "Discrete",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5
        )
        DistEntry("Normal")
        DistEntry("Weibull")
        DistEntry("Exponential")
        DistEntry("Gamma")
        Text(
            text = "Continuous",
            textAlign = TextAlign.Center
        )
        DistEntry("Normal")
        DistEntry("Weibull")
        DistEntry("Exponential")
        DistEntry("Gamma")
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        val splitterState = rememberSplitPaneState()
        HorizontalSplitPane(
            splitPaneState = splitterState
        ) {
            first(205.dp) {
                Row(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                    DistSelection()
                }
            }
            second(100.dp) {
                Box(Modifier.background(Color.Red).fillMaxSize())
            }
            splitter {
                visiblePart {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colors.background)
                    )
                }
                handle {
                    Box(
                        Modifier
                            .markAsHandle()
                            .cursorForHorizontalResize()
                            .background(SolidColor(Color.Gray), alpha = 0.50f)
                            .width(9.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
