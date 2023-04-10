import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import estimations.DistributionType
import estimations.EstimationFactory
import goodnessoffit.AbstractGofTest
import goodnessoffit.ChiSquareRequest
import goodnessoffit.GofFactory
import goodnessoffit.KSRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import ksl.utilities.distributions.ContinuousDistributionIfc
import ksl.utilities.distributions.DiscreteDistributionIfc
import ksl.utilities.distributions.DistributionIfc
import plotting.Plotting
import kotlin.reflect.full.isSubclassOf

class DistResult(
    val distType: DistributionType,
    val dist: Result<DistributionIfc<*>>,
    val tests: List<Result<AbstractGofTest>>,
    val distGoodness: String = "Goodness still needed", // TODO: Remove hardcoded value
    val score: Double = 0.0 // TODO: Remove hardcoded value
)

@Serializable
class ViewModelSavedSession(
    val data: DoubleArray,
    val selectedDists: Collection<DistributionType>
)

class ViewModel(
    val data: DoubleArray,
    private val coroutineScope: CoroutineScope
) {
    constructor(
        session: ViewModelSavedSession,
        coroutineScope: CoroutineScope
    ) : this(session.data, coroutineScope) {
        session.selectedDists.forEach { internalDistSelection[it] = true }
        runResults()
    }

    private val internalDistSelection = mutableStateMapOf(
        *DistributionType.values()
            .map { it to false }
            .toTypedArray()
    )

    val distSelection
        get() = internalDistSelection.toMap()

    val continuousDistSelection
        get() = internalDistSelection.filterKeys { it.isContinuous }

    val discreteSelection
        get() = internalDistSelection.filterKeys { it.isDiscrete }

    val currentSelection
        get() = internalDistSelection.filterValues { it }.keys

    val resultsSelection
        get() = internalTestResults.map { it.distType }

    fun distributionSelected(distType: DistributionType, newSelectedValue: Boolean) {
        internalDistSelection.replace(distType, newSelectedValue)
    }

    private val internalTestResults = mutableStateListOf<DistResult>()

    val testResults
        get() = internalTestResults.toList()

    fun runResults() = replaceResults(currentSelection)

    private fun replaceResults(dists: Set<DistributionType>) = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val results = dists.map { distType ->
                // TODO: Library should handle runCatching for dist
                val dist = runCatching { EstimationFactory.getDistribution(distType, data).getOrThrow() }
                val tests = dist.map { dist ->
                    when (dist) {
                        is ContinuousDistributionIfc -> {
                            val chiSquareTest = runCatching { GofFactory().continuousTest(
                                ChiSquareRequest(), data, dist)
                            }
                            val ksTest = runCatching { GofFactory().continuousTest(
                                KSRequest, data, dist)
                            }
                            listOf(chiSquareTest, ksTest)
                        }
                        is DiscreteDistributionIfc -> {
                            val chiSquareTest = runCatching { GofFactory().discreteTest(
                                ChiSquareRequest(), data, dist)
                            }
                            listOf(chiSquareTest)
                        }
                        else -> emptyList()
                    }
                }.getOrElse { emptyList() }
                DistResult(distType, dist, tests)
            }

            internalTestResults.clear()
            internalTestResults.addAll(results)
            reconstructPlots()
        }
    }

    private fun reconstructPlots() = coroutineScope.launch {
        val dists = internalTestResults.mapNotNull { distResult ->
            distResult.dist.getOrNull()?.let {dist ->
                distResult.distType to dist
            }
        }
        internalTheoreticalProbabilities.clear()
        internalTheoreticalData.clear()
        internalObservedProbabilities.clear()

        internalTheoreticalProbabilities += dists.associate { (distType, _) ->
            distType to Plotting.generateExpectedProbabilities(data.size)
        }
        internalTheoreticalData += dists.associate { (distType, dist) ->
            distType to Plotting.generateExpectedData(data.size, dist)
        }
        internalObservedProbabilities += dists.associate { (distType, dist) ->
            distType to Plotting.observedDataToProbabilities(observedData, dist)
        }
    }

    private val internalTheoreticalProbabilities = mutableStateMapOf<DistributionType, DoubleArray>()
    private val internalTheoreticalData = mutableStateMapOf<DistributionType, DoubleArray>()
    private val internalObservedProbabilities = mutableStateMapOf<DistributionType, DoubleArray>()

    val theoreticalProbabilities
        get() = internalTheoreticalProbabilities.toMap()
    val theoreticalData
        get() = internalTheoreticalData.toMap()
    val observedProbabilities
        get() = internalObservedProbabilities.toMap()
    val observedData
        get() = data.sortedArray()

    fun toSession() = ViewModelSavedSession(
        data,
        resultsSelection // Save last ran distributions, ignore selections made after
    )

    // Converts to StateFlow instead of Flow, since StateFlow refreshes compose UI properly
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R) =
        mapLatest { transform(it) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), transform(value))
}

val DistributionType.isContinuous get() = distType.isSubclassOf(ContinuousDistributionIfc::class)
val DistributionType.isDiscrete get() = distType.isSubclassOf(DiscreteDistributionIfc::class)
