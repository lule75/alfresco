/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.audit;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.ParameterCheck;

/**
 * Class to represent a Records Management audit entry
 * 
 * @author Gavin Cornwell
 */
public final class RecordsManagementAuditEntry
{
    private Date timestamp;
    private String userName;
    private String fullName;
    private String userRole;
    private NodeRef nodeRef;
    private String nodeName;
    private String event;
 
    /**
     * Default constructor
     */
    public RecordsManagementAuditEntry(Date timestamp, 
                String userName, String fullName, String userRole, 
                NodeRef nodeRef, String nodeName, String event)
    {
        ParameterCheck.mandatory("timestamp", timestamp);
        ParameterCheck.mandatory("userName", userName);
        ParameterCheck.mandatory("fullName", fullName);
        ParameterCheck.mandatory("userRole", userRole);
        ParameterCheck.mandatory("nodeRef", nodeRef);
        ParameterCheck.mandatory("nodeName", nodeName);
        ParameterCheck.mandatory("event", event);
        
        this.timestamp = timestamp;
        this.userName = userName;
        this.userRole = userRole;
        this.fullName = fullName;
        this.nodeRef = nodeRef;
        this.nodeName = nodeName;
        this.event = event;
    }

    public Date getTimestamp()
    {
        return this.timestamp;
    }
    
    public String getTimestampString()
    {
        return ISO8601DateFormat.format(this.timestamp);
    }

    public String getUserName()
    {
        return this.userName;
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public String getUserRole()
    {
        return this.userRole;
    }

    public NodeRef getNodeRef()
    {
        return this.nodeRef;
    }

    public String getNodeName()
    {
        return this.nodeName;
    }

    public String getEvent()
    {
        return this.event;
    }
}