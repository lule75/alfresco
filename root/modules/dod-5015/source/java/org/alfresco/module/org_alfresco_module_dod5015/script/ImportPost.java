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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.repo.importer.ACPImportPackageHandler;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.util.TempFileProvider;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Imports an ACP file into a records management container.
 * 
 * @author Gavin Cornwell
 */
public class ImportPost extends DeclarativeWebScript
{
    /** Logger */
    private static Log logger = LogFactory.getLog(ImportPost.class);
    
    protected static final String MULTIPART_FORMDATA = "multipart/form-data";
    protected static final String PARAM_DESTINATION = "destination";
    protected static final String PARAM_ARCHIVE = "archive";
    protected static final String TEMP_FILE_PREFIX = "import_";
    
    protected NodeService nodeService;
    protected DictionaryService dictionaryService;
    protected ImporterService importerService;
    protected RecordsManagementService rmService;
    protected RecordsManagementSecurityService rmSecurityService;
    
    /**
     * @param nodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService; 
    }

    /**
     * Sets the data dictionary service
     * 
     * @param dictionaryService The DictionaryService instance
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    /**
     * Sets the ImporterService to use
     * 
     * @param importerService The ImporterService
     */
    public void setImporterService(ImporterService importerService) 
    {
        this.importerService = importerService;
    }
    
    /**
     * Sets the RecordsManagementSecurityService instance
     * 
     * @param rmSecurityService The RecordsManagementSecurityService instance
     */
    public void setRecordsManagementSecurityService(RecordsManagementSecurityService rmSecurityService)
    {
        this.rmSecurityService = rmSecurityService;
    }
    
    /**
     * Sets the RecordsManagementService instance
     * 
     * @param rmService The RecordsManagementService instance
     */
    public void setRecordsManagementService(RecordsManagementService rmService)
    {
        this.rmService = rmService;
    }
    
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // get the content type of request and ensure it's multipart/form-data
        String contentType = req.getContentType();
        if (MULTIPART_FORMDATA.equals(contentType) && req instanceof WebScriptServletRequest)
        {
            String nodeRef = req.getParameter(PARAM_DESTINATION);
            
            if (nodeRef == null || nodeRef.length() == 0)
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, 
                            "Mandatory 'destination' parameter was not provided in form data");
            }
            
            // create and check noderef
            final NodeRef destination = new NodeRef(nodeRef);
            if (nodeService.exists(destination))
            {
                // check the destination is an RM container
                if (!nodeService.hasAspect(destination, RecordsManagementModel.ASPECT_FILE_PLAN_COMPONENT) ||
                    !dictionaryService.isSubClass(nodeService.getType(destination), ContentModel.TYPE_FOLDER))
                {
                    throw new WebScriptException(Status.STATUS_BAD_REQUEST, 
                                "NodeRef '" + destination + "' does not represent an Records Management container node.");
                }
            }
            else
            {
                status.setCode(HttpServletResponse.SC_NOT_FOUND, 
                            "NodeRef '" + destination + "' does not exist.");
            }
            
            // as there is no 'import capability' and the RM admin user is different from
            // the DM admin user (meaning the webscript 'admin' authentication can't be used)
            // perform a manual check here to ensure the current user has the RM admin role.
            boolean isAdmin = this.rmSecurityService.hasRMAdminRole(
                        this.rmService.getRecordsManagementRoot(destination), 
                        AuthenticationUtil.getRunAsUser());
            if (!isAdmin)
            {
                throw new WebScriptException(Status.STATUS_FORBIDDEN, "Access Denied");
            }
            
            File acpFile = null;
            try
            {
                // create a temporary file representing uploaded ACP file 
                FormField acpContent = ((WebScriptServletRequest)req).getFileField(PARAM_ARCHIVE);
                if (acpContent == null)
                {
                    throw new WebScriptException(Status.STATUS_BAD_REQUEST, 
                                "Mandatory 'archive' file content was not provided in form data");
                }
                
                acpFile = TempFileProvider.createTempFile(TEMP_FILE_PREFIX, "." + ACPExportPackageHandler.ACP_EXTENSION);
                
                // copy contents of uploaded file to temp ACP file
                FileOutputStream fos = new FileOutputStream(acpFile);
                FileCopyUtils.copy(acpContent.getInputStream(), fos);   // NOTE: this method closes both streams
                        
                if (logger.isDebugEnabled())
                    logger.debug("Importing uploaded ACP (" + acpFile.getAbsolutePath() + ") into " + nodeRef);
                
                // setup the import handler
                final ACPImportPackageHandler importHandler = new ACPImportPackageHandler(acpFile, "UTF-8");
                
                // import the ACP file as the system user
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>()
                {
                     public NodeRef doWork() throws Exception
                     {
                         importerService.importView(importHandler, new Location(destination), null, null);
                         return null;
                     }
                }, AuthenticationUtil.getSystemUserName());
                
                // create and return model
                Map<String, Object> model = new HashMap<String, Object>(1);
                model.put("success", true);
                return model;
            }
            catch (FileNotFoundException fnfe)
            {
                throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, 
                            "Failed to import ACP file", fnfe);
            }
            catch (IOException ioe)
            {
                throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, 
                            "Failed to import ACP file", ioe);
            }
            finally
            {
                if (acpFile != null)
                {
                    acpFile.delete();
                }
            }
        }
        else
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                        "Request is not " + MULTIPART_FORMDATA + " encoded");
        }
    }
}