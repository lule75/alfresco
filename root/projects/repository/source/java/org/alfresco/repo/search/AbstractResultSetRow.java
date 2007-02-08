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
package org.alfresco.repo.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.namespace.QName;

public abstract class AbstractResultSetRow implements ResultSetRow
{

    /**
     * The containing result set
     */
    private ResultSet resultSet;

    /**
     * The current position in the containing result set
     */
    private int index;

    /**
     * The direct properties of the current node
     * Used by those implementations that can cache the whole set.
     */

    private Map<Path, Serializable> properties;

    public AbstractResultSetRow(ResultSet resultSet, int index)
    {
        super();
        this.resultSet = resultSet;
        this.index = index;
    }

    public ResultSet getResultSet()
    {
        return resultSet;
    }

    public int getIndex()
    {
        return index;
    }

    public NodeRef getNodeRef()
    {
        return getResultSet().getNodeRef(getIndex());
    }

    public float getScore()
    {
        return getResultSet().getScore(getIndex());
    }

    public Map<Path, Serializable> getValues()
    {
        if (properties == null)
        {
            properties = new HashMap<Path, Serializable>();
            setProperties(getDirectProperties());
        }
        return Collections.unmodifiableMap(properties);
    }

    protected Map<QName, Serializable> getDirectProperties()
    {
        return Collections.<QName, Serializable>emptyMap();
    }
    
    protected void setProperties(Map<QName, Serializable> byQname)
    {
        for (QName qname : byQname.keySet())
        {
            Serializable value = byQname.get(qname);
            Path path = new Path();
            path.append(new Path.SelfElement());
            path.append(new Path.AttributeElement(qname));
            properties.put(path, value);
        }
    }
    
    public Serializable getValue(QName qname)
    {
        Path path = new Path();
        path.append(new Path.SelfElement());
        path.append(new Path.AttributeElement(qname));
        return getValues().get(path);
    }
    
}
