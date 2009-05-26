/**
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
 * Records Report component.
 * 
 * @namespace Alfresco
 * @class Alfresco.RecordsReport
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * Search constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.RecordsReport} The new RecordsReport instance
    * @constructor
    */
   Alfresco.RecordsReport = function(htmlId)
   {
      /* Mandatory properties */
      this.name = "Alfresco.RecordsReport";
      this.id = htmlId;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "calendar", "container", "datasource", "datatable", "json"], this.onComponentsLoaded, this);
      
      return this;
   };
   
   Alfresco.RecordsReport.prototype =
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
          * siteId to report against.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",

         /**
          * Maximum number of results displayed.
          * 
          * @property maxResults
          * @type int
          * @default 100
          */
         maxResults: 100
      },

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},

      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
      modules: {},

      /**
       * Number of search results.
       */
      resultsCount: 0,
      
      /**
       * True if there are more results than the ones listed in the table.
       */
      hasMoreResults: false,
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.RecordsReport} returns 'this' for method chaining
       */
      setOptions: function Search_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.RecordsReport} returns 'this' for method chaining
       */
      setMessages: function Search_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },
      
      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function Search_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function Search_onReady()
      {
         var me = this;
         
         // function to setup calendar localisation properties
         var calendarSetup = function calendarSetup(cal)
         {
            cal.cfg.setProperty("MONTHS_SHORT", me._msg("months.short").split(","));
            cal.cfg.setProperty("MONTHS_LONG", me._msg("months.long").split(","));
            cal.cfg.setProperty("WEEKDAYS_1CHAR", me._msg("days.initial").split(","));
            cal.cfg.setProperty("WEEKDAYS_SHORT", me._msg("days.short").split(","));
            cal.cfg.setProperty("WEEKDAYS_MEDIUM", me._msg("days.medium").split(","));
            cal.cfg.setProperty("WEEKDAYS_LONG", me._msg("days.long").split(","));
            cal.render();
         };
         
         // Generate calendar controls
         this.widgets.approvedFromCalendar = new YAHOO.widget.Calendar(this.id + "-approved-from");
         calendarSetup(this.widgets.approvedFromCalendar);
         this.widgets.approvedToCalendar = new YAHOO.widget.Calendar(this.id + "-approved-to");
         calendarSetup(this.widgets.approvedToCalendar);
         this.widgets.reviewedFromCalendar = new YAHOO.widget.Calendar(this.id + "-reviewed-from");
         calendarSetup(this.widgets.reviewedFromCalendar);
         this.widgets.reviewedToCalendar = new YAHOO.widget.Calendar(this.id + "-reviewed-to");
         calendarSetup(this.widgets.reviewedToCalendar);
         // TODO: bind events
         //this.widgets.approvedFromCalendar.selectEvent.subscribe(this.onDateSelected, this, true);
         
         // DataSource definition
         var uriSearchResults = Alfresco.constants.PROXY_URI + "slingshot/rmsearch?";
         this.widgets.dataSource = new YAHOO.util.DataSource(uriSearchResults);
         this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
         this.widgets.dataSource.connXhrMode = "queueRequests";
         this.widgets.dataSource.responseSchema =
         {
             resultsList: "items",
             fields: ["nodeRef", "name", "title", "description", "modifiedOn", "modifiedByUser", "modifiedBy",
                      "createdOn", "createdByUser", "createdBy", "size", "browseUrl",
                      "properties.identifier", "properties.dateFiled", "properties.publicationDate", "properties.dateReceived",
                      "properties.originatingOrganization", "properties.mediaType", "properties.format", "properties.location"]
         };
         
         // setup of the datatable.
         this._setupDataTable();
         
         // Finally show the component body here to prevent UI artifacts on YUI button decoration
         Dom.setStyle(this.id + "-body", "visibility", "visible");
      },
      
      _setupDataTable: function Search_setupDataTable()
      {
         /**
          * DataTable Cell Renderers
          *
          * Each cell has a custom renderer defined as a custom function. See YUI documentation for details.
          * These MUST be inline in order to have access to the Alfresco.RecordsReport class (via the "me" variable).
          */
         var me = this;
         
         /**
          * Record Icon image custom datacell formatter
          *
          * @method renderCellImage
          */
         renderCellImage = function Search_renderCellImage(elCell, oRecord, oColumn, oData)
         {
            oColumn.width = 64;
            oColumn.height = 64;
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");
            Dom.setStyle(elCell.parentNode, "height", oColumn.height + "px");
            Dom.setStyle(elCell.parentNode, "text-align", "center");
            
            var url = me._getBrowseUrlForRecord(oRecord);
            var imageUrl = Alfresco.constants.URL_CONTEXT + 'components/images/filetypes/generic-file-32.png';
            
            var name = $html(oRecord.getData("name"));
            elCell.innerHTML = '<span><a href="' + encodeURI(url) + '"><img src="' + imageUrl + '" alt="' + name + '" title="' + name + '" /></a></span>';
         };

         /**
          * URI custom datacell formatter
          *
          * @method renderCellURI
          */
         renderCellURI = function Search_renderCellURI(elCell, oRecord, oColumn, oData)
         {
            var url = me._getBrowseUrlForRecord(oRecord);
            elCell.innerHTML = '<span><a href="' + encodeURI(url) + '">' + oRecord.getData("properties.identifier") + '</a></span>';
         };
         
         /**
          * Date custom datacell formatter
          *
          * @method renderCellDate
          */
         renderCellDate = function Search_renderCellDate(elCell, oRecord, oColumn, oData)
         {
            if (oData)
            {
               elCell.innerHTML = Alfresco.util.formatDate(oData);
            }
         };
         
         /**
          * Generic HTML-safe custom datacell formatter
          */
         var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData)
         {
            elCell.innerHTML = $html(oData);
         };
         
         /**
          * URI custom datacell sorter
          */
         var sortCellURI = function sortCellURI(a, b, desc)
         {
            var numA = parseInt(a.getData("properties.identifier")),
                numB = parseInt(b.getData("properties.identifier"));
            
            if (desc)
            {
               return (numA < numB ? 1 : (numA > numB ? -1 : 0));
            }
            return (numA < numB ? -1 : (numA > numB ? 1 : 0));
         };
         
         // DataTable column defintions
         var columnDefinitions =
         [
            { key: "image", label: "", sortable: false, formatter: renderCellImage, width: "64px" },
            { key: "identifier", label: me._msg("label.identifier"), sortable: true, sortOptions: {sortFunction: sortCellURI}, resizeable: true, formatter: renderCellURI },
            { key: "title", label: me._msg("label.title"), field: "title", sortable: true, resizeable: true, formatter: renderCellSafeHTML },
            { key: "originator", label: me._msg("label.originator"), field: "createdBy", sortable: true, resizeable: true, formatter: renderCellSafeHTML },
            { key: "dateFiled", label: me._msg("label.dateFiled"), field: "properties.dateFiled", sortable: true, resizeable: true, formatter: renderCellDate },
            { key: "publicationDate", label: me._msg("label.publicationDate"), field: "properties.publicationDate", sortable: true, resizeable: true, formatter: renderCellDate },
            { key: "originatingOrganization", label: me._msg("label.originatingOrganization"), field: "properties.originatingOrganization", sortable: true, resizeable: true, hidden: true },
            { key: "mediaType", label: me._msg("label.mediaType"), field: "properties.mediaType", sortable: true, resizeable: true, hidden: true },
            { key: "format", label: me._msg("label.format"), field: "properties.format", sortable: true, resizeable: true, hidden: true },
            { key: "dateReceived", label: me._msg("label.dateReceived"), field: "properties.dateReceived", sortable: true, resizeable: true, formatter: renderCellDate, hidden: true },
            { key: "location", label: me._msg("label.location"), field: "properties.location", sortable: true, resizeable: true, hidden: true }
         ];
         
         // DataTable definition
         this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-results", columnDefinitions, this.widgets.dataSource,
         {
            renderLoopSize: 32,
            draggableColumns: true,
            initialLoad: false
         });
         
         // show initial message
         this._setDefaultDataTableErrors(this.widgets.dataTable);
         this.widgets.dataTable.set("MSG_EMPTY", "");
         
         // Override abstract function within DataTable to set custom error message
         this.widgets.dataTable.doBeforeLoadData = function Search_doBeforeLoadData(sRequest, oResponse, oPayload)
         {
            if (oResponse.error)
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  me.widgets.dataTable.set("MSG_ERROR", response.message);
               }
               catch(e)
               {
                  me._setDefaultDataTableErrors(me.widgets.dataTable);
               }
            }
            else if (oResponse.results)
            {
               // clear the empty error message
               me.widgets.dataTable.set("MSG_EMPTY", "");
               
               // update the results count, update hasMoreResults.
               me.hasMoreResults = (oResponse.results.length > me.options.maxResults);
               if (me.hasMoreResults)
               {
                  oResponse.results = oResponse.results.slice(0, me.options.maxResults);
               }
               me.resultsCount = oResponse.results.length;
               me.renderLoopSize = 32;
            }
            
            // Must return true to have the "Loading..." message replaced by the error message
            return true;
         };
      },

      /**
       * BUBBLING LIBRARY EVENT HANDLERS FOR PAGE EVENTS
       * Disconnected event handlers for inter-component event notification
       */

      /**
       * Resets the YUI DataTable errors to our custom messages
       * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
       *
       * @method _setDefaultDataTableErrors
       * @param dataTable {object} Instance of the DataTable
       */
      _setDefaultDataTableErrors: function Search__setDefaultDataTableErrors(dataTable)
      {
         var msg = Alfresco.util.message;
         dataTable.set("MSG_EMPTY", msg("message.empty", "Alfresco.RecordsReport"));
         dataTable.set("MSG_ERROR", msg("message.error", "Alfresco.RecordsReport"));
      },
      
      /**
       * Updates document list by calling data webscript with current site and path
       * 
       * @method _performSearch
       * @param path {string} Path to navigate to
       */
      _performSearch: function Search__performSearch(searchQuery)
      {
         // empty results table
         this.widgets.dataTable.deleteRows(0, this.widgets.dataTable.getRecordSet().getLength());
          
         // update the ui to show that a search is on-going
         this.widgets.dataTable.set("MSG_EMPTY", "");
         this.widgets.dataTable.render();
         
         function successHandler(sRequest, oResponse, oPayload)
         {
            this.searchQuery = searchQuery;
            this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
         }
         
         function failureHandler(sRequest, oResponse)
         {
            if (oResponse.status == 401)
            {
               // Our session has likely timed-out, so refresh to offer the login page
               window.location.reload();
            }
            else
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  this.widgets.dataTable.set("MSG_ERROR", response.message);
                  this.widgets.dataTable.showTableMessage(response.message, YAHOO.widget.DataTable.CLASS_ERROR);
               }
               catch(e)
               {
                  this._setDefaultDataTableErrors(this.widgets.dataTable);
                  this.widgets.dataTable.render();
               }
            }
         }
         
         this.widgets.dataSource.sendRequest(this._buildSearchParams(searchQuery),
         {
            success: successHandler,
            failure: failureHandler,
            scope: this
         });
      },

      /**
       * Build URI parameter string for search JSON data webscript
       *
       * @method _buildSearchParams
       * @param path {string} Path to query
       */
      _buildSearchParams: function Search__buildSearchParams(searchQuery)
      {
         var params = YAHOO.lang.substitute("site={site}&query={query}&maxResults={maxResults}",
         {
            site: encodeURIComponent(this.options.siteId),
            query : encodeURIComponent(searchQuery),
            maxResults : this.options.maxResults + 1 // to be able to know whether we got more results
         });
         
         return params;
      },
      
      /**
       * Constructs the browse url for a given record.
       */
      _getBrowseUrlForRecord: function _getBrowseUrlForRecord(oRecord)
      {
         var url = "#";
         if (oRecord.getData("browseUrl") !== undefined)
         {
            url = Alfresco.constants.URL_PAGECONTEXT + "site/" + this.options.siteId + "/" + oRecord.getData("browseUrl");
         }
         return url;
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function Search__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.RecordsReport", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();