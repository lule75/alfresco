/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.forums;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean implementation for the "Create Discusssion Dialog".
 * 
 * @author gavinc
 */
public class CreateDiscussionDialog extends CreateTopicDialog
{
   private static final long serialVersionUID = 3500493916528264014L;

   protected NodeRef discussingNodeRef;
   
   private static final Log logger = LogFactory.getLog(CreateDiscussionDialog.class);
   
   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      // get the id of the node we are creating the discussion for
      String id = parameters.get("id");
      if (id == null || id.length() == 0)
      {
         throw new AlfrescoRuntimeException("createDiscussion called without an id");
      }
      
      // create the topic to hold the discussions
      createTopic(id);
   }
   
   @Override
   public String cancel()
   {
      // if the user cancels the creation of a discussion all the setup that was done 
      // when the dialog started needs to be undone i.e. removing the created forum
      // and the discussable aspect
      deleteTopic();
      
      // as we are cancelling the creation of a discussion we know we need to go back
      // to the browse screen, this also makes sure we don't end up in the forum that
      // just got deleted!
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME + 
             AlfrescoNavigationHandler.OUTCOME_SEPARATOR + "browse";
   }
   
   // ------------------------------------------------------------------------------
   // Helper methods

   /**
    * Creates a topic for the node with the given id
    * 
    * @param id The id of the node to discuss
    */
   protected void createTopic(String id)
   {
      FacesContext context = FacesContext.getCurrentInstance();
      UserTransaction tx = null;
      NodeRef forumNodeRef = null;
      
      try
      {
         tx = Repository.getUserTransaction(context);
         tx.begin();
         
         this.discussingNodeRef = new NodeRef(Repository.getStoreRef(), id);
         
         if (this.getNodeService().hasAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE))
         {
            throw new AlfrescoRuntimeException("createDiscussion called for an object that already has a discussion!");
         }
         
         // add the discussable aspect
         this.getNodeService().addAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE, null);
         
         // create a child forum space using the child association just introduced by
         // adding the discussable aspect
         String name = (String)this.getNodeService().getProperty(this.discussingNodeRef, 
               ContentModel.PROP_NAME);
         String msg = Application.getMessage(FacesContext.getCurrentInstance(), "discussion_for");
         String forumName = MessageFormat.format(msg, new Object[] {name});
         
         Map<QName, Serializable> forumProps = new HashMap<QName, Serializable>(1);
         forumProps.put(ContentModel.PROP_NAME, forumName);
         ChildAssociationRef childRef = this.getNodeService().createNode(this.discussingNodeRef, 
               ForumModel.ASSOC_DISCUSSION,
               QName.createQName(NamespaceService.FORUMS_MODEL_1_0_URI, "discussion"), 
               ForumModel.TYPE_FORUM, forumProps);
         
         forumNodeRef = childRef.getChildRef();

         // apply the uifacets aspect
         Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(5);
         uiFacetsProps.put(ApplicationModel.PROP_ICON, "forum");
         this.getNodeService().addAspect(forumNodeRef, ApplicationModel.ASPECT_UIFACETS, uiFacetsProps);
         
         if (logger.isDebugEnabled())
            logger.debug("created forum for content: " + this.discussingNodeRef.toString());
         
         // commit the transaction
         tx.commit();
      }
      catch (Throwable e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               context, Repository.ERROR_GENERIC), e.getMessage()), e);
      }
      
      // finally setup the context for the forum we just created
      if (forumNodeRef != null)
      {
         this.browseBean.clickSpace(forumNodeRef);
      }
   }
   
   /**
    * Deletes the setup performed during the initialisation of the dialog.
    */
   protected void deleteTopic()
   {
      FacesContext context = FacesContext.getCurrentInstance();
      UserTransaction tx = null;
      
      try
      {
         tx = Repository.getUserTransaction(context);
         tx.begin();
         
         // remove the discussable aspect from the node we were going to discuss!
         // AWC-1519: removing the aspect that defines the child association now does the 
         //           cascade delete so we no longer have to delete the child explicitly
         this.getNodeService().removeAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE);
         
         // commit the transaction
         tx.commit();
         
         // remove this node from the breadcrumb if required
         Node forumNode = this.navigator.getCurrentNode();
         this.browseBean.removeSpaceFromBreadcrumb(forumNode);
               
         // clear action context
         this.browseBean.setActionSpace(null);
      }
      catch (Throwable e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               context, Repository.ERROR_GENERIC), e.getMessage()), e);
      }
   }
}
