/*
 * Copyright (C) 2005 Alfresco, Inc.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.alfresco.repo.domain;

import java.io.Serializable;
import java.util.Set;

/** 
 * The interface against which recipients of permission are persisted
 * @author andyh
 */
public interface DbAuthority extends Serializable 
{
    /**
     * @return Returns the recipient
     */
    public String getRecipient();
    
    /**
     * @param recipient the authority recipient
     */
    public void setRecipient(String recipient);
    
    /**
     * @return Returns the external keys associated with this authority
     */
    public Set<String> getExternalKeys();
    
    /**
     * Delete the access control entries related to this authority
     * 
     * @return Returns the number of entries deleted
     */
    public int deleteEntries();
}
