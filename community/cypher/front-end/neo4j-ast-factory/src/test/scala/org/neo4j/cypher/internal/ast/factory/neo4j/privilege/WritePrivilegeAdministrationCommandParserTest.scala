/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.ast.factory.neo4j.privilege

import org.neo4j.cypher.internal.ast
import org.neo4j.cypher.internal.ast.Statements
import org.neo4j.cypher.internal.ast.factory.neo4j.AdministrationAndSchemaCommandParserTestBase

class WritePrivilegeAdministrationCommandParserTest extends AdministrationAndSchemaCommandParserTestBase {

  Seq(
    ("GRANT", "TO", grantGraphPrivilege: noResourcePrivilegeFunc),
    ("DENY", "TO", denyGraphPrivilege: noResourcePrivilegeFunc),
    ("REVOKE GRANT", "FROM", revokeGrantGraphPrivilege: noResourcePrivilegeFunc),
    ("REVOKE DENY", "FROM", revokeDenyGraphPrivilege: noResourcePrivilegeFunc),
    ("REVOKE", "FROM", revokeGraphPrivilege: noResourcePrivilegeFunc)
  ).foreach {
    case (verb: String, preposition: String, func: noResourcePrivilegeFunc) =>
      Seq[Immutable](true, false).foreach {
        immutable =>
          val immutableString = immutableOrEmpty(immutable)
          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFoo)(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPHS foo $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFoo)(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          // Multiple graphs should be allowed (with and without plural GRAPHS)

          test(s"$verb$immutableString WRITE ON GRAPH * $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, ast.AllGraphsScope()(_))(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPHS * $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, ast.AllGraphsScope()(_))(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo, baz $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFooBaz)(pos),
              List(ast.ElementsAllQualifier() _),
              List(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPHS foo, baz $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFooBaz)(pos),
              List(ast.ElementsAllQualifier() _),
              List(literalRole),
              immutable
            ))
          }

          // Default and home graph should parse

          test(s"$verb$immutableString WRITE ON HOME GRAPH $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, ast.HomeGraphScope()(_))(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON DEFAULT GRAPH $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, ast.DefaultGraphScope()(_))(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          // Multiple roles should be allowed

          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition role1, role2") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFoo)(_),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole1, literalRole2),
              immutable
            ))
          }

          // Parameters and escaped strings should be allowed

          test(s"$verb$immutableString WRITE ON GRAPH $$foo $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeParamFoo)(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPH `f:oo` $preposition role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, ast.NamedGraphsScope(Seq(namespacedName("f:oo")))(_))(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition $$role") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFoo)(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(paramRole),
              immutable
            ))
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition `r:ole`") {
            yields[Statements](func(
              ast.GraphPrivilege(ast.WriteAction, graphScopeFoo)(pos),
              List(ast.ElementsAllQualifier() _),
              Seq(literalRColonOle),
              immutable
            ))
          }

          // Resource or qualifier should not be supported

          test(s"$verb$immutableString WRITE {*} ON GRAPH foo $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE {prop} ON GRAPH foo $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo NODE A $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo NODES * $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo RELATIONSHIP R $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo RELATIONSHIPS * $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo ELEMENT A $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo ELEMENTS * $preposition role") {
            failsToParse[Statements]
          }

          // Invalid/missing part of the command

          test(s"$verb$immutableString WRITE ON GRAPH f:oo $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition ro:le") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH foo $preposition") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE GRAPH foo $preposition role") {
            failsToParse[Statements]
          }

          // DEFAULT and HOME together with plural GRAPHS

          test(s"$verb$immutableString WRITE ON HOME GRAPHS $preposition role") {
            val offset = verb.length + immutableString.length + 15
            assertFailsWithMessage[Statements](
              testName,
              s"""Invalid input 'GRAPHS': expected "GRAPH" (line 1, column ${offset + 1} (offset: $offset))"""
            )
          }

          test(s"$verb$immutableString WRITE ON DEFAULT GRAPHS $preposition role") {
            val offset = verb.length + immutableString.length + 18
            assertFailsWithMessage[Statements](
              testName,
              s"""Invalid input 'GRAPHS': expected "GRAPH" (line 1, column ${offset + 1} (offset: $offset))"""
            )
          }

          // Default and home graph with named graph

          test(s"$verb$immutableString WRITE ON HOME GRAPH baz $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON DEFAULT GRAPH baz $preposition role") {
            failsToParse[Statements]
          }

          // Mix of specific graph and *

          test(s"$verb$immutableString WRITE ON GRAPH foo, * $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON GRAPH *, foo $preposition role") {
            failsToParse[Statements]
          }

          // Database instead of graph keyword

          test(s"$verb$immutableString WRITE ON DATABASES * $preposition role") {
            val offset = verb.length + immutableString.length + 10
            assertFailsWithMessage[Statements](
              testName,
              s"""Invalid input 'DATABASES': expected "DEFAULT", "GRAPH", "GRAPHS" or "HOME" (line 1, column ${offset + 1} (offset: $offset))"""
            )
          }

          test(s"$verb$immutableString WRITE ON DATABASE foo $preposition role") {
            val offset = verb.length + immutableString.length + 10
            assertFailsWithMessage[Statements](
              testName,
              s"""Invalid input 'DATABASE': expected "DEFAULT", "GRAPH", "GRAPHS" or "HOME" (line 1, column ${offset + 1} (offset: $offset))"""
            )
          }

          test(s"$verb$immutableString WRITE ON HOME DATABASE $preposition role") {
            failsToParse[Statements]
          }

          test(s"$verb$immutableString WRITE ON DEFAULT DATABASE $preposition role") {
            failsToParse[Statements]
          }
      }
  }
}
