import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun DistRanking() {
    Column(Modifier
        .height(80.dp)
        .padding(10.dp),
        Arrangement.spacedBy(5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TestWeight("Chi-Squared Test")
            TestWeight("Kolmogorov-Smirnov Test")
        }
    }
}

@Composable
@Preview
fun TestWeight(testName: String) {
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
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
                label = { Text("Email") },
                placeholder = { Text("example@gmail.com") }
            )
        }
    }
}