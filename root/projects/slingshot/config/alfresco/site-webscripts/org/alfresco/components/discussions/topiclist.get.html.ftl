<script type="text/javascript">//<![CDATA[
   new Alfresco.DiscussionsTopicList("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!''}",
      containerId: "${template.properties.container!'blog'}",
      initialFilter:
      {
         filterId: "${page.url.args.filterId!'new'}",
         filterOwner: "${page.url.args.filterOwner!'Alfresco.TopicListFilter'}",
         filterData: <#if page.url.args.filterData??>"${page.url.args.filterData}"<#else>null</#if>
      }
   }).setMessages(
      ${messages}
   );
//]]></script>
<div class="topiclist-infobar yui-g theme-bg-color-4">
   <div class="yui-u first">
      <div id="${args.htmlid}-listtitle" class="listTitle">
         ${msg("title.generic")}
      </div>
   </div>
   <div class="yui-u align-right flat-button">
      <div id="${args.htmlid}-paginator" class="paginator">&nbsp;</div>
      <div class="simple-view">
         <button id="${args.htmlid}-simpleView-button" name="topiclist-simpleView-button">${msg("header.simpleList")}</button>
      </div>
   </div>
</div>
<div id="${args.htmlid}-topiclist" class="topiclist">
</div>
