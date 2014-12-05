/*
 * Copyright (C) 2011-2014 Aestas/IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aestasit.infrastructure.winrm.client.request

import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * WinRM execute request construction test.
 *
 * @author Sergey Korenko
 */
class ExecuteCommandRequestTest extends BaseCreateRequestTest {

  @Test
  void testRequestWithoutArguments() {
    String requestString = new ExecuteCommandRequest(url, shellId, 'ver').toString()
    assertTrue 'execute command request has to include ShellId reference',
        requestString.contains("<wsman:Selector Name='ShellId'>${shellId}</wsman:Selector>")
    assertTrue 'execute command request has to include command text <rsp:Command>___COMMAND___</rsp:Command>',
        requestString.contains("<rsp:Command>ver</rsp:Command>")
  }

  @Test
  void testRequestArguments() {
    String requestString = new ExecuteCommandRequest(url, shellId, 'ping', ['-n', '12'] as String[]).toString()
    assertTrue 'execute command request has to include ShellId reference',
        requestString.contains("<wsman:Selector Name='ShellId'>${shellId}</wsman:Selector>")
    assertTrue 'execute command request has to include command text <rsp:Command>___COMMAND___</rsp:Command>',
        requestString.contains("<rsp:Command>ping</rsp:Command>")
    assertTrue 'execute command request has to include command attribute "-n"',
        requestString.contains("<rsp:Arguments>-n</rsp:Arguments>")
    assertTrue 'execute command request has to include command attribute "12"',
        requestString.contains("<rsp:Arguments>12</rsp:Arguments>")
  }

}