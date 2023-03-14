
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

// TODO: Figure out weird behavior with K-S Test on expansion
@Composable
@Preview
fun DistRanking() {
    Column(Modifier
        .padding(10.dp),
        Arrangement.spacedBy(5.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(400.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item (span = { GridItemSpan(1) }){
                TestWeight("Chi-Squared Test")
            }
            item (span = { GridItemSpan(1) }){
                TestWeight("K-S Test")
            }
        }
        RankList()
    }
}

// TODO: add lazy vertical grid to test weight to let textbox stack below checkbox on squeeze
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TestWeight(testName: String) {
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
    Card (

    ) {
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
        }
    }
}

@Composable
@Preview
fun RankList() {
    val evals = mutableStateListOf<DistEval>()
    // Example Evals
    evals.add(DistEval("Normal", "Good", 0.87436f, 0.7322f, 0.8f))
    evals.add(DistEval("Weibull", "Bad", 0.19f, 0.21f, 0.2f))
    evals.add(DistEval("Exponential", "Invalid"))
    evals.add(DistEval("Gamma", "Good", 0.87436f, 0.7322f, 0.8f))
    evals.add(DistEval("Binomial", "Bad", 0.19f, 0.21f, 0.2f))
    evals.add(DistEval("Bernoulli", "Invalid"))
    evals.add(DistEval("Poisson", "Good", 0.87436f, 0.7322f, 0.8f))
    evals.add(DistEval("Negative Binomial", "Bad", 0.19f, 0.21f, 0.2f))

    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(evals) { eval ->
            EvalDisplay(eval)
        }
    }
}

@Composable
@Preview
fun EvalDisplay(eval: DistEval) {
    Card {
        Row(
            Modifier
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            Text(
                eval.distName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(100.dp)
            )
            Text(
                eval.distGoodness,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(50.dp)
            )
            if (eval.distGoodness != "Invalid") {
                Column(
                    Modifier
                        .width(175.dp)
                ) {
                    Row {
                        Text(
                            "Chi-Squared: " + eval.chiSquaredScore.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        Text(
                            "KS: " + eval.ksScore.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        Text(
                            "Score: " + eval.totalScore.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Column(
                    Modifier
                        .width(175.dp)
                ) {
                    Text("")
                    Text(
                        "Distribution is Invalid",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("")
                }
            }
        }
    }
}