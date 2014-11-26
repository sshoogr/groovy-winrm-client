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
import com.aestasit.infrastructure.winrm.client.https.WinRMHttpsHostVerificationStrategy
import com.aestasit.infrastructure.winrm.client.https.WinRMHttpsTrustStrategy
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

import java.util.concurrent.TimeoutException

import static com.aestasit.infrastructure.winrm.client.util.Defines.*
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.POST

/**
 * A primitive implementation of WinRM client.
 *
 * @author Sergey Korenko
 */
@Canonical(includes = ['protocol','host', 'port', 'user', 'password', 'shellTimeout', 'requestTimeout'])
class WinRMClient {
  private final Logger logger = LoggerFactory.getLogger(getClass().getPackage().getName())

  String protocol    = PROTOCOL_HTTP
  String host
  int port           = PORT_HTTP
  String user
  String password

  /** Timeout for open shell*/
  long shellTimeout         = SHELL_DEFAULT_TIMEOUT
  /** Timeout of a single WinRM request in seconds*/
  int requestTimeout        = REQUEST_DEFAULT_TIMEOUT

  URL toAddress
  String shellId
  HTTPBuilder httpBuilder

  WinRMHttpsTrustStrategy trustStrategy = WinRMHttpsTrustStrategy.ALLOW_SELF_SIGNED
  WinRMHttpsHostVerificationStrategy verificationStrategy = WinRMHttpsHostVerificationStrategy.ALLOW_ALL

  void initialize() {
    Validate.notEmpty(host, 'WinRM Host has to be initialized')
    Validate.notEmpty(user, 'WinRM Username has to be initialized')
    Validate.notEmpty(password, 'WinRM Password cannot be empty')

    if (!toAddress) {
      toAddress = Utils.buildUrl(protocol, host, port)
    }

    if (!httpBuilder) {
      httpBuilder = new HTTPBuilder(toAddress.toURI())
      httpBuilder.auth.basic user, password

      if (protocol == PROTOCOL_HTTPS) {
        configureHttpsConnection()
      }
    }
  }

  private void configureHttpsConnection() {
    logger.debug 'Configuring Https connection'
    Scheme scheme = new Scheme("https", new SSLSocketFactory(trustStrategy.strategy, verificationStrategy.verifier), 443)
    httpBuilder.client.connectionManager.schemeRegistry.register(scheme)
    logger.debug 'Https connection is configured'
  }

  /**
   * Creates WinRM shell for execution of remote commands. Shell is identified by id
   *
   * @return id of the open shell in case of
   */
  String openShell() {
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
   * Runs commands
   *
   * @param command command text
   * @param args arguments to run command
   * @return command id corresponds to the transferred command
   */
  String executeCommand(String command, String[] args = []) {
    logger.debug "Sending request to execute command in previously open shell with id=${shellId}"

    Validate.notNull(shellId, 'Command cannot be executed when an open remote shell is not available')

    String request = new ExecuteCommandRequest(toAddress, shellId, command, args, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)
    String commandId = results?.'*:Body'?.'*:CommandResponse'?.'*:CommandId'?.text()

    logger.debug "Request to execute command has been finsihed in previously open shell with id=${shellId}"

    commandId
  }

  CommandOutput getCommandExecutionResults(String commandId) {
    CommandOutput outputResults = new CommandOutput(-1, '', '')

    Thread thr = new Thread() {
      void run() {
        for (; !isInterrupted();) {
          CommandOutput tempOutput = commandExecuteResults(commandId)
          outputResults.with {
            exitStatus = tempOutput.exitStatus
            output += tempOutput.output
            errorOutput = tempOutput.errorOutput
            exception = tempOutput.exception
          }
          if (-1 != outputResults?.exitStatus && CMD_IS_RUNNING != outputResults?.errorOutput) {
            break
          }
        }
      }
    }
    thr.start()

    try {
      thr.join(shellTimeout)
    } catch (InterruptedException e) {
      thr.interrupt()
      throw new Exception(e)
    }

    if (thr.isAlive()) {
      thr.interrupt()
      throw new TimeoutException()
    }

    outputResults
  }

  CommandOutput commandExecuteResults(String commandId){
    logger.debug "Reading output of command with id =[${commandId}] from shell with id=${shellId}"

    Validate.notNull(shellId, 'Command cannot be executed when an open remote shell is not available (shellId == null)')
    Validate.notNull(commandId, 'Undefined command cannot be executed (commandId == null)')

    String request = new GetCommandOutputRequest(toAddress, shellId, commandId, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)
    String commandOutputArr = ''
    String errOutputArr = ''
    results?.'*:Body'?.'*:ReceiveResponse'?.'*:Stream'?.findAll{it.@Name=='stdout' && it.@CommandId==commandId}?.each{ commandOutputArr += new String(it.text().decodeBase64())}
    results?.'*:Body'?.'*:ReceiveResponse'?.'*:Stream'?.findAll{it.@Name=='stderr' && it.@CommandId==commandId}?.each{errOutputArr += new String(it.text().decodeBase64())}

    if(results?.'*:Body'?.'*:ReceiveResponse'?.'*:CommandState'?.find{it.@CommandId==commandId && it.@State =='http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Done'}){
      Integer exitStatus = results?.'*:Body'?.'*:ReceiveResponse'?.'*:CommandState'?.'*:ExitCode'?.text()?.toInteger()

      logger.debug "retrieve command output of command with id =[${commandId}] has been processed"

      new CommandOutput(exitStatus, commandOutputArr, errOutputArr)
    } else{
      logger.debug "command with id =[${commandId}] from shell with id=${shellId} is still RUNNING"
      new CommandOutput(-1, '', CMD_IS_RUNNING)
    }
  }

  void cleanupCommand(String commandId){
    logger.debug "Release all external and internal WinRM resources for shell with id=${shellId} and command id = [${commandId}]"

    Validate.notNull(shellId, 'Clenup command cannot be executed when an open remote shell is not available (shellId == null)')
    Validate.notNull(commandId, 'Cleanup command cannot be executed if command is not defined (commandId == null)')

    String request = new CleanupCommandRequest(toAddress, shellId, commandId, requestTimeout).toString()
    sendHttpRequest(request)

    logger.debug 'Release all external and internal WinRM resources'
  }

  boolean deleteShell() {
    logger.debug "Sending Close shell request with id = ${shellId}"

    Validate.notNull(shellId, 'Deleting remote shell cannot be executed when shell is undefined (shellId == null)')

    String request = new DeleteShellRequest(toAddress, shellId, requestTimeout).toString()
    String response = sendHttpRequest(request)
    GPathResult results = new XmlSlurper().parseText(response)

    logger.debug 'Close Shell Request processing is finished'

    !results?.'*:Body'?.text()
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

  CommandOutput execute(String command, String[] arguments=[]) {
    String commandId = null
    try{
      logger.debug "Starting execution ${command} command on remote host"
      openShell()
      commandId = executeCommand(command, arguments)
      CommandOutput  output = getCommandExecutionResults(commandId)
      deleteShell()
      logger.debug "Finished execution ${command} command on remote host"

      output
    } catch (TimeoutException e) {
      stopExecution(commandId) {
        logger.warn "Execution of the command [${command}] has been terminated by timeout!"
        new CommandOutput(1, '', CMD_IS_STOPPED_BY_TIMEOUT, e)
      }
    } catch (Exception e) {
      stopExecution(commandId) {
        logger.warn "Execution of the command [${command}] has been terminated by exception!"
        new CommandOutput(1, '', CMD_IS_TERMINATED_BY_EXCEPTION, e)
      }
    }
  }

  private CommandOutput stopExecution(String commandId, Closure cl){
    if(commandId && shellId){
      cleanupCommand(commandId)
      deleteShell()
    }
    cl()
  }
}
