<script type="text/javascript">//<![CDATA[
   new Alfresco.WebView("${args.htmlid}").setOptions(
   {
      componentId: "${instance.object.id}",
      webviewURI: "${uri}",
      webviewTitle: "${webviewTitle}"
   });
//]]></script>
<div class="dashlet webview">
   <div class="title">
      <a id="${args.htmlid}-title-link" class="title-link" href="${uri}" target="_blank"><#if webviewTitle?? && webviewTitle != "">${webviewTitle}<#else>${uri}</#if></a>
      <a id="${args.htmlid}-configWebView-link" class="configure theme-color-1" href="#">${msg("label.configure")}</a>
      <span>&nbsp;</span>
   </div>

   <div class="toolbar"></div>

   <div class="body scrollablePanel" style=";" id="${args.htmlid}-iframeWrapper">
       <iframe frameborder="0" scrolling="auto" width="100%" height="100%" src="${uri}"></iframe>
   </div><#-- end of body -->

</div><#-- end of dashlet -->


