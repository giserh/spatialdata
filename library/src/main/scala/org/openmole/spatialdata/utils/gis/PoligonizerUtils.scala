package org.openmole.spatialdata.utils.gis

import java.util
import java.util.Calendar

import org.locationtech.jts.geom._
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.operation.polygonize.Polygonizer
import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.openmole.spatialdata.utils

import scala.jdk.CollectionConverters._

object PoligonizerUtils {
  private val fact = new GeometryFactory()
  var DEBUG = false

  private def getLines(inputFeatures: Seq[Geometry]) = {
    val linesList = new util.ArrayList[Geometry]()
    val lineFilter = new LinearComponentExtracter(linesList)
    for (feature <- inputFeatures) feature.apply(lineFilter)
    linesList
  }

  private def extractPoint(lines: util.List[Geometry]) = {
    lines.asScala.find(!_.isEmpty).map(g=>g.getFactory.createPoint(g.getCoordinate))
  }

  private def nodeLines(lines: util.List[Geometry]) = {
    val linesGeom = fact.createMultiLineString(lines.toArray[LineString](new Array(lines.size())))
    var unionInput = extractPoint(lines) match {
      case Some(p) => p
      case None => fact.createMultiLineString(null)
    }
    linesGeom.union(unionInput)
  }

  private def addFeatures(p: Polygonizer, inputFeatures: Seq[Geometry], pm: PrecisionModel = new PrecisionModel(100)) = {
    //utils.log(s"${Calendar.getInstance.getTime} node lines")
    val reduced = inputFeatures.map(GeometryPrecisionReducer.reduce(_, pm))
    // extract linear components from input geometries
    val lines = getLines(reduced)
    // node all geometries together
    val nodedLines = nodeLines(lines) match {
      case mls: MultiLineString =>
        val geoms = for (i <- 0 until mls.getNumGeometries) yield mls.getGeometryN(i)
        nodeLines(geoms.asJava)
      case ls: LineString => ls
    }
    //utils.log(s"${Calendar.getInstance.getTime} insert lines")
    p.add(nodedLines)
  }

  def getPolygons(features: Seq[Geometry]) = {
    val polygonizer = new Polygonizer
    addFeatures(polygonizer, features)
    if (DEBUG) println(s"${Calendar.getInstance.getTime} now with the real stuff")
    val result = polygonizer.getPolygons.asScala.toSeq.map(_.asInstanceOf[Geometry])
    if (DEBUG) println(s"${Calendar.getInstance.getTime} all done now")
    result
  }

  def getPolygonIntersection(features: Seq[Geometry], mask: Geometry) = {
    val polygonizer = new Polygonizer
    addFeatures(polygonizer, features :+ mask)
    if (DEBUG) println(s"${Calendar.getInstance.getTime} now with the real stuff")
    val result = polygonizer.getPolygons.asScala.toSeq.map(_.asInstanceOf[Polygon]).filter{p=>
      val coord = p.getInteriorPoint
      mask.contains(coord) && features.exists(_.contains(coord))
    }
    if (DEBUG) println(s"${Calendar.getInstance.getTime} all done now")
    result
  }

  def getPolygonDifference(features: Seq[Geometry], mask: Geometry) = {
    val polygonizer = new Polygonizer
    addFeatures(polygonizer, features :+ mask)
    if (DEBUG) println(s"${Calendar.getInstance.getTime} now with the real stuff")
    val result = polygonizer.getPolygons.asScala.toSeq.map(_.asInstanceOf[Polygon]).filter{p=>
      val coord = p.getInteriorPoint
      mask.intersects(coord) && !features.exists(_.intersects(coord))
    }
    if (DEBUG) println(s"${Calendar.getInstance.getTime} all done now")
    result
  }
}
