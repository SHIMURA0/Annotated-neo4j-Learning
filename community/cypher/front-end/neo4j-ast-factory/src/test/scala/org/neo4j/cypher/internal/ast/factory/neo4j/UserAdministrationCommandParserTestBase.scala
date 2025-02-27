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
package org.neo4j.cypher.internal.ast.factory.neo4j

import org.neo4j.cypher.internal.ast.ParameterName
import org.neo4j.cypher.internal.expressions.Parameter
import org.neo4j.cypher.internal.expressions.SensitiveStringLiteral
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.util.InputPosition

class UserAdministrationCommandParserTestBase extends AdministrationAndSchemaCommandParserTestBase {
  protected val userString = "user"
  protected val varUser: Variable = varFor(userString)
  protected val password: InputPosition => SensitiveStringLiteral = pw("password")
  protected val passwordNew: InputPosition => SensitiveStringLiteral = pw("new")
  protected val passwordCurrent: InputPosition => SensitiveStringLiteral = pw("current")
  protected val passwordEmpty: InputPosition => SensitiveStringLiteral = pw("")
  protected val paramPassword: Parameter = pwParam("password")
  protected val paramPasswordNew: Parameter = pwParam("newPassword")
  protected val paramPasswordCurrent: Parameter = pwParam("currentPassword")
  protected val paramDb: ParameterName = stringParamName("db")
  protected val pwParamString = s"$$password"
  protected val paramString = s"$$param"
  protected val paramAst: Parameter = stringParam("param")
}
