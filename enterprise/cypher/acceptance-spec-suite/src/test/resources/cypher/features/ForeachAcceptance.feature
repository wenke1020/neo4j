#
# Copyright (c) 2002-2018 "Neo4j,"
# Neo4j Sweden AB [http://neo4j.com]
#
# This file is part of Neo4j Enterprise Edition. The included source
# code can be redistributed and/or modified under the terms of the
# GNU AFFERO GENERAL PUBLIC LICENSE Version 3
# (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
# Commons Clause, as found in the associated LICENSE.txt file.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# Neo4j object code can be licensed independently from the source
# under separate terms from the AGPL. Inquiries can be directed to:
# licensing@neo4j.com
#
# More information is also available at:
# https://neo4j.com/licensing/
#

#encoding: utf-8

Feature: ForeachAcceptance

  Scenario: Add labels inside FOREACH
    Given an empty graph
    When executing query:
      """
      CREATE (a), (b), (c)
      WITH [a, b, c] AS nodes
      FOREACH(n IN nodes |
        SET n :Foo:Bar
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes  | 3 |
      | +labels | 2 |
    When executing control query:
      """
      MATCH (n)
      WHERE NOT(n:Foo AND n:Bar)
      RETURN n
      """
    Then the result should be:
      | n |

  Scenario: Merging inside a FOREACH using a previously matched node
    Given an empty graph
    And having executed:
      """
      CREATE (s:S)
      CREATE (s)-[:FOO]->({prop: 2})
      """
    When executing query:
      """
      MATCH (a:S)
      FOREACH(x IN [1, 2, 3] |
        MERGE (a)-[:FOO]->({prop: x})
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes         | 2 |
      | +relationships | 2 |
      | +properties    | 2 |

  Scenario: Merging inside a FOREACH using a previously matched node and a previously merged node
    Given an empty graph
    And having executed:
      """
      CREATE (:S)
      CREATE (:End {prop: 42})
      """
    When executing query:
      """
      MATCH (a:S)
      FOREACH(x IN [42] |
        MERGE (b:End {prop: x})
        MERGE (a)-[:FOO]->(b)
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +relationships | 1 |

  Scenario: Merging inside a FOREACH using two previously merged nodes
    Given an empty graph
    And having executed:
      """
      CREATE ({x: 1})
      """
    When executing query:
      """
      FOREACH(v IN [1, 2] |
        MERGE (a {x: v})
        MERGE (b {y: v})
        MERGE (a)-[:FOO]->(b)
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes         | 3 |
      | +relationships | 2 |
      | +properties    | 3 |

  Scenario: Merging inside a FOREACH using two previously merged nodes that also depend on WITH
    Given an empty graph
    When executing query:
      """
      WITH 3 AS y
      FOREACH(x IN [1, 2] |
        MERGE (a {x: x, y: y})
        MERGE (b {x: x + 1, y: y})
        MERGE (a)-[:FOO]->(b)
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes         | 3 |
      | +relationships | 2 |
      | +properties    | 6 |

  Scenario: Inside nested FOREACH
    Given an empty graph
    When executing query:
      """
      FOREACH(x IN [0, 1, 2] |
        FOREACH(y IN [0, 1, 2] |
          MERGE (a {x: x, y: y})
          MERGE (b {x: x + 1, y: y})
          MERGE (c {x: x, y: y + 1})
          MERGE (d {x: x + 1, y: y + 1})
          MERGE (a)-[:R]->(b)
          MERGE (a)-[:R]->(c)
          MERGE (b)-[:R]->(d)
          MERGE (c)-[:R]->(d)
        )
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes         | 16 |
      | +relationships | 24 |
      | +properties    | 32 |

  Scenario: Inside nested FOREACH, nodes inlined
    Given an empty graph
    When executing query:
      """
      FOREACH(x IN [0, 1, 2] |
        FOREACH(y IN [0, 1, 2] |
          MERGE (a {x: x, y: y})-[:R]->(b {x: x + 1, y: y})
          MERGE (c {x: x, y: y + 1})-[:R]->(d {x: x + 1, y: y + 1})
          MERGE (a)-[:R]->(c)
          MERGE (b)-[:R]->(d)
        )
      )
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes         | 24 |
      | +relationships | 30 |
      | +properties    | 48 |

  Scenario: Should handle running merge inside a foreach loop
    Given an empty graph
    When executing query:
      """
      FOREACH(x IN [1, 2, 3] | MERGE ({property: x}))
      """
    Then the result should be empty
    And the side effects should be:
      | +nodes      | 3 |
      | +properties | 3 |

  Scenario: Merge inside foreach should see variables introduced by update actions outside foreach
    Given an empty graph
    When executing query:
      """
      CREATE (a {name: 'Start'})
      FOREACH(x IN [1, 2, 3] | MERGE (a)-[:X]->({id: x}))
      RETURN a.name
      """
    Then the result should be:
      | a.name  |
      | 'Start' |
    And the side effects should be:
      | +nodes         | 4 |
      | +relationships | 3 |
      | +properties    | 4 |

