<html>
<head>
<title>Gerrit Truck by Repo Report</title>
<style>
 body {
   background-color: #002b36;
   font-family: "Source Code Pro", Consolas, monospace;
   color: #eee8d5;
 }
 .bar {
   width: 100%;
   height: 10px;
   background-color: #dc322f;
 }
 .bar .ok {
   background-color: #2aa198;
   height: 10px;
   width: 100%;
 }
 .repo {
  width: 100%;
   height: 100px;
 }
</style>
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />

</head>
<body style="text-align: center;">
<h5>Gerrit Truck by Repo Report ({{{reportDate}}})</h5>
{{#content}}
  <div class="repo">
  <p>{{{repoName}}} - ({{{okChangesCount}}}/{{{allChangesCount}}})</p>
  <div class="bar"><div class="ok" style="width: {{{percentageOk}}}%;"></div></div>
  </div>
{{/content}}
</body>
</html>
