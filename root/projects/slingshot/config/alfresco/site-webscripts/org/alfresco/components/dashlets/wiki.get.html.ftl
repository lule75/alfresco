<script type="text/javascript">//<![CDATA[
   new Alfresco.WikiDashlet("${args.htmlid}").setGUID(
      "${instance.object.id}"
   ).setSiteId("${page.url.templateArgs.site!""}");
//]]></script>
<div class="dashlet wiki">
   <div class="title" id="${args.htmlid}-title">${msg("label.header-prefix")} - ${pageTitle!msg("label.header")}</div>
   <div class="toolbar">
       <a href="#" id="${args.htmlid}-wiki-link">${msg("label.configure")}</a>
   </div>
   <div class="body scrollableList">
      <div id="${args.htmlid}-scrollableList" class="rich-content">
<#if wikipage?exists>
         ${wikipage}
<#else>
		   ${msg("label.noConfig")}
</#if>
      </div>
	</div>
</div>