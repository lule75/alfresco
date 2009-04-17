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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.security.authentication;

import org.apache.commons.lang.RandomStringUtils;

/**
 * Generates a user name based upon firstName and lastName. 
 * 
 * The firstNamePattern is used when seed = 0.   
 * Then a random element is added and randomNamePattern is used. 
 *
 */
public class NameBasedUserNameGenerator implements UserNameGenerator
{
    // user name length property
    private int userNameLength = 10;

    /**
     * name generator pattern
     */
    private String namePattern = "%lastName%_%firstName%";
    
    /**
     * The pattern of the user name to generate 
     * e.g. %lastName%_%firstName% would generate Fred_Bloggs
     * 
     * Patterns available:
     *  	%lastName%,  lower case last name
     *  	%firstName%, lower case first name
     *  	%emailAddress% email address
     *      %i% lower case first name inital
     * 
     * @param userNamePattern
     */
	public void setNamePattern(String userNamePattern) 
	{
		this.namePattern = userNamePattern;
	}

    /**
     * Set the user name length
     * 
     * @param userNameLength the user name length
     */
    public void setUserNameLength(int userNameLength)
    {
        this.userNameLength = userNameLength;
    }
    
    /**
     * Returns a generated user name
     * 
     * @return the generated user name
     */
    public String generateUserName(String firstName, String lastName, String emailAddress, int seed)
    {
    	String userName;
    	
     	String pattern = namePattern;
     	
     	String initial = firstName.toLowerCase().substring(0,1);
    		
    	userName = pattern
    		.replace("%i%", initial)
    		.replace("%firstName%", firstName.toLowerCase())
    		.replace("%lastName%", lastName.toLowerCase())
    		.replace("%emailAddress%", emailAddress.toLowerCase());
    	
    	if(seed > 0)
    	{
    		if (userName.length() < userNameLength + 3)
    		{
    		     userName = userName + RandomStringUtils.randomNumeric(3);	
    		}
    		else
    		{
    			// truncate the user name and slap on 3 random characters
    			userName = userName.substring(0, userNameLength -3) + RandomStringUtils.randomNumeric(3);
    		}
    	}
    	
        return userName;
    }
}
