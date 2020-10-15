/*
 * Copyright (C) 2011-2020 Aestas/IT
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
package com.aestasit.infrastructure.winrm.client

import org.junit.Test

import static com.aestasit.infrastructure.winrm.client.util.Constants.PROTOCOL_HTTPS
import static org.junit.Assert.*

/**
 * Integration class to test WinRM client via HTTPS
 *
 * @author Sergey Korenko
 */
class WinRMHttpsClientRunningTest {

  @Test
  void testExecute() {

    WinRMClient client = new WinRMClient(PROTOCOL_HTTPS, '192.168.25.25', 5986, 'vagrant', 'vagrant')
    client.openShell()

    def commandId = client.executeCommand('hostname')
    assertEquals 36, commandId.length()

    def output = client.commandExecuteResults(commandId)
    assertEquals 0, output.exitStatus
    assertNotNull output.output
    assertTrue output.errorOutput.empty

    client.deleteShell()

  }

}
