
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import goodnessoffit.AbstractGofTest
import goodnessoffit.ChiSquareGofTest
import goodnessoffit.KolmogorovSmirnovGofTest
import goodnessoffit.PValueIfc

// TODO: Figure out weird behavior with K-S Test on expansion
@Composable
@Preview
fun DistRanking(results: List<DistResult>) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize()){
            Text(
                text = "No results to display",
                modifier = Modifier.align(Alignment.Center),
                fontStyle = FontStyle.Italic
            )
        }
    }
    else {
        LazyColumn (
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results) { eval ->
                EvalDisplay(eval)
            }
        }
    }
}

@Composable
@Preview
fun EvalDisplay(result: DistResult) {
    Card {
        Row(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                result.distType.distName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(200.dp),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Score: %.6f".format(result.score),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(150.dp)
            )
            result.dist.onSuccess {
                Column(
                    Modifier
                        .width(400.dp)
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
                            if (test is PValueIfc){
                                Row {
                                    Text(
                                        "P-Value: ${formatDecimal(test.pValue)}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }.onFailure { error ->
                            Row {
                                Text(
                                    "Test Failed: ${error.message}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }.onFailure {
                Column(
                    Modifier
                        .width(400.dp)
                ) {
                    Text(
                        "Distribution is Invalid: ${it.message}",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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