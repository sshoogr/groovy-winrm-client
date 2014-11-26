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

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import static com.aestasit.infrastructure.winrm.client.util.Defines.*

/**
 * Integration class to test WinRM client via HTTPS
 *
 * @author Sergey Korenko
 */
class WinRMHttpsClientRunningTest{
  static WinRMClient client
  @BeforeClass
  static void initClient(){
    client = new WinRMClient(PROTOCOL_HTTPS, '192.168.56.102', 5986, 'vagrant', 'vagrant')
    client.initialize()
  }

  @Test
  void testExecute() {
    CommandOutput output = client.execute('hostname')

    assertEquals 0, output.exitStatus
    assertTrue output.output.contains('vagrant')
    assertTrue !output.errorOutput
  }
}
