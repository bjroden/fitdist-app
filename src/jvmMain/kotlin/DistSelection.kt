
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import estimations.DistributionType


@Composable
@Preview
fun DistEntry(
    distType: DistributionType,
    selected: Boolean,
    onSelect: (DistributionType, Boolean) -> Unit = { _, _ -> }
) {
    Row(
        Modifier
            .width(170.dp)
            .height(56.dp)
            .toggleable(
                value = selected,
                onValueChange = { onSelect(distType, !selected) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null // null recommended for accessibility with screen readers
        )
        Text(
            text = distType.distName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
@Composable
@Preview
fun DistSelection(
    continuousSelection: Map<DistributionType, Boolean> = emptyMap(),
    discreteSelection: Map<DistributionType, Boolean> = emptyMap(),
    onSelect: (DistributionType, Boolean) -> Unit = { _, _ -> },
    onRun: () -> Unit = {},
    runButtonEnabled: Boolean,
    binWidthData: NumberInputData,
    binWidthOnValueChange: (String) -> Unit,
    testWeights: Map<GofTestType, TestWeightData>,
    onTestWeightChange: (GofTestType, String) -> Unit,
    onTestSelected: (GofTestType, Boolean) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxHeight()
            .width(200.dp)
            .padding(10.dp)
    ) {
        val stateVertical = rememberScrollState(0)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .verticalScroll(stateVertical)
                .padding(end = 12.dp)
        ) {
            Column(Modifier
                .padding(0.dp),
                Arrangement.spacedBy(5.dp)) {

                Card(){

                    Text(
                        modifier = Modifier.fillMaxWidth(1f),
                        text = "Discrete",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    discreteSelection
                        .toSortedMap(compareBy { it.distName })
                        .forEach { (dist, value) ->
                            DistEntry(dist, value, onSelect = onSelect)
                        }

                }

                Card{
                    Text(
                        modifier = Modifier.fillMaxWidth(1f),
                        text = "Continuous",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    continuousSelection
                        .toSortedMap(compareBy { it.distName })
                        .forEach { (dist, value) ->
                            DistEntry(dist, value, onSelect = onSelect)
                        }
                }


                // TODO: redo card to make this pretty
                NumberInput(
                    label = "Bin Width",
                    data = binWidthData,
                    placeHolder = "n > 0",
                    onValueChange = binWidthOnValueChange
                )

                testWeights.forEach { (testType, testWeightData) ->
                    TestWeight(
                        testType = testType,
                        testWeightData = testWeightData,
                        onSelect = onTestSelected,
                        onValueChange = onTestWeightChange
                    )

                }

                Button(onClick = onRun, enabled = runButtonEnabled) {
                    Text(
                        text = "Run",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TestWeight(
    testType: GofTestType,
    testWeightData: TestWeightData,
    onValueChange: (GofTestType, String) -> Unit = { _, _ -> },
    onSelect: (GofTestType, Boolean) -> Unit = { _, _ -> }
) {
    Card {
        Column {
            Row(
                Modifier
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {

                Box(
                    Modifier
                        .toggleable(
                            value = testWeightData.selected,
                            onValueChange = { onSelect(testType, !testWeightData.selected) },
                            role = Role.Checkbox
                        )
                ) {
                    Checkbox(
                        checked = testWeightData.selected,
                        onCheckedChange = null // null recommended for accessibility with screen readers
                    )
                }

                Box {
                    Text(
                        text = testType.testName
                    )
                }
            }
            Box {
                NumberInput(
                    label = testType.testName,
                    data = testWeightData.numberInputData,
                    placeHolder = "0 < x < 1",
                    onValueChange = { onValueChange(testType, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun NumberInput(
    label: String,
    data: NumberInputData,
    placeHolder: String? = null,
    onValueChange: (String) -> Unit = {}
) {
    Box {
        val validInputPattern = Regex("""\d*\.?\d*""")

        TextField(
            value = data.text,
            onValueChange = { if (validInputPattern.matches(it)) { onValueChange(it) } },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { placeHolder?.let { Text(it) } },
            isError = data.isError,
            singleLine = true
        )
    }
}