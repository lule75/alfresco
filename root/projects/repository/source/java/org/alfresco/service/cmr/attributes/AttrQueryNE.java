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

package org.alfresco.service.cmr.attributes;

/**
 * Not equals query.
 * @author britt
 */
public class AttrQueryNE extends AttrQuery 
{
    private static final long serialVersionUID = 103038173214972590L;

    /**
     * @param name
     */
    public AttrQueryNE(String name) 
    {
        super(name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttrQuery#getPredicate()
     */
    @Override
    public String getPredicate(AttrQueryHelper helper) 
    {
        String name = "name" + helper.getNextSuffix();
        helper.setParameter(name, fValue);
        return "me.key.key <> :" + name;
    }
}
