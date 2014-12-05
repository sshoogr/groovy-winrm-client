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

import org.junit.Test
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class WinRMClientConnectedTest {

  @Test
  void testIsConnected() {
    def client = new WinRMClient(protocol:'http', host:'127.0.0.1', user:'vagrant', password:'vagrant', port:5985)
    assertFalse client.connected
    client.shellId = '11112222-3333-4444-ACDC-THUNDERSTRUC'
    assertTrue client.connected
  }
}
