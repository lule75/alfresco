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
package org.alfresco.repo.version;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.debug.NodeStoreInspector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Roy Wetherall
 */
public class NodeServiceImplTest extends BaseVersionStoreTest 
{
    private static Log logger = LogFactory.getLog(NodeServiceImplTest.class);
    
	/**
	 * Light weight version store node service
	 */
	protected NodeService lightWeightVersionStoreNodeService = null;
	
	/**
	 * Error message
	 */
	private final static String MSG_ERR = 
        "This operation is not supported by a version store implementation of the node service.";
	
	/**
	 * Dummy data used in failure tests
	 */
	private NodeRef dummyNodeRef = null;
	private QName dummyQName = null;
	
	/**
     * Called during the transaction setup
     */
    protected void onSetUpInTransaction() throws Exception
    {
		super.onSetUpInTransaction();
		
        // Get the node service by name
        this.lightWeightVersionStoreNodeService = (NodeService)this.applicationContext.getBean("versionNodeService");
        
        // Create some dummy data used during the tests
        this.dummyNodeRef = new NodeRef(
				this.versionService.getVersionStoreReference(),
				"dummy");
		this.dummyQName = QName.createQName("{dummy}dummy");
    }
	
    /**
     * Test getType
     */
	public void testGetType()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
	
        // Get the type from the versioned state
        QName versionedType = this.lightWeightVersionStoreNodeService.getType(version.getFrozenStateNodeRef());
        assertNotNull(versionedType);
        assertEquals(this.dbNodeService.getType(versionableNode), versionedType);
    }
    
    /**
     * Test getProperties
     */
    public void testGetProperties()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Get a list of the nodes properties
        Map<QName, Serializable> origProps = this.dbNodeService.getProperties(versionableNode);
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        
        // Get the properties of the versioned state 
        Map<QName, Serializable> versionedProperties = this.lightWeightVersionStoreNodeService.getProperties(version.getFrozenStateNodeRef());
        
        if (logger.isDebugEnabled())
        {
            logger.debug("original ("+origProps.size()+"):  " + origProps.keySet());
            logger.debug("versioned ("+versionedProperties.size()+"): " + versionedProperties.keySet());
        }
        
        for (QName key : origProps.keySet())
        {
            assertTrue(versionedProperties.containsKey(key));
            assertEquals(""+key, origProps.get(key), versionedProperties.get(key));
        }
        
        // NOTE: cm:versionLabel is an expected additional property
        //assertEquals(origProps.size(), versionedProperties.size());
        
        // check version label
        assertEquals("1.0", versionedProperties.get(ContentModel.PROP_VERSION_LABEL));
        
        // TODO do futher versioning and check by changing values
    }
    
    /**
     * Test getProperty
     */
    public void testGetProperty()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        
        // Check the property values can be retrieved
        Serializable value1 = this.lightWeightVersionStoreNodeService.getProperty(
                version.getFrozenStateNodeRef(),
                PROP_1);
        assertEquals(VALUE_1, value1);
        
        // Check the mlText property
        // TODO
        
        // Check the multi values property specifically
        Collection<String> multiValue = (Collection<String>)this.lightWeightVersionStoreNodeService.getProperty(version.getFrozenStateNodeRef(), MULTI_PROP);
        assertNotNull(multiValue);
        assertEquals(2, multiValue.size());
        String[] array = multiValue.toArray(new String[multiValue.size()]);
        assertEquals(MULTI_VALUE_1, array[0]);
        assertEquals(MULTI_VALUE_2, array[1]);
    }
    
    /**
     * Test getChildAssocs
     */
    public void testGetChildAssocs()
    {
        if (logger.isTraceEnabled())
        {
            // Let's have a look at the version store ..
            logger.trace(NodeStoreInspector.dumpNodeStore(
                    this.dbNodeService, 
                    this.versionService.getVersionStoreReference()) + "\n\n");
            logger.trace("");
        }
        
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        Collection<ChildAssociationRef> originalChildren = this.dbNodeService.getChildAssocs(versionableNode);
        assertNotNull(originalChildren);
        
        // Store the original children in a map for easy navigation later
        HashMap<String, ChildAssociationRef> originalChildAssocRefs = new HashMap<String, ChildAssociationRef>();
        for (ChildAssociationRef ref : originalChildren)
        {
            originalChildAssocRefs.put(ref.getChildRef().getId(), ref);
        }
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        
        if (logger.isTraceEnabled())
        {
            // Let's have a look at the version store ..
            logger.trace(NodeStoreInspector.dumpNodeStore(
                    this.dbNodeService,
                    this.versionService.getVersionStoreReference()));
            logger.trace("");
        }
        
        // Get the children of the versioned node
        Collection<ChildAssociationRef> versionedChildren = this.lightWeightVersionStoreNodeService.getChildAssocs(version.getFrozenStateNodeRef());
        assertNotNull(versionedChildren);
        assertEquals(originalChildren.size(), versionedChildren.size());
        
        for (ChildAssociationRef versionedChildRef : versionedChildren)
        {
            ChildAssociationRef origChildAssocRef = originalChildAssocRefs.get(versionedChildRef.getChildRef().getId());
            assertNotNull(origChildAssocRef);
                        
            assertEquals(
                    origChildAssocRef.getChildRef(),
                    versionedChildRef.getChildRef());
            assertEquals(
                    origChildAssocRef.isPrimary(),
                    versionedChildRef.isPrimary());
            assertEquals(
                    origChildAssocRef.getNthSibling(),
                    versionedChildRef.getNthSibling());
        }
    }
    
    /**
     * Test getAssociationTargets
     * 
     * @deprecated
     */
    public void testGetAssociationTargets()
    {
        // Switch VersionStore depending on configured impl
        if (versionService.getVersionStoreReference().getIdentifier().equals(Version2Model.STORE_ID))
        {
        	// V2 version store (eg. workspace://version2Store)
            // See ETHREEOH-3632: method now doesn't blow up.
            List<AssociationRef> assocs = this.lightWeightVersionStoreNodeService.getTargetAssocs(
                    dummyNodeRef,
                    RegexQNamePattern.MATCH_ALL);
            assertEquals("Currently the assocs will be empty", 0, assocs.size());
        }
        else if (versionService.getVersionStoreReference().getIdentifier().equals(VersionModel.STORE_ID))
        {
        	// Deprecated V1 version store (eg. workspace://lightWeightVersionStore)
        	
            // Create a new versionable node
            NodeRef versionableNode = createNewVersionableNode();
            
            // Store the current details of the target associations
            List<AssociationRef> origAssocs = this.dbNodeService.getTargetAssocs(
                    versionableNode,
                    RegexQNamePattern.MATCH_ALL);
            
            // Create a new version
            Version version = createVersion(versionableNode, this.versionProperties);
    
            List<AssociationRef> assocs = this.lightWeightVersionStoreNodeService.getTargetAssocs(
                    version.getFrozenStateNodeRef(), 
                    RegexQNamePattern.MATCH_ALL);
            assertNotNull(assocs);
            assertEquals(origAssocs.size(), assocs.size());
        }
    }
    
    /**
     * Test hasAspect
     */
    public void testHasAspect()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        
        boolean test1 = this.lightWeightVersionStoreNodeService.hasAspect(
                version.getFrozenStateNodeRef(), 
                ApplicationModel.ASPECT_UIFACETS);
        assertFalse(test1);
        
        boolean test2 = this.lightWeightVersionStoreNodeService.hasAspect(
                version.getFrozenStateNodeRef(),
                ContentModel.ASPECT_VERSIONABLE);
        assertTrue(test2);
    }

    /**
     * Test getAspects
     */
    public void testGetAspects() 
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        Set<QName> origAspects = this.dbNodeService.getAspects(versionableNode);
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        
        Set<QName> aspects = this.lightWeightVersionStoreNodeService.getAspects(version.getFrozenStateNodeRef());
        assertEquals(origAspects.size(), aspects.size());
        
        for (QName origAspect : origAspects)
        { 
            assertTrue(origAspect+"",aspects.contains(origAspect));
        }
    }
	
    /**
     * Test getParentAssocs
     */
    public void testGetParentAssocs()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        NodeRef nodeRef = version.getFrozenStateNodeRef();
        
        List<ChildAssociationRef> results = this.lightWeightVersionStoreNodeService.getParentAssocs(nodeRef);
        assertNotNull(results);
        assertEquals(1, results.size());
        ChildAssociationRef childAssoc = results.get(0);
        assertEquals(nodeRef, childAssoc.getChildRef());
        NodeRef versionStoreRoot = this.dbNodeService.getRootNode(this.versionService.getVersionStoreReference());
        assertEquals(versionStoreRoot, childAssoc.getParentRef());
    }
    
    /**
     * Test getPrimaryParent
     */
    public void testGetPrimaryParent()
    {
        // Create a new versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Create a new version
        Version version = createVersion(versionableNode, this.versionProperties);
        NodeRef nodeRef = version.getFrozenStateNodeRef();
        
        ChildAssociationRef childAssoc = this.lightWeightVersionStoreNodeService.getPrimaryParent(nodeRef);
        assertNotNull(childAssoc);
        assertEquals(nodeRef, childAssoc.getChildRef());
        NodeRef versionStoreRoot = this.dbNodeService.getRootNode(this.versionService.getVersionStoreReference());
        assertEquals(versionStoreRoot, childAssoc.getParentRef());        
    }
    
	/** ================================================
	 *  These test ensure that the following operations
	 *  are not supported as expected.
	 */
	
	/**
	 * Test createNode
	 */
	public void testCreateNode()
    {
		try
		{
			this.lightWeightVersionStoreNodeService.createNode(
					dummyNodeRef,
					null,
					dummyQName,
                    ContentModel.TYPE_CONTENT);
			fail("This operation is not supported.");
		}
		catch (UnsupportedOperationException exception)
		{
			if (exception.getMessage() != MSG_ERR)
			{
				fail("Unexpected exception raised during method excution: " + exception.getMessage());
			}
		}
    }
    
    /**
     * Test addAspect
     */
    public void testAddAspect()
    {
        try
        {
            this.lightWeightVersionStoreNodeService.addAspect(
                    dummyNodeRef,
                    TEST_ASPECT_QNAME,
                    null);
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }
    
    /**
     * Test removeAspect
     */
    public void testRemoveAspect() 
    {
        try
        {
            this.lightWeightVersionStoreNodeService.removeAspect(
                    dummyNodeRef,
                    TEST_ASPECT_QNAME);
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }
    
	/**
	 * Test delete node
	 */
    public void testDeleteNode()
    {
		try
		{
			this.lightWeightVersionStoreNodeService.deleteNode(this.dummyNodeRef);
			fail("This operation is not supported.");
		}
		catch (UnsupportedOperationException exception)
		{
			if (exception.getMessage() != MSG_ERR)
			{
				fail("Unexpected exception raised during method excution: " + exception.getMessage());
			}
		}
    }
    
	/**
	 * Test addChild
	 */
    public void testAddChild()
    {
		try
		{
			this.lightWeightVersionStoreNodeService.addChild(
					this.dummyNodeRef,
					this.dummyNodeRef,
                    this.dummyQName,
					this.dummyQName);
			fail("This operation is not supported.");
		}
		catch (UnsupportedOperationException exception)
		{
			if (exception.getMessage() != MSG_ERR)
			{
				fail("Unexpected exception raised during method excution: " + exception.getMessage());
			}
		}
    }
    
	/**
	 * Test removeChild
	 */
    public void testRemoveChild()
    {
		try
		{
			this.lightWeightVersionStoreNodeService.removeChild(
					this.dummyNodeRef, 
					this.dummyNodeRef);
			fail("This operation is not supported.");
		}
		catch (UnsupportedOperationException exception)
		{
			if (exception.getMessage() != MSG_ERR)
			{
				fail("Unexpected exception raised during method excution: " + exception.getMessage());
			}
		}	
    }
    
    /**
     * Test setProperties
     */
    public void testSetProperties()
    {
        try
        {
            this.lightWeightVersionStoreNodeService.setProperties(
                    this.dummyNodeRef,
                    new HashMap<QName, Serializable>());
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }
    
    /**
     * Test setProperty
     */
    public void testSetProperty()
	{
        try
        {
            this.lightWeightVersionStoreNodeService.setProperty(
                    this.dummyNodeRef,
                    this.dummyQName,
                    "dummy");
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }   
    
    /**
     * Test createAssociation
     */
    public void testCreateAssociation()
    {
        try
        {
            this.lightWeightVersionStoreNodeService.createAssociation(
                    this.dummyNodeRef,
                    this.dummyNodeRef,
                    this.dummyQName);
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }
    
    /**
     * Test removeAssociation
     */
    public void testRemoveAssociation()
    {
        try
        {
            this.lightWeightVersionStoreNodeService.removeAssociation(
                    this.dummyNodeRef,
                    this.dummyNodeRef,
                    this.dummyQName);
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }       
    
    /**
     * Test getAssociationSources
     */
    public void testGetAssociationSources()
    {
        try
        {
            this.lightWeightVersionStoreNodeService.getSourceAssocs(
                    this.dummyNodeRef,
                    this.dummyQName);
            fail("This operation is not supported.");
        }
        catch (UnsupportedOperationException exception)
        {
            if (exception.getMessage() != MSG_ERR)
            {
                fail("Unexpected exception raised during method excution: " + exception.getMessage());
            }
        }
    }
    
    /**
     * Test getPath
     */
    public void testGetPath()
    {
        Path path = this.lightWeightVersionStoreNodeService.getPath(this.dummyNodeRef);
    }
    
    /**
     * Test getPaths
     */
    public void testGetPaths()
    {
        List<Path> paths = this.lightWeightVersionStoreNodeService.getPaths(this.dummyNodeRef, false);
    }
}
