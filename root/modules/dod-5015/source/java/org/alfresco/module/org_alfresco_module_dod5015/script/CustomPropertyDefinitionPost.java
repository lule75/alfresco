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
package org.alfresco.module.org_alfresco_module_dod5015.script;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.CustomisableRmElement;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementCustomModel;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Implementation for Java backed webscript to add RM custom property definitions
 * to the custom model.
 * 
 * @author Neil McErlean
 */
public class CustomPropertyDefinitionPost extends AbstractRmWebScript
{
    protected RecordsManagementAdminService rmAdminService;

    private static final String PARAM_DATATYPE = "dataType";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_DEFAULT_VALUE = "defaultValue";
    private static final String PARAM_MULTI_VALUED = "multiValued";
    private static final String PARAM_MANDATORY = "mandatory";
    private static final String PARAM_PROTECTED = "protected";
    private static final String PARAM_CONSTRAINT_REF = "constraintRef";
    private static final String PARAM_ELEMENT = "element";
    private static final String PARAM_LABEL = "label";
    private static final String PROP_ID = "propId";
    private static final String URL = "url";

    public void setRecordsManagementAdminService(RecordsManagementAdminService rmAdminService)
    {
        this.rmAdminService = rmAdminService;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        JSONObject json = null;
        Map<String, Object> ftlModel = null;
        try
        {
            json = new JSONObject(new JSONTokener(req.getContent().getContent()));
            
            ftlModel = createPropertyDefinition(req, json);
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Could not read content from req.", iox);
        }
        catch (JSONException je)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                        "Could not parse JSON from req.", je);
        }
        
        return ftlModel;
    }

    /**
     * Applies custom properties.
     */
    protected Map<String, Object> createPropertyDefinition(WebScriptRequest req, JSONObject json)
            throws JSONException
    {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Serializable> params = getParamsFromUrlAndJson(req, json);
        
        QName propertyQName = createNewPropertyDefinition(params);
        String localName = propertyQName.getLocalName();
        
        result.put(PROP_ID, localName);
    
        String urlResult = req.getServicePath() + "/" + propertyQName.getLocalName();
        result.put(URL, urlResult);
    
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Serializable> getParamsFromUrlAndJson(WebScriptRequest req, JSONObject json)
            throws JSONException
    {
        Map<String, Serializable> params;
        params = new HashMap<String, Serializable>();
        params.put(PARAM_ELEMENT, req.getParameter(PARAM_ELEMENT));
        
        for (Iterator iter = json.keys(); iter.hasNext(); )
        {
            String nextKeyString = (String)iter.next();
            String nextValueString = json.getString(nextKeyString);
            
            params.put(nextKeyString, nextValueString);
        }
        
        return params;
    }

    protected QName createNewPropertyDefinition(Map<String, Serializable> params)
    {
        // Need to select the correct aspect in the customModel to which we'll add the property.
        String customisableElement = (String)params.get(PARAM_ELEMENT);
        CustomisableRmElement ce = CustomisableRmElement.getEnumFor(customisableElement);
        String aspectName = ce.getCorrespondingAspect();
        
        String label = (String)params.get(PARAM_LABEL);
        
        //According to the wireframes, type here can only be date|text|number
        Serializable serializableParam = params.get(PARAM_DATATYPE);
        QName type = null;
        if (serializableParam != null)
        {
            if (serializableParam instanceof String)
            {
                type = QName.createQName((String)serializableParam, namespaceService);
            }
            else if (serializableParam instanceof QName)
            {
                type = (QName)serializableParam;
            }
            else
            {
                throw new AlfrescoRuntimeException("Unexpected type of dataType param: "+serializableParam+" (expected String or QName)");
            }
        }
        
        // The title is actually generated, so this parameter will be ignored
        // by the RMAdminService
        String title = (String)params.get(PARAM_TITLE);
        String description = (String)params.get(PARAM_DESCRIPTION);
        String defaultValue = (String)params.get(PARAM_DEFAULT_VALUE);
        
        boolean mandatory = false;
        serializableParam = params.get(PARAM_MANDATORY);
        if (serializableParam != null)
        {
            mandatory = Boolean.valueOf(serializableParam.toString());
        }
        
        boolean isProtected = false;
        serializableParam = params.get(PARAM_PROTECTED);
        if (serializableParam != null)
        {
            isProtected = Boolean.valueOf(serializableParam.toString());
        }
        
        boolean multiValued = false;
        serializableParam = params.get(PARAM_MULTI_VALUED);
        if (serializableParam != null)
        {
            multiValued = Boolean.valueOf(serializableParam.toString());
        }
        
        serializableParam = params.get(PARAM_CONSTRAINT_REF);
        QName constraintRef = null;
        if (serializableParam != null)
        {
            if (serializableParam instanceof String)
            {
                constraintRef = QName.createQName((String)serializableParam, namespaceService);
            }
            else if (serializableParam instanceof QName)
            {
                constraintRef = (QName)serializableParam;
            }
            else
            {
                throw new AlfrescoRuntimeException("Unexpected type of constraintRef param: "+serializableParam+" (expected String or QName)");
            }
        }
        
        // if propId is specified, use it.
        QName proposedQName = null;
        String propId = (String)params.get(PROP_ID);
        if (propId != null)
        {
            proposedQName = QName.createQName(RecordsManagementCustomModel.RM_CUSTOM_PREFIX, propId, namespaceService);
        }
        return rmAdminService.addCustomPropertyDefinition(proposedQName, aspectName, label, type,
            title, description, defaultValue, multiValued, mandatory, isProtected, constraintRef);
    }
}