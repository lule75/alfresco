/**
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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * Tag Filter component.
 * 
 * @namespace Alfresco
 * @class Alfresco.TagFilter
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * Tag Filter constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.TagFilter} The new TagFilter instance
    * @constructor
    */
   Alfresco.TagFilter = function(htmlId)
   {
      Alfresco.TagFilter.superclass.constructor.call(this, "Alfresco.TagFilter", htmlId);
      
      // Override unique event key, as we want tag events to be page-global
      this.uniqueEventKey = "tag-link";
      
      // Decoupled event listeners
      YAHOO.Bubbling.on("tagRefresh", this.onTagRefresh, this);
      
      return this;
   };
   
   YAHOO.extend(Alfresco.TagFilter, Alfresco.component.BaseFilter,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",

         /**
          * ContainerId representing root container
          *
          * @property containerId
          * @type string
          * @default ""
          */
         containerId: "",

         /**
          * Number of tags to show
          *
          * @property numTags
          * @type int
          * @default 15
          */
         numTags: 15
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Registers event handler on 'onTagRefresh' event. If a component wants to refresh
       * the tags component, they need to fire this event.
       *
       * @method onReady
       */   
      onReady: function TagFilter_onReady()
      {
         Alfresco.TagFilter.superclass.onReady.call(this);

         // Kick-off tag population
         if (this.options.siteId && this.options.containerId)
         {
            YAHOO.Bubbling.fire("tagRefresh");
         }
      },
      

      /**
       * BUBBLING LIBRARY EVENT HANDLERS
       * Disconnected event handlers for inter-component event notification
       */
      
      /**
       * Function that gets called when another component fires "tagRefresh"
       * Issues a request to retrieve the latest tag data.
       *
       * @method onTagRefresh
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onTagRefresh: function TagFilter_onRefresh(layer, args)
      {
         var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/tagscopes/site/{site}/{container}/tags?d={d}&tn={tn}",
         {
            site: this.options.siteId,
            container: this.options.containerId,
            d: new Date().getTime(),
            tn: this.options.numTags
         });
         
         Alfresco.util.Ajax.request(
         {
            method: Alfresco.util.Ajax.GET,
				url: url,
				successCallback:
				{
					fn: this.onTagRefresh_success,
					scope: this
				},
				failureMessage: this.msg("message.refresh.failure")
			});
      },
      
      /**
       * Event handler for when the tag data loads successfully.
       *
       * @method onTagRefresh_success
       * @param response {object} Server response object
       */ 
      onTagRefresh_success: function TagFilter_onTagRefresh_success(response)
      {
         if (response && response.json && !YAHOO.lang.isUndefined(response.json.tags))
         {
            var html = "",
               tags = response.json.tags;
            for (var i = 0, ii = tags.length; i < ii; i++)
            {
               html += this._generateTagMarkup(tags[i]);
            }
            
            Dom.get(this.id + "-tags").innerHTML = html;
         }
      },
      

      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Generates the HTML for a tag.
       *
       * @method _generateTagMarkup
       * @param tag {object} the tag to render
       */
      _generateTagMarkup: function TagFilter__generateTagMarkup(tag)
      {
         var html = '<li><span class="tag">';
         html += '<a href="#" class="' + this.uniqueEventKey + '" rel="' + $html(tag.name) + '">' + $html(tag.name) + '</a>&nbsp;(' + tag.count + ')';
         html += '</span></li>';
         return html;
      }
   });
})();