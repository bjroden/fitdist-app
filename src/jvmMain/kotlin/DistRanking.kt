import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// TODO: Figure out weird behavior with K-S Test on expansion
@Composable
@Preview
fun DistRanking() {
    Column(Modifier
        .height(400.dp)
        .padding(10.dp),
        Arrangement.spacedBy(5.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(400.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.Top
            ) {
                item (span = { GridItemSpan(1) }){
                    TestWeight("Chi-Squared Test")
                }
                item (span = { GridItemSpan(1) }){
                    TestWeight("K-S Test")
                }
        }
    }
}

// TODO: add lazy vertical grid to test weight to let textbox stack below checkbox on squeeze
@OptIn(ExperimentalMaterial3Api::class)
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
                label = { Text("Weight") },
                placeholder = { Text("0") }
            )
        }
    }
}