<html>
<head>
<title>Gerrit Truck by Repo Report</title>
  <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAH4AAAAAv6Hl0QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB94CAxcwI+jd2QUAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAAzklEQVQ4y8WTuxHCQAxE3x4eEpuMCqARaiAmpBGaoQZmqICcDqiAGNtL4N8Z83EAg5LT7Jx2Je0d/DsEMD3JoYBJDkkprqtSO3AGZEAKbEBHcArMamwBCgCWkUACj1GMIlSgMP1i8ZmsJbAc0Ve5R5KEfmNqc70p8rMOLDDVGV/SCJLf2Dg/BCeFCDlMCnFZ51rukwiD8/b23kbjeivdko17c720sSWJSJtdxc69tJHQKNIWqZFWJ5UMbRy+hT5aD6QPNjaEFlh6MNDf+413oHNZGVgDxJMAAAAASUVORK5CYII=" />
  <link rel="stylesheet" href="./bootstrap-3.3.2-dist/css/bootstrap.min.css">
  <link rel="stylesheet" href="./octoicons/octicons.css">
<style>
  body {
    background-color: #002b36;
    font-family: "Source Code Pro", Consolas, monospace;
    font-size: 150%;
    color: #eee8d5;
    text-align: center;
  }
  .branch-too-mutch {
    color: #dc322f;
  }
  .bar {
    width: 100%;
    height: 10px;
    background-color: #dc322f;
    margin-bottom: .5em;
    border-radius:3px;
  }
  .bar .ok {
    background-color: #2aa198;
    height: 10px;
    width: 100%;
    border-radius:3px;
  }
  .repo {
    width: 100%;
    border-radius:9px;
    border: 3px solid #073642;
    margin: 1em 0 1em 0;
    display: block;
    padding: 0 .5em .5em .5em;
  }
  .repo .title {
    margin-top: .6em;
    margin-bottom: .6em;
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
  .colled {
    padding-left: .5em;
    padding-right: .5em;
  }
  .title-name {
    text-align: left;
  }
  .title-details {
    text-align: right;
  }
</style>
</head>

<body>
<h5>Gerrit Truck by Repo Report ({{{reportDate}}})</h5>
<h6>{{{content.newestCommitDate}}} - {{{content.latestCommitDate}}}</h6>

<div class="container-fluid">
  <div class="row">
  {{#content.slots}}
      <div class="content">
      <div class="col-md-4 colled">
      {{#repos}}
        <div class="repo">
          <div class="row title">
            <div class="col-xs-5 title-name">
              <span class="octicon octicon-repo"></span> {{{repoName}}}
            </div>
            <div class="col-xs-2">
            <span title="{{{branchNamesText}}}" {{^branchCountOk}}class="branch-too-mutch"{{/branchCountOk}}><span class="octicon octicon-git-branch"></span>{{{branchCount}}}</span>
            </div>
            <div class="col-xs-5 title-details">
                    <span class="activity-{{{activityIndex}}}"><span class="octicon octicon-pulse"></span> <span title="with / without review">{{{okChangesCount}}}/{{{allChangesCount}}}</span>
                    | <span title="changes per day and committer">{{{changesPerDay}}}<sub>dc<sub></span>
                    | <span title="main committers based on SD">{{{mainComitters}}}<sub>mc<sub></span></span>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12">
              <div class="bar"><div class="ok" style="width: {{{percentageOk}}}%;"></div></div>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12">
            <div class="members">
                    {{#members}}
                      <span class="contributor {{{activityValue}}}" >
                        <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=160&amp;d=identicon" title="{{{email}}} - {{{typ}}}" />
                      </span>
                    {{/members}}
                    </div>
            </div>
          </div>
        </div>
      {{/repos}}
      </div>
  {{/content.slots}}
  </div>
</div>
</body>
</html>
