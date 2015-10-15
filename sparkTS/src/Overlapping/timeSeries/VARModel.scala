package overlapping.timeSeries

import breeze.linalg.{DenseMatrix, DenseVector}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import overlapping.containers.SingleAxisBlock
import overlapping.timeSeries.secondOrder.multivariate.frequentistEstimators.procedures.RybickiMulti

import scala.reflect.ClassTag

/**
 * Created by Francois Belletti on 7/13/15.
 */
class VARModel[IndexT <: Ordered[IndexT] : ClassTag](
    p: Int,
    mean: Option[DenseVector[Double]] = None)
    (implicit config: TSConfig, sc: SparkContext)
  extends CrossCovariance[IndexT](p, mean){

  def estimateVARMatrices(crossCovMatrices: Array[DenseMatrix[Double]], covMatrix: DenseMatrix[Double]): (Array[DenseMatrix[Double]], DenseMatrix[Double]) ={
    val nCols = covMatrix.rows

    val coeffMatrices = RybickiMulti(p, nCols,
      crossCovMatrices.slice(1, 2 * p),
      crossCovMatrices.slice(p + 1, 2 * p + 1))

    coeffMatrices.foreach(x => x := x.t)

    var noiseVariance = covMatrix
    for(i <- 1 to p){
      noiseVariance :+= - coeffMatrices(i - 1) * crossCovMatrices(p - i)
    }

    (coeffMatrices, noiseVariance)
  }

  override def estimate(timeSeries: RDD[(Int, SingleAxisBlock[IndexT, DenseVector[Double]])]): (Array[DenseMatrix[Double]], DenseMatrix[Double])= {

    val (crossCovMatrices, covMatrix) = super.estimate(timeSeries)
    estimateVARMatrices(crossCovMatrices, covMatrix)

  }

}