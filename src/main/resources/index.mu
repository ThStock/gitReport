<html>
<head>
<title>Gerrit Truck Trend Report</title>
<style>
 body {
   background-color: #F5F5F5;
   overflow-y:hidden;
 }
 .contributor {
   border-radius:3px;
   border: 0px solid gray;
   opacity: .3;
   -webkit-filter: grayscale(1);
   height: 40px;
   width: 40px;
   margin: 0;
   padding: 0;
   float: left;
 }
 .team {
   border-radius:3px;
   border: 1px solid gray;
   margin: 2px;
   float: left;
   height: 55px;
 }
 .team .spacer {
   width: 6px;
   height: 10px;
   margin: 0;
   float: left;
 }
 .team.warn {
   background-color: red;
 }
 .team.ok {
   background-color: green;
 }
</style>
</head>
<body style="text-align: center;">
<h5>Gerrit Truck Trend Report</h5>
{{#content}}
  <div class="team {{{color}}}" title="{{{title}}}">
    {{#members}}
    <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=80&amp;d=identicon" title="{{{email}}} - {{{typ}}}"class="contributor" />
    {{^isAuthor}}<div class="spacer"></div>{{/isAuthor}}
    {{/members}}
  </div>
{{/content}}
</body>
</html>
