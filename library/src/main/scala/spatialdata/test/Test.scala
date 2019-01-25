package spatialdata.test

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, MultiPolygon}
import spatialdata.osm.BuildingExtractor


object Test extends App {

//  TestSynthetic.testRandomGrids()
//  BuildingExtractor.getBuildingIntersection(48.82864, 2.36238, 48.83040, 2.36752).foreach(println)
  val lon = 2.36238
  val lat = 48.82864
  val shift = 50 // in meters
  //48.82864, 2.36238, 48.83040, 2.36752
  val (x, y) = BuildingExtractor.WGS84ToPseudoMercator(lon, lat)
  val (west, south) = BuildingExtractor.PseudoMercatorToWGS84Mercator(x - shift, y - shift)
  val (east, north) = BuildingExtractor.PseudoMercatorToWGS84Mercator(x + shift, y + shift)
  val g = BuildingExtractor.getNegativeBuildingIntersection(south, west, north, east)
  println(g)
  if (g.isInstanceOf[MultiPolygon]) println(asInstanceOf[MultiPolygon].getNumGeometries)
}