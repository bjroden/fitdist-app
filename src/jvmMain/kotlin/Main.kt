
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.UndecoratedWindowAlertDialogProvider.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ksl.utilities.io.KSLFileUtil
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import ui.theme.AppTheme
import java.awt.Cursor
import java.awt.FileDialog
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText


@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
@Preview
fun App(viewModel: ViewModel) {
    AppTheme() {
        val splitterState = rememberSplitPaneState(initialPositionPercentage = 0.6f, moveEnabled = true)

        val continuousDists = viewModel.continuousDistSelection
        val discreteDists = viewModel.discreteSelection
        val testResults = viewModel.testResults
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ){
            Row(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                Box {
                    DistSelection(
                        continuousSelection = continuousDists,
                        discreteSelection = discreteDists,
                        onSelect = { dist, newSelectedValue ->
                            viewModel.distributionSelected(dist, newSelectedValue)
                        },
                        onRun = { viewModel.runResults() }
                    )
                }

                HorizontalSplitPane(
                    splitPaneState = splitterState
                ) {
                    first(205.dp) {
                        DistRanking(testResults)
                    }
                    second(100.dp) {
                        FitVisualization(
                            viewModel.qqData,
                            viewModel.ppData,
                            viewModel.histogramTheoretical,
                            viewModel.histogramEmpirical
                        )
                    }
                    splitter {
                        visiblePart {
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.background)
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
}

@OptIn(ExperimentalMaterialApi::class)
fun main() = application {
    val windowTitle = "FitDistApp"
    Window(
        title = windowTitle,
        onCloseRequest = ::exitApplication
    ) {
        val coroutineScope = rememberCoroutineScope()
        var viewModel by remember { mutableStateOf(ViewModel(doubleArrayOf(), coroutineScope)) }
        val openDialog = remember { mutableStateOf(false) }

        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("Input Data", onClick = {
                    val path = FileDialog(window, "Open Input Data", FileDialog.LOAD)
                        .getPath()
                        ?: return@Item
                    val data = KSLFileUtil.scanToArray(path)
                    viewModel = ViewModel(data, coroutineScope)
                })
                Item("Save", onClick = {
                    val path = FileDialog(window, "Select a file to save session to", FileDialog.SAVE)
                        .getPath()
                        ?: return@Item
                    val json = Json.encodeToString(viewModel.toSession())
                    path.writeText(json)
                })
                Item("Load", onClick = {
                    val string = FileDialog(window, "Load Saved Session", FileDialog.LOAD)
                        .getPath()
                        ?.readText()
                        ?: return@Item
                    val json = runCatching {
                        Json.decodeFromString<ViewModelSavedSession>(string)
                    }.getOrElse {
                        // TODO: Trigger an error message / dialog on invalid json.
                        //  The Compose AlertDialog is a composable function, so Main will need to be adjusted
                        //  to manage dialog state if we use that
                        openDialog.value = true
                        return@Item
                    }
                    viewModel = ViewModel(json, coroutineScope)
                })
            }
        }

        // Alert Dialog Trigger for invalid JSON
        if(openDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    openDialog.value = false
                }
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "The JSON file that you imported is invalid.",
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(
                            onClick = {
                                openDialog.value = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }

        App(viewModel)
    }
}

fun FileDialog.getPath(): Path? {
    isVisible = true
    val dir = directory ?: return null
    val file = file ?: return null
    return File(dir, file).toPath()
}