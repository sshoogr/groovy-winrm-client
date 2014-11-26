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
class DeleteShellRequestTest extends BaseCreateRequest {
  @Test
  void testRequestCreation() {
    String requestString = new DeleteShellRequest(url, shellId).toString()
    assertTrue 'Delete shell request has to include ShellId reference',
            requestString.contains("<wsman:Selector Name='ShellId'>${shellId}</wsman:Selector>")
  }
}
