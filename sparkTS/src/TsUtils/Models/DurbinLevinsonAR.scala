package TsUtils.Models

import TsUtils.TimeSeries
import breeze.linalg._
import breeze.numerics._

/**
 * Created by Francois Belletti on 7/13/15.
 */
class DurbinLevinsonAR(h: Int)
  extends AutoCovariance(h){

  /*

  This calibrate one univariate AR model per columns.
  Returns an array of calibrated parameters (Coeffs, variance of noise).

  Check out Brockwell, Davis, Time Series: Theory and Methods, 1987 (p 234)
  TODO: shield procedure against the following edge cases, autoCov.size < 1, autoCov(0) = 0.0
   */
  private [this] def proceed(autoCov: DenseVector[Double]): (DenseVector[Double], Double) ={
    var prevPhiEst          = DenseVector.zeros[Double](1)
    prevPhiEst(0)           = autoCov(1) / autoCov(0)
    var prevVarEst: Double  = autoCov(0) * (1.0 - prevPhiEst(0) * prevPhiEst(0))

    var newVarEst: Double   = 0.0

    for(m <- 2 to h){
      val newPhiEst               = DenseVector.zeros[Double](m)
      val temp                    = reverse(autoCov(1 until m))
      newPhiEst(m - 1)            = (autoCov(m) - sum(prevPhiEst :* temp)) / prevVarEst
      newPhiEst(0 to (m - 2))     := prevPhiEst - (reverse(prevPhiEst) :* newPhiEst(m - 1))

      newVarEst                   = prevVarEst * (1.0 - newPhiEst(m - 1) * newPhiEst(m - 1))

      prevPhiEst = newPhiEst
      prevVarEst = newVarEst
    }

    (prevPhiEst, prevVarEst)
  }

  override def estimate(timeSeries: TimeSeries[_, Double]): Array[(DenseVector[Double], Double)] = {
    val autoCovs = super.estimate(timeSeries)
    autoCovs.asInstanceOf[Array[DenseVector[Double]]].map(proceed)
  }

  override def estimate(timeSeriesTile: Seq[Array[Double]]): Array[(DenseVector[Double], Double)] = {
    val autoCovs = super.estimate(timeSeriesTile)
    autoCovs.asInstanceOf[Array[DenseVector[Double]]].map(proceed)
  }


}
