class DistEval(distNameIn: String, distGoodnessIn: String, chiSquaredScoreIn: Float = 0f, ksScoreIn: Float = 0f, totalScoreIn: Float = 0f) {
    val distName = distNameIn
    val distGoodness = distGoodnessIn
    val chiSquaredScore = chiSquaredScoreIn
    val ksScore = ksScoreIn
    val totalScore = totalScoreIn
}