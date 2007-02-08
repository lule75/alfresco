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
package org.alfresco.web.bean.repository;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;

/**
 * Simple client service to retrieve the Preferences object for the current User.
 * 
 * @author Kevin Roast
 */
public final class PreferencesService
{
   /**
    * Private constructor
    */
   private PreferencesService()
   {
   }
   
   /**
    * @return The Preferences for the current User instance.
    */
   public static Preferences getPreferences()
   {
      return getPreferences(FacesContext.getCurrentInstance());
   }
   
   /**
    * @param fc   FacesContext
    * @return The Preferences for the current User instance.
    */
   public static Preferences getPreferences(FacesContext fc)
   {
      User user = Application.getCurrentUser(fc);
      return getPreferences(user);
   }
   
   /**
    * @param user User instance
    * @return The Preferences for the current User instance.
    */
   public static Preferences getPreferences(User user)
   {
      return user.getPreferences();
   }
}
