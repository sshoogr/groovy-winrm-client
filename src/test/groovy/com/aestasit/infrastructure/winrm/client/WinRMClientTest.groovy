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

package com.aestasit.infrastructure.winrm.client

import com.aestasit.infrastructure.winrm.server.HttpTestServer
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import java.util.concurrent.TimeoutException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * WinRM cleanup request construction test.
 *
 * @author Sergey Korenko
 */
class WinRMClientTest {

  HttpTestServer httpTestServer
  static WinRMClient client

  @BeforeClass
  static void initClient() {
    client = new WinRMClient(protocol: 'http', host: '127.0.0.1', user: 'vagrant', password: 'vagrant', port: 5985)
    client.initialize()
  }

  @Before
  void startHttpTestServer() {
    httpTestServer = new HttpTestServer()
    httpTestServer.start(true)
  }

  @After
  void shutdownHttpTestServer() {
    httpTestServer.stop()
  }

  @Test
  void testOpenShell() {
    httpTestServer.mockResponseData = WinRMClientTest.getClass().getResourceAsStream('/OpenShellResponse.xml').text
    String expectedShellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    String receivedShellId = client.openShell()
    assertEquals 'Wrong shellId returned by server!', expectedShellId, receivedShellId
  }

  @Test
  void testExecuteCommand() {
    httpTestServer.mockResponseData = WinRMClientTest.getClass().getResourceAsStream('/ExecuteCommandResponse.xml').text
    client.shellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    String expectedCommandId = '6521B144-B2A2-4129-AE91-85D38F7B76BB'
    String receivedCommandId = client.executeCommand('ver')
    assertEquals 'Wrong command id is got', expectedCommandId, receivedCommandId
  }

  @Test
  void testGetCommandOutput() {
    // response to 'ver' command execution
    httpTestServer.mockResponseData = WinRMClientTest.getClass().getResourceAsStream('/ExecutionResultsResponse.xml').text
    client.shellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    CommandOutput executionResults = client.getCommandExecutionResults('6521B144-B2A2-4129-AE91-85D38F7B76BB')
    int expectedExitCode = 0
    assertEquals expectedExitCode, executionResults.exitStatus

    assertTrue executionResults.output.contains('Microsoft Windows [Version 6.1.7601]')
    assertTrue !executionResults.errorOutput
  }

  @Test(expected = TimeoutException.class)
  void testGetCommandOutputByTimeout() {
    // command execution has to be terminated by timeout after 5 seconds
    httpTestServer.mockResponseData = WinRMClientTest.getClass().getResourceAsStream('/RunningExecutionResponse.xml').text
    client.shellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    client.shellTimeout = 5000l
    client.getCommandExecutionResults('6521B144-B2A2-4129-AE91-85D38F7B76BB')
  }

  @Test
  void testCloseShell() {
    httpTestServer.mockResponseData = WinRMClientTest.getClass().getResourceAsStream('/DeleteShellResponse.xml').text
    client.shellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    Boolean shellClosed = client.deleteShell()

    assertTrue shellClosed
  }
}
