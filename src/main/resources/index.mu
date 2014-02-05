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
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />

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
