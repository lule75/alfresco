package org.alfresco.web.app;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.dictionary.NamespaceService;
import org.alfresco.repo.dictionary.impl.DictionaryBootstrap;
import org.alfresco.repo.node.NodeService;
import org.alfresco.repo.ref.ChildAssocRef;
import org.alfresco.repo.ref.DynamicNamespacePrefixResolver;
import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.repo.search.ResultSet;
import org.alfresco.repo.search.Searcher;
import org.alfresco.web.bean.repository.Repository;
import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * ServletContextListener implementation that initialises the application.
 * 
 * NOTE: This class must appear after the Spring context loader listener
 * 
 * @author gavinc
 */
public class ContextListener implements ServletContextListener, HttpSessionListener
{
   private static Logger logger = Logger.getLogger(ContextListener.class);
   
   /**
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   public void contextInitialized(ServletContextEvent event)
   {
      // make sure that the spaces store in the repository exists
      ServletContext servletContext = event.getServletContext();
      WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
      NodeService nodeService = (NodeService)ctx.getBean(Repository.BEAN_NODE_SERVICE);
      Searcher searchService = (Searcher)ctx.getBean(Repository.BEAN_SEARCH_SERVICE);

      String repoStoreName = Application.getRepositoryStoreName(servletContext);
      if (repoStoreName == null)
      {
         throw new Error("Repository store name has not been configured, is 'store-name' element missing?");
      }
      
      // check the repository exists, create if it doesn't
      UserTransaction tx = null;
      String companySpaceId = null;
      try
      {
         tx = (UserTransaction)ctx.getBean(Repository.BEAN_USER_TRANSACTION);
         tx.begin();
         
         if (nodeService.exists(Repository.getStoreRef(servletContext)) == false)
         {
            nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, repoStoreName);
            
            if (logger.isDebugEnabled())
               logger.debug("Created store with name: " + repoStoreName);
         }
      
         // see if the company home space is present
         boolean companySpaceFound = false;
         String companySpaceName = Application.getCompanyRootName(servletContext);
         String companySpaceDescription = Application.getCompanyRootDescription(servletContext);
         String glossaryFolderName = Application.getGlossaryFolderName(servletContext);
         String templatesFolderName = Application.getTemplatesFolderName(servletContext);
         
         if (companySpaceName == null)
         {
            throw new Error("Company root name has not been configured, is 'company-root-name' element missing?");
         }
         if (glossaryFolderName == null)
         {
            throw new Error("Glossary folder name has not been configured, is 'glossary-folder-name' element missing?");
         }
         if (templatesFolderName == null)
         {
            throw new Error("Templates folder name has not been configured, is 'templates-folder-name' element missing?");
         }
            
         String xpath = NamespaceService.ALFRESCO_PREFIX + ":" + Repository.createValidQName(companySpaceName);
         DynamicNamespacePrefixResolver resolver = new DynamicNamespacePrefixResolver(null);
         resolver.addDynamicNamespace(NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI);
         List<ChildAssocRef> nodes = nodeService.selectNodes(nodeService.getRootNode(
               Repository.getStoreRef(servletContext)), 
               xpath, null, resolver, false);
         if (nodes.size() > 0)
         {
            companySpaceFound = true;
            companySpaceId = nodes.get(0).getChildRef().getId();
            
            if (logger.isDebugEnabled())
               logger.debug("Found company space with id: " + companySpaceId);
         }
         
         // if we didn't find the company space, create it
         // NOTE: This will eventually be performed by the install process, for now
         //       we need to do it here
         if (companySpaceFound == false)
         {
            // create the folder node to represent the company home
            String qname = Repository.createValidQName(companySpaceName);
            ChildAssocRef assocRef = nodeService.createNode(
                  nodeService.getRootNode(Repository.getStoreRef(servletContext)),
                  DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
                  QName.createQName(NamespaceService.ALFRESCO_URI, qname),
                  DictionaryBootstrap.TYPE_QNAME_FOLDER);
            
            NodeRef companyNodeRef = assocRef.getChildRef();
            companySpaceId = companyNodeRef.getId();

            // set the name property on the node
            nodeService.setProperty(companyNodeRef, DictionaryBootstrap.PROP_QNAME_NAME, companySpaceName);

            // apply the uifacets aspect - icon, title and description props
            Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(3);
            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_ICON, "space-icon-default");
            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_TITLE, companySpaceName);
            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_DESCRIPTION, companySpaceDescription);
            nodeService.addAspect(companyNodeRef, DictionaryBootstrap.ASPECT_QNAME_UIFACETS, uiFacetsProps);
            
            // apply the auditable aspect - created and modified date
            Map<QName, Serializable> auditProps = new HashMap<QName, Serializable>(2);
            Date now = new Date( Calendar.getInstance().getTimeInMillis() );
            auditProps.put(DictionaryBootstrap.PROP_QNAME_CREATED, now);
            auditProps.put(DictionaryBootstrap.PROP_QNAME_MODIFIED, now);
            nodeService.addAspect(companyNodeRef, DictionaryBootstrap.ASPECT_QNAME_AUDITABLE, auditProps);
            
            // now create the glossary system folder under the company space
//            qname = Repository.createValidQName(glossaryFolderName);
//            assocRef = nodeService.createNode(companyNodeRef, 
//                  DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
//                  QName.createQName(NamespaceService.ALFRESCO_URI, qname),
//                  DictionaryBootstrap.TYPE_QNAME_FOLDER);
//            
//            NodeRef glossaryNodeRef = assocRef.getChildRef();
//
//            // set the name property on the node
//            nodeService.setProperty(glossaryNodeRef, DictionaryBootstrap.PROP_QNAME_NAME, glossaryFolderName);
//
//            // apply the uifacets aspect - icon, title and description props
//            uiFacetsProps = new HashMap<QName, Serializable>(2);
//            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_ICON, "space-icon-spanner");
//            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_TITLE, glossaryFolderName);
//            nodeService.addAspect(glossaryNodeRef, DictionaryBootstrap.ASPECT_QNAME_UIFACETS, uiFacetsProps);
//            
//            // apply the auditable aspect - created and modified date
//            auditProps = new HashMap<QName, Serializable>(2);
//            now = new Date( Calendar.getInstance().getTimeInMillis() );
//            auditProps.put(DictionaryBootstrap.PROP_QNAME_CREATED, now);
//            auditProps.put(DictionaryBootstrap.PROP_QNAME_MODIFIED, now);
//            nodeService.addAspect(glossaryNodeRef, DictionaryBootstrap.ASPECT_QNAME_AUDITABLE, auditProps);
            
//            // now create the templates system folder under the glossary folder
//            qname = Repository.createValidQName(templatesFolderName);
//            assocRef = nodeService.createNode(glossaryNodeRef, 
//                  DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
//                  QName.createQName(NamespaceService.ALFRESCO_URI, qname),
//                  DictionaryBootstrap.TYPE_QNAME_FOLDER);
//            
//            NodeRef templatesNodeRef = assocRef.getChildRef();
//
//            // set the name property on the node
//            nodeService.setProperty(templatesNodeRef, DictionaryBootstrap.PROP_QNAME_NAME, templatesFolderName);
//
//            // apply the uifacets aspect - icon, title and description props
//            uiFacetsProps = new HashMap<QName, Serializable>(2);
//            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_ICON, "space-icon-spanner");
//            uiFacetsProps.put(DictionaryBootstrap.PROP_QNAME_TITLE, templatesFolderName);
//            nodeService.addAspect(templatesNodeRef, DictionaryBootstrap.ASPECT_QNAME_UIFACETS, uiFacetsProps);
//            
//            // apply the auditable aspect - created and modified date
//            auditProps = new HashMap<QName, Serializable>(2);
//            now = new Date( Calendar.getInstance().getTimeInMillis() );
//            auditProps.put(DictionaryBootstrap.PROP_QNAME_CREATED, now);
//            auditProps.put(DictionaryBootstrap.PROP_QNAME_MODIFIED, now);
//            nodeService.addAspect(templatesNodeRef, DictionaryBootstrap.ASPECT_QNAME_AUDITABLE, auditProps);
            
            // commit the transaction
            tx.commit();
            
            if (logger.isDebugEnabled())
               logger.debug("Created company root space with id: " + companySpaceId);
         }
      }
      catch (Exception e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         throw new AlfrescoRuntimeException("Failed to initialise ", e);
      }
      
      // make sure the current user has the company space set as it's home space
      Application.getCurrentUser(servletContext).setHomeSpaceId(companySpaceId);
      
      if (logger.isDebugEnabled())
         logger.debug("Server is running in portal server: " + Application.inPortalServer(servletContext));
   }

   /**
    * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
    */
   public void contextDestroyed(ServletContextEvent event)
   {
      // nothing to do
   }

   public void sessionCreated(HttpSessionEvent event)
   {
      logger.info("HTTP session created: " + event.getSession().getId());
   }

   public void sessionDestroyed(HttpSessionEvent event)
   {
      logger.info("HTTP session destroyed: " + event.getSession().getId());
   }
}
