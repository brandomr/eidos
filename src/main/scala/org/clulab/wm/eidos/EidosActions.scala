package org.clulab.wm.eidos

import com.typesafe.scalalogging.LazyLogging
import org.clulab.odin._
import org.clulab.odin.impl.Taxonomy
import org.clulab.wm.eidos.attachments._
import org.clulab.wm.eidos.utils.{DisplayUtils, FileUtils}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.collection.mutable.ArrayBuffer
import scala.io.BufferedSource


// 1) the signature for an action `(mentions: Seq[Mention], state: State): Seq[Mention]`
// 2) the methods available on the `State`

//TODO: need to add polarity flipping


class EidosActions(val taxonomy: Taxonomy) extends Actions with LazyLogging {

  /**
    * @author Gus Hahn-Powell
    * Copies the label of the lowest overlapping entity in the taxonomy
    */
  def customAttachmentFilter(mentions: Seq[Mention]): Seq[Mention] = {

    // --- To distinguish between :
    // 1. Attachments: Quantification(high,Some(Vector(record))), Increase(high,None)
    // 2. Attachments: Quantification(high,None), Increase(high,None)
    // --- and select 2.

    val mention_attachmentSz: Seq[(Mention, Int)] = for (mention <- mentions) yield {

      // number of Arguments, number of attachments, the set of all attachments
      val (numArgs, modSize, attachmentsSet) = mention match {
        case tb: TextBoundMention => {
          val tbModSize = tb.attachments.size * 10
          val tbAttachmentSet = tb.attachments
          (0, tbModSize, tbAttachmentSet)
        }
        case rm: RelationMention => {
          val rmSize = rm.arguments.values.flatten.size * 100
          val rmModSize = rm.arguments.values.flatten.map(arg => arg.attachments.size).sum * 10
          val rmAttachmentSet = rm.arguments.values.flatten.flatMap(m => m.attachments).toSet
          (rmSize, rmModSize, rmAttachmentSet)
        }
        case em: EventMention => {
          val emSize = em.arguments.values.flatten.size * 100
          val emModSize = em.arguments.values.flatten.map(arg => arg.attachments.size).sum * 10
          val emAttachmentSet = em.arguments.values.flatten.flatMap(m => m.attachments).toSet
          (emSize, emModSize, emAttachmentSet)
        }
        case _ => (0, 0,  mention.attachments)
      }

      // disgusting!
      val attachArgumentsSz = attachmentsSet.toSeq.map(_.asInstanceOf[EidosAttachment].argumentSize).sum + mention.attachments.map(a=> attachmentTriggerLength(a)).sum
      // smart this up
      // problem: Quant("moderate to heavy", None) considered the same as Quant("heavy", none)
      // MAYBE merge them...? here maybe no bc string overlap... keep superset/longest
      // BUT: what about "persistent and heavy seasonal rainfall" -- Quant("persistent", None),  Quant("heavy", none)
      // want merged -> Quant("persistent", None), Quant("heavy", None) ??

      // may be helpful
      //tb.newWithAttachment()

      (mention, (attachArgumentsSz + modSize + numArgs)) // The size of a mention is the sum of i) how many attachments are present ii) sum of args in each of the attachments iii) if (EventMention) ==>then include size of arguments
    }



    val maxModAttachSz = mention_attachmentSz.map(_._2).max
    val filteredMentions = mention_attachmentSz.filter(m => m._2 == maxModAttachSz).map(_._1)
    filteredMentions
  }

  def attachmentTriggerLength(a: Attachment): Int = {
    a match {
      case inc:Increase => inc.trigger.length
      case dec: Decrease => dec.trigger.length
      case quant: Quantification => quant.quantifier.length
      case _ => throw new UnsupportedClassVersionError("Not a valid Attachment!")
    }
  }

  // remove incomplete EVENT Mentions
  def keepMostCompleteEvents(ms: Seq[Mention], state: State): Seq[Mention] = {

    val (events, nonEvents) = ms.partition(_.isInstanceOf[EventMention])
    val (textBounds, relationMentions) = nonEvents.partition(_.isInstanceOf[TextBoundMention])
    // remove incomplete entities (i.e. under specified when more fully specified exists)

    val tbMentionGroupings =
      textBounds.map(_.asInstanceOf[TextBoundMention]).groupBy(m => (m.tokenInterval, m.label, m.sentence))
    // remove incomplete mentions
    val completeTBMentions =
      for ((k, tbms) <- tbMentionGroupings) yield {
//        val maxModSize: Int = tbms.map(tbm => tbm.attachments.size).max
//        val filteredTBMs = tbms.filter(m => m.attachments.size == maxModSize)
        val filteredTBMs = customAttachmentFilter(tbms)

        filteredTBMs.head
      }

    // We need to remove underspecified EventMentions of near-duplicate groupings
    // (ex. same phospho, but one is missing a site)
    val eventMentionGroupings =
      events.map(_.asInstanceOf[EventMention]).groupBy(m => (m.label, m.tokenInterval, m.sentence))

    // remove incomplete mentions
    val completeEventMentions =
      for ((_, ems) <- eventMentionGroupings) yield {
        // max number of arguments
        val maxSize: Int = ems.map(_.arguments.values.flatten.size).max
        // max number of argument modifications
        // todo not all attachments are equal
//        val maxArgMods = ems.map(em => em.arguments.values.flatten.map(arg => arg.attachments.size).sum).max
//        val maxModSize: Int = ems.map(em => em.arguments.values.flatMap(ms => ms.map(_.modifications.size)).max).max
//        val filteredEMs = ems.filter(m => m.arguments.values.flatten.size == maxSize &&
//          m.arguments.values.flatMap(ms => ms.map(_.attachments.size)).sum == maxArgMods)
        val filteredEMs = customAttachmentFilter(ems)
        filteredEMs.head
      }

    completeTBMentions.toSeq ++ relationMentions ++ completeEventMentions.toSeq
  }

  //Rule to apply quantifiers directly to the state of an Entity (e.g. "small puppies") and
  //Rule to add Increase/Decrease to the state of an entity
  //TODO Heather: write toy test for this
  //TODO: perhaps keep token interval of the EVENT because it will be longer?
  def applyAttachment(ms: Seq[Mention], state: State): Seq[Mention] = for {
    m <- ms
    //if m matches "EntityModifier"
    attachment = getAttachment(m)

    copyWithMod = m match {
      case tb: TextBoundMention => tb.copy(attachments = tb.attachments ++ Set(attachment), foundBy = s"${tb.foundBy}++mod")
      // Here, we want to keep the theme that is being modified, not the modification event itself
      case rm: RelationMention =>
        val theme = rm.arguments("theme").head.asInstanceOf[TextBoundMention]
        theme.copy(attachments = theme.attachments ++ Set(attachment), foundBy = s"${theme.foundBy}++${rm.foundBy}")
      case em: EventMention =>
        val theme = em.arguments("theme").head.asInstanceOf[TextBoundMention]
        theme.copy(attachments = theme.attachments ++ Set(attachment), foundBy = s"${theme.foundBy}++${em.foundBy}")
    }
  } yield copyWithMod

  def debug(ms: Seq[Mention], state: State): Seq[Mention] = {
    println("DEBUG ACTION")
    ms
  }

  def getAttachment(mention: Mention): EidosAttachment = EidosAttachment.newEidosAttachment(mention)

  // Currently used as a GLOBAL ACTION in EidosSystem:
  // Merge many Mentions of a single entity that have diff attachments, so that you have only one entity with
  // all the attachments.  Also handles filtering of attachments of the same type whose triggers are substrings
  // of each other.
  def mergeAttachments(mentions: Seq[Mention], state: State): Seq[Mention] = {
    val (entities, nonentities) = mentions.partition(m => m matches "Entity")

    // Get all the entity mentions for this span (i.e. all the "rainfall in Spain" mentions)
    val spanGroup = entities.groupBy(m => (m.sentence, m.tokenInterval, m.label))
    val mergedEntities = for {
      (span, group) <- spanGroup
      // Get all attachments for these mentions
      attachments = group.flatMap(m1 => m1.attachments)
      // filter them
      filtered = filterAttachments(attachments)
      // Make a new mention with these attachments
      exampleMention = group.head
    } yield copyWithAttachments(exampleMention, filtered)

    mergedEntities.toSeq ++ nonentities
  }

  // Iteratively creates a mention which contains all of the passed in Attachments
  def copyWithAttachments(m: Mention, attachments: Seq[Attachment]): Mention = {
    var outMention = m
    for {
      a <- attachments
    } outMention = outMention.withAttachment(a)
    outMention
  }

  // Filter out substring attachments, then keep most complete
  def filterAttachments(attachments: Seq[Attachment]): Seq[Attachment] = {
    // Filter out substring attachments
    val attachmentGroup = attachments.groupBy(a => a.getClass)
    val filtered = for {
      (classType, attachmentsToCondense) <- attachmentGroup
      filtered = filterSubstringTriggers(attachmentsToCondense)
    } yield filtered
    // Now that substrings are filtered... keep only most complete of each type-trigger-combo
    val groupedByTriggerToo = filtered.flatten.groupBy(a => typeAndTrigger(a))
    val mostCompleteAttachments = for {
      (typeAndTrigg, attachmentsToCondense2) <- groupedByTriggerToo
    } yield mostComplete(attachmentsToCondense2.toSeq)

    mostCompleteAttachments.toSeq
  }
  // Keep the most complete attachment here.
  protected def mostComplete(as: Seq[Attachment]): Attachment = {
    val most = as.maxBy(_.asInstanceOf[EidosAttachment].argumentSize)
    most
  }
  // Filter out substring attachments
  protected def filterSubstringTriggers(as: Seq[Attachment]): Seq[Attachment] = {
    // sorted longest first
    val sorted = as.sortBy(a => -triggerOf(a).length)
    val triggersKept = scala.collection.mutable.Set[String]()
    val out = new ArrayBuffer[Attachment]

    for (a <- sorted) {
      if (!isSubstring(triggerOf(a), triggersKept.toSet)) {
        // add this trigger
        triggersKept.add(triggerOf(a))
        // keep the attachment
        out.append(a)
      }
    }
    out
  }
  // Check if current string is a subtring in our string set
  def isSubstring(current: String, kept: Set[String]): Boolean = {
    for (k <- kept) {
      if (k.contains(current)) {
        return true
      }
    }
    false
  }
  // Get trigger from an attachment
  protected def triggerOf(a: Attachment): String = {
    a match {
      case inc: Increase => inc.trigger
      case dec: Decrease => dec.trigger
      case quant: Quantification => quant.quantifier
      case _ => throw new UnsupportedClassVersionError()
    }
  }
  // Get type and trigger of an attachment.
  protected def typeAndTrigger(a: Attachment): (String, String) = {
    a match {
      case inc: Increase => ("Increase", inc.trigger)
      case dec: Decrease => ("Decrease", dec.trigger)
      case quant: Quantification => ("Quantification", quant.quantifier)
      case _ => throw new UnsupportedClassVersionError()
    }
  }

}

object EidosActions extends Actions {

  def apply(taxonomyPath: String) =
    new EidosActions(readTaxonomy(taxonomyPath))

  private def readTaxonomy(path: String): Taxonomy = {
    val input = FileUtils.getTextFromResource(path)
    val yaml = new Yaml(new Constructor(classOf[java.util.Collection[Any]]))
    val data = yaml.load(input).asInstanceOf[java.util.Collection[Any]]
    Taxonomy(data)
  }
}
