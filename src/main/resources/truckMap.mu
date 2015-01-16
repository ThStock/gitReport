<html>
<head>
<title>Gerrit Truck Trend Report</title>
<style>
 body {
   background-color: #002b36;
   overflow-y:hidden;
   font-family: "Source Code Pro", Consolas, monospace;
 }
 .contributor {
   border-radius:3px;
   border: 0px solid #073642;
   opacity: .3;
   -webkit-filter: grayscale(1);
   height: 30px;
   width: 30px;
   margin: 0;
   padding: 0;
   float: left;
 }
 .team {
   border-radius:3px;
   border: 1px solid #073642;
   margin: 2px;
   float: left;
   height: 40px;
 }
 .team .spacer {
   width: 6px;
   height: 10px;
   margin: 0;
   float: left;
 }
 .team.warn {
   background-color: #dc322f;
 }
 .team.ok {
   background-color: #2aa198;
 }
</style>
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />

</head>
<body style="text-align: center;">
<h5 style="color: #eee8d5;">Gerrit Truck Trend Report ({{{reportDate}}})</h5>
{{#truckMapContent}}
  <div class="team {{{color}}}" title="{{{title}}}">
    {{#members}}
    <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=80&amp;d=identicon" title="{{{email}}} - {{{typ}}}"class="contributor" />
    {{^isAuthor}}<div class="spacer"></div>{{/isAuthor}}
    {{/members}}
  </div>
{{/truckMapContent}}
</body>
</html>
