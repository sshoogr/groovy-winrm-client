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

package com.aestasit.infrastructure.winrm.client.util

import org.junit.Test

/**
 * <code>Utils</code> class tests.
 *
 * @author Sergey Korenko
 */
class UtilsTest extends GroovyTestCase {

  @Test
  void testUrlBuild() {
    String expected = 'http://192.168.33.17:5985/wsman'
    String calculated = Utils.buildUrl('http', '192.168.33.17', 5985).toString()
    assertEquals expected, calculated
  }

}
