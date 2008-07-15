<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/requestutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/searchutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/generic-paged-results.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/blogs/blogpost.lib.js">

const DEFAULT_NUM_DAYS = 7;

/**
 * Fetches all posts of the given blog
 */
function getBlogPostList(node, numdays, index, count)
{
   var fromDate = getTodayMinusXDays(numdays);

   // query information
   var luceneQuery = " +TYPE:\"{http://www.alfresco.org/model/content/1.0}content\"" +
                     " +PATH:\"" + node.qnamePath + "/*\" " +
                     " +ASPECT:\"{http://www.alfresco.org/model/blogintegration/1.0}released\" " +
                     getCreationDateRangeQuery(fromDate, null);

   var sortAttribute = "@{http://www.alfresco.org/model/blogintegration/1.0}released";

   // get the data
   return getPagedResultsDataByLuceneQuery(node, luceneQuery, sortAttribute, false, index, count, getBlogPostData);
}

function main()
{
   // get requested node
   var node = getRequestNode();
   if (status.getCode() != status.STATUS_OK)
   {
      return;
   }

   // process additional parameters
   var index = args["startIndex"] != undefined ? parseInt(args["startIndex"]) : 0;
   var count = args["pageSize"] != undefined ? parseInt(args["pageSize"]) : 10;
   var numdays = args["numdays"] != undefined ? parseInt(args["numdays"]) : DEFAULT_NUM_DAYS;
   
   // fetch and assign the data
   model.data = getBlogPostList(node, numdays, index, count);
   model.contentFormat = (args["contentFormat"] != undefined) ? args["contentFormat"] : "full";
}

main();
