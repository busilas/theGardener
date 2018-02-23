package services

import java.io.File
import java.util.{List => JList}

import gherkin.ast.GherkinDocument
import gherkin.{AstBuilder, Parser, ast}
import models._

import scala.collection.JavaConverters._
import scala.io.Source

class ComponentService {

  def parseFeatureFile(projectId: String, filePath: String): Feature = {
    val featureFile = new File(filePath)

    val id = s"$projectId/${featureFile.getName}"

    var branch = filePath.substring(filePath.indexOf(projectId + "/") + projectId.length + 1)
    branch = branch.substring(0, branch.indexOf("/"))

    val parser = new Parser[GherkinDocument](new AstBuilder())
    val gherkinDocument = parser.parse(Source.fromFile(featureFile).mkString)

    val comments = gherkinDocument.getComments.asScala.map(_.getText)
    val feature = gherkinDocument.getFeature
    val (tags, _, _, _) = mapGherkinTags(feature.getTags)


    val scenarios = feature.getChildren.asScala.zipWithIndex.flatMap {

      case (background: ast.Background, id: Int) => Some(Background(id, background.getKeyword, background.getName, trim(background.getDescription), mapGherkinSteps(background.getSteps)))


      case (scenario: ast.Scenario, id: Int) =>

        val (tags, abstractionLevel, caseType, workflowStep) = mapGherkinTags(scenario.getTags)

        Some(Scenario(id, tags, abstractionLevel, caseType, workflowStep, scenario.getKeyword, scenario.getName, trim(scenario.getDescription), mapGherkinSteps(scenario.getSteps)))


      case (scenario: ast.ScenarioOutline, id: Int) =>
        val (tags, abstractionLevel, caseType, workflowStep) = mapGherkinTags(scenario.getTags)

        Some(ScenarioOutline(id, tags, abstractionLevel, caseType, workflowStep, scenario.getKeyword, scenario.getName, trim(scenario.getDescription), mapGherkinSteps(scenario.getSteps), mapGherkinExamples(scenario.getExamples)))

      case _ => None
    }

    Feature(id, branch, tags, feature.getLanguage, feature.getKeyword, feature.getName, trim(feature.getDescription), scenarios, comments)
  }

  private def mapGherkinSteps(gherkinSteps: JList[ast.Step]): Seq[Step] = {
    gherkinSteps.asScala.zipWithIndex.map { case (step, id) =>
      val argument = step.getArgument match {
        case dataTable: ast.DataTable => dataTable.getRows.asScala.map(_.getCells.asScala.map(_.getValue))
        case tableRow: ast.TableRow => Seq(tableRow.getCells.asScala.map(_.getValue))
        case _ => Seq()
      }

      Step(id, step.getKeyword.trim, step.getText, argument)
    }
  }

  private def mapGherkinTags(gherkinTags: JList[ast.Tag]): (Seq[String], String, String, String) = {
    val tags = gherkinTags.asScala.map(_.getName.replace("@", ""))
    val abstractionLevel = Feature.abstractionLevels.intersect(tags.toSet).headOption.getOrElse("level_1")
    val caseType = Feature.caseTypes.intersect(tags.toSet).headOption.getOrElse("nominal_case")
    val workflowStep = Feature.workflowSteps.intersect(tags.toSet).headOption.getOrElse("valid")

    (tags, abstractionLevel, caseType, workflowStep)
  }

  private def mapGherkinExamples(gherkinExamples: JList[ast.Examples]): Seq[Examples] = {
    gherkinExamples.asScala.zipWithIndex.map { case (examples, id) =>

      val (tags, _, _, _) = mapGherkinTags(examples.getTags)
      val tableHeader = examples.getTableHeader.getCells.asScala.map(_.getValue)
      val tableBody = examples.getTableBody.asScala.map(_.getCells.asScala.map(_.getValue))

      Examples(id, tags, examples.getKeyword, trim(examples.getDescription), tableHeader, tableBody)
    }
  }

  private def trim(s: String) = Option(s).map(_.trim).getOrElse("")
}
