/*-----------------------------------------------------------------------------
*  Copyright 2007 Alfresco Inc.
*  
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful, but
*  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
*  
*  You should have received a copy of the GNU General Public License along
*  with this program; if not, write to the Free Software Foundation, Inc.,
*  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.  As a special
*  exception to the terms and conditions of version 2.0 of the GPL, you may
*  redistribute this Program in connection with Free/Libre and Open Source
*  Software ("FLOSS") applications as described in Alfresco's FLOSS exception.
*  You should have recieved a copy of the text describing the FLOSS exception,
*  and it is also available here:   http://www.alfresco.com/legal/licensing
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    VirtServerInfo.java
*----------------------------------------------------------------------------*/


package org.alfresco.mbeans;

public class VirtServerInfo implements VirtServerInfoMBean
{
    // Local virtualization server info
    String virtDomain_;
    int    virtJmxRmiPort_;
    int    virtHttpPort_;
    int    retryInterval_;  // in milliseconds

    String   virtCifsAvmVersionTreeWin_;
    boolean  virtCifsAvmVersionTreeWinAutomount_;
    String   virtCifsAvmVersionTreeUnix_;
    boolean  virtCifsAvmVersionTreeUnixAutomount_;

    String   virtOsName_;

    // Remote Alfresco server info
    String alfrescoJmxRmiHost_;
    int    alfrescoJmxRmiPort_;

    // Admin user name on Alfresco server.
    String alfrescoServerUser_;
    
    // Admin user password on Alfresco server.
    String alfrescoServerPassword_;

    public VirtServerInfo() { }

    public String   getVirtServerDomain()                { return virtDomain_; }
    public void     setVirtServerDomain(String host)     { virtDomain_ = host; }

    public int      getVirtServerHttpPort()              { return virtHttpPort_; }
    public void     setVirtServerHttpPort(int port)      { virtHttpPort_ = port; }

    public int      getVirtServerJmxRmiPort()            { return virtJmxRmiPort_; }
    public void     setVirtServerJmxRmiPort(int port)    { virtJmxRmiPort_ = port; }


    // local CIFS automount props

    public String   getVirtServerCifsAvmVersionTreeWin()  
                    { return virtCifsAvmVersionTreeWin_;}

    public void     setVirtServerCifsAvmVersionTreeWin(String mountPoint)
                    { virtCifsAvmVersionTreeWin_ = mountPoint;}

    public boolean  getVirtServerCifsAvmVersionTreeWinAutomount()
                    { return virtCifsAvmVersionTreeWinAutomount_; }

    public void     setVirtServerCifsAvmVersionTreeWinAutomount(boolean doAutomount)
                    { virtCifsAvmVersionTreeWinAutomount_ = doAutomount; }

    public String   getVirtServerCifsAvmVersionTreeUnix()
                    { return  virtCifsAvmVersionTreeUnix_; }

    public void     setVirtServerCifsAvmVersionTreeUnix(String mountPoint)
                    {  virtCifsAvmVersionTreeUnix_ = mountPoint; }

    public boolean  getVirtServerCifsAvmVersionTreeUnixAutomount()
                    { return virtCifsAvmVersionTreeUnixAutomount_; }

    public void     setVirtServerCifsAvmVersionTreeUnixAutomount(boolean doAutomount)
                    { virtCifsAvmVersionTreeUnixAutomount_ = doAutomount; }

   
    /**   Fetches the value of os.name on the Virtualization server. */
    public String   getVirtServerOsName()                { return  virtOsName_; }

    /**   Sets virtualization server's exposed value for OS name. 
    *     Note:  this method is only called from within the virtualzation server itself,
    *     not remote JMXRMI clients. 
    */
    public void     setVirtServerOsName(String osName)   { virtOsName_ = osName; }


    public String   getAlfrescoJmxRmiHost()              { return alfrescoJmxRmiHost_; }
    public void     setAlfrescoJmxRmiHost(String host)   { alfrescoJmxRmiHost_ = host; }

    public int      getAlfrescoJmxRmiPort()              { return alfrescoJmxRmiPort_; }
    public void     setAlfrescoJmxRmiPort(int port)      { alfrescoJmxRmiPort_ = port; }
    
    public String   getAlfrescoServerUser()              { return alfrescoServerUser_; }
    public void     setAlfrescoServerUser(String user)   { alfrescoServerUser_ = user; }

    public String   getAlfrescoServerPassword()
                    { return alfrescoServerPassword_; }

    public void     setAlfrescoServerPassword(String password)
                    { alfrescoServerPassword_ = password; }

    public int      getVirtServerConnectionRetryInterval() 
                    { return retryInterval_; }

    public void     setVirtServerConnectionRetryInterval(int milliseconds)
                    { retryInterval_ = milliseconds; }
}
