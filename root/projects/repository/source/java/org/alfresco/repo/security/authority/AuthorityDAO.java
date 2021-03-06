/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.authority;

import java.util.Collection;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;

public interface AuthorityDAO
{
    /**
     * Add a child authority to the given parent authorities
     * 
     * @param parentNames
     * @param childName
     */
    void addAuthority(Collection<String> parentNames, String childName);

    /**
     * Create an authority.
     * 
     * @param name
     * @param authorityDisplayName
     * @param authorityZones
     */
    void createAuthority(String name, String authorityDisplayName, Set<String> authorityZones);

    /**
     * Delete an authority.
     * 
     * @param name
     */
    void deleteAuthority(String name);

    /**
     * Get contained authorities.
     * 
     * @param type
     * @param name
     * @param immediate
     * @return
     */
    Set<String> getContainedAuthorities(AuthorityType type, String name, boolean immediate);

    /**
     * Remove an authority.
     * 
     * @param parentName
     * @param childName
     */
    void removeAuthority(String parentName, String childName);

    /**
     * Get the authorities that contain the one given.
     * 
     * @param type
     * @param name
     * @param immediate
     * @return
     */
    Set<String> getContainingAuthorities(AuthorityType type, String name, boolean immediate);

    /**
     * Get all authorities by type
     * 
     * @param type
     * @return
     */
    Set<String> getAllAuthorities(AuthorityType type);
    
    /**
     * Test if an authority already exists.
     * 
     * @param name
     * @return
     */
    boolean authorityExists(String name);
    
    /**
     * Get a node ref for the authority if one exists
     * 
     * @param name
     * @return
     */
    NodeRef getAuthorityNodeRefOrNull(String name);

    /**
     * Gets the name for the given authority node
     * 
     * @param authorityRef  authority node
     * @return  name
     */
    public String getAuthorityName(NodeRef authorityRef);

    /**
     * Get the display name for an authority
     * 
     * @param authorityName
     * @return the display name
     */
    String getAuthorityDisplayName(String authorityName);

    /**
     * Set the display name for an authority
     * 
     * @param authorityName
     * @param authorityDisplayName
     */
    void setAuthorityDisplayName(String authorityName, String authorityDisplayName);

    /**
     * Find authorities by display name pattern.
     * 
     * @param type
     * @param parentAuthority if non-null, will look only for authorities who are a child of the named parent
     * @param immediate if <code>true</code> then only search root groups if parentAuthority is null, or immediate children of parentAuthority if it is non-null.
     * @param displayNamePattern
     * @param zoneName - may be null to indicate all zones
     * @return
     */
    public Set<String> findAuthorities(AuthorityType type, String parentAuthority, boolean immediate,
            String displayNamePattern, String zoneName);

    /**
     * Extract the short name of an authority from its full identifier.
     * 
     * @param name
     * @return
     */
    public String getShortName(String name);

    /**
     * Create the full identifier for an authority given its short name and
     * type.
     * 
     * @param type
     * @param shortName
     * @return
     */
    public String getName(AuthorityType type, String shortName);

    /**
     * Gets or creates an authority zone node with the specified name
     * 
     * @param zoneName
     *            the zone name
     * @return reference to the zone node
     */
    public NodeRef getOrCreateZone(String zoneName);
    
    /**
     * Gets an authority zone node with the specified name
     * 
     * @param zoneName
     *            the zone name
     * @return reference to the zone node ot null if the zone does not exists
     */
    public NodeRef getZone(String zoneName);
    
    /**
     * Gets the name of the zone containing the specified authority.
     * 
     * @param name
     *            the authority long name
     * @return the set of names of all zones containing the specified authority, an empty set if the
     *         authority exists but has no zone, or <code>null</code> if the authority does not exist.
     */
    public Set<String> getAuthorityZones(String name);
    
    /**
     * Gets the names of all authorities in a zone, optionally filtered by type.
     * 
     * @param zoneName
     *            the zone name
     * @param type
     *            the authority type to filter by or <code>null</code> for all authority types
     * @return the names of all authorities in a zone, optionally filtered by type
     */
    public Set<String> getAllAuthoritiesInZone(String zoneName, AuthorityType type);
    
    /**
     * Add an authority to zones
     * @param authorityName
     * @param zones
     */
    public void addAuthorityToZones(String authorityName, Set<String> zones);

    /**
     * Remove an authority from zones.
     * @param authorityName
     * @param zones
     */
    public void removeAuthorityFromZones(String authorityName, Set<String> zones);    
}
