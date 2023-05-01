
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import org.jetbrains.letsPlot.export.ggsave
import ui.theme.AppTheme
import java.awt.Cursor
import java.awt.FileDialog
import java.awt.TrayIcon.MessageType
import java.io.File
import java.nio.file.Path
import javax.swing.JOptionPane
import kotlin.io.path.readText
import kotlin.io.path.writeText


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
        val testResults = viewModel.distResults
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
                        onRun = { viewModel.runResults() },
                        runButtonEnabled = viewModel.runButtonEnabled,
                        binWidthData = viewModel.binWidthData,
                        binWidthOnValueChange = { viewModel.onBinWidthTextChange(it) },
                        testWeights = viewModel.testWeights,
                        onTestWeightChange = { type, weight -> viewModel.onGofWeightValueChange(type, weight) },
                        onTestSelected = { type, selected -> viewModel.onWeightSelected(type, selected) }
                    )
                }

                HorizontalSplitPane(
                    splitPaneState = splitterState
                ) {
                    first(205.dp) {
                        // TODO: probably want to pass the nullable runResults in here,
                        //  and have it display bin width data from last run
                        DistRanking(testResults ?: emptyList())
                    }
                    second(100.dp) {
                        FitVisualization(
                            viewModel.qqPlot,
                            viewModel.ppPlot,
                            viewModel.histogramPlot,
                            viewModel.cdfPlot
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

fun main() = application {
    val windowTitle = "FitDistApp"
    Window(
        title = windowTitle,
        onCloseRequest = ::exitApplication
    ) {
        val coroutineScope = rememberCoroutineScope()
        var viewModel by remember { mutableStateOf(ViewModel(doubleArrayOf(), coroutineScope)) }

        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("Input Data", onClick = {
                    val path = FileDialog(window, "Open Input Data", FileDialog.LOAD)
                        .getPath()
                        ?: return@Item
                    val data = KSLFileUtil.scanToArray(path)
                    if (data.isNotEmpty()) {
                        JOptionPane.showMessageDialog(window, "Data imported successfully")
                        viewModel = ViewModel(data, coroutineScope)
                    } else {
                        JOptionPane.showMessageDialog(
                            window,
                            "No data was found in file",
                            "Invalid data",
                            MessageType.ERROR.ordinal
                        )
                    }
                })
                Item("Save", onClick = {
                    val session = viewModel.toSession()
                        ?: run {
                            JOptionPane.showMessageDialog(
                                window,
                                "Cannot save session: no results exist",
                                "Invalid session",
                                MessageType.ERROR.ordinal
                            )
                            return@Item
                        }
                    val path = FileDialog(window, "Select a file to save session to", FileDialog.SAVE)
                        .getPath()
                        ?: return@Item
                    path.writeText(Json.encodeToString(session))
                })
                Item("Load", onClick = {
                    val string = FileDialog(window, "Load Saved Session", FileDialog.LOAD)
                        .getPath()
                        ?.readText()
                        ?: return@Item
                    val json = runCatching {
                        Json.decodeFromString<ViewModelSavedSession>(string)
                    }.getOrElse {
                        JOptionPane.showMessageDialog(
                            window,
                            "The JSON file that you imported is invalid.",
                            "Invalid session",
                            MessageType.ERROR.ordinal
                        )
                        return@Item
                    }
                    viewModel = ViewModel(json, coroutineScope)
                })
                Item("Clear data", onClick = {
                    viewModel = ViewModel(doubleArrayOf(), coroutineScope)
                })
                Item("Save image", onClick = {
                    val plots = viewModel.allPlots
                        ?: run {
                            JOptionPane.showMessageDialog(
                                window,
                                "No plots to save",
                                "Invalid plots",
                                MessageType.ERROR.ordinal
                            )
                            return@Item
                        }
                    val (dir, file) = FileDialog(window, "Select a file to save image to", FileDialog.SAVE)
                        .apply { file = ".png" }
                        .getDirAndFile()
                        ?: return@Item
                    runCatching {
                        ggsave(plots, file, path = dir)
                    }.onFailure {
                        JOptionPane.showMessageDialog(
                            window,
                            "Failure on saving: ${it.message}",
                            "Save failed",
                            MessageType.ERROR.ordinal
                        )
                    }
                })
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

fun FileDialog.getDirAndFile(): Pair<String, String>? {
    isVisible = true
    val dir = directory ?: return null
    val file = file ?: return null
    return Pair(dir, file)
}