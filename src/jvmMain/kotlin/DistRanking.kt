
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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