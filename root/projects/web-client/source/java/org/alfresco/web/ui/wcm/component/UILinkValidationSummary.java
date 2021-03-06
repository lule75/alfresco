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
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wcm.LinkValidationState;
import org.alfresco.web.ui.common.PanelGenerator;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JSF component that shows the summary information for a link
 * validation report.
 * 
 * @author gavinc
 */
public class UILinkValidationSummary extends AbstractLinkValidationReportComponent
{
   private static Log logger = LogFactory.getLog(UILinkValidationSummary.class);
   
   // ------------------------------------------------------------------------------
   // Component implementation
   
   @Override
   public String getFamily()
   {
      return "org.alfresco.faces.LinkValidationSummary";
   }
   
   @SuppressWarnings("unchecked")
   @Override
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      // get the link validation state object to get the data from
      ResourceBundle bundle = Application.getBundle(context);
      ResponseWriter out = context.getResponseWriter();
      LinkValidationState linkState = getValue();
      
      if (logger.isDebugEnabled())
         logger.debug("Rendering summary from state object: " + linkState);
      
      // determine what to display i.e. surrounding panel and title 
      boolean showPanel = true;
      boolean showTitle = true;
      
      Object showPanelObj = this.getAttributes().get("showPanel");
      if (showPanelObj instanceof Boolean)
      {
         showPanel = ((Boolean)showPanelObj).booleanValue();
      }
      
      Object showTitleObj = this.getAttributes().get("showTitle");
      if (showTitleObj instanceof Boolean)
      {
         showTitle = ((Boolean)showTitleObj).booleanValue();
      }
      
      if (showPanel)
      {
         // render the summary area with a surrounding panel
         PanelGenerator.generatePanelStart(out, context.getExternalContext().getRequestContextPath(), 
                  "innerwhite", "white");
      }
      
      String styleClass = (String)this.getAttributes().get("styleClass");
      if (styleClass == null || styleClass.length() == 0)
      {
         styleClass = "linkValidationSummaryPanel";
      }
      
      out.write("<div class='");
      out.write(styleClass);
      out.write("'>");
      
      if (showTitle)
      {
         out.write("<div class='linkValidationSummaryTitle'>");
         out.write(bundle.getString("report_summary"));
         out.write("</div>");
      }
      
      if (linkState != null)
      {
         if (linkState.getError() == null)
         {
            // render the main summary info
            
            int latestVersion = linkState.getLatestSnapshotVersion();
            int baseVersion = linkState.getBaseSnapshotVersion();
   
            String pattern = bundle.getString("link_check_completed_at");
            Date checkAt = linkState.getCheckCompletedAt();
            String checkTime = Utils.getDateTimeFormat(context).format(checkAt);
            String checkTimeSummary = MessageFormat.format(pattern, 
                    new Object[] {checkTime, baseVersion});
            
            out.write("<div class='linkValSummaryText'>");
            out.write(checkTimeSummary);
            
            // NOTE: Whenever latestVersion > baseVersion, link validation  is "behind".
            if (latestVersion > baseVersion)
            {
               pattern = bundle.getString("link_check_not_latest");
               String latestVersionInfo = 
                        MessageFormat.format(
                          pattern, new Object[] { new Integer( latestVersion )});
   
               out.write("&nbsp;<img src='");
               out.write(context.getExternalContext().getRequestContextPath());
               out.write("/images/icons/warning.gif' />&nbsp;");
               out.write( latestVersionInfo );
            }
            
            pattern = bundle.getString("link_check_items_found");
            String checkedSummary = MessageFormat.format(pattern, 
                     new Object[] {linkState.getNumberFilesChecked(), 
                                   linkState.getNumberLinksChecked()});
            
            pattern = bundle.getString("link_check_items_broken");
            
            String numBrokenLinks = Integer.toString(linkState.getNumberBrokenLinks());
            if (linkState.hasMaxNumberLinksExceeded())
            {
               numBrokenLinks = Integer.toString(linkState.getMaxNumberLinksInReport()) + "+";
            }
            
            String brokenSummary = MessageFormat.format(pattern, 
                     new Object[] {numBrokenLinks, linkState.getNumberBrokenFiles()});
            
            out.write("</div><div class='linkValSummaryText'>");
            out.write(checkedSummary);
            out.write("&nbsp;<img src='");
            out.write(context.getExternalContext().getRequestContextPath());
            
            if (linkState.getNumberBrokenLinks() == 0)
            {
               out.write("/images/icons/info_icon.gif' />&nbsp;");
               out.write(bundle.getString("link_check_no_broken"));
            }
            else
            {
               out.write("/images/icons/warning.gif' />&nbsp;");
               out.write(brokenSummary);
            }
            out.write("</div>");
         }
         else
         {
            // render the error that occurred
            String pattern = bundle.getString("link_check_error");
            Date initialCheck = linkState.getCheckCompletedAt();
            String initialCheckTime = Utils.getDateTimeFormat(context).format(initialCheck);
            String initialCheckSummary = MessageFormat.format(pattern, 
                     new Object[] {initialCheckTime});
            
            out.write(initialCheckSummary);
            out.write("&nbsp;<span class='errorMessage'>");
            String err = linkState.getError().getMessage();
            if (err == null)
            {
               out.write(linkState.getError().toString());
            }
            else
            {
               out.write(err);
            }
            out.write("</span>");
         }
      }
      else
      {
         // if the report was not found at all, show an error to that effect
         out.write("<span class='errorMessage'>");
         out.write(bundle.getString("failed_to_find_validation_report"));
         out.write("</span>");
      }
      
      out.write("</div>");
      
      if (showPanel)
      {
         // finish the surrounding panel
         PanelGenerator.generatePanelEnd(out, context.getExternalContext().getRequestContextPath(), 
                  "innerwhite");
      }
   }
}
