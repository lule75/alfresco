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
package org.alfresco.web.ui.wcm.component;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wcm.AVMUtil;
import org.alfresco.web.ui.common.Utils;
import org.springframework.extensions.webscripts.ui.common.component.SelfRenderingComponent;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;

/**
 * JSF component that displays information about the workflows a node is involved in.
 * <p>
 * The node to show workflow information on.
 * 
 * @author gavinc
 */
public class UILinkValidationProgress extends SelfRenderingComponent
{
   // ------------------------------------------------------------------------------
   // Component Impl 

   @Override
   public String getFamily()
   {
      return "org.alfresco.faces.LinkValidationProgress";
   }
   
   @Override
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
   }
   
   @Override
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[1];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      return values;
   }
   
   @Override
   @SuppressWarnings("unchecked")
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (!isRendered()) return;
      
      ResponseWriter out = context.getResponseWriter();
      
      // render the hidden action used to close the dialog
      UIActionLink action = findOrCreateHiddenAction(context);
      Utils.encodeRecursive(context, action);
      
      // determine the polling frequency value
      int pollFreq = AVMUtil.getLinkValidationPollingFrequency();
      
      // output the script
      out.write("<script type='text/javascript'>");
      out.write("Alfresco.linkMonitorPollFreq = ");
      out.write(Integer.toString(pollFreq));
      out.write(";</script>\n");
      out.write("<script type='text/javascript' src='");
      out.write(context.getExternalContext().getRequestContextPath());
      out.write("/scripts/ajax/link-validation-progress.js'></script>\n");
      
      // output the HTML
      out.write("<div class='linkValidationProgressPanel'><table border='0' cellspacing='4' cellpadding='2'>");
      out.write("<tr><td valign='top'><img src='");
      out.write(context.getExternalContext().getRequestContextPath());
      out.write("/images/icons/ajax_anim.gif' /></td><td>");
      out.write("<div class='linkValidationProgressWait'>");
      out.write(Application.getMessage(context, "checking_links_progress"));
      out.write("</div><div class='linkValidationProgressStatus'>");
      out.write(Application.getMessage(context, "checking_links_status"));
      out.write("</div></td></tr></table></div>\n");
   }

   @Override
   public void encodeEnd(FacesContext context) throws IOException
   {
      if (!isRendered()) return;
   }
      
   // ------------------------------------------------------------------------------
   // Helper methods
   
   @SuppressWarnings("unchecked")
   private UIActionLink findOrCreateHiddenAction(FacesContext fc)
   {
      UIActionLink action = null;
      String actionId = "validation-callback-link";

      for (UIComponent component : (List<UIComponent>)getChildren())
      {
         if (actionId.equals(component.getId()))
         {
            action = (UIActionLink)component;
            break;
         }
      }
      
      if (action == null)
      {
         javax.faces.application.Application facesApp = fc.getApplication();
         action = (UIActionLink)facesApp.createComponent(UIActions.COMPONENT_ACTIONLINK);
         
         action.setRendererType(UIActions.RENDERER_ACTIONLINK);
         action.setId(actionId);
         action.setValue("Callback");
         action.setShowLink(false);
         MethodBinding callback = facesApp.createMethodBinding(
                  "#{DialogManager.bean.linkCheckCompleted}", new Class[] {});
         action.setAction(callback);
         action.getAttributes().put("style", "display:none;");
         
         this.getChildren().add(action);
      }
      
      return action;
   }
}
