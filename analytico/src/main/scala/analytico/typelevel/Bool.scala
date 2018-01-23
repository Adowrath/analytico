package analytico
package typelevel

import scala.language.higherKinds

/**
  * Ein Type-Level Boolean.
  *
  * @see [[Bool.True$ True]]
  * @see [[Bool.False$ False]]
  * @author Cyrill
  * @since 08.01.2018
  * @contentDiagram
  * @inheritanceDiagram hideSuperclasses
  *
  * @define DottyUnion [[http://dotty.epfl.ch/docs/reference/union-types.html Union-Types]]
  */
sealed trait Bool {
  /**
    * Wenn [[Bool.True True]], gibt `Then` zurück, wenn [[Bool.False False]], dann `Else`.
    *
    * `Upper` wird ab Dottys $DottyUnion mit `Then|Else` ersetzt werden können.
    *
    * @tparam Then  Rückgabetyp falls dies [[Bool.True]] ist.
    * @tparam Else  Rückgabetyp falls dies [[Bool.False]] ist.
    * @tparam Upper Gemeinsamer Supertype von `Then` und `Else`. Wenn Dotty erscheint, wird dieser entfernt und mit `Then|Else` ersetzt.
    */
  type If[Then <: Upper, Else <: Upper, Upper] <: Upper
  /** Eine Type-Level-Repräsentation des boolschen Not-Operators. */
  type Not <: Bool
  /** Eine Type-Level-Repräsentation des boolschen And-Operators. */
  type And[O <: Bool] <: Bool
  /** Eine Type-Level-Repräsentation des boolschen Or-Operators. */
  type Or[O <: Bool] <: Bool
  /** Eine Type-Level-Repräsentation des boolschen Xor-Operators. */
  type Xor[O <: Bool] <: Bool
  /** Eine Type-Level-Repräsentation des Gleichheits-Operators. */
  type Equals[O <: Bool] <: Bool

  /** Die Methodische Repräsentation der If-Expression.
    * @see [[Bool.If]] */
  def If[Then <: Upper, Else <: Upper, Upper](t: ⇒ Then, e: ⇒ Else): If[Then, Else, Upper]
  /** Die Methodische Repräsentation des Not-Operators.
    * @see [[Bool.Not]] */
  def Not: Not
  /** Die Methodische Repräsentation des And-Operators.
    * @see [[Bool.And]] */
  def And(o: Bool): And[o.type]
  /** Die Methodische Repräsentation des Or-Operators.
    * @see [[Bool.Or]] */
  def Or(o: Bool): Or[o.type]
  /** Die Methodische Repräsentation des Xor-Operators.
    * @see [[Bool.Xor]] */
  def Xor(o: Bool): Xor[o.type]
  /** Die Methodische Repräsentation des Equals-Operators.
    * @see [[Bool.Equals]] */
  def Equals(o: Bool): Equals[o.type]
}

object Bool {

  type True = True.type
  type False = False.type

  /**
    * Ein Type-Level Bool mit dem Wert `wahr` bzw. `true`.
    */
  case object True extends Bool {
    /**
      * @usecase type If[Then, _1, _2] = Then
      *
      *          Gibt den ersten Typ, `Then`, zurück.
      */
    override type If[Then <: Upper, Else <: Upper, Upper] = Then
    override type Not = False
    override type And[O <: Bool] = O
    override type Or[O <: Bool] = True
    override type Xor[O <: Bool] = O#Not
    override type Equals[O <: Bool] = O

    override def If[Then <: Upper, Else <: Upper, Upper](t: ⇒ Then, e: ⇒ Else): Then = t
    override def Not: False = False
    override def And(o: Bool): o.type = o
    override def Or(o: Bool): True = True
    override def Xor(o: Bool): o.Not = o.Not
    override def Equals(o: Bool): o.type = o
  }

  /**
    * Ein Type-Level Bool mit dem Wert `falsch` bzw. `false`.
    */
  case object False extends Bool {
    /**
      * @usecase type If[_0, Else, _2] = Else
      *
      *          Gibt den zweiten Typ, `Else`, zurück.
      */
    override type If[Then <: Upper, Else <: Upper, Upper] = Else
    override type Not = True
    override type And[O <: Bool] = False
    override type Or[O <: Bool] = O
    override type Xor[O <: Bool] = O
    override type Equals[O <: Bool] = O#Not

    override def If[Then <: Upper, Else <: Upper, Upper](t: ⇒ Then, e: ⇒ Else): Else = e
    override def Not: True = True
    override def And(o: Bool): False = False
    override def Or(o: Bool): o.type = o
    override def Xor(o: Bool): o.type = o
    override def Equals(o: Bool): this.Equals[o.type] = o.Not
  }

}
