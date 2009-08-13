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
package org.alfresco.module.org_alfresco_module_dod5015.caveat;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.EventCompletionDetails;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementSearchBehaviour;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.VitalRecordDefinition;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.action.impl.CompleteEventAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.impl.FreezeAction;
import org.alfresco.module.org_alfresco_module_dod5015.capability.RMPermissionModel;
import org.alfresco.module.org_alfresco_module_dod5015.caveat.RMCaveatConfigImpl;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PublicServiceAccessService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.PropertyMap;

/**
 * @author Mark Rogers
 */
public class RMCaveatConfigImplTest extends BaseSpringTest implements DOD5015Model
{    
	protected static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	private NodeRef filePlan;
	
	private NodeService nodeService;
	private TransactionService transactionService;
	private RMCaveatConfigImpl caveatConfigImpl;
	
	private AuthenticationService authenticationService;
	private PersonService personService;
	private AuthorityService authorityService;

	
	// example base test data for supplemental markings list (see also recordsModel.xml)
	protected final static String NOFORN     = "NOFORN";     // Not Releasable to Foreign Nationals/Governments/Non-US Citizens
	protected final static String NOCONTRACT = "NOCONTRACT"; // Not Releasable to Contractors or Contractor/Consultants
	protected final static String FOUO       = "FOUO";       // For Official Use Only 
	protected final static String FGI        = "FGI";        // Foreign Government Information
	
	protected final static String RM_LIST = "rma:smList";
	protected final static String RM_LIST_ALT = "rma:prjList"; // valid but empty list
	
	@Override
	protected void onSetUpInTransaction() throws Exception 
	{
		super.onSetUpInTransaction();

		// Get the service required in the tests
		this.nodeService = (NodeService)this.applicationContext.getBean("NodeService"); // use upper 'N'odeService (to test access config interceptor)	
		this.authenticationService = (AuthenticationService)this.applicationContext.getBean("AuthenticationService");
		this.personService = (PersonService)this.applicationContext.getBean("PersonService");
		this.authorityService = (AuthorityService)this.applicationContext.getBean("AuthorityService");
		this.caveatConfigImpl = (RMCaveatConfigImpl)this.applicationContext.getBean("caveatConfigImpl");

		this.transactionService = (TransactionService)this.applicationContext.getBean("TransactionService");
		
		
		// Set the current security context as admin
		AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
		
		// Get the test data
		setUpTestData();
        
        URL url = AbstractContentTransformerTest.class.getClassLoader().getResource("testCaveatConfig1.json"); // from test-resources
        assertNotNull(url);
        File file = new File(url.getFile());
        assertTrue(file.exists());
        
        caveatConfigImpl.updateOrCreateCaveatConfig(file);
	}
	
	private void setUpTestData()
	{
	}

    @Override
    protected void onTearDownInTransaction() throws Exception
    {
        try
        {
            UserTransaction txn = transactionService.getUserTransaction(false);
            txn.begin();
            this.nodeService.deleteNode(filePlan);
            txn.commit();
        }
        catch (Exception e)
        {
            // Nothing
            //System.out.println("DID NOT DELETE FILE PLAN!");
        }
    }
    
    @Override
    protected void onTearDownAfterTransaction() throws Exception
    {
        // TODO Auto-generated method stub
        super.onTearDownAfterTransaction();
    }
    
    public void testSetup()
    {
        // NOOP
    }    
     	
 
    /**
     * Test of Caveat Config
     * 
     * @throws Exception
     */
    public void testAddRMConstraintList() throws Exception
    {
        setComplete();
        endTransaction();
        
        cleanCaveatConfigData();
        
        startNewTransaction();
       
        /**
         * Now remove the entire list (rma:smList);
         */
        logger.debug("test remove entire list rma:smList");
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        
        /**
         * Now add the list again
         */
        logger.debug("test add back rma:smList");
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Negative test - add a list that already exists
         */
        logger.debug("try to create duplicate list rma:smList");
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Negative test - remove a list that does not exist
         */
        logger.debug("test remove entire list rma:smList");
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        
        
        /**
         * Negative test - add a constraint to property that does not exist
         */
        logger.debug("test property does not exist");
        try 
        {
            caveatConfigImpl.addRMConstraintList("rma:mer");
            fail("unknown property should have thrown an exception");
        }
        catch (Exception e)
        {
            // expect to go here
        }
        endTransaction();
        cleanCaveatConfigData();
 
    }
    
    /**
     * Test of addRMConstraintListValue
     * 
     * @throws Exception
     */
    public void testAddRMConstraintListValue() throws Exception
    {
        setComplete();
        endTransaction();
        
        cleanCaveatConfigData();
        setupCaveatConfigData();
        
        startNewTransaction();
       
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Add a user to the list
         */
        List<String> values = new ArrayList<String>();
        values.add(NOFORN);
        values.add(NOCONTRACT);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "jrogers", values);
        
        /**
         * Add another value to that list
         */
        caveatConfigImpl.addRMConstraintListValue(RM_LIST, "jrogers", FGI);
     
        /**
         * Negative test - attempt to add a duplicate value
         */
        caveatConfigImpl.addRMConstraintListValue(RM_LIST, "jrogers", FGI);
        
        /**
         * Negative test - attempt to add to a list that does not exist
         */
        try
        {
            caveatConfigImpl.addRMConstraintListValue(RM_LIST_ALT, "mhouse", FGI);
            fail("exception not thrown");
        } 
        catch (Exception re)
        {
            // should go here
            
        }
        
        /**
         * Negative test - attempt to add to a list that does exist and user that does not exist
         */
        try 
        {
            caveatConfigImpl.addRMConstraintListValue(RM_LIST, "mhouse", FGI);
            fail("exception not thrown");
        }
        catch (Exception e)
        {
            // should go here
        }
        
    }
    

    /**
     * Test of UpdateRMConstraintListAuthority
     * 
     * @throws Exception
     */
    public void testUpdateRMConstraintListAuthority() throws Exception
    {
        setComplete();
        endTransaction();
        
        cleanCaveatConfigData();
        setupCaveatConfigData();
        
        startNewTransaction();
       
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Add a user to the list
         */
        List<String> values = new ArrayList<String>();
        values.add(NOFORN);
        values.add(NOCONTRACT);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "jrogers", values);
        
        /**
         * Add to a authority that already exists
         * Should replace existing authority
         */
        List<String> updatedValues = new ArrayList<String>();
        values.add(FGI);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "jrogers", updatedValues);
        
        /**
         * Add a group to the list
         */
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "Engineering", values);
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        
        /**
         * Add to a list that does not exist
         * Should create a new list
         */
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "jrogers", values);
        
        
        /**
         * Add to a authority that already exists
         * Should replace existing authority
         */
        
        endTransaction();
        cleanCaveatConfigData();
 
    }
    
    /**
     * Test of RemoveRMConstraintListAuthority
     * 
     * @throws Exception
     */
    public void testRemoveRMConstraintListAuthority() throws Exception
    {
        setComplete();
        endTransaction();
        
        cleanCaveatConfigData();
        setupCaveatConfigData();
        
        startNewTransaction();
       
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        List<String> values = new ArrayList<String>();
        values.add(FGI);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST, "jrogers", values);
        
        /**
         * Remove a user from a list
         */
        caveatConfigImpl.removeRMConstraintListAuthority(RM_LIST, "jrogers");
        
        /**
         * Negative test - remove a user that does not exist
         */
        caveatConfigImpl.removeRMConstraintListAuthority(RM_LIST, "jrogers");
        
        /**
         * Negative test - remove a user from a list that does not exist.
         * Should create a new list
         */
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        caveatConfigImpl.updateRMConstraintListAuthority(RM_LIST_ALT, "jrogers", values);
           
        endTransaction();
        cleanCaveatConfigData();
 
    }

 
 
    
    /**
     * Test of Caveat Config
     * 
     * @throws Exception
     */
    public void testRMCaveatConfig() throws Exception
    {
        setComplete();
        endTransaction();
        
        cleanCaveatConfigData();
        setupCaveatConfigData();
        
        startNewTransaction();
        
        // Test list of allowed values for caveats
        
        List<String> allowedValues = AuthenticationUtil.runAs(new RunAsWork<List<String>>()
        {
            public List<String> doWork()
            {
                // get allowed values for given caveat (for current user)
                return caveatConfigImpl.getRMAllowedValues(RM_LIST);
            }
        }, "dfranco");
        
        assertEquals(2, allowedValues.size());
        assertTrue(allowedValues.contains(NOFORN));
        assertTrue(allowedValues.contains(FOUO));
        
        
        allowedValues = AuthenticationUtil.runAs(new RunAsWork<List<String>>()
        {
            public List<String> doWork()
            {
                // get allowed values for given caveat (for current user)
                return caveatConfigImpl.getRMAllowedValues(RM_LIST);
            }
        }, "dmartinz");
        
        assertEquals(4, allowedValues.size());
        assertTrue(allowedValues.contains(NOFORN));
        assertTrue(allowedValues.contains(NOCONTRACT));
        assertTrue(allowedValues.contains(FOUO));
        assertTrue(allowedValues.contains(FGI));   
       
        /**
         * Now remove the entire list (rma:smList);
         */
        logger.debug("test remove entire list rma:smList");
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        
        
        /**
         * Now add the list again
         */
        logger.debug("test add back rma:smList");
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Negative test - add a list that already exists
         */
        logger.debug("try to create duplicate list rma:smList");
        caveatConfigImpl.addRMConstraintList(RM_LIST);
        
        /**
         * Negative test - remove a list that does not exist
         */
        logger.debug("test remove entire list rma:smList");
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        caveatConfigImpl.deleteRMConstraintList(RM_LIST);
        
        
        /**
         * Negative test - add a constraint to property that does not exist
         */
        logger.debug("test property does not exist");
        try 
        {
            caveatConfigImpl.addRMConstraintList("rma:mer");
            fail("unknown property should have thrown an exception");
        }
        catch (Exception e)
        {
            // expect to go here
        }
        endTransaction();
        cleanCaveatConfigData();
 
    }
    
    private void cleanCaveatConfigData()
    {
        startNewTransaction();
        
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        deleteUser("jrangel");
        deleteUser("dmartinz");
        deleteUser("jrogers");
        deleteUser("hmcneil");
        deleteUser("dfranco");
        deleteUser("gsmith");
        deleteUser("eharris");
        deleteUser("bbayless");
        deleteUser("mhouse");
        deleteUser("aly");
        deleteUser("dsandy");
        deleteUser("driggs");
        deleteUser("test1");
        
        deleteGroup("Engineering");
        deleteGroup("Finance");
        deleteGroup("test1");
        
        caveatConfigImpl.updateOrCreateCaveatConfig("{}"); // empty config !
        
        setComplete();
        endTransaction();
    }
    
    private void setupCaveatConfigData()
    {
        startNewTransaction();
        
        // Switch to admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // Create test users/groups (if they do not already exist)
        
        createUser("jrangel");
        createUser("dmartinz");
        createUser("jrogers");
        createUser("hmcneil");
        createUser("dfranco");
        createUser("gsmith");
        createUser("eharris");
        createUser("bbayless");
        createUser("mhouse");
        createUser("aly");
        createUser("dsandy");
        createUser("driggs");
        createUser("test1");
        
        createGroup("Engineering");
        createGroup("Finance");
        createGroup("test1");
        
        addToGroup("jrogers", "Engineering");
        addToGroup("dfranco", "Finance");
        
        // not in grouo to start with - added later
        //addToGroup("gsmith", "Engineering");
        
        
        URL url = AbstractContentTransformerTest.class.getClassLoader().getResource("testCaveatConfig2.json"); // from test-resources
        assertNotNull(url);
        File file = new File(url.getFile());
        assertTrue(file.exists());
        
        caveatConfigImpl.updateOrCreateCaveatConfig(file);
        
        setComplete();
        endTransaction();
    }
    
    protected void createUser(String userName)
    {
        if (! authenticationService.authenticationExists(userName))
        {
            authenticationService.createAuthentication(userName, "PWD".toCharArray());
        }
        
        if (! personService.personExists(userName))
        {
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            personService.createPerson(ppOne);
        }
    }
    
    protected void deleteUser(String userName)
    {
        if (personService.personExists(userName))
        {
            personService.deletePerson(userName);
        }
        
        if (authenticationService.authenticationExists(userName))
        {
            authenticationService.deleteAuthentication(userName);
        }
    }
    
    protected void createGroup(String groupShortName)
    {
        createGroup(null, groupShortName);
    }
    
    protected void createGroup(String parentGroupShortName, String groupShortName)
    {
        if (parentGroupShortName != null)
        {
            String parentGroupFullName = authorityService.getName(AuthorityType.GROUP, parentGroupShortName);
            if (authorityService.authorityExists(parentGroupFullName) == false)
            {
                authorityService.createAuthority(AuthorityType.GROUP, groupShortName, groupShortName, null);
                authorityService.addAuthority(parentGroupFullName, groupShortName);
            }
        }
        else
        {
            authorityService.createAuthority(AuthorityType.GROUP, groupShortName, groupShortName, null);
        }
    }
    
    protected void deleteGroup(String groupShortName)
    {
        String groupFullName = authorityService.getName(AuthorityType.GROUP, groupShortName);
        if (authorityService.authorityExists(groupFullName) == true)
        {
            authorityService.deleteAuthority(groupFullName);
        }
    }
    
    protected void addToGroup(String authorityName, String groupShortName)
    {
        authorityService.addAuthority(authorityService.getName(AuthorityType.GROUP, groupShortName), authorityName);
    }
    
    protected void removeFromGroup(String authorityName, String groupShortName)
    {
        authorityService.removeAuthority(authorityService.getName(AuthorityType.GROUP, groupShortName), authorityName);
    }
 
}