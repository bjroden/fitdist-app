import androidx.compose.runtime.mutableStateMapOf
import estimations.DistributionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ksl.utilities.distributions.ContinuousDistributionIfc
import ksl.utilities.distributions.DiscreteDistributionIfc
import kotlin.reflect.full.isSubclassOf


class ViewModel(private val coroutineScope: CoroutineScope) {
    private val internalDistSelection = MutableStateFlow(
        mutableStateMapOf(
            *DistributionType.values()
                .map { it to false }
                .toTypedArray()
        )
    )

    val distSelection
        get() = internalDistSelection.mapState { it.toMap() }

    val continuousDistSelection
        get() = internalDistSelection.mapState { dists ->
            dists.filterKeys { it.isContinuous }
        }

    val discreteSelection
        get() = internalDistSelection.mapState { dists ->
            dists.filterKeys { it.isDiscrete }
        }

    val selectedDists
        get() = internalDistSelection.mapState { dists ->
            dists.filterValues { it }.keys
        }

    fun distributionSelected(distType: DistributionType, newSelectedValue: Boolean) {
        internalDistSelection.value.replace(distType, newSelectedValue)
    }

    // Converts to StateFlow instead of Flow, since StateFlow refreshes compose UI properly
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R) =
        mapLatest { transform(it) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), transform(value))
}

val DistributionType.isContinuous get() = distType.isSubclassOf(ContinuousDistributionIfc::class)
val DistributionType.isDiscrete get() = distType.isSubclassOf(DiscreteDistributionIfc::class)
