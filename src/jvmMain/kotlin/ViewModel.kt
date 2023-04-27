
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import datascoring.DataScoring
import estimations.DistributionType
import estimations.EstimationFactory
import goodnessoffit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import ksl.utilities.distributions.*
import ksl.utilities.statistic.Histogram
import org.jetbrains.letsPlot.geom.geomDensity
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.geom.geomQQ2
import org.jetbrains.letsPlot.geom.geomQQ2Line
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import plotting.Plotting
import kotlin.math.ceil
import kotlin.reflect.full.isSubclassOf

class DistResult(
    val distType: DistributionType,
    val dist: Result<DistributionIfc<*>>,
    val tests: List<Result<AbstractGofTest>>,
    val score: Double
)

@Serializable
class ViewModelSavedSession(
    val data: DoubleArray,
    val binWidth: Double,
    val selectedDists: Collection<DistributionType>,
    val testWeights: Map<GofTestType, TestWeightData>
)

data class RunResults(
    val binWidth: Double,
    val testWeights: Map<GofTestType, TestWeightData>,
    val distResults: Collection<DistResult>
)

@Serializable
data class TestWeightData(
    val selected: Boolean,
    val numberInputData: NumberInputData
)

enum class GofTestType(val testName: String) {
    ChiSquare("Chi-Squared test"),
    KS("K-S test")
}

@Serializable
data class NumberInputData(
    val text: String,
    val computedValue: Double?
) {
    constructor(computedValue: Double) : this(computedValue.toString(), computedValue)

    val isError
        get() = computedValue == null
}

class ViewModel(
    val data: DoubleArray,
    private val coroutineScope: CoroutineScope
) {
    constructor(
        session: ViewModelSavedSession,
        coroutineScope: CoroutineScope
    ) : this(session.data, coroutineScope) {
        session.selectedDists.forEach { internalDistSelection[it] = true }
        internalBinWidthData.value = NumberInputData(
            session.binWidth.toString(),
            session.binWidth
        )
        internalTestWeights.clear()
        internalTestWeights += session.testWeights
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
        get() = runResults?.distResults?.map { it.distType }

    fun distributionSelected(distType: DistributionType, newSelectedValue: Boolean) {
        internalDistSelection.replace(distType, newSelectedValue)
    }

    private val internalRunResults: MutableState<RunResults?> = mutableStateOf(null)

    val runResults
        get() = internalRunResults.value

    val distResults
        get() = runResults?.distResults?.toList()

    fun runResults() = replaceResults(currentSelection)

    private var internalTestWeights = mutableStateMapOf(
        *GofTestType.values().map {
            it to TestWeightData(true, NumberInputData(1.0 / GofTestType.values().size))
        }.sortedBy { (type, _) -> type.testName }
            .toTypedArray()
    )

    val testWeights
        get() = internalTestWeights.toMap()

    fun onWeightSelected(testType: GofTestType, selected: Boolean) {
        val oldValue = internalTestWeights[testType]
        oldValue?.copy(selected = selected)?.let { internalTestWeights[testType] = it }
    }

    fun onGofWeightValueChange(testType: GofTestType, newValue: String) {
        val oldValue = internalTestWeights[testType]
        val newDouble = newValue.toDoubleOrNull()?.let {
            if (it in 0.0..1.0) { it }
            else { null }
        }
        oldValue?.copy(numberInputData = NumberInputData(newValue, newDouble))
            ?.let { internalTestWeights[testType] = it }
    }

    private fun replaceResults(dists: Set<DistributionType>) = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val currentBinWidth = binWidthData.computedValue ?: return@withContext
            val bins = getBins(currentBinWidth)
            val tests = testWeights
                .filter { (_, data) -> data.selected }
                .mapNotNull { (type, data) ->
                    if (data.selected) {
                        data.numberInputData.computedValue?.let { type to it }
                    } else {
                        null
                    }
                }.toMap()
            val requests = tests.map { (type, score) ->
                val request = when (type) {
                    GofTestType.ChiSquare -> ChiSquareRequest(bins)
                    GofTestType.KS -> KSRequest
                }
                request to score
            }.toMap()

            val results = dists.map { distType ->
                // TODO: Library should handle runCatching for dist
                val dist = runCatching { EstimationFactory.getDistribution(distType, data).getOrThrow() }
                val tests = dist.map { dist ->
                    when (dist) {
                        is ContinuousDistributionIfc -> {
                            val continuousRequests = requests.mapNotNull { (request, score) ->
                                val continuousRequest = request as? ContinuousRequest<*>
                                continuousRequest?.let { it to score }
                            }
                            continuousRequests.map { (request, score) ->
                                val test = runCatching { GofFactory().continuousTest(request, data, dist) }
                                test to score
                            }
                        }
                        is DiscreteDistributionIfc -> {
                            val discreteRequests = requests.mapNotNull { (request, score) ->
                                val discreteRequest = request as? DiscreteRequest<*>
                                discreteRequest?.let { it to score }
                            }
                            discreteRequests.map { (request, score) ->
                                val test = runCatching { GofFactory().discreteTest(request, data, dist) }
                                test to score
                            }
                        }
                        else -> emptyList()
                    }
                }.getOrElse { emptyList() }
                val score = DataScoring.scoreTests(
                    *tests.mapNotNull { (request, score) -> request.getOrNull()?.let { it to score }
                }.toTypedArray())
                DistResult(distType, dist, tests.map { it.first }, score)
            }.sortedByDescending {
                it.score
            }

            val runResults = RunResults(currentBinWidth, testWeights, results)
            internalRunResults.value = runResults
            reconstructPlots(runResults)
        }
    }

    private fun reconstructPlots(runResults: RunResults) = coroutineScope.launch {
        runQQData(runResults)
        runPPData(runResults)
        runHistogram(runResults)
    }

    sealed interface PlotResult

    class PlotSuccess(val plot: Plot): PlotResult
    class PlotError(val error: String): PlotResult

    private val internalQQData = mutableStateMapOf<String, Any?>()

    val qqData
        get() = internalQQData.toMap()

    val qqPlot
        get() = PlotSuccess(
            letsPlot(qqData) { x = "Theoretical"; y = "Empirical"; color = "cond" } +
                    geomQQ2(size = 4, alpha = .7) +
                    geomQQ2Line(size = 1, color="#000000")
        )

    private fun runQQData(runResults: RunResults) = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = letsPlotFromDists(
                runResults,
                "Theoretical" to { data, dist -> Plotting.generateExpectedData(data.size, dist) },
                "Empirical" to { data, _ -> data }
            )
            internalQQData.clear()
            if (letsPlotData == null) { return@withContext }
            internalQQData += letsPlotData
        }
    }

    private val internalPPData = mutableStateMapOf<String, Any?>()

    val ppData
        get() = internalPPData.toMap()

    val ppPlot
        get() = if(internalQQData.isNotEmpty()) {
            PlotSuccess(
                letsPlot(ppData) { x = "Theoretical"; y = "Empirical"; color = "cond" } +
                        geomQQ2(size = 4, alpha = .7) +
                        geomQQ2Line(size = 1, color="#000000")
            )
        } else {
            PlotError("No data imported")
        }

    private fun runPPData(runResults: RunResults) = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = letsPlotFromDists(
                runResults,
                "Theoretical" to { data, _ -> Plotting.generateExpectedProbabilities(data.size) },
                "Empirical" to { data, dist -> Plotting.observedDataToProbabilities(data, dist) }
            )
            internalPPData.clear()
            if (letsPlotData == null) { return@withContext }
            internalPPData += letsPlotData
        }
    }

    private val internalHistogramTheoretical = mutableStateMapOf<String, Any?>()

    private val internalHistogramEmpirical = mutableStateMapOf<String, Any?>()

    val histogramTheoretical
        get() = internalHistogramTheoretical.toMap()

    val dummy = mapOf<String, Any?>(
        "cond" to emptyList<String>(),
        "data" to emptyList<Number>()
    )
    val histogramPlot
        get() = if(internalHistogramEmpirical.isNotEmpty() && internalHistogramTheoretical.isNotEmpty()) {
            PlotSuccess(letsPlot(dummy) { x = "data"; color = "cond" } +
                    ggsize(500, 250) +
                    geomHistogram(data = histogramEmpirical, binWidth=runResults?.binWidth, color="black", fill="white") { y = "..density.." } +
                    geomDensity(data = histogramTheoretical, alpha=0.2)
            )
        } else {
            PlotError("No data imported.")
        }

    val histogramEmpirical
        get() = internalHistogramEmpirical.toMap()

    private fun runHistogram(runResults: RunResults) = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val empirical = mapOf<String, Any?>(
                "cond" to List(data.size) { "Empirical" },
                "data" to data.sortedArray()
            )
            val theoretical = letsPlotFromDists(
                runResults,
                "data" to { data, dist -> Plotting.generateExpectedData(data.size, dist) }
            )

            internalHistogramTheoretical.clear()
            internalHistogramEmpirical.clear()
            if (theoretical == null || data.isEmpty()) { return@withContext }
            internalHistogramTheoretical += theoretical
            internalHistogramEmpirical += empirical
        }
    }

    private fun letsPlotFromDists(
        runResults: RunResults,
        vararg transformers: Pair<String, (DoubleArray, DistributionIfc<*>) -> DoubleArray>
    ): Map<String, Any?>? {
        val sortedData = data.sortedArray()
        val dists = runResults.distResults.mapNotNull { distResult ->
            distResult.dist.getOrNull()?.let { dist ->
                distResult.distType to dist
            }
        }
        if (dists.isEmpty()) { return null }
        val cond = dists.flatMap { (distType, _) ->
            List(sortedData.size) { distType.distName }
        }
        val data = transformers.associate { (key, transformer) ->
            key to dists.flatMap { (_, dist) ->
                transformer(sortedData, dist).toList()
            }
        }
        return mapOf("cond" to cond) + data
    }

    private val internalBinWidthData = mutableStateOf(
        NumberInputData(
            defaultBinWidth().toString(),
            defaultBinWidth(),
        )
    )

    val binWidthData
        get() = internalBinWidthData.value

    fun onBinWidthTextChange(newValue: String) {
        val computedDouble = newValue.toDoubleOrNull()
        val newDouble = if (computedDouble?.let { it > 0 } == true) {
            computedDouble
        } else {
            null
        }
        internalBinWidthData.value = NumberInputData(newValue, newDouble)
    }

    private fun defaultBinWidth() = runCatching {
        val breaks = Histogram.recommendBreakPoints(data)
        breaks[1] - breaks[0]
    }.getOrElse {
        1.0
    }

    private fun getBins(width: Double): DoubleArray {
        val numBins = ceil((data.max() - data.min()) / width).toInt()
        val arr = DoubleArray(numBins)
        var value = data.min()
        for (i in arr.indices) {
            arr[i] = value
            value += width
        }
        return arr
    }

    fun toSession() = runResults?.let { results ->
        ViewModelSavedSession(
            data,
            results.binWidth,
            results.distResults.map { it.distType }, // Save last ran distributions, ignore selections made after
            results.testWeights
        )
    }

    // Converts to StateFlow instead of Flow, since StateFlow refreshes compose UI properly
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R) =
        mapLatest { transform(it) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), transform(value))
}

val DistributionType.isContinuous get() = distType.isSubclassOf(ContinuousDistributionIfc::class)
val DistributionType.isDiscrete get() = distType.isSubclassOf(DiscreteDistributionIfc::class)
