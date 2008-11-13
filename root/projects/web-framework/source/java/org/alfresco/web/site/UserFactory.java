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
package org.alfresco.web.site;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.connector.User;
import org.alfresco.web.site.exception.UserFactoryException;

/**
 * Factory class for producing and loading User objects. Generally this is
 * overriden on a per-application bases to build specific User instances.
 * 
 * @author muzquiano
 * @author Kevin Roast
 */
public abstract class UserFactory
{
    public static final String USER_GUEST = "guest";
    public static String SESSION_ATTRIBUTE_KEY_USER_OBJECT = "USER_OBJECT";
    public static String SESSION_ATTRIBUTE_KEY_USER_ID = "USER_ID";

    protected User guestUser = null;
    protected String id;
    
    
    protected User getGuestUser(RequestContext context) throws UserFactoryException
    {
        if (this.guestUser == null)
        {
            User user = new User(USER_GUEST);
            user.setFirstName("Guest");
            
            this.guestUser = user;
        }
        return this.guestUser;
    }
    
    /**
     * Loads a user from the remote user store and store it into the session.
     * 
     * @param context
     * @param request
     * @return
     * @throws UserFactoryException
     */
    public User faultUser(RequestContext context, HttpServletRequest request)
        throws UserFactoryException
    {
        return faultUser(context, request, false);
    }
       

    /**
     * Loads a user from the remote user store and stores it into the session.
     * 
     * If the force flag is set, the current in-session user
     * object will be purged, forcing the user object to reload.
     * 
     * @param context
     * @param request
     * @param force
     * @return
     * @throws UserFactoryException
     */
    public User faultUser(RequestContext context, HttpServletRequest request, boolean force)
        throws UserFactoryException
    {
        User user = null;
        
        // do we want to force a user fault?
        if (force)
        {
            // remove the user object from session
            request.getSession().removeAttribute(SESSION_ATTRIBUTE_KEY_USER_OBJECT);
        }
        
        // check whether there is a "USER_ID" marker in the session
        String userId = (String) request.getSession().getAttribute(SESSION_ATTRIBUTE_KEY_USER_ID);
        if (userId != null)
        {
            // check whether there is a user object loaded already
            user = (User) request.getSession().getAttribute(SESSION_ATTRIBUTE_KEY_USER_OBJECT);
            if (user == null)
            {
                // load the user from whatever store...
                user = loadUser(context, userId);
                
                // if we got the user, set onto session
                if (user != null)
                {
                    request.getSession().setAttribute(SESSION_ATTRIBUTE_KEY_USER_OBJECT, user);
                }
                else
                {
                    // unable to load the user
                    request.getSession().removeAttribute(SESSION_ATTRIBUTE_KEY_USER_OBJECT);                    
                }
            }
        }
        
        // TODO: should we do this?
        // return the guest user
        if (user == null)
        {
            user = getGuestUser(context);
        }
        
        return user;
    }
    
    /**
     * Load the user from a store
     * 
     * @param context
     * @param userId
     * @return
     * @throws UserFactoryException
     */
    public abstract User loadUser(RequestContext context, String userId)
        throws UserFactoryException;
    
    /**
     * Authentication the user given the supplied username/password
     * 
     * @param request
     * @param username
     * @param password
     * 
     * @return success/failure
     */
    public abstract boolean authenticate(HttpServletRequest request, String username, String password);
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    public String getId()
    {
        return this.id;
    }
}
