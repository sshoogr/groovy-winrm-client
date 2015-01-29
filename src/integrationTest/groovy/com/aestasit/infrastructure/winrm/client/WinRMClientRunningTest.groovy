/*
 * Copyright (C) 2011-2015 Aestas/IT
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

import com.aestasit.infrastructure.winrm.WinRMException
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.*

/**
 * Simple tests to check WinRM client via HTTP
 *
 * @author Sergey Korenko
 */
class WinRMClientRunningTest {

  static WinRMClient client

  @BeforeClass
  static void initClient() {
    client = new WinRMClient(protocol: 'http', host: '192.168.25.25', port: 5985, user: 'vagrant', password: 'vagrant')
  }

  @Test
  void testOpenClose() {
    def shellId = client.openShell()
    assertNotNull 'Correctly opened shell ', shellId
    assertEquals 36, shellId.length()
    client.deleteShell()
  }

  @Test
  void testFictionalCommand() {
    client.openShell()
    def commandId = client.executeCommand('pong')
    assertEquals 36, commandId.length()
    def output = client.commandExecuteResults(commandId)
    assertEquals 1, output.exitStatus
    assertNotNull output.errorOutput
    client.deleteShell()
  }

  @Test(expected = UnknownHostException.class)
  void testUnknownHost() {
    def newClient = new WinRMClient(host: 'NotExistingHost1234_', user: 'vagrant_12434', password: 'vagrant_978')
    def shellId = newClient.openShell()
    assertNull shellId
  }

  @Test(expected = WinRMException.class)
  void testWrongPrincipalCredential() {
    def newClient = new WinRMClient(protocol: 'http', host: '192.168.25.25', port: 5985, user: 'usr', password: 'pwd')
    def shellId = newClient.openShell()
    assertNull shellId
  }

}