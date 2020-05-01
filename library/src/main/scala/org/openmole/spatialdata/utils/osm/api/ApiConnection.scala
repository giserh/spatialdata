package org.openmole.spatialdata.utils.osm.api

import java.io.{InputStreamReader, StringWriter}
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.regex.Pattern

import org.apache.commons.io.IOUtils
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{HttpGet, HttpPut}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{BasicCredentialsProvider, CloseableHttpClient, HttpClientBuilder}
import org.openmole.spatialdata.utils
import org.openmole.spatialdata.utils.osm._
import org.openmole.spatialdata.utils.osm.xml._


/**
  * http://wiki.openstreetmap.org/wiki/API_v0.6
  */
object ApiConnection {
  private val defaultServerURL = "https://api.openstreetmap.org/api"
  private val displayNamePattern = Pattern.compile("<user\\s*.* display_name=\"([^\"]+)\"")
  private val uidPattern = Pattern.compile("<user\\s*.* id=\"([^\"]+)\"")
  val apiVersion = "0.6"
}

class ApiConnection(userServerURL: String = ApiConnection.defaultServerURL,val timeout: Int = 1000) {

  def removeTrailing(s: (String,Boolean)): (String,Boolean) = if(s._1.endsWith("/")) removeTrailing((s._1.substring(0, s._1.length - 1),false)) else (s._1+"/",true)

  val serverURL: String = Iterator.iterate((userServerURL,false))(removeTrailing).takeWhile(!_._2).toSeq.last._1

  private val prefix: String = this.serverURL + "/" + ApiConnection.apiVersion + "/"

  val requestBuilder: RequestConfig.Builder = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)

  private var httpClient: CloseableHttpClient = HttpClientBuilder.create.setDefaultRequestConfig(requestBuilder.build()).build
//  private var serverURL = null

  //private var httpClient: CloseableHttpClient = _
  private var username: String = ""
  private var uid = 0L
  private var displayName: String = ""
  private val getApiType = new OsmObjectVisitor[String]() {
    override def visit(node: Node) = "node"
    override def visit(way: Way) = "way"
    override def visit(relation: Relation) = "relation"
  }

  @throws[Exception]
  def close() = httpClient.close

  @throws[Exception]
  def authenticate(username: String, password: String) = {
    httpClient.close
    val credsProvider: CredentialsProvider = new BasicCredentialsProvider
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password))
    httpClient = HttpClientBuilder.create.setDefaultCredentialsProvider(credsProvider).setUserAgent("osm-common").build
    this.username = username
    val response = httpClient.execute(new HttpGet(prefix + "user/details"))
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      var xml = IOUtils.toString(response.getEntity.getContent, "UTF8")
      xml = xml.replaceAll("\n+", " ")
      var matcher = ApiConnection.uidPattern.matcher(xml)
      if (!matcher.find) throw new RuntimeException("No user id in xml!\n" + xml)
      this.uid = matcher.group(1).toLong
      matcher = ApiConnection.displayNamePattern.matcher(xml)
      if (!matcher.find) throw new RuntimeException("No display name in xml!\n" + xml)
      this.displayName = matcher.group(1)
    } finally response.close
  }

  @throws[Exception]
  def get(south: Double, west: Double, north: Double, east: Double):Root = {
    val root = new PojoRoot
    val delta = get(root, south, west, north, east)
    root
  }

  /**
    * HTTP status code 400 (Bad Request)
    * When any of the node/way/relation limits are crossed
    * <p/>
    * HTTP status code 509 (Bandwidth Limit Exceeded)
    * "Error: You have downloaded too much data. Please try again later." See Developer FAQ.
    *
    * @param root
    * @param south
    * @param west
    * @param north
    * @param east
    */
  @throws[Exception]
  def get(root: Root, south: Double, west: Double, north: Double, east: Double):InstantiatedOsmXmlParserDelta = {
    val otherSymbols = new DecimalFormatSymbols
    otherSymbols.setDecimalSeparator('.')
    val df = new DecimalFormat("#.#######", otherSymbols)
    val bottom = df.format(south)
    val left = df.format(west)
    val top = df.format(north)
    val right = df.format(east)
    val response = httpClient.execute(new HttpGet(prefix + "map?bbox=" + left + "," + bottom + "," + right + "," + top))

    utils.log(prefix + "map?bbox=" + left + "," + bottom + "," + right + "," + top)

    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val parser = InstantiatedOsmXmlParser.newInstance
      parser.setRoot(root)
      parser.parse(response.getEntity.getContent)
    } finally response.close
  }

  /**
    * @param comment
    * @return id of new changeset
    */
  @throws[Exception]
  def createChangeset(comment: String) = {
    val xml = new StringWriter(1024)
    val generator = getClass.getName + "#createChangeset"
    xml.write("<?xml version='1.0' encoding='UTF-8'?>\n")
    xml.write("<osm version='")
    xml.write(ApiConnection.apiVersion)
    xml.write("' generator='")
    xml.write(generator)
    xml.write("'>\n")
    xml.write("<changeset>")
    xml.write("<tag k='created_by' v='")
    xml.write(generator)
    xml.write("'/>")
    if (comment != null) {
      xml.write("<tag k='comment' v='")
      xml.write(comment)
      xml.write("'/>")
    }
    xml.write("</changeset>")
    xml.write("</osm>\n")
    val put = new HttpPut(prefix + "changeset/create")
    put.setEntity(new StringEntity(xml.toString, "UTF8"))

    // FIXME create a decent logging service
    System.out.println(prefix + "changeset/create")
    val response = httpClient.execute(put)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val id = IOUtils.toString(response.getEntity.getContent).toLong
      id
    } finally response.close
    /*

    Create: PUT /api/0.6/changeset/create[edit]
    The payload of a changeset creation request has to be one or more changeset elements optionally including an arbitrary number of tags.

        <osm>
      <changeset>
        <tag k="created_by" v="JOSM 1.61"/>
        <tag k="comment" v="Just adding some streetnames"/>
        ...
      </changeset>
      ...
    </osm>

    The ID of the newly created changeset with a content type of text/plain


         */
  }

  @throws[Exception]
  def closeChangeset(id: Long) = {
    /*

       Close: PUT /api/0.6/changeset/#id/close[edit]
   Closes a changeset. A changeset may already have been closed without the owner issuing this API call. In this case an error code is returned.
   Parameters[edit]
   id
   The id of the changeset to close. The user issuing this API call has to be the same that created the changeset.


        */ val put = new HttpGet(prefix + "changeset/" + id + "/close")
    val response = httpClient.execute(put)
    try
        if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
    finally response.close
  }

  @throws[Exception]
  def getNode(id: Long): Node = {
    val root = new PojoRoot
    val delta = getNode(root, id)
    delta.getCreatedNodes.iterator.next
  }

  @throws[Exception]
  def getNode(root: Root, id: Long): InstantiatedOsmXmlParserDelta = {
    val get = new HttpGet(prefix + "node/" + id)
    val response = httpClient.execute(get)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val parser = InstantiatedOsmXmlParser.newInstance
      parser.setRoot(root)
      parser.parse(new InputStreamReader(response.getEntity.getContent, "UTF8"))
    } finally response.close
  }

  @throws[Exception]
  def getWay(id: Long): Way = {
    val root = new PojoRoot
    val delta = getWay(root, id)
    delta.getCreatedWays.iterator.next
  }

  @throws[Exception]
  def getWay(root: Root, id: Long): InstantiatedOsmXmlParserDelta = {
    val get = new HttpGet(prefix + "way/" + id)
    val response = httpClient.execute(get)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val parser = InstantiatedOsmXmlParser.newInstance
      parser.setRoot(root)
      parser.parse(new InputStreamReader(response.getEntity.getContent, "UTF8"))
    } finally response.close
  }

  @throws[Exception]
  def getRelation(id: Long): Relation = {
    val root = new PojoRoot
    val delta = getRelation(root, id)
    delta.getCreatedRelations.iterator.next
  }

  @throws[Exception]
  def getRelation(root: Root, id: Long): InstantiatedOsmXmlParserDelta = {
    val get = new HttpGet(prefix + "relation/" + id)
    val response = httpClient.execute(get)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val parser = InstantiatedOsmXmlParser.newInstance
      parser.setRoot(root)
      parser.parse(new InputStreamReader(response.getEntity.getContent, "UTF8"))
    } finally response.close
  }

  @throws[Exception]
  def create(changeset: Long, `object`: OsmObject) = {
    `object`.setTimestamp(System.currentTimeMillis)
    `object`.setChangeset(changeset)
    val sw = new StringWriter(4096)
    val xml = new OsmXmlWriter(sw)
    xml.write(`object`)
    xml.close
    val put = new HttpPut(prefix + `object`.accept(getApiType) + "/create")
    put.setEntity(new StringEntity(sw.toString, "UTF8"))
    val response = httpClient.execute(put)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val id = IOUtils.toString(response.getEntity.getContent).toLong
      `object`.setId(id)
      `object`.setVersion(1)
      `object`.setVisible(true)
      `object`.setUid(uid)
      `object`.setUser(displayName)
    } finally response.close
    `object`.setChangeset(changeset)
  }

  def update(`object`: OsmObject) = {
    throw new UnsupportedOperationException
  }

  @throws[Exception]
  def delete(changeset: Long, `object`: OsmObject) = {
    val xml = new StringWriter(1024)
    val generator = getClass.getName + "#createChangeset"
    xml.write("<?xml version='1.0' encoding='UTF-8'?>\n")
    xml.write("<osm version='")
    xml.write(ApiConnection.apiVersion)
    xml.write("' generator='")
    xml.write(generator)
    xml.write("'>\n")
    xml.write("<")
    xml.write(`object`.accept(getApiType))
    xml.write(" id='")
    xml.write(String.valueOf(`object`.getId))
    xml.write("' version='")
    xml.write(String.valueOf(`object`.getVersion))
    xml.write("' changeset='")
    xml.write(String.valueOf(changeset))
    if (`object`.isInstanceOf[Node]) { // todo really need this?
      val node = `object`.asInstanceOf[Node]
      xml.write("' lat='")
      xml.write(String.valueOf(node.getLatitude))
      xml.write("' lon='")
      xml.write(String.valueOf(node.getLongitude))
    }
    xml.write("' />")
    xml.write("</osm>\n")
    val put = new HttpPut(prefix + `object`.accept(getApiType) + "/" + `object`.getId)
    put.setHeader("X_HTTP_METHOD_OVERRIDE", "DELETE")
    put.setEntity(new StringEntity(xml.toString, "UTF8"))
    val response = httpClient.execute(put)
    try {
      if (response.getStatusLine.getStatusCode != 200) throw new RuntimeException("HTTP status " + response.getStatusLine.getStatusCode + ", " + response.getStatusLine.getReasonPhrase)
      val version = Integer.valueOf(IOUtils.toString(response.getEntity.getContent))
      `object`.setVersion(version)
      `object`.setVisible(false)
      `object`.setUid(uid)
      `object`.setUser(displayName)
    } finally response.close
    `object`.setChangeset(changeset)
  }
}
