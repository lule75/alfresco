/**
 * Document List Component: action
 *
 * For a single-asset action, template paramters address the asset.
 * For multi-asset actions, template parameters address the source or destination node,
 * and a JSON body addresses the assets involved in the action.
 * (note: HTTP DELETE methods must use URI)
 *
 * @param uri {string} site/{siteId}/{containerId}/{filepath} : full path to file or folder name involved in the action
 * @param uri {string} node/{store_type}/{store_id}/{id}/{filepath} : full path to file or folder name involved in the action
 */

/**
 * Main script entry point
 * @method main
 */
function main()
{
   // Params object contains commonly-used arguments
   var params = {},
      files, rootNode;
   
   if (url.templateArgs.store_type != undefined)
   {
      params = getNodeRefInputParams();
   }
   else if (url.templateArgs.site != undefined)
   {
      params = getSiteInputParams();
   }
   if (typeof params == "string")
   {
      status.setCode(status.STATUS_BAD_REQUEST, params);
      return;
   }

   // Resolve path if available
   var path = url.templateArgs.path;
   // Path might be null for the root folder
   if (!path)
   {
      path = "";
   }
   // Remove any leading or trailing "/" from the path
   // Fix-up parent path to have no leading or trailing slashes
   if (path.length > 0)
   {
      var aPaths = path.split("/");
      while (aPaths[0] === "")
      {
         aPaths.shift();
      }
      while (aPaths[aPaths.length-1] === "")
      {
         aPaths.pop();
      }
      path = aPaths.join("/");
   }
   params.path = path;

   // Multiple input files in the JSON body?
   files = getMultipleInputValues("nodeRefs");
   if (typeof files != "string")
   {
      params.files = files;
   }
   
   // Check runAction function is provided the action's webscript
   if (typeof runAction != "function")
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Action webscript must provide runAction() function.");
      return;
   }

   // Actually run the action
   var results = runAction(params);
   if ((results !== null) && (results !== undefined))
   {
      if (typeof results == "string")
      {
         status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, results);
      }
      else if (typeof results.status == "object")
      {
         // Status fields have been manually set
         status.redirect = true;
         for (var s in results.status)
         {
            status[s] = results.status[s];
         }
      }
      else
      {
         /**
          * NOTE: Webscripts run within one transaction only.
          * If a single operation fails, the transaction is marked for rollback and all
          * previous (successful) operations are also therefore rolled back.
          * We therefore need to scan the results for a failed operation and mark the entire
          * set of operations as failed.
          */
         var overallSuccess = true;
         var successCount = 0;
         var failureCount = 0;
         for (var i = 0, j = results.length; i < j; i++)
         {
            overallSuccess = overallSuccess && results[i].success;
            results[i].success ? ++successCount : ++failureCount;
         }
         model.overallSuccess = overallSuccess;
         model.successCount = successCount;
         model.failureCount = failureCount;
         model.results = results;
      }
   }
}


/**
 * Get and check existence of mandatory input parameters (Site-based)
 *
 * @method getSiteInputParams
 * @return {object|string} object literal containing parameters value or string error
 */
function getSiteInputParams()
{
   var params = {};
   var error = null;
   var template = url.template;
   
   try
   {
      var siteId, containerId, sideNode, rootNode;

      // Try to get the parameters from the URI
      siteId = url.templateArgs.site;
      containerId = url.templateArgs.container;

      // SiteId
      if (template.indexOf("{site}") != -1)
      {
         if ((siteId === null) || (siteId.length === 0))
         {
            return "'site' parameter is missing.";
         }

         // Find the site
         siteNode = siteService.getSite(siteId);
         if (siteNode === null)
         {
            return "Site '" + siteId + "' not found.";
         }

         // ContainerId
         if (template.indexOf("{container}") != -1)
         {
            if ((containerId === null) || (containerId.length === 0))
            {
               return "'container' parameter is missing.";
            }

            // Find the component container
            var rootNode = siteNode.getContainer(containerId);
            if (rootNode === null)
            {
               rootNode = siteNode.createContainer(containerId);
               if (rootNode === null)
               {
                  return "Component container '" + containerId + "' not found in '" + siteId + "'.";
               }

               /**
                * MOB-593: Add email alias on documentLibrary container creation
                *
               rootNode.addAspect("emailserver:aliasable");
               var emailAlias = siteId;
               if (containerId != "documentLibrary")
               {
                  emailAlias += "-" + containerId;
               }
               rootNode.properties["emailserver:alias"] = emailAlias;
               rootNode.save();
               */
            }
         }

         // Populate the return object
         params =
         {
            usingNodeRef: false,
            siteId: siteId,
            containerId: containerId,
            siteNode: siteNode,
            rootNode: rootNode
         }
      }
   }
   catch(e)
   {
      error = e.toString();
   }
   
   // Return the params object, or the error string if it was set
   return (error !== null ? error : params);
}

/**
 * Get and check existence of mandatory input parameters (nodeRef-based)
 *
 * @method getNodeRefInputParams
 * @return {object|string} object literal containing parameters value or string error
 */
function getNodeRefInputParams()
{
   var params = {},
      error = null;
   
   try
   {
      // First try to get the parameters from the URI
      var storeType = url.templateArgs.store_type,
         storeId = url.templateArgs.store_id,
         id = url.templateArgs.id;

      var nodeRef = storeType + "://" + storeId + "/" + id,
         rootNode = null;

      if (nodeRef == "alfresco://company/home")
      {
         rootNode = companyhome;
      }
      else if (nodeRef == "alfresco://user/home")
      {
         rootNode = userhome;
      }
      else
      {
         rootNode = search.findNode(nodeRef);

         if (rootNode === null)
         {
            return "'" + nodeRef  + "' is not a valid nodeRef.";
         }
      }

      // Populate the return object
      params =
      {
         usingNodeRef: true,
         nodeRef: nodeRef,
         rootNode: rootNode
      }
   }
   catch(e)
   {
      error = e.toString();
   }
   
   // Return the params object, or the error string if it was set
   return (error !== null ? error : params);
}

/**
 * Get multiple input values
 *
 * @method getMultipleInputValues
 * @return {array|string} Array containing multiple values, or string error
 */
function getMultipleInputValues(param)
{
   var values = [];
   var error = null;
   
   try
   {
      // Was a JSON parameter list supplied?
      if (typeof json == "object")
      {
         if (!json.isNull(param))
         {
            var jsonValues = json.get(param);
            // Convert from JSONArray to JavaScript array
            for (var i = 0, j = jsonValues.length(); i < j; i++)
            {
               values.push(jsonValues.get(i));
            }
         }
      }
   }
   catch(e)
   {
      error = e.toString();
   }
   
   // Return the values array, or the error string if it was set
   return (error !== null ? error : values);
}


/**
 * Obtain the asset node for the given rootNode and filepath
 *
 * @method getAssetNode
 * @param p_rootNode {object} valid repository node
 * @param p_assetPath {string} rootNode-relative path to asset
 * @return {object|string} valid repository node or string error
 */
function getAssetNode(p_rootNode, p_assetPath)
{
   var assetNode = p_rootNode;
   var error = null;

   try
   {
      if (p_assetPath && (p_assetPath.length > 0))
      {
         assetNode = assetNode.childByNamePath(p_assetPath);
      }
      
      if (assetNode === null)
      {
         return "Asset '" + p_assetPath + " not found.";
      }
   }
   catch(e)
   {
      error = e.toString();
   }

   // Return the node object, or the error string if it was set
   return (error !== null ? error : assetNode);
}
