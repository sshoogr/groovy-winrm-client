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

import org.apache.http.conn.ssl.TrustSelfSignedStrategy

import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Provides trust strategy for https connection
 *
 * @author Sergey Korenko
 */
enum TrustStrategy {

  ALLOW_NONE(null),
  ALLOW_SELF_SIGNED(new TrustSelfSignedStrategy()),
  ALLOW_ALL(new TrustAllStrategy())

  private TrustStrategy(org.apache.http.conn.ssl.TrustStrategy strategy) {
    this.strategy = strategy
  }

  org.apache.http.conn.ssl.TrustStrategy strategy

  public static class TrustAllStrategy implements org.apache.http.conn.ssl.TrustStrategy {
    @Override
    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      true
    }
  }

}