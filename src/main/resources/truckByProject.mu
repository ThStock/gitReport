<html>
<head>
<title>Gerrit Truck by Repo Report</title>
<style>
  body {
    background-color: #002b36;
    font-family: "Source Code Pro", Consolas, monospace;
    color: #eee8d5;
  }
  .content {
    -webkit-column-count: 3; /* Chrome, Safari, Opera */
    -moz-column-count: 3; /* Firefox */
    column-count: 3;
  }
  .branch-too-mutch {
    color: #dc322f;
  }
  .bar {
    width: 100%;
    height: 10px;
    background-color: #dc322f;
    margin-bottom: .5em;
  }
  .bar .ok {
    background-color: #2aa198;
    height: 10px;
    width: 100%;
  }
  .repo {
    width: 95%;
    border-radius:9px;
    border: 3px solid #073642;
    margin-bottom: 1em;
    margin-right: 1em;
    display: inline-block;
    padding-bottom: .3em;
  }
  .contributor {
    border-radius:3px;
    border: 0px solid #073642;
    height: 30px;
    width: 30px;
    margin: 0;
    padding: 0;
  }
  .members {
    margin-right: .5em;
    margin-left: .5em;
  }
  .octicon {
    font-size: 32px !important;
  }

</style>
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />
  <link rel="stylesheet" href="./octoicons/octicons.css">
</head>
<body style="text-align: center;">
<h5>Gerrit Truck by Repo Report ({{{reportDate}}})</h5>
<div class="content">
{{#content}}
  <div class="repo">
  <p> <span class="octicon octicon-repo"></span> {{{repoName}}}
  <span title="{{{branchNamesText}}}" {{^branchCountOk}}class="branch-too-mutch"{{/branchCountOk}}><span class="octicon octicon-git-branch"></span>{{{branchCount}}}</span>
  <span class="octicon octicon-pulse"></span> {{{okChangesCount}}}/{{{allChangesCount}}}</p>
  <div class="bar"><div class="ok" style="width: {{{percentageOk}}}%;"></div></div>
  <div class="members">
  {{#members}}
    <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=80&amp;d=identicon" title="{{{email}}} - {{{typ}}}" class="contributor" />
  {{/members}}
  </div>
  </div>
{{/content}}
</div>
</body>
</html>
