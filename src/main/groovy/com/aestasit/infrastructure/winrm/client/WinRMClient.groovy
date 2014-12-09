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

import com.aestasit.infrastructure.winrm.WinRMException
import com.aestasit.infrastructure.winrm.client.https.HostStrategy
import com.aestasit.infrastructure.winrm.client.https.TrustStrategy
import com.aestasit.infrastructure.winrm.client.request.*
import com.aestasit.infrastructure.winrm.client.util.Utils
import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovyx.net.http.HTTPBuilder
import org.apache.commons.lang3.Validate
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.aestasit.infrastructure.winrm.client.util.Constants.*
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.POST

/**
 * WinRM client implementation.
 *
 * @author Sergey Korenko
 */
@Canonical(includes = ['protocol', 'host', 'port', 'user', 'password', 'requestTimeout', 'trustStrategy', 'verificationStrategy'])
class WinRMClient {

  private final Logger logger = LoggerFactory.getLogger(getClass().getPackage().getName())

  String protocol = PROTOCOL_HTTP
  String host
  int port = PORT_HTTP
  String user
  String password

  /** Timeout for single WinRM request in seconds. */
  int requestTimeout = REQUEST_DEFAULT_TIMEOUT

  TrustStrategy trustStrategy = TrustStrategy.ALLOW_SELF_SIGNED
  HostStrategy verificationStrategy = HostStrategy.ALLOW_ALL

  private URL toAddress
  private String shellId
  private HTTPBuilder httpBuilder

  private static final String MISSING_SHELL_ID = 'Command cannot be executed without open remote shell! Use the openShell() method to start new shell!'
  private static final String MISSING_COMMAND_ID = 'Command results cannot be retrieved without valid command ID! The executeCommand() method returns ID of the started command!'

  /**
   * Checks if shell is open for command execution on a remote host.
   *
   * @return <code>true</code> if shell has been opened by <code>openShell()</code> method
   * and has not been closed by <code>deleteShell()</code> method, otherwise <code>false</code>.
   */
  boolean isConnected() {
    shellId
  }

  /**
   * Creates WinRM shell for further execution of remote commands.
   *
   * @return id of the opened shell.
   */
  String openShell() {
    initialize()
    logger.debug 'Sending request to create WinRM Shell'
    String request = new OpenShellRequest(toAddress, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)
    shellId = results?.'*:Body'?.'*:ResourceCreated'?.'*:ReferenceParameters'?.'*:SelectorSet'?.'*:Selector'?.find {
      it.@Name == 'ShellId'
    }?.text()
    logger.debug "'Create WinRM Shell' request has been processed"
    if (!shellId) {
      logger.warn "Remote shell creation failed (shellId = null)"
    }
    shellId
  }

  /**
   * Runs remote command in currently open shell.
   *
   * @param command command text.
   * @param args arguments to run command.
   * @return command id corresponds to the transferred command.
   */
  String executeCommand(String command, String[] args = []) {

    logger.debug "Sending request to execute command in previously open shell with id=${shellId}"

    Validate.notNull(shellId, MISSING_SHELL_ID)

    String request = new ExecuteCommandRequest(toAddress, shellId, command, args, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)
    String commandId = results?.'*:Body'?.'*:CommandResponse'?.'*:CommandId'?.text()

    logger.debug "Request to execute command has been finsihed in previously open shell with id=${shellId}"

    commandId

  }

  /**
   * Retrieves results of the command execution on a remote host.
   *
   * @param commandId identify command id which output/error output will be retrieved.
   * @return CommandOutput containing output, error output, exit code of the command execution on a remote host.
   */
  CommandOutput commandExecuteResults(String commandId) {

    logger.debug "Reading output of command with id =[${commandId}] from shell with id=${shellId}"

    Validate.notNull(shellId, MISSING_SHELL_ID)
    Validate.notNull(commandId, MISSING_COMMAND_ID)

    String request = new GetCommandOutputRequest(toAddress, shellId, commandId, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)

    String commandOutputArr = ''
    String errOutputArr = ''

    results?.'*:Body'?.'*:ReceiveResponse'?.'*:Stream'?.findAll {
      it.@Name == 'stdout' && it.@CommandId == commandId
    }?.each { commandOutputArr += new String(it.text().decodeBase64()) }

    results?.'*:Body'?.'*:ReceiveResponse'?.'*:Stream'?.findAll {
      it.@Name == 'stderr' && it.@CommandId == commandId
    }?.each { errOutputArr += new String(it.text().decodeBase64()) }

    if (results?.'*:Body'?.'*:ReceiveResponse'?.'*:CommandState'?.find {
      it.@CommandId == commandId && it.@State == 'http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Done'
    }) {
      Integer exitStatus = results?.'*:Body'?.'*:ReceiveResponse'?.'*:CommandState'?.'*:ExitCode'?.text()?.toInteger()
      logger.debug "retrieve command output of command with id =[${commandId}] has been processed"
      return new CommandOutput(exitStatus, commandOutputArr, errOutputArr)
    } else {
      logger.debug "command with id =[${commandId}] from shell with id=${shellId} is still RUNNING"
      return new CommandOutput(-1, commandOutputArr, CMD_IS_RUNNING)
    }

  }

  /**
   * Stops command execution on a remote host (Ctrl+C).
   *
   * @param commandId id of the command which has to be cleaned.
   */
  void cleanupCommand(String commandId) {

    logger.debug "Release all external and internal WinRM resources for shell with id=${shellId} and command id = [${commandId}]"

    Validate.notNull(shellId, MISSING_SHELL_ID)
    Validate.notNull(commandId, MISSING_COMMAND_ID)

    String request = new CleanupCommandRequest(toAddress, shellId, commandId, requestTimeout).toString()
    sendHttpRequest(request)

    logger.debug 'Release all external and internal WinRM resources'

  }

  /**
   * Deletes shell releasing all resources allocated for the current shell on a remote host.
   *
   * @return <code>true</code> in case of successful shell closing, otherwise <code>false</code>.
   */
  boolean deleteShell() {

    logger.debug "Sending Close shell request with id = ${shellId}"

    Validate.notNull(shellId, MISSING_SHELL_ID)

    String request = new DeleteShellRequest(toAddress, shellId, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)

    logger.debug 'Close Shell Request processing is finished'

    shellId = null

    !results?.'*:Body'?.text()

  }

  /*
   * PRIVATE METHODS
   */

  /**
   * Initializes <code>WinRMClient</code> object.
   */
  private void initialize() {
    if (!toAddress) {
      if (!host) {
        throw new WinRMException('WinRM host has to be initialized!')
      }
      toAddress = Utils.buildUrl(protocol, host, port)
    }
    if (!httpBuilder) {
      httpBuilder = new HTTPBuilder(toAddress.toURI())
      if (!user) {
        throw new WinRMException('WinRM username has to be initialized!')
      }
      if (!password) {
        throw new WinRMException('WinRM password has to be initialized!')
      }
      httpBuilder.auth.basic user, password
      if (protocol == PROTOCOL_HTTPS) {
        configureHttpsConnection()
      }
    }
  }

  private synchronized String sendHttpRequest(String request) {

    logger.debug "Sending http request to remote host"

    String responseXml = null
    httpBuilder.request(POST, TEXT) {

      headers.Accept = 'application/soap+xml; charset=utf-8'
      headers.'Content-Type' = "application/soap+xml; charset=utf-8"
      body = request

      response.success = { resp, stream ->
        responseXml = stream.text
      }

      response.failure = { resp ->
        logger.warn "An error details: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
        throw new WinRMException(resp?.statusLine?.statusCode, resp?.entity?.content?.text)
      }

    }

    logger.debug "Finished processing sending http request"

    responseXml

  }

  private void configureHttpsConnection() {
    logger.debug 'Configuring Https connection'
    Scheme scheme = new Scheme("https", new SSLSocketFactory(trustStrategy.strategy, verificationStrategy.verifier), 443)
    httpBuilder.client.connectionManager.schemeRegistry.register(scheme)
    logger.debug 'Https connection is configured'
  }
}
