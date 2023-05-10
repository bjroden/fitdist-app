
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
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
import ksl.utilities.statistic.Statistic
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.gggrid
import org.jetbrains.letsPlot.ggsize
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
    data: DoubleArray,
    private val coroutineScope: CoroutineScope
) {
    val data: DoubleArray

    init {
        this.data = data.sortedArray()
    }

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

    val runButtonEnabled
        get() = data.isNotEmpty() &&
                currentSelection.isNotEmpty() &&
                !binWidthData.isError &&
                testWeights.any { it.value.selected } &&
                testWeights.filter { it.value.selected }
                    .all { !it.value.numberInputData.isError }

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
            reconstructPlotData(results)
            internalSelectableTabs.clear()
            internalSelectableTabs += results.mapNotNull { result ->
                result.dist.getOrNull()?.let { result.distType }
            }
            internalSelectedDist.value = internalSelectableTabs.firstOrNull()
        }
    }

    private val internalSelectableTabs = mutableStateListOf<DistributionType>()

    val selectableDistsForPlots
        get() = internalSelectableTabs.toList()

    private val internalSelectedDist = mutableStateOf<DistributionType?>(null)

    val selectedDist
        get() = internalSelectedDist.value

    fun onPlotDistSelected(distType: DistributionType) {
        internalSelectedDist.value = distType
    }

    private fun reconstructPlotData(distResults: Collection<DistResult>) = coroutineScope.launch {
        val newObservedProbabilities = distResults.mapNotNull { distResult ->
            distResult.dist.getOrNull()?.let { dist ->
                distResult.distType to Plotting.observedDataToProbabilities(data, dist)
            }
        }
        val newExpectedData = distResults.mapNotNull { distResult ->
            distResult.dist.getOrNull()?.let { dist ->
                distResult.distType to Plotting.generateExpectedData(data.size, dist)
            }
        }
        expectedData.clear()
        observedProbabilities.clear()
        expectedData += newExpectedData
        observedProbabilities += newObservedProbabilities
    }

    sealed interface PlotResult

    class PlotSuccess(val value: Figure): PlotResult
    class PlotError(val error: String): PlotResult

    private val expectedProbabilities = Plotting.generateExpectedProbabilities(data.size)
    private val expectedData = mutableStateMapOf<DistributionType, DoubleArray>()
    private val observedProbabilities = mutableStateMapOf<DistributionType, DoubleArray>()

    private val qqData
        get() = mapOf(
            "Theoretical" to expectedData[selectedDist]?.toList(),
            "Empirical" to data.toList()
        )

    private fun qqCord(percentile: Double) = runResults
            ?.distResults
            ?.find { it.distType == selectedDist }
            ?.dist
            ?.getOrNull()
            ?.let { dist ->
                Pair(dist.invCDF(percentile), Statistic.percentile(data, percentile))
            }
    private val qqCoord25
        get() = qqCord(0.25)

    private val qqCoord75
        get() = qqCord(0.75)

    private val qqSlope
        get() = qqCoord75?.let { (x2, y2) ->
            qqCoord25?.let { (x1, y1) ->
                (y2 - y1) / (x2 - x1)
            }
        }

    private val qqIntercept
        get() = qqSlope?.let { slope ->
            qqCoord25?.let { (x, y) ->
                y - (slope * x)
            }
        }

    val qqPlot
        get() = if(expectedData.isNotEmpty() && data.isNotEmpty()) {
            PlotSuccess(
                letsPlot(qqData) { x = "Theoretical"; y = "Empirical" } +
                        geomPoint(size = 4, alpha = .7) +
                        geomABLine(size = 1, color="#000000", slope = qqSlope, intercept = qqIntercept)
            )
        } else {
            PlotError("No data imported")
        }

    private val ppData
        get() = mapOf(
            "Theoretical" to expectedProbabilities.toList(),
            "Empirical" to observedProbabilities[selectedDist]?.toList()
        )

    val ppPlot
        get() = if(observedProbabilities.isNotEmpty() && expectedProbabilities.isNotEmpty()) {
            PlotSuccess(
                letsPlot(ppData) { x = "Theoretical"; y = "Empirical" } +
                        geomPoint(size = 4, alpha = .7) +
                        geomABLine(size = 1, color="#000000", slope = 1, intercept = 0)
            )
        } else {
            PlotError("No data imported")
        }

    private val histogramTheoretical
        get() = mutableStateMapOf(
            "data" to expectedData[selectedDist]?.toList()
        )

    private val histogramEmpirical
        get() = mutableStateMapOf(
            "data" to data.toList()
        )

    val histogramPlot
        get() = if (
            data.isNotEmpty() &&
            expectedData.isNotEmpty() &&
            observedProbabilities.isNotEmpty() &&
            expectedProbabilities.isNotEmpty()
        ) {
            PlotSuccess(letsPlot() +
                    ggsize(500, 250) +
                    geomHistogram(
                        binWidth=runResults?.binWidth,
                        color="black",
                        fill="white"
                    ) {
                        x = histogramEmpirical["data"]
                        y = "..density.."
                    } +
                    geomDensity(alpha=0.2) {
                        x = histogramTheoretical["data"]
                    }
            )
        } else {
            PlotError("No data imported.")
        }

    private val cdfData
        get() = mapOf(
            "empiricalData" to data.toList(),
            "empiricalProbs" to expectedProbabilities.toList(),
            "theoreticalData" to expectedData[selectedDist]?.toList(),
            "theoreticalProbs" to expectedProbabilities.toList()
        )

    // TODO: Add horizontal lines between points
    val cdfPlot
        get() = if (
            data.isNotEmpty() &&
            expectedData.isNotEmpty() &&
            observedProbabilities.isNotEmpty() &&
            expectedProbabilities.isNotEmpty()
        ) {
            PlotSuccess(letsPlot() +
                    geomPoint(color="black") {
                        x = cdfData["empiricalData"]
                        y = cdfData["empiricalProbs"]
                    } +
                    geomLine {
                        x = cdfData["theoreticalData"]
                        y = cdfData["theoreticalProbs"]
                    }
            )
        } else {
            PlotError("No data imported.")
        }

    val allPlots
        get() = run {
            fun getPlot(plot: PlotResult) = (plot as? PlotSuccess)?.value
            val plots = listOf(qqPlot, ppPlot, histogramPlot, cdfPlot).mapNotNull { getPlot(it) }
            if (plots.isNotEmpty()) {
                PlotSuccess(gggrid(plots, ncol = 2))
            } else {
                PlotError("No plots to display")
            }
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
