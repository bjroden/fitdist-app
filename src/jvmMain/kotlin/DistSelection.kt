import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun DistEntry(distName: String){
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toggleable(
                value = checkedState,
                onValueChange = { onStateChange(!checkedState) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = null // null recommended for accessibility with screen readers
        )
        Text(
            text = distName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
@Composable
@Preview
fun DistSelection() {
    Column(Modifier.fillMaxSize().padding(10.dp), Arrangement.spacedBy(5.dp)) {
        Text(
            text = "Discrete",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5
        )
        DistEntry("Normal")
        DistEntry("Weibull")
        DistEntry("Exponential")
        DistEntry("Gamma")
        Text(
            text = "Continuous",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5
        )
        DistEntry("Binomial")
        DistEntry("Bernoulli")
        DistEntry("Poisson")
        DistEntry("Negative Binomial")
        OutlinedButton(onClick = {/* Functionality Here */ }) {
            Text(
                text = "Run",
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}