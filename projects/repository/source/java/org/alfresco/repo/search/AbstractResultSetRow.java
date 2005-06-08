/*
 * Created on 06-May-2005
 * 
 * TODO Comment this class
 * 
 * 
 */
package org.alfresco.repo.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.Path;
import org.alfresco.repo.ref.QName;

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
