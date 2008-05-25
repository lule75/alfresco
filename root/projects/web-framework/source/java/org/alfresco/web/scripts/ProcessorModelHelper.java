/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of the GPL,
 * you may redistribute this Program in connection with Free/Libre and Open
 * Source Software ("FLOSS") applications as described in Alfresco's FLOSS
 * exception. You should have recieved a copy of the text describing the FLOSS
 * exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.scripts;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.web.site.HttpRequestContext;
import org.alfresco.web.site.RenderUtil;
import org.alfresco.web.site.RequestContext;
import org.alfresco.web.site.ThemeUtil;
import org.alfresco.web.site.WebFrameworkConstants;
import org.alfresco.web.site.exception.RendererExecutionException;
import org.alfresco.web.site.model.ModelObject;
import org.alfresco.web.site.model.TemplateInstance;
import org.alfresco.web.site.renderer.RendererContext;

/**
 * @author muzquiano
 */
public class ProcessorModelHelper
{
    public static final String PROP_HTMLID = "htmlid";
    
    /**
     * Templates have the following:
     * 
     * sitedata
     * context
     * content
     * user
     * instance (current object being rendered)
     * page (legacy but souped up)
     * theme
     * htmlid
     * 
     * url
     * head
     * 
     * 
     * Components have the following
     * 
     * sitedata
     * context
     * content
     * user
     * instance (current object being rendered)
     * page (legacy but souped up)
     * theme
     * htmlid
     * 
     */

    /**
     * Populates the model with common things for all processors
     * 
     * @param rendererContext
     * @param model
     */
    public static void populateModel(RendererContext rendererContext, Map<String, Object> model)
    {
        if(model == null)
        {
            return;
        }
        
        RequestContext context = rendererContext.getRequestContext();
        if(context == null)
        {
            return;
        }

        // information about the current page being rendererd
        if (context.getPage() != null)
        {
            // TODO:Remove these
            // it should be ${page.description} and ${page.title}
            model.put("description", context.getPage().getDescription());
            model.put("title", context.getPage().getTitle());
            // TODO: End of remove this
            
            // custom page properties - add to model
            // TODO: Remove this
            // Instead of retrieving properties from the root scope
            // we should use ${instance.properties["abc"]}
            model.putAll(context.getPage().getCustomProperties());
            // TODO: End of remove this

            
            // "page" object
            Map<String, Object> pageModel = new HashMap<String, Object>(4);
            if(context instanceof HttpRequestContext)
            {
                HttpServletRequest request = ((HttpRequestContext)context).getRequest();
                
                Map<String, String> args = new HashMap<String, String>(request.getParameterMap().size(), 1.0f);
                Enumeration names = request.getParameterNames();
                while (names.hasMoreElements())
                {
                    String name = (String)names.nextElement();
                    args.put(name, request.getParameter(name));
                }
                URLHelper urlHelper = new URLHelper(request, args);
                pageModel.put("url", urlHelper);
            }
            pageModel.put("id", context.getPage().getId());
            pageModel.put("title", context.getPage().getTitle());
            pageModel.put("description", context.getPage().getDescription());
            pageModel.put("theme", ThemeUtil.getCurrentThemeId(context));
            model.put("page", pageModel);
        }
        
        
        // things from the current template
        // TODO: Remove this
        // instead of getting template properties from the root scope
        // we should use ${instance.properties["abc"]}
        if (context.getTemplate() != null)
        {
            model.putAll(context.getTemplate().getCustomProperties());
        }
        // TODO: End of Remove this
        
        model.put("theme", ThemeUtil.getCurrentThemeId(context));
        
        //
        // add in the root-scoped web framework script objects
        //
        ScriptSiteData scriptSiteData = new ScriptSiteData(context); 
        model.put("sitedata", scriptSiteData);
        //
        ScriptRequestContext scriptRequestContext = new ScriptRequestContext(context);
        model.put("context", scriptRequestContext);
        //
        if(context.getCurrentObject() != null)
        {
            ScriptContentObject scriptContent = new ScriptContentObject(context, context.getCurrentObject());
            model.put("content", scriptContent);
        }
        //
        ScriptRenderingInstance scriptRenderer = new ScriptRenderingInstance(rendererContext);
        model.put("instance", scriptRenderer);
        //
        if (context.getUser() != null)
        {
            ScriptUser scriptUser = new ScriptUser(context, context.getUser());
            model.put("user", scriptUser);
        }                        

        
        // we are also given the "rendering configuration" for the current
        // object.  usually, this is either a component or a template.
        // in either case, the configuration is set up ahead of time
        // our job here is to make sure that freemarker has everything
        // it needs for the component or template to process
        if (rendererContext != null)
        {
            // TODO: Remove this, it's redundant to do this
            /*
            ModelObject object = rendererContext.getObject();
            if (object != null)
            {
                model.putAll(object.getCustomProperties());
            }
            */
            // TODO: End of remove this
            
            String htmlBindingId = (String) rendererContext.get(WebFrameworkConstants.RENDER_DATA_HTML_BINDING_ID);
            if (htmlBindingId != null && htmlBindingId.length() > 0)
            {
                model.put(PROP_HTMLID, htmlBindingId);
            }
            
            // TODO: Remove this
            // This has been replaced by ${instance.<property>} and stuff like that
            // copy in render data settings
            //model.putAll(rendererContext.map());
            // TODO: End of remove this
        }
    }
    
    public static void populateScriptModel(RendererContext rendererContext, Map<String, Object> model)
    {
        if(model == null)
        {
            return;
        }
        
        RequestContext context = rendererContext.getRequestContext();
        if(context == null)
        {
            return;
        }
        
        // common population
        populateModel(rendererContext, model);

        // script specific
        // none... at the moment
    }
    
    public static void populateTemplateModel(RendererContext rendererContext, Map<String, Object> model)
        throws RendererExecutionException
    {
        if(model == null)
        {
            return;
        }
        
        RequestContext context = rendererContext.getRequestContext();
        if(context == null)
        {
            return;
        }

        // common population
        populateModel(rendererContext, model);

        /**
         * We add in the "url" object if we're processing against
         * a TemplateInstance
         * 
         * If we're processing against a Web Script, it will already be there
         */
        if (rendererContext.getObject() instanceof TemplateInstance)
        {
            if (context instanceof HttpRequestContext)
            {
                HttpServletRequest request = ((HttpRequestContext)context).getRequest();
                
                Map<String, String> args = new HashMap<String, String>(request.getParameterMap().size());
                Enumeration names = request.getParameterNames();
                while (names.hasMoreElements())
                {
                    String name = (String)names.nextElement();
                    args.put(name, request.getParameter(name));
                }
                
                /**
                 * The template processor could be called standalone or as a
                 * processor for a web script.  Some variables might have
                 * already been provided by the web script engine, such as the
                 * "url" variable here.  Thus, if one already exists, we can
                 * just use that one.
                 */
                URLHelper urlHelper = new URLHelper(request, args);
                model.put("url", urlHelper);
            }
            
            // add in the ${head} tag
            if (rendererContext.getObject() instanceof TemplateInstance)
            {               
                model.put("head", RenderUtil.processHeader(rendererContext));
            }
        }
        
        /**
         * TAGS
         */
        
        // TODO: I would like this code to look at the tag libraries
        // registered with the configuration, parse the tag classes
        // and auto-instantiate directives for tags.
        
        // This would allow for developers to create their own tag libraries
        // that work in JSP, Freemarker and any parsed content using a
        // single, consistent approach
        
        model.put("region", new RegionFreemarkerTagDirective(context));
        model.put("component", new ComponentFreemarkerTagDirective(context));
        addDirective(context, model, "componentInclude", "org.alfresco.web.site.taglib.ComponentIncludeTag");
        addDirective(context, model, "pageTitle", "org.alfresco.web.site.taglib.PageTitleTag");
        
        // TODO: remove the directive tag
        addDirective(context, model, "require", "org.alfresco.web.site.taglib.RequireTag");
        
        // content specific
        addDirective(context, model, "anchor", "org.alfresco.web.site.taglib.ObjectAnchorTag");
        addDirective(context, model, "edit", "org.alfresco.web.site.taglib.ObjectEditTag");
        addDirective(context, model, "print", "org.alfresco.web.site.taglib.ObjectPrintTag");
        addDirective(context, model, "link", "org.alfresco.web.site.taglib.ObjectLinkTag");
        
        // temporary: add floating menu
        addDirective(context, model, "floatingMenu", "org.alfresco.web.site.taglib.FloatingMenuTag");        
    }
    
    protected static void addDirective(RequestContext context, Map<String, Object> model, String name, String className)
    {
        GenericFreemarkerTagDirective directive = new GenericFreemarkerTagDirective(context, name, className);
        model.put(name, directive);
    }
}
