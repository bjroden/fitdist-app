import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun DistRanking() {
    Column(Modifier.fillMaxSize().padding(10.dp), Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TestWeight("Chi-Squared Test")
            TestWeight("Kolmogorov-Smirnov Test")
        }
    }
}

@Composable
@Preview
fun TestWeight(testName: String) {
    Row {
        Text(
            text = testName,
            Modifier.border(width = 1.dp, color = Color.Black).padding(5.dp)
        )
        Text(
            text = "RESULT HERE",
            Modifier.border(width = 1.dp, color = Color.Black).padding(5.dp)
        )
    }
}