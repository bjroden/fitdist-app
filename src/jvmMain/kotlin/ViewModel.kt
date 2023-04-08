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
        runQQData()
        runPPData()
    }

    private val internalQQData = mutableStateMapOf<String, Any?>()

    val qqData
        get() = internalQQData.toMap()

    private fun runQQData() = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = createLetsPlotData(
                xTransformer = { data, dist -> Plotting.generateExpectedData(data.size, dist) },
                yTransformer = { data, _ -> data }
            )
            internalQQData.clear()
            internalQQData += letsPlotData
        }
    }

    private val internalPPData = mutableStateMapOf<String, Any?>()

    val ppData
        get() = internalPPData.toMap()

    private fun runPPData() = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = createLetsPlotData(
                xTransformer = { data, _ -> Plotting.generateExpectedProbabilities(data.size) },
                yTransformer = { data, dist -> Plotting.observedDataToProbabilities(data, dist) }
            )
            internalPPData.clear()
            internalPPData += letsPlotData
        }
    }

    private fun createLetsPlotData(
        xTransformer: (DoubleArray, DistributionIfc<*>) -> DoubleArray,
        yTransformer: (DoubleArray, DistributionIfc<*>) -> DoubleArray
    ): Map<String, Any?> {
        val sortedData = data.sortedArray()
        val dists = internalTestResults.mapNotNull { distResult ->
            distResult.dist.getOrNull()?.let { dist ->
                distResult.distType to dist
            }
        }
        val cond = dists.flatMap { entry ->
            List(sortedData.size) { entry.first.distName }
        }
        val xs = dists.flatMap { entry ->
            xTransformer(sortedData, entry.second).toList()
        }
        val ys = dists.flatMap { entry ->
            yTransformer(sortedData, entry.second).toList()
        }
        return mapOf(
            "cond" to cond,
            "Theoretical" to xs,
            "Empirical" to ys
        )
    }

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
