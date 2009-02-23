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
package org.alfresco.repo.web.scripts.forms;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;

public abstract class AbstractTestFormRestApi extends BaseWebScriptTest
{
    protected static final String TEST_FORM_DESCRIPTION = "Test form description";
    protected static final String TEST_FORM_TITLE = "Test form title";
    protected String testNodeUrl;
    protected NodeRef testNodeRef;
    protected Map<QName, Serializable> testNodePropertiesAfterCreation;

    protected NodeService nodeService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private Repository repositoryHelper;

    public AbstractTestFormRestApi()
    {
        super();
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.fileFolderService = (FileFolderService) getServer().getApplicationContext().getBean(
                "FileFolderService");
        this.contentService = (ContentService) getServer().getApplicationContext().getBean(
                "ContentService");
        this.repositoryHelper = (Repository) getServer().getApplicationContext().getBean(
                "repositoryHelper");
        this.nodeService = (NodeService) getServer().getApplicationContext().getBean("NodeService");

        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());

        NodeRef companyHomeNodeRef = this.repositoryHelper.getCompanyHome();

        String guid = GUID.generate();

        FileInfo fileInfoTestNode = this.fileFolderService.create(companyHomeNodeRef,
                "test_forms_doc" + guid + ".txt", ContentModel.TYPE_CONTENT);
        testNodeRef = fileInfoTestNode.getNodeRef();

        // Add an aspect.
        Map<QName, Serializable> aspectProps = new HashMap<QName, Serializable>(2);
        aspectProps.put(ContentModel.PROP_TITLE, TEST_FORM_TITLE);
        aspectProps.put(ContentModel.PROP_DESCRIPTION, TEST_FORM_DESCRIPTION);
        nodeService.addAspect(testNodeRef, ContentModel.ASPECT_TITLED, aspectProps);

        // Write some content into the node.
        ContentWriter contentWriter = this.contentService.getWriter(fileInfoTestNode.getNodeRef(),
                ContentModel.PROP_CONTENT, true);
        contentWriter.setEncoding("UTF-8");
        contentWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        contentWriter.putContent("The quick brown fox jumped over the lazy dog.");

        // Create and store the path
        StringBuilder builder = new StringBuilder();
        builder.append("/api/forms/node/workspace/").append(testNodeRef.getStoreRef().getIdentifier())
                .append("/").append(testNodeRef.getId());
        this.testNodeUrl = builder.toString();
        
        // Store the original properties of this node
        this.testNodePropertiesAfterCreation = nodeService.getProperties(testNodeRef);
    }

    @Override
    public void tearDown()
    {
        nodeService.deleteNode(testNodeRef);
    }

    protected Response sendGetReq(String url, int expectedStatusCode)
            throws IOException, UnsupportedEncodingException
    {
        Response result = sendRequest(new GetRequest(url), expectedStatusCode);
        return result;
    }
}