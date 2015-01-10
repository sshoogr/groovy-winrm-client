# groovy-winrm-client
=======================

## Overview

The `groovy-winrm-client` is a **Groovy**-based library for executing commands on a remote (Windows) servers through **WinRM**. The library allows:

- connecting,
- executing remote command,
- reading output of the execution,
- stopping remote command,
- deleting opened shell on a remote host.

### Using `groovy-winrm-client` in Groovy scripts

The easiest way to use `groovy-winrm-client` in a **Groovy** script is by importing the dependency using [Grape](http://groovy.codehaus.org/Grape).

    @Grab('com.aestasit.infrastructure.winrm:groovy-winrm-client:0.5')
    import com.aestasit.infrastructure.winrm.client.WinRMClient

The entry point is the `WinRMClient` class. By default `WinRMClient` uses `HTTP` protocol
with `5985` port for connecting with remote host by `WinRM`.
For starting it is necessary to define host name, user and password used to connect to the remote host:

    WinRMClient client = new WinRMClient(host: 'hostname', user: 'user', password: 'pwd')

To use the `WinRMClient` via HTTPS we need to set corresponding protocol and port.

    WinRMClient client = new WinRMClient(protocol: 'https', host: 'hostname', port: 5986, user: 'user', password: 'pwd')

`HTTPS` usage required defining trust strategy and verification strategy of the client. By default trust strategy for `WinRMClient` is set to  `TrustStrategy.ALLOW_SELF_SIGNED` and verification strategy is set to `HostStrategy.ALLOW_ALL`. 
There are also other options available in `WinRMClient` for trust strategy `TrustStrategy.ALLOW_NONE` and `TrustStrategy.ALLOW_ALL`; and for verification strategy `HostStrategy.ALLOW_STRICT`, `HostStrategy.ALLOW_BROWSER_COMPATIBLE`.

`requestTimeout` parameter is important for slow connections. The value defines time within which WinRM request has to be finished. By default this value is set to 60 seconds. If a WinRM request cannot be executed within this time period an error occurred and WinRM request processing is terminated with the corresponding message.

After `WinRMClient` object is created it is necessary to open shell on a remote host where commands will be executed:

    client.openShell()

After shell is open remote commands are executed 

    String commandId = client.executeCommand('C:\Windows\System32\cmd.exe', ['/C', 'del /q/f/s %TEMP%\*] as String[])

`executeCommand` method returns command id which is used to receive the results of the command execution

    CommandOutput  output = client.commandExecuteResults(commandId)
    output.with{
      println exitStatus
      println output
      println errorOutput
    }

`CommandOutput` contains 

  - `exitStatus` to indicate the results of the invocation of a remote command, in case of succesful finishing the return value is `0`
  -  `output` standard output of the command execution
  -  `errorOutput` error details of command execution
  -  `exception` object contains exceptions occurres during command execution processing on client side

Sometimes command execution requires long time period. When the command is being executed `commandExecuteResults()` returns output with `exitCode = -1` and `errorOutput='Remote command is still running!`. In this case it is necessary to monitor the execution of the remote command in a loop with the simultaneous accumulation of output results. This may be done in a separate thread:

    def completeExecOutput = new CommandOutput(-1,'','')
    Thread thread = new Thread() {
      void run() {
        for (; !isInterrupted();) {
          def currentOutput = client.commandExecuteResults(commandId)

          completeExecOutput.with{
            exitStatus = currentOutput.exitStatus
            output += currentOutput.output
            errorOutput = currentOutput.errorOutput
            exception = currentOutput.exception
          }

          if (!completeExecOutput.commandRunning) {
            break
          }
        }
      }
    }
    thread.start()


To check if the remote command is still running there is a convinient method in `CommandOutput` class `boolean isCommandRunning()` which is calculated based on the output results recieved from the remote host.
In the described scenario an endless loop may occur and therefore it is necessary to take care about the shell live timeout:

    try {
      thread.join(options.maxWait)
    } catch (InterruptedException e) {
      thread.interrupt()
    }

When such interruption occurrs or it is needed to finish command execution unexpectedly for some other reason we invoke

    client.cleanupCommand(commandId)

Several remote commands may be executed within one open remote shell one by one.
After all commands are executed the shell on the remote host has to be closed to release all used resources

    client.deleteShell()

### Running `groovy-winrm-client` integration tests

Project's integration tests can be found in the `./src/integrationTest/` folder.
To run the integration tests:

1. Create base box using [groowin-test-box](https://github.com/aestasit/groowin-test-box) project. After finishing you will have `windows_2008_r2_virtualbox.box`
2. Add 'aestasit/2008r2' box using the command `vagrant box add --name aestasit/2008r2 windows_2008_r2_virtualbox.box`
3. Execute `vagrant up` using the `./Vagrantfile` file
4. execute `gradle integration` command to run integration tests