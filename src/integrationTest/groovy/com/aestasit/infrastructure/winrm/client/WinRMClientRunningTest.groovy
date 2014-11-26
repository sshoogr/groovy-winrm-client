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

import org.junit.BeforeClass
import org.junit.Test

import static com.aestasit.infrastructure.winrm.client.util.Constants.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class WinRMClientRunningTest{
  static WinRMClient client

  @BeforeClass
  static void initClient(){
    client = new WinRMClient(host:'192.168.56.102', user:'vagrant', password:'vagrant')
    client.initialize()
  }

  @Test
  void testExecute() {
    client.shellTimeout = SHELL_DEFAULT_TIMEOUT
    CommandOutput output = client.execute('ver')
    assertEquals 0, output.exitStatus
    assertTrue output.output.contains('Microsoft Windows [Version 6.1.7601]')
    assertTrue !output.errorOutput
  }

  @Test
  void testLongOperation(){
    client.shellTimeout = 5000l
    CommandOutput output = client.execute('ping localhost', ['-n', '12'] as String[])
    assertEquals(1, output.exitStatus)
    assertTrue output.failed()
    assertTrue CMD_IS_STOPPED_BY_TIMEOUT == output.errorOutput
  }

  @Test
  void testFictionalCommand(){
    client.shellTimeout = SHELL_DEFAULT_TIMEOUT
    CommandOutput output = client.execute('dur')
    assertEquals 1, output.exitStatus
    assertTrue output.failed()
    assertTrue "'dur' is not recognized as an internal or external command,\r\noperable program or batch file.\r\n" == output.errorOutput
  }

  @Test
  void testNoConnection(){
    def newClient = new WinRMClient(host:'NotExistingHost', user:'vagrant', password:'vagrant')
    newClient.initialize()
    CommandOutput output = newClient.execute('dir')
    assertEquals 1, output.exitStatus
    assertTrue output.failed()
    assertTrue CMD_IS_TERMINATED_BY_EXCEPTION == output.errorOutput
    assertTrue 'java.net.UnknownHostException: NotExistingHost' == output.exception.toString()
  }
}
