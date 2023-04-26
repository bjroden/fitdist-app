
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import estimations.DistributionType
import estimations.EstimationFactory
import goodnessoffit.AbstractGofTest
import goodnessoffit.ChiSquareRequest
import goodnessoffit.GofFactory
import goodnessoffit.KSRequest
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
    val selectedDists: Collection<DistributionType>
)

@Serializable
data class NumberInputData(
    val text: String,
    val computedValue: Double?
) {
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
        val currentBinWidth = binWidthData.computedValue ?: return@launch
        val bins = getBins(currentBinWidth)
        withContext(Dispatchers.Default) {
            val results = dists.map { distType ->
                // TODO: Library should handle runCatching for dist
                val dist = runCatching { EstimationFactory.getDistribution(distType, data).getOrThrow() }
                val tests = dist.map { dist ->
                    when (dist) {
                        is ContinuousDistributionIfc -> {
                            val chiSquareTest = runCatching { GofFactory().continuousTest(
                                ChiSquareRequest(bins), data, dist)
                            }
                            val ksTest = runCatching { GofFactory().continuousTest(
                                KSRequest, data, dist)
                            }
                            listOf(chiSquareTest, ksTest)
                        }
                        is DiscreteDistributionIfc -> {
                            val chiSquareTest = runCatching { GofFactory().discreteTest(
                                ChiSquareRequest(bins), data, dist)
                            }
                            listOf(chiSquareTest)
                        }
                        else -> emptyList()
                    }
                }.getOrElse { emptyList() }
                // TODO: use weights from UI
                val score = tests.sumOf { 0.5 * (it.getOrNull()?.universalScore ?: 0.0) }
                DistResult(distType, dist, tests, score)
            }.sortedByDescending {
                it.score
            }

            internalTestResults.clear()
            internalTestResults.addAll(results)
            reconstructPlots()
        }
    }

    private fun reconstructPlots() = coroutineScope.launch {
        runQQData()
        runPPData()
        runHistogram()
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

    private fun runQQData() = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = letsPlotFromDists(
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

    private fun runPPData() = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val letsPlotData = letsPlotFromDists(
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
                    geomHistogram(data = histogramEmpirical, binWidth=0.5, color="black", fill="white") { y = "..density.." } +
                    geomDensity(data = histogramTheoretical, alpha=0.2)
            )
        } else {
            PlotError("No data imported.")
        }

    val histogramEmpirical
        get() = internalHistogramEmpirical.toMap()

    private fun runHistogram() = coroutineScope.launch {
        withContext(Dispatchers.Default) {
            val empirical = mapOf<String, Any?>(
                "cond" to List(data.size) { "Empirical" },
                "data" to data.sortedArray()
            )
            val theoretical = letsPlotFromDists(
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
        vararg transformers: Pair<String, (DoubleArray, DistributionIfc<*>) -> DoubleArray>
    ): Map<String, Any?>? {
        val sortedData = data.sortedArray()
        val dists = internalTestResults.mapNotNull { distResult ->
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
            defaultBins().toString(),
            defaultBins(),
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

    private fun defaultBins() = runCatching {
        val breaks = Histogram.recommendBreakPoints(data)
        breaks[1] - breaks[0]
    }.getOrElse {
        1.0
    }

    private fun getBins(width: Double): DoubleArray {
        val numBins = ceil(data.max() / data.min()).toInt()
        val arr = DoubleArray(numBins)
        var value = data.min()
        for (i in arr.indices) {
            arr[i] = value
            value += width
        }
        return arr
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
