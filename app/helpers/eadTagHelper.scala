package helpers

object EadTagHelper {
  def hi(): String = { "hi" }

  def stripEadTags(input: String): String = {

    val i1 = """<emph render\=\"italic\">""".r.replaceAllIn(input, "")

    """<\/emph>""".r.replaceAllIn(i1, "")
  }
}