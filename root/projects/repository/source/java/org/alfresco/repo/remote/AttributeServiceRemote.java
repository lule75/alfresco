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

package org.alfresco.repo.remote;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.attributes.Attribute;
import org.alfresco.service.cmr.attributes.AttrQuery;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.remote.AttributeServiceTransport;
import org.alfresco.util.Pair;

/**
 * Client side AttributeService implementation.
 * @author britt
 */
public class AttributeServiceRemote implements AttributeService
{
    private AttributeServiceTransport fTransport;
    
    private ClientTicketHolder fTicketHolder;
    
    public AttributeServiceRemote()
    {
    }

    public void setAttributeServiceTransport(AttributeServiceTransport transport)
    {
        fTransport = transport;
    }
    
    public void setClientTicketHolder(ClientTicketHolder holder)
    {
        fTicketHolder = holder;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#addAttribute(java.lang.String, org.alfresco.repo.attributes.Attribute)
     */
    public void addAttribute(String path, Attribute value)
    {
        fTransport.addAttribute(fTicketHolder.getTicket(), path, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#addAttribute(java.util.List, org.alfresco.repo.attributes.Attribute)
     */
    public void addAttribute(List<String> keys, Attribute value)
    {
        fTransport.addAttribute(fTicketHolder.getTicket(), keys, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getAttribute(java.lang.String)
     */
    public Attribute getAttribute(String path)
    {
        return fTransport.getAttribute(fTicketHolder.getTicket(), path);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getAttribute(java.util.List)
     */
    public Attribute getAttribute(List<String> keys)
    {
        return fTransport.getAttribute(fTicketHolder.getTicket(), keys);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getKeys(java.lang.String)
     */
    public List<String> getKeys(String path)
    {
        return fTransport.getKeys(fTicketHolder.getTicket(), path);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getKeys(java.util.List)
     */
    public List<String> getKeys(List<String> keys)
    {
        return fTransport.getKeys(fTicketHolder.getTicket(), keys);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#query(java.lang.String, org.alfresco.service.cmr.attributes.AttrQuery)
     */
    public List<Pair<String, Attribute>> query(String path, AttrQuery query)
    {
        return fTransport.query(fTicketHolder.getTicket(), path, query);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#query(java.util.List, org.alfresco.service.cmr.attributes.AttrQuery)
     */
    public List<Pair<String, Attribute>> query(List<String> keys,
            AttrQuery query)
    {
        return fTransport.query(fTicketHolder.getTicket(), keys, query);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeAttribute(java.lang.String, java.lang.String)
     */
    public void removeAttribute(String path, String name)
    {
        fTransport.removeAttribute(fTicketHolder.getTicket(), path, name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeAttribute(java.util.List, java.lang.String)
     */
    public void removeAttribute(List<String> keys, String name)
    {
        fTransport.removeAttribute(fTicketHolder.getTicket(), keys, name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeAttribute(java.lang.String, int)
     */
    public void removeAttribute(String path, int index)
    {
        fTransport.removeAttribute(fTicketHolder.getTicket(), path, index);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeAttribute(java.util.List, int)
     */
    public void removeAttribute(List<String> keys, int index)
    {
        fTransport.removeAttribute(fTicketHolder.getTicket(), keys, index);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttribute(java.lang.String, java.lang.String, org.alfresco.repo.attributes.Attribute)
     */
    public void setAttribute(String path, String name, Attribute value)
    {
        fTransport.setAttribute(fTicketHolder.getTicket(), path, name, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttribute(java.util.List, java.lang.String, org.alfresco.repo.attributes.Attribute)
     */
    public void setAttribute(List<String> keys, String name, Attribute value)
    {
        fTransport.setAttribute(fTicketHolder.getTicket(), keys, name, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttribute(java.lang.String, int, org.alfresco.repo.attributes.Attribute)
     */
    public void setAttribute(String path, int index, Attribute value)
    {
        fTransport.setAttribute(fTicketHolder.getTicket(), path, index, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttribute(java.util.List, int, org.alfresco.repo.attributes.Attribute)
     */
    public void setAttribute(List<String> keys, int index, Attribute value)
    {
        fTransport.setAttribute(fTicketHolder.getTicket(), keys, index, value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#exists(java.util.List)
     */
    public boolean exists(List<String> keys)
    {
        return fTransport.exists(fTicketHolder.getTicket(), keys);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#exists(java.lang.String)
     */
    public boolean exists(String path)
    {
        return fTransport.exists(fTicketHolder.getTicket(), path);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getCount(java.util.List)
     */
    public int getCount(List<String> keys)
    {
        return fTransport.getCount(fTicketHolder.getTicket(), keys);    
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#getCount(java.lang.String)
     */
    public int getCount(String path)
    {
        return fTransport.getCount(fTicketHolder.getTicket(), path);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#addAttributes(java.util.List, java.util.List)
     */
    public void addAttributes(List<String> keys, List<Attribute> values)
    {
        fTransport.addAttributes(fTicketHolder.getTicket(), keys, values);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#addAttributes(java.lang.String, java.util.List)
     */
    public void addAttributes(String path, List<Attribute> values)
    {
        fTransport.addAttributes(fTicketHolder.getTicket(), path, values);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttributes(java.util.List, java.util.Map)
     */
    public void setAttributes(List<String> keys, Map<String, Attribute> entries)
    {
        fTransport.setAttributes(fTicketHolder.getTicket(), keys, entries);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#setAttributes(java.lang.String, java.util.Map)
     */
    public void setAttributes(String path, Map<String, Attribute> entries)
    {
        fTransport.setAttributes(fTicketHolder.getTicket(), path, entries);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeEntries(java.util.List, org.alfresco.service.cmr.attributes.AttrQuery)
     */
    public void removeEntries(List<String> keys, AttrQuery query)
    {
        fTransport.removeEntries(fTicketHolder.getTicket(), keys, query);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.attributes.AttributeService#removeEntries(java.lang.String, org.alfresco.service.cmr.attributes.AttrQuery)
     */
    public void removeEntries(String path, AttrQuery query)
    {
        fTransport.removeEntries(fTicketHolder.getTicket(), path, query);
    }
}
