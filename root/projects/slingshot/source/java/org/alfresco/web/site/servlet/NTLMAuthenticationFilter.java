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
package org.alfresco.web.site.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.jlan.server.auth.ntlm.NTLM;
import org.alfresco.jlan.server.auth.ntlm.NTLMLogonDetails;
import org.alfresco.jlan.server.auth.ntlm.NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type1NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type2NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type3NTLMMessage;
import org.alfresco.util.log.NDC;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.RequestContextUtil;
import org.springframework.extensions.surf.UserFactory;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.exception.PlatformRuntimeException;
import org.springframework.extensions.surf.exception.RequestContextException;
import org.springframework.extensions.surf.site.AuthenticationUtil;
import org.springframework.extensions.surf.types.Page;
import org.springframework.extensions.surf.util.Base64;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.Response;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * NTLM Authentication Filter Class for web-tier.
 * 
 * @see org.alfresco.web.app.servlet.NTLMAuthenticationFilter
 * 
 * @author Kevin Roast
 */
public class NTLMAuthenticationFilter implements Filter 
{
    private static Log logger = LogFactory.getLog(NTLMAuthenticationFilter.class);
    
    // NTLM authentiation request/response headers
    private static final String AUTH_NTLM = "NTLM";
    private static final String HEADER_WWWAUTHENTICATE = "WWW-Authenticate";
    
    // NTLM authentication session object names
    private static final String NTLM_AUTH_SESSION = "_alfwfNTLMAuthSess";
    private static final String NTLM_AUTH_DETAILS = "_alfwfNTLMDetails";
    private static final String LOGIN_PAGE_PASSTHROUGH = "_alfwfLoginPassthrough";
    
    private ConnectorService connectorService;
    private String endpoint;
    private ServletContext servletContext;
    
    
    /**
     * Initialize the filter
     */
    public void init(FilterConfig args) throws ServletException
    {
        // get the endpoint id to use
        this.endpoint = args.getInitParameter("endpoint");
        
        // get reference to our ServletContext
        this.servletContext = args.getServletContext();
        
        if (logger.isInfoEnabled())
            logger.info("NTLMAuthenticationFilter initialised.");
    }

    /**
     * Run the filter
     * 
     * @param sreq ServletRequest
     * @param sresp ServletResponse
     * @param chain FilterChain
     * 
     * @exception IOException
     * @exception ServletException
     */
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain)
        throws IOException, ServletException
    {
        NDC.remove();
        NDC.push(Thread.currentThread().getName());
        final boolean debug = logger.isDebugEnabled();
        
        // Get the HTTP request/response/session
        HttpServletRequest req = (HttpServletRequest)sreq;
        HttpServletResponse res = (HttpServletResponse)sresp;
        HttpSession session = req.getSession();
        
        if (debug) logger.debug("Processing request " + req.getRequestURI() + " SID:" + session.getId());
        
        // initialize a new request context
        RequestContext context = null;
        try
        {
            // perform a "silent" init - i.e. no user creation or remote connections
            context = RequestContextUtil.initRequestContext(getApplicationContext(), (HttpServletRequest)sreq, true);
        }
        catch (RequestContextException ex)
        {
            throw new ServletException(ex);
        }

        // Check if there is an authorization header with an NTLM security blob
        String authHdr = req.getHeader("Authorization");
        boolean reqAuth = (authHdr != null && authHdr.startsWith(AUTH_NTLM));
        boolean cachedNtlm = session.getAttribute(NTLM_AUTH_DETAILS) != null; 
        // touch the repo to ensure we still have an authenticated  session
        if ((!reqAuth && cachedNtlm) || AuthenticationUtil.isAuthenticated(req))        
        {
            try
            {
                Response remoteRes;
                if (cachedNtlm)
                {
                    Connector conn = connectorService.getConnector(this.endpoint, session);
                    ConnectorContext ctx = new ConnectorContext(null, getConnectionHeaders(conn));
                    remoteRes = conn.call("/touch", ctx, req, null);
                }
                else
                {
                    Connector conn = connectorService.getConnector(this.endpoint, AuthenticationUtil.getUserId(req),
                            session);
                    ConnectorContext ctx = new ConnectorContext();
                    remoteRes = conn.call("/touch", ctx);
                }
                if (Status.STATUS_UNAUTHORIZED == remoteRes.getStatus().getCode())
                {
                    if (debug) logger.debug("Repository session timed out - restarting auth process...");
                    
                    if (cachedNtlm)
                    {
                        // restart NTLM login as the repo has timed us out
                        restartAuthProcess(session, res);
                    }
                    else
                    {
                       // restart manual login
                        session.invalidate();
                        redirectToLoginPage(req, res);
                    }
                    return;
                }
                else
                {
                    // we have local auth in the session and the repo session is also valid
                    // this means we do not need to perform any further auth handshake
                    if (debug) logger.debug("Authentication not required, chaining ...");

                    chain.doFilter(sreq, sresp);
                    return;
                }
            }
            catch (ConnectorServiceException cse)
            {
                throw new PlatformRuntimeException("Incorrectly configured endpoint ID: " + this.endpoint);
            }
        }
        
        // get the page from the model if any - it may not require authentication
        Page page = context.getPage();
        if (page != null && page.getAuthentication() == RequiredAuthentication.none)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unauthenticated page requested - skipping auth filter...");
            chain.doFilter(sreq, sresp);
            return;
        }

        // Check if the login page is being accessed, do not intercept the login page
        if (session.getAttribute(LOGIN_PAGE_PASSTHROUGH) != null)
        {
            if (debug) logger.debug("Login page requested, chaining ...");
            
            // Chain to the next filter
            chain.doFilter(sreq, sresp);
            return;
        }
        
        // Check if the browser is Opera, if so then display the login page as Opera does not
        // support NTLM and displays an error page if a request to use NTLM is sent to it
        String userAgent = req.getHeader("user-agent");
        if (userAgent != null && userAgent.indexOf("Opera ") != -1)
        {
            if (debug) logger.debug("Opera detected, redirecting to login page");

            redirectToLoginPage(req, res);
            return;
        }
        
        // Check the authorization header
        if (authHdr == null)
        {
            if (debug) logger.debug("New NTLM auth request from " + req.getRemoteHost() + " (" +
                                    req.getRemoteAddr() + ":" + req.getRemotePort() + ")");
            
            restartAuthProcess(session, res);
        }
        else
        {
            // Decode the received NTLM blob and validate
            final byte[] authHdrByts = authHdr.substring(5).getBytes();
            final byte[] ntlmByts = Base64.decode(authHdrByts);
            int ntlmTyp = NTLMMessage.isNTLMType(ntlmByts);
            
            if (ntlmTyp == NTLM.Type1)
            {
                // Process the type 1 NTLM message
                Type1NTLMMessage type1Msg = new Type1NTLMMessage(ntlmByts);
                // Start with a fresh session
                session.invalidate();
                session = req.getSession();
                processType1(type1Msg, req, res, session);
            }
            else if (ntlmTyp == NTLM.Type3)
            {
                // Process the type 3 NTLM message
                Type3NTLMMessage type3Msg = new Type3NTLMMessage(ntlmByts);
                processType3(type3Msg, req, res, session, chain);
            }
            else
            {
                if (debug) logger.debug("NTLM not handled, redirecting to login page");
                
                redirectToLoginPage(req, res);
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
    }
    
    /**
     * Return the connection headers for a /touch request
     * 
     * @param conn      Connector
     * 
     * @return the headers required for the request - if any
     */
    private Map<String, String> getConnectionHeaders(Connector conn)
    {
        Map<String, String> headers = null;
        if (conn.getConnectorSession().getCookie("JSESSIONID") == null)
        {
            // Ensure we do not proxy over the Session ID from the browser request:
            // If Alfresco and SURF app are deployed into the same app-server and user is
            // user same browser instance to access both apps then we could get wrong session ID!
            headers = new HashMap<String, String>(1);
            headers.put("Cookie", "");
        }
        return headers;
    }
    
    /**
     * Restart the authentication process for NTLM - clear current security details
     */
    private void restartAuthProcess(HttpSession session, HttpServletResponse res) throws IOException
    {
        // Clear any cached logon details from the sessiom
        session.removeAttribute(NTLM_AUTH_DETAILS);
        
        // restart the authentication process for NTLM
        res.setHeader(HEADER_WWWAUTHENTICATE, AUTH_NTLM);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.flushBuffer();
    }

    /**
     * Process a type 1 NTLM message
     * 
     * @param type1Msg Type1NTLMMessage
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @param session HttpSession
     * 
     * @exception IOException
     */
    private void processType1(Type1NTLMMessage type1Msg, HttpServletRequest req, HttpServletResponse res,
            HttpSession session) throws IOException
    {
        if (logger.isDebugEnabled())
            logger.debug("Received type1 " + type1Msg);
        
        // Get the existing NTLM details
        NTLMLogonDetails ntlmDetails = (NTLMLogonDetails)session.getAttribute(NTLM_AUTH_DETAILS);
        
        // Check if cached logon details are available
        if (ntlmDetails != null && ntlmDetails.hasType2Message() && ntlmDetails.hasNTLMHashedPassword())
        {
            // Get the authentication server type2 response
            Type2NTLMMessage cachedType2 = ntlmDetails.getType2Message();
            
            byte[] type2Bytes = cachedType2.getBytes();
            String ntlmBlob = "NTLM " + new String(Base64.encodeBytes(type2Bytes, Base64.DONT_BREAK_LINES));
            
            if (logger.isDebugEnabled())
                logger.debug("Sending cached NTLM type2 to client - " + cachedType2);
            
            // Send back a request for NTLM authentication
            res.setHeader(HEADER_WWWAUTHENTICATE, ntlmBlob);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.flushBuffer();
        }
        else
        {
            // Clear any cached logon details
            session.removeAttribute(NTLM_AUTH_DETAILS);
            
            try
            {
                Connector conn = getConnector(this.endpoint, session);
                ConnectorContext ctx = new ConnectorContext(null, getConnectionHeaders(conn));
                Response remoteRes = conn.call("/touch", ctx, req, null);
                if (Status.STATUS_UNAUTHORIZED == remoteRes.getStatus().getCode())
                {
                    String authHdr = remoteRes.getStatus().getHeaders().get(HEADER_WWWAUTHENTICATE);
                    if (authHdr.startsWith(AUTH_NTLM) && authHdr.length() > 4)
                    {
                        // Decode the received NTLM blob and validate
                        final byte[] authHdrByts = authHdr.substring(5).getBytes();
                        final byte[] ntlmByts = Base64.decode(authHdrByts);
                        int ntlmType = NTLMMessage.isNTLMType(ntlmByts);
                        if (ntlmType == NTLM.Type2)
                        {
                            // Retrieve the type2 NTLM message
                            Type2NTLMMessage type2Msg = new Type2NTLMMessage(ntlmByts);
                            
                            // Store the NTLM logon details, cache the type2 message, and token if using passthru
                            ntlmDetails = new NTLMLogonDetails();
                            ntlmDetails.setType2Message(type2Msg);
                            session.setAttribute(NTLM_AUTH_DETAILS, ntlmDetails);
                            
                            if (logger.isDebugEnabled())
                                logger.debug("Sending NTLM type2 to client - " + type2Msg);
                            
                            // Send back a request for NTLM authentication
                            byte[] type2Bytes = type2Msg.getBytes();
                            String ntlmBlob = "NTLM " + new String(Base64.encodeBytes(type2Bytes, Base64.DONT_BREAK_LINES));
                            
                            res.setHeader(HEADER_WWWAUTHENTICATE, ntlmBlob);
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.flushBuffer();
                        }
                        else
                        {
                            if (logger.isDebugEnabled())
                                logger.debug("Unexpected NTLM message type from repository: NTLMType" + ntlmType);
                            redirectToLoginPage(req, res);
                        }
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Unexpected response from repository: WWW-Authenticate:" + authHdr);
                        redirectToLoginPage(req, res);
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Unexpected response from repository: " + remoteRes.getStatus().getMessage());
                    redirectToLoginPage(req, res);
                }
            }
            catch (ConnectorServiceException cse)
            {
                throw new PlatformRuntimeException("Incorrectly configured endpoint ID: " + this.endpoint);
            }
        }
    }
    
    /**
     * Process a type 3 NTLM message
     * 
     * @param type3Msg Type3NTLMMessage
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @param session HttpSession
     * @param chain FilterChain
     * @exception IOException
     * @exception ServletException
     */
    private void processType3(Type3NTLMMessage type3Msg, HttpServletRequest req, HttpServletResponse res,
            HttpSession session, FilterChain chain) throws IOException, ServletException
    {
        if (logger.isDebugEnabled())
            logger.debug("Received type3 " + type3Msg);
        
        // Get the existing NTLM details
        NTLMLogonDetails ntlmDetails = (NTLMLogonDetails) session.getAttribute(NTLM_AUTH_DETAILS);
        String userId = AuthenticationUtil.getUserId(req);
        
        // Get the NTLM logon details
        String userName = type3Msg.getUserName();
        String workstation = type3Msg.getWorkstation();
        String domain = type3Msg.getDomain();
        
        boolean authenticated = false;
        
        // Check if we are using cached details for the authentication
        if (userId != null && ntlmDetails != null && ntlmDetails.hasNTLMHashedPassword())
        {
            // Check if the received NTLM hashed password matches the cached password
            byte[] ntlmPwd = type3Msg.getNTLMHash();
            byte[] cachedPwd = ntlmDetails.getNTLMHashedPassword();
            
            if (ntlmPwd != null)
            {
                if (ntlmPwd.length == cachedPwd.length)
                {
                    authenticated = true;
                    for (int i = 0; i < ntlmPwd.length; i++)
                    {
                        if (ntlmPwd[i] != cachedPwd[i])
                        {
                            authenticated = false;
                            break;
                        }
                    }
                }
            }
            
            if (logger.isDebugEnabled())
                logger.debug("Using cached NTLM hash, authenticated = " + authenticated);
            
            if (!authenticated)
            {
                restartAuthProcess(session, res);
            }
            else
            {
                // Allow the user to access the requested page
                chain.doFilter(req, res);
            }
        }
        else
        {
            try
            {
                Connector conn = getConnector(this.endpoint, session);
                ConnectorContext ctx = new ConnectorContext(null, getConnectionHeaders(conn));
                Response remoteRes = conn.call("/touch", ctx, req, null);
                if (Status.STATUS_UNAUTHORIZED == remoteRes.getStatus().getCode())
                {
                    String authHdr = remoteRes.getStatus().getHeaders().get(HEADER_WWWAUTHENTICATE);
                    if (authHdr.equals(AUTH_NTLM))
                    {
                        // authentication failed on repo side - being login process again
                        res.setHeader(HEADER_WWWAUTHENTICATE, AUTH_NTLM);
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.flushBuffer();
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Unexpected response from repository: WWW-Authenticate:" + authHdr);
                        redirectToLoginPage(req, res);
                    }
                }
                else if (Status.STATUS_OK == remoteRes.getStatus().getCode() ||
                         Status.STATUS_TEMPORARY_REDIRECT == remoteRes.getStatus().getCode())
                {
                    //
                    // NTLM login successful - Update the NTLM logon details in the session
                    //
                    if (ntlmDetails == null)
                    {
                        // No cached NTLM details
                        ntlmDetails = new NTLMLogonDetails(userName, workstation, domain, false, null);
                        ntlmDetails.setNTLMHashedPassword(type3Msg.getNTLMHash());
                        session.setAttribute(NTLM_AUTH_DETAILS, ntlmDetails);
                        
                        if (logger.isDebugEnabled())
                            logger.debug("No cached NTLM details, created");
                    }
                    else
                    {
                        // Update the cached NTLM details
                        ntlmDetails.setDetails(userName, workstation, domain, false, null);
                        ntlmDetails.setNTLMHashedPassword(type3Msg.getNTLMHash());
                        
                        if (logger.isDebugEnabled())
                            logger.debug("Updated cached NTLM details");
                    }
                    
                    if (logger.isDebugEnabled())
                        logger.debug("User logged on via NTLM, " + ntlmDetails);
                    
                    // Create User ID in session so the web-framework dispatcher knows we have logged in
                    session.setAttribute(UserFactory.SESSION_ATTRIBUTE_KEY_USER_ID, userName);
                    
                    // Set the external auth flag so the UI knows we are using SSO etc.
                    session.setAttribute(UserFactory.SESSION_ATTRIBUTE_EXTERNAL_AUTH, Boolean.TRUE);
                    
                    // Allow the user to access the requested page
                    chain.doFilter(req, res);
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Unexpected response from repository: " + remoteRes.getStatus().getMessage());
                    redirectToLoginPage(req, res);
                }
            }
            catch (ConnectorServiceException cse)
            {
                throw new PlatformRuntimeException("Incorrectly configured endpoint: " + this.endpoint);
            }
        }
    }
    
    /**
     * Redirect to the root of the website - ignore further NTLM auth requests
     */
    private void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        // Redirect to the root of the website - mark the session for login passthrough
        // as we ignore requests for login page during NTLM processing.
        req.getSession().setAttribute(LOGIN_PAGE_PASSTHROUGH, Boolean.TRUE);
        res.sendRedirect(req.getContextPath());
    }
    
    /**
     * Return a Connector for the given Endpoint against the current Session
     * 
     * @param endpoint  Endpoint ID for the Connector
     * @param session   Session
     * @return Connector
     * @throws ConnectorServiceException
     */
    private Connector getConnector(String endpoint, HttpSession session)
        throws ConnectorServiceException
    {
        if (this.connectorService == null)
        {
            // retrieve the connector service
            this.connectorService = (ConnectorService) getApplicationContext().getBean("connector.service");
        }
        return this.connectorService.getConnector(endpoint, session);
    }

    /**
     * Retrieves the root application context
     * 
     * @return application context
     */
    private ApplicationContext getApplicationContext()
    {
    	return WebApplicationContextUtils.getWebApplicationContext(servletContext);
    }
}
