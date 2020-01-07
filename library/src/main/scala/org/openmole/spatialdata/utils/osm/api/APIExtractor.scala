package org.openmole.spatialdata.utils.osm.api

import java.io.StringReader
import java.sql.Connection
import java.util.Locale

import org.locationtech.jts.geom._
import org.openmole.spatialdata
import org.openmole.spatialdata.utils.database.{MongoConnection, PostgisConnection}
import org.openmole.spatialdata.utils.gis.GISUtils.WGS84toPseudoMercatorFilter
import org.openmole.spatialdata.utils.gis.PoligonizerUtils
import org.openmole.spatialdata.utils.osm.JtsGeometryFactory
import org.openmole.spatialdata.utils.osm._
import org.openmole.spatialdata.utils.osm.xml.InstantiatedOsmXmlParser

import scala.util.Try

object APIExtractor {

  sealed trait OSMAPIMode
  case object OSMOverpass extends OSMAPIMode
  case object OSMDirect extends OSMAPIMode
  case class Postgresql(port: Int = 5433) extends OSMAPIMode
  case class Mongo(port: Int = 27017) extends OSMAPIMode


  /**
    * Methods to extract buildings
    */
  object Buildings {

    def asPolygonSeq(e: Root.Enumerator[Way]) = {
      var result = scala.collection.mutable.Buffer[Polygon]()
      val fact = new JtsGeometryFactory()
      var way: Way = e.next
      while (way != null) {
        val building = way.getTag("building")
        if (building != null /* && building.equals("yes")*/ ) {
          // FIXME some OSM polygons seem to be bad formed (failure "Way expected to be a polygon" for large sampling) => wrap in Try
          val potentialPolygon = Try(fact.createPolygon(way))
          if (potentialPolygon.isSuccess) {
            result += potentialPolygon.get
          }
        }
        way = e.next
      }
      result.toSeq
    }


    /**
      *
      * @param south
      * @param west
      * @param north
      * @param east
      * @param mode osm,overpass, postgresql
      * @return
      */
    def getBuildings(south: Double, west: Double, north: Double, east: Double, mode: OSMAPIMode = OSMOverpass): Seq[Polygon] = {
      Locale.setDefault(Locale.ENGLISH)
      mode match {
        case OSMOverpass => {
          val overpass = new Overpass
          overpass.setUserAgent("Spatial Data extraction")
          overpass.open()
          val root = new PojoRoot
          val parser = InstantiatedOsmXmlParser.newInstance
          parser.setRoot(root)
          parser.parse(new StringReader(overpass.execute(
            s"""
               |  <query type="way">
               |    <has-kv k="building" v="yes"/>
               |    <bbox-query e="$east" n="$north" s="$south" w="$west"/>
               |  </query>
               |  <union>
               |    <item />
               |    <recurse type="way-node"/>
               |  </union>
               |  <print/>
           """.stripMargin)))
          if (spatialdata.DEBUG) println("retrieved via overpass " + east + " n=" + north + " s=" + south + "w=" + west)
          asPolygonSeq(root.enumerateWays)
        }
        case OSMDirect => {
          val api = new ApiConnection()
          val res = api.get(south, west, north, east)
          if (spatialdata.DEBUG) println("retrieved via standard api " + east + " n=" + north + " s=" + south + "w=" + west)
          asPolygonSeq(res.enumerateWays)
        }
        case Postgresql(port) => {
          implicit val connection: Connection = PostgisConnection.initPostgis(database ="buildings",port = port)
          val polygons = PostgisConnection.bboxRequest(west,south,east,north,"ways")
          if (spatialdata.DEBUG) println("retrieved via postgresql " + east + " n=" + north + " s=" + south + "w=" + west+" : "+polygons.size+" buildings")
          PostgisConnection.closeConnection
          polygons
        }
        case Mongo(port) => {
          MongoConnection.initMongo(database = "buildings",port=port)
          val polygons = MongoConnection.bboxRequest(west,south,east,north,"buildings")
          if (spatialdata.DEBUG) println("retrieved via mongo " + east + " n=" + north + " s=" + south + "w=" + west+" : "+polygons.size+" buildings")
          MongoConnection.closeMongo()
          polygons
        }
      }
      /*
      // FIXME simplify is not used ?
      def simplify(polygon: Polygon) = {
        GeometryPrecisionReducer.reduce(polygon, new PrecisionModel(10000)) match {//FIXME: This is arbitrary
          case p: Polygon => Seq(p)
          case mp: MultiPolygon => for (i <- 0 until mp.getNumGeometries) yield mp.getGeometryN(i).asInstanceOf[Polygon]
          case _ => Seq()
        }
      }*/
      //.flatMap(simplify)
    }

    def getBuildingIntersection(south: Double, west: Double, north: Double, east: Double, mode: OSMAPIMode = OSMOverpass): Seq[Geometry] = {
      val buildings = getBuildings(south, west, north, east, mode)
      val fact = new GeometryFactory()
      val env = fact.createPolygon(fact.createLinearRing(Array(new Coordinate(west, north), new Coordinate(east, north), new Coordinate(east, south), new Coordinate(west, south), new Coordinate(west, north))), Array())
      //buildings.map(_.intersection(env))
      (buildings :+ env).foreach(_.apply(new WGS84toPseudoMercatorFilter))
      PoligonizerUtils.getPolygonIntersection(buildings, env)
    }


    def getNegativeBuildingIntersection(south: Double, west: Double, north: Double, east: Double, mode: OSMAPIMode = OSMOverpass): Geometry = {
      val buildings = getBuildings(south, west, north, east, mode)
      val fact = new GeometryFactory()
      val env = fact.createPolygon(fact.createLinearRing(Array(new Coordinate(west, north), new Coordinate(east, north), new Coordinate(east, south), new Coordinate(west, south), new Coordinate(west, north))), Array())
//      var res = Try {
//        val union = fact.createMultiPolygon(buildings.toArray).union()
//        var result = scala.collection.mutable.Buffer[Polygon]()
//        for (i <- 0 until union.getNumGeometries) result += fact.createPolygon(fact.createLinearRing(union.getGeometryN(i).asInstanceOf[Polygon].getExteriorRing.getCoordinateSequence), Array())
//        env.difference(fact.createMultiPolygon(result.toArray).union)
//      }
//      if (res.isSuccess) {
//        res.get.apply(new WGS84toPseudoMercatorFilter)
//        res.get
//      }else{
//        env
//      }
      (buildings :+ env).foreach(_.apply(new WGS84toPseudoMercatorFilter))
      val res = fact.createMultiPolygon(PoligonizerUtils.getPolygonDifference(buildings, env).toArray)
      res
    }
  }


  /**
    * Methods to extract highways
    */
  object Highways {


    def asLineStringSeq(e: Root.Enumerator[Way], tags: Map[String,Seq[String]]): Seq[LineString] = {
      var result = scala.collection.mutable.Buffer[LineString]()
      val fact = new JtsGeometryFactory()
      var way: Way = e.next
      while (way != null) {
        val validway = tags.toSeq.map{
          case (tag,values) =>
            val waytag = way.getTag(tag)
            if(tag == null) false
            else {
              values.contains(waytag)
            }
        }.reduce(_&_)
        if (validway) {
          val potentialLine = Try(fact.createLineString(way))
          if (potentialLine.isSuccess) {
            result += potentialLine.get
          }
        }
        way = e.next
      }
      result.toSeq
    }

    def getHighways(south: Double, west: Double, north: Double, east: Double,tags: Map[String,Seq[String]]): Seq[LineString] = {
      Locale.setDefault(Locale.ENGLISH)
      val api = new ApiConnection()
      val root = api.get(south, west, north, east)
      asLineStringSeq(root.enumerateWays,tags)
    }
  }
}
