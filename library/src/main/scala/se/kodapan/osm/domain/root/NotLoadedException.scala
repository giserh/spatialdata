package se.kodapan.osm.domain.root

import se.kodapan.osm.domain.OsmObject


/**
  * Created by kalle on 10/20/13.
  */
class NotLoadedException(detailMessage: String, throwable: Throwable) extends java.lang.RuntimeException(detailMessage, throwable) {
  def this(`object`: OsmObject) {
    this(`object`.getClass.getSimpleName + "#id " + `object`.getId + " is not loaded!", null)
  }
  def this(detailMessage: String) {
    this(detailMessage, null)
  }
}
