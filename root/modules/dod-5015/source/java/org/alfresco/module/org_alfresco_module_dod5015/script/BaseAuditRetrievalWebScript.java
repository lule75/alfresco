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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.module.org_alfresco_module_dod5015.script;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.audit.RecordsManagementAuditQueryParameters;
import org.alfresco.module.org_alfresco_module_dod5015.audit.RecordsManagementAuditService;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.web.scripts.content.StreamContent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.scripts.WebScriptRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Base class for all audit retrieval webscripts.
 * 
 * @author Gavin Cornwell
 */
public class BaseAuditRetrievalWebScript extends StreamContent
{
    /** Logger */
    private static Log logger = LogFactory.getLog(BaseAuditRetrievalWebScript.class);
    
    protected final static String PARAM_USER = "user";
    protected final static String PARAM_SIZE = "size";
    protected final static String PARAM_EVENT = "event";
    protected final static String PARAM_FROM = "from";
    protected final static String PARAM_TO = "to";
    protected final static String DATE_PATTERN = "yyyy-MM-dd";
    
    protected RecordsManagementAuditService rmAuditService;
    
    /**
     * Sets the RecordsManagementAuditService instance
     * 
     * @param auditService The RecordsManagementAuditService instance
     */
    public void setRecordsManagementAuditService(RecordsManagementAuditService rmAuditService)
    {
        this.rmAuditService = rmAuditService;
    }
    
    /**
     * Parses the given request and builds an instance of 
     * RecordsManagementAuditQueryParameters to retrieve the relevant audit entries
     * 
     * @param req The request
     * @return RecordsManagementAuditQueryParameters instance
     */
    protected RecordsManagementAuditQueryParameters parseQueryParameters(WebScriptRequest req)
    {
        // create parameters for audit trail retrieval
        RecordsManagementAuditQueryParameters params = new RecordsManagementAuditQueryParameters();
        
        // the webscripts can have a couple of different forms of url, work out
        // whether a nodeRef has been supplied or whether the whole audit
        // log should be displayed
        NodeRef nodeRef = null;
        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        String storeType = templateVars.get("store_type");
        if (storeType != null && storeType.length() > 0)
        {
            // there is a store_type so all other params are likely to be present
            String storeId = templateVars.get("store_id");
            String nodeId = templateVars.get("id");
            
            // create the nodeRef
            nodeRef = new NodeRef(new StoreRef(storeType, storeId), nodeId);
        }
        
        // gather all the common filtering parameters, these could be on the
        // query string, in a multipart/form-data request or in a JSON body
        String size = null;
        String user = null;
        String event = null;
        String from = null;
        String to = null;
        
        if (MimetypeMap.MIMETYPE_JSON.equals(req.getContentType()))
        {
            try
            {
                JSONObject json = new JSONObject(new JSONTokener(req.getContent().getContent()));
                if (json.has(PARAM_SIZE))
                {
                    size = json.getString(PARAM_SIZE);
                }
                if (json.has(PARAM_USER))
                {
                    user = json.getString(PARAM_USER);
                }
                if (json.has(PARAM_EVENT))
                {
                    event = json.getString(PARAM_EVENT);
                }
                if (json.has(PARAM_FROM))
                {
                    from = json.getString(PARAM_FROM);
                }
                if (json.has(PARAM_TO))
                {
                    to = json.getString(PARAM_TO);
                }
            }
            catch (IOException ioe)
            {
                // log a warning
                if (logger.isWarnEnabled())
                    logger.warn("Failed to parse JSON parameters for audit query: " + ioe.getMessage());
            }
            catch (JSONException je)
            {
                // log a warning
                if (logger.isWarnEnabled())
                    logger.warn("Failed to parse JSON parameters for audit query: " + je.getMessage());
            }
        }
        else
        {
            size = req.getParameter(PARAM_SIZE);
            user = req.getParameter(PARAM_USER);
            event = req.getParameter(PARAM_EVENT);
            from = req.getParameter(PARAM_FROM);
            to = req.getParameter(PARAM_TO);
        }

        // setup the audit query parameters object
        params.setNodeRef(nodeRef);
        params.setUser(user);
        params.setEvent(event);
        
        if (size != null && size.length() > 0)
        {
            try
            {
                params.setMaxEntries(Integer.parseInt(size));
            }
            catch (NumberFormatException nfe)
            {
                if (logger.isWarnEnabled())
                    logger.warn("Ignoring size parameter as '" + size + "' is not a number!");
            }
        }
        
        if (from != null && from.length() > 0)
        {
            try
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
                params.setDateFrom(dateFormat.parse(from));
            }
            catch (ParseException pe)
            {
                if (logger.isWarnEnabled())
                    logger.warn("Ignoring from parameter as '" + from + "' does not conform to the date pattern: " + DATE_PATTERN);
            }
        }
        
        if (to != null && to.length() > 0)
        {
            try
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
                params.setDateTo(dateFormat.parse(to));
            }
            catch (ParseException pe)
            {
                if (logger.isWarnEnabled())
                    logger.warn("Ignoring to parameter as '" + to + "' does not conform to the date pattern: " + DATE_PATTERN);
            }
        }
        
        return params;
    }
}