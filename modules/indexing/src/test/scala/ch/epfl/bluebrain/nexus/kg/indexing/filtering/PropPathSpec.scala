package ch.epfl.bluebrain.nexus.kg.indexing.filtering

import java.io.ByteArrayInputStream

import ch.epfl.bluebrain.nexus.kg.indexing.filtering.PropPath.{
  AlternativeSeqPath,
  PathZeroOrMore,
  PathZeroOrOne,
  PropPathError,
  SeqPath,
  UriPath,
  fromJena
}
import io.circe.Json
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.jena.sparql.path.PathParser
import org.scalatest.{Inspectors, Matchers, TryValues, WordSpecLike}

class PropPathSpec extends WordSpecLike with Matchers with Inspectors with TryValues {

  "A PropPath" should {
    val nx      = "http://127.0.0.1:8080/v0/voc/nexus/core/"
    val context = Json.obj("@context" -> Json.obj("nx" -> Json.fromString(nx)))
    val str     = context.noSpaces
    val model   = ModelFactory.createDefaultModel()
    RDFDataMgr.read(model, new ByteArrayInputStream(str.getBytes), Lang.JSONLD)

    val graph         = model.getGraph
    val prefixMapping = graph.getPrefixMapping

    "build a PathProp form a path with only one uri with prefix and no hoops" in {
      val path       = "nx:schema"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.success.value shouldEqual UriPath(s"${nx}schema")
    }

    "build a PathProp form a path with only one uri and no hoops" in {
      val path       = s"<${nx}schema>"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.success.value shouldEqual UriPath(s"${nx}schema")
    }

    "build a PathProp form a follow sequence of paths (3 hoops) with prefixes" in {
      val path       = "nx:schema / nx:schemaGroup ? / nx:name"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.success.value shouldEqual SeqPath(SeqPath(UriPath(s"${nx}schema"),
                                                                           PathZeroOrOne(s"${nx}schemaGroup")),
                                                                   UriPath(s"${nx}name"))
    }

    "build a PathProp form a follow sequence of paths (3 hoops)" in {
      val path       = s"<${nx}schema> / <${nx}schemaGroup> ? / <${nx}name>*"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.success.value shouldEqual SeqPath(SeqPath(UriPath(s"${nx}schema"),
                                                                           PathZeroOrOne(s"${nx}schemaGroup")),
                                                                   PathZeroOrMore(s"${nx}name"))
    }

    "build a PathProp form a alternate sequence of paths (3 hoops) with prefixes" in {
      val path       = "nx:schema / nx:schemaGroup ? | nx:name"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.success.value shouldEqual AlternativeSeqPath(
        SeqPath(UriPath(s"${nx}schema"), PathZeroOrOne(s"${nx}schemaGroup")),
        UriPath(s"${nx}name"))
    }

    "failed in building a PathProp with an unsupported path property" in {
      val path       = "nx:schema ^ nx:schemaGroup ? | nx:name"
      val parsedPath = PathParser.parse(path, prefixMapping)
      fromJena(parsedPath).toTry.failure.exception shouldBe a[PropPathError]
    }

  }

}