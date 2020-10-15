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
package com.aestasit.infrastructure.winrm.client.https

import org.apache.http.conn.ssl.X509HostnameVerifier

import static org.apache.http.conn.ssl.SSLSocketFactory.*

/**
 * Provides host names verification strategies for https connection.
 *
 * @author Sergey Korenko
 */
enum HostStrategy {

  ALLOW_STRICT(STRICT_HOSTNAME_VERIFIER),
  ALLOW_BROWSER_COMPATIBLE(BROWSER_COMPATIBLE_HOSTNAME_VERIFIER),
  ALLOW_ALL(ALLOW_ALL_HOSTNAME_VERIFIER)

  private HostStrategy(X509HostnameVerifier verifier) {
    this.verifier = verifier
  }

  X509HostnameVerifier verifier

}