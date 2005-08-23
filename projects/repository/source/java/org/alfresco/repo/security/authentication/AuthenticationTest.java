/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.security.authentication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationManager;
import net.sf.acegisecurity.BadCredentialsException;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.SaltSource;
import net.sf.acegisecurity.providers.encoding.PasswordEncoder;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

public class AuthenticationTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private NodeService nodeService;
    
    private SearchService searchService;

    private NodeRef rootNodeRef;

    private NodeRef systemNodeRef;

    private NodeRef typesNodeRef;

    private NodeRef personAndyNodeRef;

    private DictionaryService dictionaryService;

    private PasswordEncoder passwordEncoder;

    private RepositoryAuthenticationDao dao;

    private AuthenticationManager authenticationManager;

    private SaltSource saltSource;

    private TicketComponent ticketComponent;

    private AuthenticationService authenticationService;

    public AuthenticationTest()
    {
        super();
    }

    public AuthenticationTest(String arg0)
    {
        super(arg0);
    }

    public void setUp() throws Exception
    {

        nodeService = (NodeService) ctx.getBean("nodeService");
        searchService = (SearchService) ctx.getBean("searchService");
        dictionaryService = (DictionaryService) ctx.getBean("dictionaryService");
        passwordEncoder = (PasswordEncoder) ctx.getBean("passwordEncoder");
        ticketComponent = (TicketComponent) ctx.getBean("ticketComponent");
        authenticationService = (AuthenticationService) ctx.getBean("authenticationService");

        dao = (RepositoryAuthenticationDao) ctx.getBean("alfDaoImpl");
        authenticationManager = (AuthenticationManager) ctx.getBean("authenticationManager");
        saltSource = (SaltSource) ctx.getBean("saltSource");

        TransactionService transactionService = (TransactionService) ctx.getBean(ServiceRegistry.TRANSACTION_SERVICE.getLocalName());
        UserTransaction userTransaction = transactionService.getUserTransaction();
        userTransaction.begin();

        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        StoreContextHolder.setContext(storeRef);
        rootNodeRef = nodeService.getRootNode(storeRef);

        QName children = ContentModel.ASSOC_CHILDREN;
        QName system = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "system");
        QName container = ContentModel.TYPE_CONTAINER;
        QName types = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "people");

        systemNodeRef = nodeService.createNode(rootNodeRef, children, system, container).getChildRef();
        typesNodeRef = nodeService.createNode(systemNodeRef, children, types, container).getChildRef();
        Map<QName, Serializable> props = createPersonProperties("andy");
        personAndyNodeRef = nodeService.createNode(typesNodeRef, children, ContentModel.TYPE_PERSON, container, props).getChildRef();
        assertNotNull(personAndyNodeRef);
    }

    private Map<QName, Serializable> createPersonProperties(String userName)
    {
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, "andy");
        return properties;
    }

    public void testPersonAndyExists()
    {
        RepositoryAuthenticationDao dao = new RepositoryAuthenticationDao();
        dao.setNodeService(nodeService);
        dao.setSearchService(searchService);
        dao.setDictionaryService(dictionaryService);
        dao.setNamespaceService(getNamespacePrefixReolsver(""));
        dao.setPasswordEncoder(passwordEncoder);

        assertNotNull(dao.getPersonOrNull("andy"));
    }

    public void testCreateAndyUserAndOtherCRUD()
    {
        RepositoryAuthenticationDao dao = new RepositoryAuthenticationDao();
        dao.setNodeService(nodeService);
        dao.setSearchService(searchService);
        dao.setDictionaryService(dictionaryService);
        dao.setNamespaceService(getNamespacePrefixReolsver(""));
        dao.setPasswordEncoder(passwordEncoder);

        dao.createUser("andy", "cabbage");
        assertNotNull(dao.getUserOrNull("andy"));

        RepositoryUserDetails andyDetails = (RepositoryUserDetails) dao.loadUserByUsername("andy");
        assertNotNull(andyDetails);
        assertNotNull(andyDetails.getPersonNodeRef());
        assertNotNull(andyDetails.getUserNodeRef());
        assertEquals("andy", andyDetails.getUsername());
        assertNotNull(andyDetails.getSalt());
        assertTrue(andyDetails.isAccountNonExpired());
        assertTrue(andyDetails.isAccountNonLocked());
        assertTrue(andyDetails.isCredentialsNonExpired());
        assertTrue(andyDetails.isEnabled());
        assertNotSame("cabbage", andyDetails.getPassword());
        assertEquals(andyDetails.getPassword(), passwordEncoder.encodePassword("cabbage", saltSource.getSalt(andyDetails)));
        assertEquals(1, andyDetails.getAuthorities().length);

        dao.updateUser("andy", "carrot");
        RepositoryUserDetails newDetails = (RepositoryUserDetails) dao.loadUserByUsername("andy");
        assertNotNull(newDetails);
        assertNotNull(newDetails.getPersonNodeRef());
        assertNotNull(newDetails.getUserNodeRef());
        assertEquals("andy", newDetails.getUsername());
        assertNotNull(newDetails.getSalt());
        assertTrue(newDetails.isAccountNonExpired());
        assertTrue(newDetails.isAccountNonLocked());
        assertTrue(newDetails.isCredentialsNonExpired());
        assertTrue(newDetails.isEnabled());
        assertNotSame("carrot", newDetails.getPassword());
        assertEquals(1, newDetails.getAuthorities().length);

        assertNotSame(andyDetails.getPassword(), newDetails.getPassword());
        assertNotSame(andyDetails.getSalt(), newDetails.getSalt());
        assertEquals(andyDetails.getPersonNodeRef(), newDetails.getPersonNodeRef());
        assertEquals(andyDetails.getUserNodeRef(), newDetails.getUserNodeRef());

        dao.deleteUser("andy");
        assertNull(dao.getUserOrNull("andy"));
    }

    public void testAuthentication()
    {
        dao.createUser("andy", "squash");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "squash");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        assertNotNull(result);
    }

    public void testAuthenticationFailure()
    {
        dao.createUser("andy", "squash");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "turnip");
        token.setAuthenticated(false);

        try
        {
            Authentication result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (BadCredentialsException e)
        {
            // Expected
        }
    }

    public void testTicket()
    {
        dao.createUser("andy", "ticket");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        result = ticketComponent.addTicket(result);
        result = ticketComponent.validateTicket(result);

        try
        {
            result = ticketComponent.validateTicket(result);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        result = ticketComponent.addTicket(result);
        String ticketString = ticketComponent.extractTicket(result);
        result = ticketComponent.validateTicket(ticketString);

        result = ticketComponent.addTicket(result);
        ticketString = ticketComponent.extractTicket(result);
        ticketComponent.invalidateTicket(ticketString);
        try
        {
            result = ticketComponent.validateTicket(ticketString);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        result = ticketComponent.addTicket(result);
        ticketComponent.invalidateTicket(result);
        try
        {
            ticketComponent.validateTicket(result);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

    }

    public void testTicketRepeat()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(false);
        tc.setValidDuration("P0D");

        dao.createUser("andy", "ticket");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        result = tc.addTicket(result);
        tc.validateTicket(result);
        tc.validateTicket(result);
    }

    public void testTicketOneOff()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(true);
        tc.setTicketsExpire(false);
        tc.setValidDuration("P0D");

        dao.createUser("andy", "ticket");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        result = tc.addTicket(result);
        tc.validateTicket(result);
        try
        {
            tc.validateTicket(result);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
    }

    public void testTicketExpires()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(true);
        tc.setValidDuration("P5S");

        dao.createUser("andy", "ticket");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        result = tc.addTicket(result);
        tc.validateTicket(result);
        tc.validateTicket(result);
        tc.validateTicket(result);
        synchronized (this)
        {
            try
            {
                wait(10000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            tc.validateTicket(result);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
    }

    public void testTicketDoesNotExpire()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(true);
        tc.setValidDuration("P1D");

        dao.createUser("andy", "ticket");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        result = tc.addTicket(result);
        tc.validateTicket(result);
        tc.validateTicket(result);
        tc.validateTicket(result);
        synchronized (this)
        {
            try
            {
                wait(10000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        tc.validateTicket(result);

    }

    public void testAuthenticationService()
    {
        // token for user name and password pair
        UsernamePasswordAuthenticationToken token1 = new UsernamePasswordAuthenticationToken("andy", "auth1");
        UsernamePasswordAuthenticationToken token2 = new UsernamePasswordAuthenticationToken("andy", "auth2");
        UsernamePasswordAuthenticationToken token3 = new UsernamePasswordAuthenticationToken("andy", "auth3");
        // create an authentication object e.g. the user
        authenticationService.createAuthentication(rootNodeRef.getStoreRef(), token1);

        // authenticate with this user details
        Authentication result = authenticationService.authenticate(rootNodeRef.getStoreRef(), token1);
        assertNotNull(result);

        // assert the user is authenticated
        assertTrue(authenticationService.getCurrentAuthentication().isAuthenticated());
        // delete the user authenticatiom object

        authenticationService.deleteAuthentication(rootNodeRef.getStoreRef(), token1);

        // create a new authentication user object
        authenticationService.createAuthentication(rootNodeRef.getStoreRef(), token2);
        // change the password
        authenticationService.updateAuthentication(rootNodeRef.getStoreRef(), token3);
        // authenticate again to assert password changed
        result = authenticationService.authenticate(rootNodeRef.getStoreRef(), token3);
        assertTrue(authenticationService.getCurrentAuthentication().isAuthenticated());

        try
        {
            result = authenticationService.authenticate(rootNodeRef.getStoreRef(), token1);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
        try
        {
            result = authenticationService.authenticate(rootNodeRef.getStoreRef(), token2);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        // get the ticket that represents the current user authentication instance
        String ticket = authenticationService.getCurrentTicket();
        // validate our ticket is still valid
        result = authenticationService.validate(ticket);

        // destroy the ticket instance
        authenticationService.invalidate(ticket);
        try
        {
            result = authenticationService.validate(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
        
        // clear any context and check we are no longer authenticated
        authenticationService.clearCurrentSecurityContext();
        assertNull(authenticationService.getCurrentAuthentication());
    }

    public void testPassThroughLogin()
    {
        UsernamePasswordAuthenticationToken token1 = new UsernamePasswordAuthenticationToken("andy", "auth1");
        authenticationService.createAuthentication(rootNodeRef.getStoreRef(), token1);
        
        authenticationService.setAuthenticatedUser("andy");
        assertTrue(authenticationService.getCurrentAuthentication().isAuthenticated());
        assertEquals(1, authenticationService.getCurrentAuthentication().getAuthorities().length);
    }
    
    private NamespacePrefixResolver getNamespacePrefixReolsver(String defaultURI)
    {
        DynamicNamespacePrefixResolver nspr = new DynamicNamespacePrefixResolver(null);
        nspr.addDynamicNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        nspr.addDynamicNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        nspr.addDynamicNamespace(ContentModel.USER_MODEL_PREFIX, ContentModel.USER_MODEL_URI);
        nspr.addDynamicNamespace("namespace", "namespace");
        nspr.addDynamicNamespace(NamespaceService.DEFAULT_PREFIX, defaultURI);
        return nspr;
    }
}
