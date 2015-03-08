<html>
<head>
<title>Gerrit Truck by Repo Report</title>
<style>
  body {
    background-color: #002b36;
    font-family: "Source Code Pro", Consolas, monospace;
    color: #eee8d5;
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
    display: inline-block;
    padding-bottom: .3em;
  }
  .contributor img {
    border-radius:3px;
    border: 0px solid #073642;
    height: 30px;
    width: 30px;
    margin: 0;
    padding: 0;
    -webkit-transition: all .2s ease-in-out; /* Safari and Chrome */
    -moz-transition: all .2s ease-in-out; /* Firefox */
    -ms-transition: all .2s ease-in-out; /* IE 9 */
    -o-transition: all .2s ease-in-out; /* Opera */
    transition: all .2s ease-in-out;
  }
  .contributor img:hover {
    -webkit-transform:scale(3.25); /* Safari and Chrome */
    -moz-transform:scale(3.25); /* Firefox */
    -ms-transform:scale(3.25); /* IE 9 */
    -o-transform:scale(3.25); /* Opera */
    transform:scale(3.25);
    opacity: 1.0 !important;
  }
  .contributor.lowest img {
    opacity: 0.1;
  }
  .contributor.low img {
    opacity: 0.4;
  }
  .contributor.mid img {
    opacity: 0.6;
  }
  .contributor.high img {
    opacity: 0.8;
  }
  .contributor.highest img {
    opacity: 1.0;
  }
  .members {
    margin-right: .5em;
    margin-left: .5em;
  }
  .octicon {
    font-size: 32px !important;
  }
  .activity-low {
    color: #40565e;
  }
  .activity-normal {
    color: #839496;
  }
  .activity-high {
    color: #2aa198;
  }

  #left { float:left; width:33%; }
  #right { float:right; width:33%; }
  #center { margin-left:33%; margin-right:33%; }
  #clear { clear:both; }
  #left, #right, #center { padding:1px }

</style>
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />
  <link rel="stylesheet" href="./octoicons/octicons.css">
</head>
<body style="text-align: center;">
<h5>Gerrit Truck by Repo Report ({{{reportDate}}} | {{{content.newestCommitDate}}} - {{{content.latestCommitDate}}})</h5>
<div id="container">
  {{#content.slots}}
    <div id="{{{name}}}">
      <div class="content">
      {{#repos}}
        <div class="repo">
        <p> <span class="octicon octicon-repo"></span> {{{repoName}}}
        <span title="{{{branchNamesText}}}" {{^branchCountOk}}class="branch-too-mutch"{{/branchCountOk}}><span class="octicon octicon-git-branch"></span>{{{branchCount}}}</span>
        <span class="activity-{{{activityIndex}}}"><span class="octicon octicon-pulse"></span> {{{okChangesCount}}}/{{{allChangesCount}}}</span></p>
        <div class="bar"><div class="ok" style="width: {{{percentageOk}}}%;"></div></div>
        <div class="members">
        {{#members}}
          <span class="contributor {{{activityValue}}}" >
            <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=160&amp;d=identicon" title="{{{email}}} - {{{typ}}}" />
          </span>
        {{/members}}
        </div>
        </div>
      {{/repos}}
      </div>
    </div>
  {{/content.slots}}
  <div id="clear">-</div>
</div>
</body>
</html>
