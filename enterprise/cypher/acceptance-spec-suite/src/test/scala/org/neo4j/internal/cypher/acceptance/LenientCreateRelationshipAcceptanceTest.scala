/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.internal.cypher.acceptance

import org.neo4j.cypher.{ExecutionEngineFunSuite, QueryStatisticsTestSupport}
import org.neo4j.graphdb.config.Setting
import org.neo4j.graphdb.factory.GraphDatabaseSettings
import org.neo4j.internal.cypher.acceptance.CypherComparisonSupport.Configs

class LenientCreateRelationshipAcceptanceTest extends ExecutionEngineFunSuite with QueryStatisticsTestSupport with CypherComparisonSupport {

  override def databaseConfig(): collection.Map[Setting[_], String] = super.databaseConfig() ++ Map(
    GraphDatabaseSettings.cypher_lenient_create_relationship -> "true"
  )

  // No CLG decision on this AFAIK, so not TCK material
  test("should silently not CREATE relationship if start-point is missing") {
    graph.execute("CREATE (a), (b)")

    val config = Configs.Version3_5 + Configs.Version3_4 - Configs.Compiled - Configs.AllRulePlanners
    val result = executeWith(config, """MATCH (a), (b)
                                       |WHERE id(a)=0 AND id(b)=1
                                       |OPTIONAL MATCH (b)-[:LINK_TO]->(c)
                                       |CREATE (b)-[:LINK_TO]->(a)
                                       |CREATE (c)-[r:MISSING_C]->(a)""".stripMargin)

    assertStats(result, relationshipsCreated = 1)
  }

  // No CLG decision on this AFAIK, so not TCK material
  test("should silently not CREATE relationship if end-point is missing") {
    graph.execute("CREATE (a), (b)")

    val config = Configs.Version3_5 + Configs.Version3_4 - Configs.Compiled - Configs.AllRulePlanners
    val result = executeWith(config, """MATCH (a), (b)
                                       |WHERE id(a)=0 AND id(b)=1
                                       |OPTIONAL MATCH (b)-[:LINK_TO]->(c)
                                       |CREATE (b)-[:LINK_TO]->(a)
                                       |CREATE (a)-[r:MISSING_C]->(c)""".stripMargin)

    assertStats(result, relationshipsCreated = 1)
  }

}
