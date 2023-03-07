import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ksl.utilities.io.KSLFileUtil
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor
import java.awt.FileDialog
import java.io.File


@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
@Preview
fun App(window: ComposeWindow) {
    MaterialTheme {
        val splitterState = rememberSplitPaneState()
        var data by remember { mutableStateOf(doubleArrayOf()) }

        Row(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Box {
                DistSelection()
                Button(onClick = {
                    data = FileDialog(window).let { dialog ->
                        dialog.isVisible = true
                        if (dialog.directory != null && dialog.file != null) {
                            val path = File(dialog.directory, dialog.file).toPath()
                            KSLFileUtil.scanToArray(path)
                        } else {
                            doubleArrayOf()
                        }
                    }
                    println()
                }) {
                    Text(
                        text = "Get data",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
                Text(
                    text = data.contentToString()
                )
            }

            HorizontalSplitPane(
                splitPaneState = splitterState
            ) {
                first(205.dp) {
                    DistRanking()
                }
                second(100.dp) {
                    FitVisualization()
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
}

fun main() = application {
    val windowTitle = "FitDistApp"
    Window(
        title = windowTitle,
        onCloseRequest = ::exitApplication
    ) {
        App(this.window)
    }
}
