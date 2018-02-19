package com.github.choppythelumberjack.trivialgen

import scala.collection.immutable.{ListMap, ListSet}
import scala.collection.mutable

object MapExtensions {

  def zipMaps[K, V](one:Map[K, V], two:Map[K, V]): Map[K, (Option[V], Option[V])] = {
    (for (key <- one.keys ++ two.keys)
      yield (key, (one.get(key), two.get(key))))
      .toMap
  }

  def zipMapsOrdered[K, V](one:Map[K, V], two:Map[K, V]): ListMap[K, (Option[V], Option[V])] = {
    val outList = (for (key <- (ListSet() ++ one.keys.toSeq.reverse) ++ (ListSet() ++ two.keys.toSeq.reverse))
      yield (key, (one.get(key), two.get(key))))
    (new ListMap() ++ outList.toSeq.reverse)
  }
}
