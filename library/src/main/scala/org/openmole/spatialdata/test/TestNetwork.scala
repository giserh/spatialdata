package org.openmole.spatialdata.test

import org.openmole.spatialdata.vector.Point
import org.openmole.spatialdata.utils.Implicits._
import org.openmole.spatialdata.network.measures.NetworkMeasures.ShortestPathsNetworkMeasures
import org.openmole.spatialdata.network.real.OSMNetworkGenerator
import org.openmole.spatialdata.network.synthetic.{RandomNetworkGenerator, TreeMinDistGenerator}
import org.openmole.spatialdata.network.{Network, Node, ShortestPaths}
import org.openmole.spatialdata.utils.graph.GraphAlgorithms
import org.openmole.spatialdata.utils.graph.GraphAlgorithms.{DijkstraJGraphT, FloydWarshallJGraphT, shortestPaths}
import org.openmole.spatialdata.utils.{visualization, withTimer}

import scala.util.Random

object TestNetwork {

  implicit val rng = new Random


  def testSimplification: Unit = {
    val (lat,lon) = (51.5213835,-0.1347904)
    val nw = OSMNetworkGenerator(lon,lat,20000,simplifySnapping = 0.01).generateNetwork
    val (xmin,xmax,ymin,ymax) = (nw.nodes.map{_.x}.min,nw.nodes.map{_.x}.max,nw.nodes.map{_.y}.min,nw.nodes.map{_.y}.max)
    def position(n: Node): Point = ((n.x - xmin)/(xmax-xmin),(n.y - ymin)/(ymax-ymin))
    val simplified = GraphAlgorithms.SimplificationAlgorithm.simplifyNetwork(nw)
    println("Nodes: "+nw.nodes.size+" ; Links: "+nw.links.size)
    println("Simpl Nodes: "+simplified.nodes.size+" ; Simpl Links: "+simplified.links.size)
    //visualization.staticNetworkVisualization(Seq(nw,simplified),nodePositioning = position)
    visualization.staticNetworkVisualization(Seq(simplified),nodePositioning = position)
  }


  def testOSMNetwork: Unit = {
    val (lat,lon) = (51.5213835,-0.1347904)
    //val nw = OSMNetworkGenerator(lon,lat,5000).generateNetwork
    val nw = OSMNetworkGenerator(lon,lat,10000,simplifySnapping = 0.02).generateNetwork
    val (xmin,xmax,ymin,ymax) = (nw.nodes.map{_.x}.min,nw.nodes.map{_.x}.max,nw.nodes.map{_.y}.min,nw.nodes.map{_.y}.max)
    def position(n: Node): Point = ((n.x - xmin)/(xmax-xmin),(n.y - ymin)/(ymax-ymin))
    println("Nodes: "+nw.nodes.size+" ; Links: "+nw.nodes.size)
    visualization.staticNetworkVisualization(Seq(nw),nodePositioning = position)
  }



  def testCycles: Unit = {
    val nw = RandomNetworkGenerator(10,15,true,false,false).generateNetwork
    val cycles = GraphAlgorithms.cycles(nw)
    val colorMap: Map[Node,Int] = cycles.zipWithIndex.flatMap{case (nk,k) => nk.nodes.toSeq.map{(_,k)}}.toMap
    visualization.staticNetworkVisualization(cycles,edgeColors = cycles.indices,
      nodeColorClasses = n => colorMap(n),
      nodePositioning = n => (n.x + colorMap(n)/20,n.y + colorMap(n)/20)
    )
  }


  def testTreeMinDist: Unit = {
    //val nw = TreeMinDistGenerator(200,connexificationAlgorithm = n => n.projectionConnect).generateNetwork
    val nw = TreeMinDistGenerator(200).generateNetwork
    //val nw = TreeMinDistGenerator(100).generateNetwork
    // remove links to have disconnected trees
    //val nwrem = nw.removeLinks(nw.links.sampleWithoutReplacement(100).toSet)
    val nwrem = nw.removeRandomLinks(50)
    //println("Components = "+GraphAlgorithms.connectedComponents(nw).size)
    visualization.staticNetworkVisualization(Seq(nwrem),nodeColorClasses = {_ => Seq(1,2,3,4,5,6,7,8).sampleWithReplacement(1).head})
  }

  /**
    * JGraphT in average around 10 times faster !
    */
  def testShortestPathImplementations(): Unit = {
    //val nwgen = RandomNetworkGenerator(15,50,true,false,false)
    val nwgen = RandomNetworkGenerator(10,20,true,false,false)

    val res = (0 until 100).map{k =>
      if(k%10==0) println(k)
      //val nodes = nw.nodes.sampleWithoutReplacement(10)
      val nw = nwgen.generateNetwork
      val nodes = nw.nodes.toSeq
      //val (sp1,t1) = withTimer[Network,ShortestPaths](nw => shortestPaths(nw,nodes,{l => l.length},ScalaGraph()))(nw)
      val (sp2,t2) = withTimer[Network,ShortestPaths](nw => shortestPaths(nw,nodes,{l => l.length},DijkstraJGraphT()))(nw)
      val (sp3,t3) = withTimer[Network,ShortestPaths](nw => shortestPaths(nw,nodes,{l => l.length},FloydWarshallJGraphT()))(nw)
      (sp2 |-| sp2,0.0,t2,t3,nodes.size.toDouble)
      //(0.0,0.0,0.0,0.0,0.0)
    }

    println("Total difference = "+res.map(_._1).sum)
    println("Average nw size = "+res.map(_._5).sum/res.size)
    println("Time scala graph = "+res.map(_._2).sum/res.size)
    println("Time jgraphT = "+res.map(_._3).sum/res.size)
    println("Time jgraphT FloydWarshall = "+res.map(_._4).sum/res.size)
  }


  def testPlanarization(): Unit = {
    val nw = RandomNetworkGenerator(20,200,false,false,false).generateNetwork
    //val nw = GridNetworkGenerator(10).generateNetwork

    //println(nw.links.size)
    //println(network.isPlanar(nw)) // rq: the grid network should already be planar: OK
    // but the random not: WTF?

    val planarized = nw.planarize
    //println(planarized)
    //println(network.isPlanar(planarized))
    //println(planarized.links.map{_.length})

    val measures = ShortestPathsNetworkMeasures(planarized)
    println(measures.toString)


  }

  def testRandomNetwork(): Unit = {
    val nw = RandomNetworkGenerator(15,10,true,false,false).generateNetwork
    visualization.staticNetworkVisualization(Seq(nw))
  }


}
