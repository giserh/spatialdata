
package org.openmole.spatialdata.grid.synthetic

import org.openmole.spatialdata._
import org.openmole.spatialdata.grid._
import org.openmole.spatialdata.network._
import org.openmole.spatialdata.network.synthetic.PercolationNetworkGenerator

import scala.util.Random


case class PercolationGridGenerator(
                                   size: Int,
                                   percolationProba: Double,
                                   bordPoints: Int,
                                   linkwidth: Double,
                                   maxIterations: Int,
                                   /**
                                     * does the percolated stuff corresponds to buildings or streets ?
                                     */
                                   percolateBuildings: Boolean = false
                                   ) extends GridGenerator {

  override def generateGrid(implicit rng: Random): RasterLayerData[Double] = {
    //println("Percolation grid of size "+size+" ; "+percolationProba+" ; "+bordPoints+" ; "+linkwidth)
    val percolatedGrid = network.networkToGrid(PercolationNetworkGenerator(size,percolationProba,bordPoints,linkwidth,maxIterations).generateNetwork(rng),linkwidth=linkwidth)
    if (percolateBuildings) percolatedGrid else percolatedGrid.map{_.map{1.0 - _}}
  }

}

object PercolationGridGenerator


