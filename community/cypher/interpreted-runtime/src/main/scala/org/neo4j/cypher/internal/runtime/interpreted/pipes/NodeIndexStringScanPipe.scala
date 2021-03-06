/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.interpreted.pipes

import org.neo4j.cypher.internal.runtime.interpreted.ExecutionContext
import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Expression
import org.neo4j.cypher.internal.runtime.{QueryContext, ResultCreator}
import org.neo4j.cypher.internal.v3_5.logical.plans.IndexedProperty
import org.neo4j.internal.kernel.api.IndexReference
import org.neo4j.values.storable.{TextValue, Values}
import org.opencypher.v9_0.expressions.LabelToken
import org.opencypher.v9_0.util.CypherTypeException
import org.opencypher.v9_0.util.attribution.Id

abstract class AbstractNodeIndexStringScanPipe(ident: String,
                                               label: LabelToken,
                                               property: IndexedProperty,
                                               valueExpr: Expression) extends Pipe with IndexPipeWithValues {

  override val propertyIndicesWithValues: Array[Int] = if (property.shouldGetValue) Array(0) else Array.empty
  override val propertyNamesWithValues: Array[String] = Array(ident + "." + property.propertyKeyToken.name)
  protected val needsValues = propertyIndicesWithValues.nonEmpty

  private var reference: IndexReference = IndexReference.NO_INDEX

  private def reference(context: QueryContext): IndexReference = {
    if (reference == IndexReference.NO_INDEX) {
      reference = context.indexReference(label.nameId.id, property.propertyKeyToken.nameId.id)
    }
    reference
  }

  valueExpr.registerOwningPipe(this)

  override protected def internalCreateResults(state: QueryState): Iterator[ExecutionContext] = {
    val baseContext = state.createOrGetInitialContext(executionContextFactory)
    val value = valueExpr(baseContext, state)

    val resultNodes = value match {
      case value: TextValue =>
        queryContextCall(state, reference(state.query), value.stringValue(), CtxResultCreatorWithValues(baseContext))
      case Values.NO_VALUE =>
        Iterator.empty
      case x => throw new CypherTypeException(s"Expected a string value, but got $x")
    }

    resultNodes
  }

  protected def queryContextCall(state: QueryState,
                                 indexReference: IndexReference,
                                 value: String,
                                 resultCreator: ResultCreator[ExecutionContext]): Iterator[ExecutionContext]
}

case class NodeIndexContainsScanPipe(ident: String,
                                     label: LabelToken,
                                     property: IndexedProperty,
                                     valueExpr: Expression)
                                    (val id: Id = Id.INVALID_ID)
  extends AbstractNodeIndexStringScanPipe(ident, label, property, valueExpr) {

  override protected def queryContextCall(state: QueryState,
                                          indexReference: IndexReference,
                                          value: String,
                                          resultCreator: ResultCreator[ExecutionContext]): Iterator[ExecutionContext] =
    state.query.indexSeekByContains(indexReference, needsValues, resultCreator, value)
}

case class NodeIndexEndsWithScanPipe(ident: String,
                                     label: LabelToken,
                                     property: IndexedProperty,
                                     valueExpr: Expression)
                                    (val id: Id = Id.INVALID_ID)
  extends AbstractNodeIndexStringScanPipe(ident, label, property, valueExpr) {

  override protected def queryContextCall(state: QueryState,
                                          indexReference: IndexReference,
                                          value: String,
                                          resultCreator: ResultCreator[ExecutionContext]): Iterator[ExecutionContext] =
    state.query.indexSeekByEndsWith(indexReference, needsValues, resultCreator, value)
}
