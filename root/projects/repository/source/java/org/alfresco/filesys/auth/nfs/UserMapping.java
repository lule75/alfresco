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
package org.alfresco.filesys.auth.nfs;

/**
 * Represents a user mapping for the {@link AlfrescoRpcAuthenticator}.
 */
public class UserMapping
{

    /** The name. */
    private String name;

    /** The uid. */
    private int uid;

    /** The gid. */
    private int gid;

    /**
     * Default constructor for container initialisation.
     */
    public UserMapping()
    {
    }

    /**
     * The Constructor.
     * 
     * @param name
     *            the name
     * @param uid
     *            the uid
     * @param gid
     *            the gid
     */
    public UserMapping(String name, int uid, int gid)
    {
        super();
        this.name = name;
        this.uid = uid;
        this.gid = gid;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *            the new name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the uid.
     * 
     * @return the uid
     */
    public int getUid()
    {
        return uid;
    }

    /**
     * Sets the uid.
     * 
     * @param uid
     *            the new uid
     */
    public void setUid(int uid)
    {
        this.uid = uid;
    }

    /**
     * Gets the gid.
     * 
     * @return the gid
     */
    public int getGid()
    {
        return gid;
    }

    /**
     * Sets the gid.
     * 
     * @param gid
     *            the new gid
     */
    public void setGid(int gid)
    {
        this.gid = gid;
    }
}