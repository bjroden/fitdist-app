
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import goodnessoffit.AbstractGofTest
import goodnessoffit.ChiSquareGofTest
import goodnessoffit.KolmogorovSmirnovGofTest

// TODO: Figure out weird behavior with K-S Test on expansion
@Composable
@Preview
fun DistRanking(results: List<DistResult>) {
    Column(Modifier
        .padding(10.dp),
        Arrangement.spacedBy(5.dp)
    ) {
        LazyColumn (
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item (){
                TestWeight("Chi-Squared Test") { binWidth() }
            }
            item (){
                TestWeight("K-S Test")
            }
            items(results) { eval ->
                EvalDisplay(eval)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun binWidth() {
    Box{
        var text by rememberSaveable { mutableStateOf("") }

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Bin Width", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("0") },
            singleLine = true
        )
    }
}

// TODO: add lazy vertical grid to test weight to let textbox stack below checkbox on squeeze
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TestWeight(testName: String, content: @Composable() () -> Unit = {}) {
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
    Card {
        Row(
            Modifier
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {

            Box(
                Modifier
                    .toggleable(
                        value = checkedState,
                        onValueChange = { onStateChange(!checkedState) },
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = null // null recommended for accessibility with screen readers
                )
            }

            Divider(
                color = Color.Black,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            Box {
                Text(
                    text = testName
                )
            }

            Divider(
                color = Color.Black,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            Box {
                var text by rememberSaveable { mutableStateOf("") }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Weight", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("0") },
                    singleLine = true
                )
            }
            content()
        }
    }
}

@Composable
@Preview
fun EvalDisplay(result: DistResult) {
    Card {
        Row(
            Modifier
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            Text(
                result.distType.distName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(100.dp)
            )
            Text(
                result.distGoodness,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(50.dp)
            )
            result.dist.onSuccess {
                Column(
                    Modifier
                        .width(175.dp)
                ) {
                    result.tests.forEach { testResult ->
                        testResult.onSuccess { test ->
                            Row {
                                Text(
                                    "${getTestName(test)}: ${formatDecimal(test.testScore)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }.onFailure { error ->
                            Row {
                                Text(
                                    "Test Failed: ${error.message}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Row {
                        Text(
                            "Score: ${formatDecimal(result.score)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }.onFailure {
                Column(
                    Modifier
                        .width(175.dp)
                ) {
                    Text("")
                    Text(
                        "Distribution is Invalid: ${it.message}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("")
                }
            }
        }
    }
}

fun getTestName(test: AbstractGofTest) = when (test) {
    is ChiSquareGofTest -> "Chi-square"
    is KolmogorovSmirnovGofTest -> "K-S"
    else -> "Unknown"
}

fun formatDecimal(x: Double) = "%,.8f".format(x)