package analytico.youtube.apis

class ApiParameter(val name: String, val isPart: Boolean) {
  def repr: String = name

  def getParts: Seq[ApiParameter] =
    if(isPart)
      this :: Nil
    else
      Nil
}

class ApplicableParameter[Parameter <: Singleton](name: String,
                                                  parameterSupply: Parameter,
                                                  isPart: Boolean,
                                                  innerParameters: ApiParameter*)
  extends ApiParameter(name, isPart) {

  def apply(subItems: (Parameter ⇒ ApiParameter)*): ApiParameter = {
    new ApplicableParameter(name, parameterSupply, isPart, subItems.map(_ (parameterSupply)): _*)
  }

  override def repr: String = innerParameters.length match {
    case 0 ⇒ super.repr
    case 1 ⇒ s"${super.repr}/${innerParameters.head.repr}"
    case _ ⇒ innerParameters.map(_.repr).mkString(s"${super.repr}(", ", ", ")")
  }

  override def getParts: Seq[ApiParameter] =
    if(isPart) {
      new ApiParameter(name, isPart) :: (List[ApiParameter]() /: innerParameters) (_ ++ _.getParts)
    } else {
      (List[ApiParameter]() /: innerParameters) (_ ++ _.getParts)
    }
}
