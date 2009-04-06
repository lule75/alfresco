/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.security.authority.script;

import java.util.List;
import java.util.Set;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;




/**
 * Script object representing the authority service.
 * 
 * Provides Script access to groups and may in future be extended for roles and people.
 * 
 * @author Mark Rogers
 */
public class ScriptAuthorityService extends BaseScopableProcessorExtension
{    
	
    /** The  service */
    private AuthorityService authorityService;

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public AuthorityService getAuthorityService() {
		return authorityService;
	}
	
	/**
	 * Search the root groups, those without a parent group.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] searchRootGroups(String pattern, boolean includeInternal)
	{
		ScriptGroup[] groups = new ScriptGroup[0];
		Set<String> authorities = authorityService.getAllRootAuthorities(AuthorityType.GROUP);
		return groups;
	}
	
	/**
	 * Get the root groups, those without a parent group.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] getAllRootGroups(boolean includeInternal)
	{
		ScriptGroup[] groups = new ScriptGroup[0];
		Set<String> authorities = authorityService.getAllRootAuthorities(AuthorityType.GROUP);
		return groups;
	}
    
	/**
	 * Get a group given its short name
	 * @param shortName
	 * @return
	 */
	public ScriptGroup getGroup(String shortName)
	{
		Set<String> authorities = authorityService.findAuthorities(AuthorityType.GROUP, shortName);
		return new ScriptGroup();
	}
	
	/**
	 * Create a new root group
	 * @return
	 */
	public ScriptGroup createRootGroup(String shortName, String displayName)
	{
		String newName = authorityService.createAuthority(AuthorityType.GROUP, null, shortName, displayName);
		
		return new ScriptGroup();
	}
	
	/**
	 * Search for groups
	 * 
	 * @param shortNameFilter partial match on shortName (* and ?) work. if empty then matches everything.
	 * @param includeInternal
	 * @return the groups matching the query
	 */
	public ScriptGroup[] listGroups(String shortNameFilter, boolean includeInternal)
	{
		ScriptGroup[] groups = new ScriptGroup[0];
		Set<String> authorities = authorityService.findAuthorities(AuthorityType.GROUP, shortNameFilter);
		return groups;
	}
	
}