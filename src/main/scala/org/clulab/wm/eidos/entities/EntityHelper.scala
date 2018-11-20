package org.clulab.wm.eidos.entities

import org.clulab.odin.{Mention, State, TextBoundMention}
import org.clulab.struct.Interval

import scala.annotation.tailrec

object EntityHelper {
  /***
    * Recursively splits a TextBoundMention (flat) on likely coordinations.
    */
  @tailrec
  protected def splitCoordinatedEntities(m: TextBoundMention, entities: Seq[Mention]): Seq[Mention] = {

    val coordIndex: Option[Int] = m.tokenInterval.find(i => isCoord(i, m))

    coordIndex match {
      // No coordination
      case None => entities ++ List(m)
      // mention contains only CC
      case Some(skipTok) if skipTok == m.start && m.end == m.start + 1 =>
        entities ++ List(m)
      // mention begins with CC, then skip this token and advance one
      case Some(skipTok) if skipTok == m.start =>
        val remaining = m.copy(tokenInterval = Interval(skipTok + 1, m.end))
        splitCoordinatedEntities(remaining, entities)
      // mention ends with CC, then discard and return
      case Some(skipTok) if skipTok == m.end - 1 =>
        val chunk = List(m.copy(tokenInterval = Interval(m.start, skipTok)))
        entities ++ chunk
      // otherwise, we need to split again
      case Some(idx) =>
        val chunk = if (m.start == idx) Nil else List(m.copy(tokenInterval = Interval(m.start, idx)))
        val remaining = m.copy(tokenInterval = Interval(idx + 1, m.end))
        splitCoordinatedEntities(remaining, entities ++ chunk)
    }
  }

  def splitCoordinatedEntities(m: Mention): Seq[Mention] = m match {
    case tb: TextBoundMention => splitCoordinatedEntities(tb, Nil)
    case _ => Seq(m)
  }

  /** Checks if the indexed token is a coordination **/
  def isCoord(i: Int, m: Mention): Boolean = EntityConstraints.isCoord(i, m)


  /**
    * Trims found entities of leading or trailing unwanted tokens.  Currently, we define "unwanted" as being POS tagged
    * with one of the tags in invalidEdgeTags (see INVALID_EDGE_TAGS for examples).
    * @param entity
    * @param invalidEdgeTags
    * @return TextBoundMention with valid Interval
    */
  def trimEntityEdges(entity: Mention, invalidEdgeTags: Set[scala.util.matching.Regex]): Mention = {
    //     println(s"trying to trim entity: ${entity.text}")
    // Check starting tag, get the location of first valid tag

    val tags = entity.document.sentences(entity.sentence).tags.get
    val startToken = entity.tokenInterval.start
    val startTag = tags(startToken)
    val firstValidStart = if (validEdgeTag(startTag, invalidEdgeTags)) startToken else firstValid(tags, startToken, invalidEdgeTags)

    // Check ending tag, get the location of last valid tag
    val endToken = entity.tokenInterval.end - 1  // subtracting 1 bc interval is exclusive
    val endTag = tags(endToken)
    val lastValidEnd = if (validEdgeTag(endTag, invalidEdgeTags)) endToken else lastValid(tags, endToken, invalidEdgeTags)


    if (firstValidStart == startToken && lastValidEnd == endToken) {
      // No trimming needed because both first and last were valid
      entity
    } else if (firstValidStart > lastValidEnd) {
      // If you trimmed everything...
      entity
    }
    else {
      // Return a new entity with the trimmed token interval
      val interval = Interval(firstValidStart, lastValidEnd + 1)
      entity.asInstanceOf[TextBoundMention].copy(tokenInterval = interval)
    }
  }

  // Find the first valid token in the mention's token interval
  def firstValid(
    tags: Seq[String],
    mentionStart: Int,
    invalidEdgeTags: Set[scala.util.matching.Regex]
  ): Int = {
    // As indexWhere returns -1 in the event it doesn't find any, here we add the max to default to the first token
    math.max(tags.indexWhere(tag => validEdgeTag(tag, invalidEdgeTags), from = mentionStart), 0)
  }

  // Find the last valid token in the mention's token interval
  // mentionEnd is inclusive
  def lastValid(
    tags: Seq[String],
    mentionEnd: Int,
    invalidEdgeTags: Set[scala.util.matching.Regex]
  ): Int = {
    // As indexWhere returns -1 in the event it doesn't find any, here we add the max to default to the first token
    // Note: end is inclusive
    math.max(tags.lastIndexWhere(tag => validEdgeTag(tag, invalidEdgeTags), end = mentionEnd), 0)
  }

  def validEdgeTag(
    tag: String,
    invalidEdgeTags: Set[scala.util.matching.Regex]
  ): Boolean = ! invalidEdgeTags.exists(pattern => pattern.findFirstIn(tag).nonEmpty)

  // Set of tags that we don't want to begin or end an entity
  // FIXME: adapt to include UD tags
  val INVALID_EDGE_TAGS = Set[scala.util.matching.Regex](
    "^PRP".r,
    "^IN".r,
    "^TO".r,
    "^DT".r,
    ",".r,
    // PORTUGUESE
    "PRON".r,
    //"ADP".r,
    "DET".r
    //"[!\"#$%&'*+,-\\./:;<=>?@\\^_`{|}~]".r
  )


}