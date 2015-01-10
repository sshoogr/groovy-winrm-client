# Configure WinRM to Use HTTPS

=======================

## Overview

This is a short description of how to configure the WinRM host through the HTTPS protocol. In the description Windows Server 2008 R2 is used as the WinRM host OS.

To communicate through the HTTPS protocol the WinRM host requires a certificate.
A certificate can be either generated or obtained. This description deals with a generated certificate.
To generate a self-signed certificate we will use `makecert.exe` and `pvk2pfx.exe` tools that are part of the .NET Windows SDK.

### Certificate creation

To generate certificate with private key execute the following command:

    makecert.exe -r -pe -n "CN=__IP_HOST__" -b 01/01/2015 -e 01/01/2022 -eku 1.3.6.1.5.5.7.3.1 -sky exchange __FILE_NAME.cer -sv __FILE_NAME.pvk

During the execution of the command you will be asked to enter and re-enter a password several times.
Insert the password your entered on the previous step in the following command:

    pvk2pfx.exe -pvk __FILE_NAME.pvk -spc __FILE_NAME.cer -pfx __FILE_NAME.pfx -pi __PASSWORD_WERE_ENETERED__

After the execution of the command you will get a pfx file containing the public and private keys information.

### Configuring the WinRM host

Running the following commands requires that you logged in to the WinRM host as administrator or you can run applications with the rights of Administrator.

* Add Inbound firewall rule to open 5986 port

        netsh firewall add portopening TCP 5986 "Port 5986"

* Install the generated certificate to Trusted Root Certification Authorities and Personal folder of Local Computer executing the following command

        certutil -f -p __PASSWORD_WERE_ENETERED__ -importpfx __FILE_NAME.pfx

* Create an HTTPS listener. Save the following code to addHttpsListener.ps1 file and execute the command `powershell -File addHttpsListener.ps1`

        $Thumbprint = (Get-ChildItem -Path Cert:\LocalMachine\My | Where-Object {$_.Subject -match "__IP_HOST__"}).Thumbprint;
        winrm create winrm/config/Listener?Address=*+Transport=HTTPS '@{Hostname="__IP_HOST__";CertificateThumbprint="'$Thumbprint'"}'


### Configuring client host

For Windows client hosts to enable WinRM through HTTPS communication between the client host and the WinRM host install the generated pfx certificate to Trusted Root Certification Authorities on the client host.

### Links

1. [Configure WinRM to Use HTTPS](http://pubs.vmware.com/orchestrator-plugins/index.jsp?topic=%2Fcom.vmware.using.powershell.plugin.doc_10%2FGUID-2F7DA33F-E427-4B22-8946-03793C05A097.html)
2. [groowin-test-box](https://github.com/aestasit/groowin-test-box)