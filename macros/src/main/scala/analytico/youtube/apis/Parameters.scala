package analytico
package youtube
package apis

/** Ein Api-Parameter einer YouTube-API.
  *
  * @param name   der Name des Parameters
  * @param isPart ist es auch ein Part?
  */
class ApiParameter(val name: String, val isPart: Boolean) {

  /** Ein String als Parameter für "setFields" Aufruf. */
  def repr: String = name

  /** @return Alle Parameter welche auch Parts sind. */
  def getParts: Seq[ApiParameter] =
    if(isPart)
      this :: Nil
    else
      Nil
}

/**
  * Ein Parameter mit Kind-Parametern.
  *
  * @param name            der Name des Parameters
  * @param parameterSupply das Singleton-Objekt, aus dem die Kind-Parameter stammen.
  * @param isPart          ist es auch ein Part?
  * @param innerParameters ein Aufruf von [[apply]] konstruiert ein neues Objekt mit den definierten Kind-Parametern.
  * @tparam Supply woher stammen die Kind-Parameter? Wird zu `parameterSupply.type`, aber auch von aussen sichtbar.
  */
class ApplicableParameter[Supply <: scala.Singleton](name: String,
                                                     val parameterSupply: Supply,
                                                     isPart: Boolean,
                                                     val innerParameters: ApiParameter*)
  extends ApiParameter(name, isPart) {

  /**
    * Erstellt einen neuen Parameter mit den gegebenen Kind-Parametern.
    *
    * @param subItems die `_.param` Aufrüfe für alle Kind-Parameter.
    *
    * @return einen neuen Parameter. Zwar ein [[ApplicableParameter]],
    *         aber `apply` soll darauf nicht mehr ausgeführt werden.
    */
  def apply(subItems: (Supply ⇒ ApiParameter)*): ApiParameter = {
    new ApplicableParameter(name, parameterSupply, isPart, subItems.map(_ (parameterSupply)): _*)
  }

  /** @inheritdoc
    * @example
    * {{{
    *   val parent = new ApplicableParameter("parent", parentParameters, false)
    *   object parentParameters {
    *     val child1 = new ApiParameter("child1", false)
    *     val child2 = new ApiParameter("child2", false)
    *   }
    *
    *   parent(_.child1)          .repr == "parent/child1"
    *   parent(_.child2)          .repr == "parent/child2"
    *   parent(_.child1, _.child2).repr == "parent(child1, child2)"
    *   parent                    .repr == "parent"
    * }}}
    */
  override def repr: String = innerParameters.length match {
    case 0 ⇒ super.repr
    case 1 ⇒ s"${super.repr}/${innerParameters.head.repr}"
    case _ ⇒ innerParameters.map(_.repr).mkString(s"${super.repr}(", ", ", ")")
  }

  override def getParts: Seq[ApiParameter] =
    (if(isPart)
      Seq(new ApiParameter(name, isPart))
    else
      Nil) ++ innerParameters.flatMap(_.getParts)

}
