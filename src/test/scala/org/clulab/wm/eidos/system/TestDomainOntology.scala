package org.clulab.wm.eidos.system

import org.clulab.processors.fastnlp.FastNLPProcessor
import org.clulab.wm.eidos.groundings.CompactDomainOntology.CompactDomainOntologyBuilder
import org.clulab.wm.eidos.groundings._
import org.clulab.wm.eidos.test.TestUtils._
import org.clulab.wm.eidos.utils.Timer

class TestDomainOntology extends Test {

  def hasDuplicates(name: String, domainOntology: DomainOntology): Boolean = {
    val pathSeq = 0.until(domainOntology.size).map { i => domainOntology.getNamer(i).name }
    val pathSet = pathSeq.toSet

//    println(s"""The domain ontology "${domainOntology.name}" node count: ${ontologyNodes.length}""")
//    ontologyNodes.foreach(println)

    if (pathSeq.size != pathSet.size) {
      val pathBag = pathSeq.foldLeft(Map[String, Int]())((map, path) => map + (path -> (map.getOrElse(path, 0) + 1)))
      val duplicatePaths = pathBag.toSeq.filter(_._2 > 1).map(_._1)

      println(s"""The domain ontology "$name" includes duplicate nodes:""")
      duplicatePaths.foreach(println)
      true
    }
    else
      false
  }

  val baseDir = "/org/clulab/wm/eidos/english/ontologies"
  val proc = new FastNLPProcessor()
  val filter = true


  def show1(ontology: DomainOntology): Unit = {
    0.until(ontology.size).foreach { i =>
      println(ontology.getNamer(i).name + " = " + ontology.getValues(i).mkString(", "))
    }
    println
  }

  def show3(newOntology: DomainOntology, newerOntology: DomainOntology, newestOntology: DomainOntology): Unit = {
    show1(newOntology)
    show1(newerOntology)
    show1(newestOntology)
  }

  def cachePath(name: String): String = s"./cache/english/${name}.serialized"

  behavior of "un ontology"
  it should "load and not have duplicates" in {
    val path = baseDir + "/un_ontology.yml"

    val newOntology = Timer.time("Load UN without cache") {
      UNOntology(path, "", proc, filter, useCache = false)
    }
    val newerOntology = Timer.time("Load UN with cache") {
      //UNOntology(path, "", proc, filter, loadSerialized = true)
      new CompactDomainOntologyBuilder(newOntology.asInstanceOf[TreeDomainOntology]).build
    }

//    val newestOntology = Timer.time("Load UN with cache") {
//      UNOntology("", cachePath("un"), proc, filter, loadSerialized = true)
//    }
//
//    show3(newOntology, newerOntology, newestOntology)

    hasDuplicates("un", newOntology) should be (false)
    hasDuplicates("un", newerOntology) should be (false)
  }

  behavior of "fao ontology"
  it should "load and not have duplicates" in {
    val path = baseDir + "/fao_variable_ontology.yml"

    val newOntology = Timer.time("Load FAO without cache") {
      FAOOntology(path, "", proc, filter, useCache =false)
    }
    val newerOntology = Timer.time("Load FAO with cache") {
      //FAOOntology(path, "", proc, filter, loadSerialized = true)
      new CompactDomainOntologyBuilder(newOntology.asInstanceOf[TreeDomainOntology]).build
    }
//    val newestOntology = Timer.time("Load FAO with cache") {
//      FAOOntology("", cachePath("fao"), proc, filter, loadSerialized = true)
//    }
//
//    show3(newOntology, newerOntology, newestOntology)

    hasDuplicates("fao", newOntology) should be (false)
    hasDuplicates("fao", newerOntology) should be (false)
  }

  behavior of "wdi ontology"
  it should "load and not have duplicates" in {
    val path = baseDir + "/wdi_ontology.yml"

    val newOntology = Timer.time("Load WDI without cache") {
      WDIOntology(path, "", proc, filter, useCache = false)
    }
    val newerOntology = Timer.time("Load WDI with cache") {
      //WDIOntology(path, "", proc, filter, loadSerialized = true)
      new CompactDomainOntologyBuilder(newOntology.asInstanceOf[TreeDomainOntology]).build
    }
//    val newestOntology = Timer.time("Load WDI with cache") {
//      WDIOntology("", cachePath("wdi"), proc, filter, loadSerialized = true)
//    }
//
//    show3(newOntology, newerOntology, newestOntology)

    hasDuplicates("wdi", newOntology) should be (false)
    hasDuplicates("wdi", newerOntology) should be (false)
  }

  // TODO: This one appears to have many duplicates.
  behavior of "topoFlow ontology"
  ignore should "load and not have duplicates" in {
    val path = baseDir + "/topoflow_ontology.yml"

    val newOntology = Timer.time("Load TOPO without cache") {
      TopoFlowOntology(path, "", proc, filter, useCache = false)
    }

    hasDuplicates("topo", newOntology) should be (false)
  }

  behavior of "mesh ontology"
  it should "load and not have duplicates" in {
    val path = baseDir + "/mesh_ontology.yml"

    val newOntology = Timer.time("Load MeSH without cache") {
      MeshOntology(path, "", proc, filter, useCache = false)
    }
    val newerOntology = Timer.time("Load MeSH with cache") {
      //MeshOntology(path, "", proc, filter, loadSerialized = true)
      new CompactDomainOntologyBuilder(newOntology.asInstanceOf[TreeDomainOntology]).build
    }
//    val newestOntology = Timer.time("Load MeSH with cache") {
//      MeshOntology("", cachePath("mesh"), proc, filter, loadSerialized = true)
//    }

//    show3(newOntology, newerOntology, newestOntology)

    hasDuplicates("mesh", newOntology) should be (false)
    hasDuplicates("mesh", newerOntology) should be (false)
  }
}
