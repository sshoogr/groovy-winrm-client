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
package com.aestasit.infrastructure.winrm.server

import org.apache.commons.io.IOUtils
import org.junit.Ignore
import org.mortbay.jetty.Handler
import org.mortbay.jetty.HttpConnection
import org.mortbay.jetty.Request
import org.mortbay.jetty.Server
import org.mortbay.jetty.handler.AbstractHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.apache.commons.io.IOUtils.write

/**
 * HTTP server implementation for use during WinRM tests.
 *
 * @author Sergey Korenko
 */
@Ignore
public class HttpTestServer {

  public static final int HTTP_PORT = 5985
  public static final int HTTPS_PORT = 5986

  Server server
  String responseBody
  String requestBody
  String mockResponseData

  public void start(useHTTP) throws Exception {
    server = new Server(useHTTP ? HTTP_PORT : HTTPS_PORT)
    server.handler = getMockHandler()
    server.start()
  }

  public Handler getMockHandler() {
    new AbstractHandler() {
      public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        Request baseRequest = request instanceof Request ? request : HttpConnection.currentConnection.request
        responseBody = mockResponseData
        requestBody = IOUtils.toString(baseRequest.inputStream)
        response.status = SC_OK
        response.contentType = "text/xml;charset=utf-8"
        write responseBody, response.outputStream
        baseRequest.handled = true
      }
    }
  }

  public void stop() throws Exception {
    server.stop()
  }
}