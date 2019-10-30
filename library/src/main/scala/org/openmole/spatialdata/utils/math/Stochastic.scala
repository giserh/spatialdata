package org.openmole.spatialdata.utils.math

import scala.util.Random

object Stochastic {

  /**
    * The probability function is assumed to effectively define a probability measure on elements of sampled
    * @param sampled
    * @param probability
    * @param rng
    * @tparam T
    * @return
    */
  def sampleOneBy[T](sampled: Traversable[T], probability: T => Double)(implicit rng: Random): T = {
    def f(s: (Traversable[T],T,Double,Double)) =  (s._1.tail,s._1.head ,s._3 + probability(s._1.head),s._4)
    f(Iterator.iterate((sampled,sampled.head,0.0,rng.nextDouble()))(f).takeWhile(s => s._3 < s._4).toSeq.last)._2
  }

  def sampleWithReplacementBy[T](sampled: Traversable[T], probability: T => Double, samples: Int)(implicit rng: Random): Vector[T] =
    (0 until samples).map(_ => sampleOneBy(sampled, probability)).toVector

  def sampleWithoutReplacementBy[T](sampled: Traversable[T], probability: T => Double, samples: Int)(implicit rng: Random): Vector[T] = {
    assert(samples <= sampled.size,"Can not sample more than vector size : "+samples+" / "+sampled.size)
    Iterator.iterate((sampled, Vector.empty[T])) { case (rest, res) => {
      val totproba = rest.map(probability(_)).sum // FIXME not efficient, could be computed from the previously sampled proba
      val normalizedProba = rest.toSeq.map(probability(_) / totproba)
      val sample = sampleOneBy[((T, Int), Double)](rest.toSeq.zipWithIndex.zip(normalizedProba), _._2)
      (rest.toSeq.zipWithIndex.filter(_._2 != sample._1._2).map(_._1), Vector(sample._1._1) ++ res)
    }
    }.take(samples).toSeq.last._2
  }

}
