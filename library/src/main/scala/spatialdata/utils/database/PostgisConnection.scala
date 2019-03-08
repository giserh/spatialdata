
package spatialdata.utils.database


import java.sql.{Connection, DriverManager}
import java.util.Properties

import com.vividsolutions.jts.io._
import com.vividsolutions.jts.geom.{Geometry, GeometryFactory, Polygon}
import org.geotools.data.simple.{SimpleFeatureIterator, SimpleFeatureSource}
import org.geotools.data.{DataStore, DataStoreFinder}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.geometry.jts.ReferencedEnvelope
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.{Filter, FilterFactory2}
import org.opengis.referencing.crs.CoordinateReferenceSystem

import scala.collection.mutable.ArrayBuffer


object PostgisConnection {

  var connection: Connection = null

  def initPostgis(database: String,port: Int = 5432): Unit = {
    /*
    val params: java.util.Map[String,AnyRef] = new java.util.HashMap()
    params.put("dbtype", "postgis")
    params.put("host", "localhost") // always assumed on localhost
    params.put("port", port.asInstanceOf[AnyRef])
    params.put("schema", "public")
    params.put("database", database)
    //params.put("user", "postgres")
    //params.put("passwd", "postgres")
    datastore = DataStoreFinder.getDataStore(params)
    */
    val url = "jdbc:postgresql://localhost/"+database
    val props = new Properties
    props.setProperty("user","postgres")
    connection = DriverManager.getConnection(url, props)
  }

  def bboxRequest(xmin: Double,ymin: Double,xmax: Double,ymax: Double,table: String): Seq[Polygon] = {

    /*
    val ff: FilterFactory2  = CommonFactoryFinder.getFilterFactory2()
    val featuretype: SimpleFeatureType = datastore.getSchema(table)
    val source: SimpleFeatureSource = datastore.getFeatureSource(table)
    val geometryPropertyName = featuretype.getGeometryDescriptor().getLocalName()
    val crs: CoordinateReferenceSystem = featuretype.getGeometryDescriptor().getCoordinateReferenceSystem()

    val bbox: ReferencedEnvelope = new ReferencedEnvelope(xmin, ymin, xmax, ymax, crs)

    val filter: Filter = ff.bbox(ff.property(geometryPropertyName), bbox)

    val featureiterator: SimpleFeatureIterator = source.getFeatures(filter).features()

    val res = new ArrayBuffer[Polygon]

    while(featureiterator.hasNext){
      val feature: SimpleFeature = featureiterator.next()
      res.append(feature.getDefaultGeometry.asInstanceOf[Polygon])
    }
    res
    */
    val st = connection.createStatement()
    val rs = st.executeQuery("select ST_AsText(linestring) from "+table+
      " WHERE ST_Intersects(ST_MakeEnvelope("+xmin+","+ymin+","+xmax+","+ymax+",4326),linestring);")

    val res = new ArrayBuffer[Polygon]

    val reader = new WKTReader
    val geomfact = new GeometryFactory

    while (rs.next()){
      res.append(geomfact.createPolygon(geomfact.createLinearRing(reader.read(rs.getString(1)).getCoordinates)))
    }

    rs.close()
    st.close()

    res
  }

}

