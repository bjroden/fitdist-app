
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    onRun: () -> Unit = {}
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
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            Column(Modifier
                .padding(0.dp),
                Arrangement.spacedBy(5.dp)) {

                Text(
                    text = "Discrete",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )

                discreteSelection
                    .toSortedMap(compareBy { it.distName })
                    .forEach { (dist, value) ->
                        DistEntry(dist, value, onSelect = onSelect)
                    }

                Text(
                    text = "Continuous",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )

                continuousSelection
                    .toSortedMap(compareBy { it.distName })
                    .forEach { (dist, value) ->
                        DistEntry(dist, value, onSelect = onSelect)
                    }

                TestWeight("Chi-Squared Test") { binWidth() }

                TestWeight("K-S Test")

                Button(onClick = onRun) {
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
fun TestWeight(testName: String, content: @Composable() () -> Unit = {}) {
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
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
            }
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