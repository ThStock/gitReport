<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Gerrit Truck by Repo Report</title>
  <link rel="icon" type="image/png" href="git-report-xs.png" />
  <link rel="stylesheet" href="./bootstrap-3.3.2-dist/css/bootstrap.min.css">
  <link rel="stylesheet" href="./octoicons/octicons.css">
<style>
  body {
    background-color: #002b36;
    font-family: "Source Code Pro", Consolas, monospace;
    font-size: 150%;
    color: #eee8d5;
    text-align: center;
    padding-bottom: 1em;
    -webkit-backface-visibility: hidden;
    -webkit-transform:translate3d(0,0,0);
  }
  .branch-too-mutch {
    color: #dc322f;
  }
  .bar {
    width: 100%;
    height: 10px;
    background-color: #dc322f;
    margin-bottom: .5em;
    border-radius: 3px;
  }
  .bar-66 {
    background-color: rgba(203, 75, 22, 0.5);
  }
  .bar-80 {
    background-color: #073642;
  }
  .bar-no {
    background-color: #073642;
  }
  .bar .ok {
    background-color: #2aa198;
    height: 10px;
    width: 100%;
    border-radius: 3px;
  }
  .bar .needle {
    position: absolute;
    width: 1px;
    background-color: #002b36;
    height: 10px;
    top: 0;
  }
  .repo {
    width: 100%;
    border-radius:9px;
    border: 3px solid #073642;
    margin: 1em 0 1em 0;
    display: block;
    padding: 0 .5em 0 .5em;
    position: relative;
  }
  .repo.repo-high.repo-66 {
    border-color: rgba(42, 161, 152, 0.5);
  }
  .repo.repo-high.repo-80 {
    border-color: #2aa198;
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
    margin-bottom: .6em;
    padding: 0;
    position: relative;
    -webkit-transition: all .2s ease-in-out; /* Safari and Chrome */
    -moz-transition: all .2s ease-in-out; /* Firefox */
    -ms-transition: all .2s ease-in-out; /* IE 9 */
    -o-transition: all .2s ease-in-out; /* Opera */
    transition: all .2s ease-in-out;
    -webkit-transform-style: preserve-3d;
    z-index: 10;
  }
  .contributor img:hover {
    -webkit-transform:scale(3); /* Safari and Chrome */
    -moz-transform:scale(3); /* Firefox */
    -ms-transform:scale(3); /* IE 9 */
    -o-transform:scale(3); /* Opera */
    transform:scale(3);
    opacity: 1 !important;
    z-index: 100 !important;
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
  .contributor.no-gerrit-contrib img {
    opacity: 0.6;
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
  .glow {
    color: #eee8d5;
    font-weight: bolder;
  }
  h5, h6 {
    margin: 0;
    font-weight: bolder;
  }
  header {
    background-color: #2aa198;
    color: #002b36;
    padding-top: .6em;
    padding-bottom: .5em;
    padding-left: 2.5em;
  }
  footer {
    clear: both;
    text-align: right;
    bottom: 0;
    width: 100%;
    height: 60px;
    padding: 1em;
    color: #2aa198;
  }
  .participation-graph {
    z-index: -1;
    position: absolute;
    top: 0;
    width: 250px;
  }
</style>
</head>

<body>
<header>
<img src="./git-report.svg" style="width: 2.3em; position: absolute; left: .3em; top: .3em;" />
<h5>Gerrit Truck by Repo Report ({{{reportDate}}})</h5>
<h6>{{{content.newestCommitDate}}} - {{{content.latestCommitDate}}} -
  <span title="sprint lenght in days">Δ {{{content.sprintLength}}}</span></h6>
</header>
<div class="container-fluid">
  <div class="row">
  {{#content.slots}}
      <div class="content">
      <div class="col-md-4 colled">
      {{#repos}}
        <div class="repo repo-{{{activityIndex}}} {{#percentageOkGt66}}repo-66{{/percentageOkGt66}}
        {{#percentageOkGt80}}repo-80{{/percentageOkGt80}}">
          <div class="row title">
            <div class="col-xs-6 title-name">
              <span class="octicon octicon-repo"></span> {{{repoName}}}
            </div>
            <div class="col-xs-6 title-details">
                    <span class="activity-{{{activityIndex}}}"><span class="octicon octicon-pulse"></span> <span title="with / without review">{{^noGerrit}}{{{okChangesCount}}}/{{/noGerrit}}{{{allChangesCount}}}</span>
                    <span title="{{{branchNamesText}}}" {{^branchCountOk}}class="branch-too-mutch"{{/branchCountOk}}><span class="octicon octicon-git-branch"></span>{{{branchCount}}}</span>
                    <span title="changes per day and committer">{{{changesPerDay}}}<sub>dc<sub></span>
                    <span class="{{#topComitter}}glow{{/topComitter}}" title="main committers based on SD">{{{mainComitters}}}<sub>mc<sub></span></span>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12">
              <div class="bar {{#percentageOkGt66}}bar-66{{/percentageOkGt66}} {{#percentageOkGt80}}bar-80{{/percentageOkGt80}} {{#noGerrit}}bar-no{{/noGerrit}}"><div class="ok" style="width: {{{percentageOk}}}%;"></div>
              {{^noGerrit}}<div class="needle" style="left: 66%;"></div><div class="needle" style="left: 80%;"></div>{{/noGerrit}}</div>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12">
            <div class="members">
                    {{#members}}
                      <span class="contributor {{{activityValue}}}{{#noGerrit}} no-gerrit-contrib{{/noGerrit}}" >
                        <img src="https://lb.gravatar.com/avatar/{{{hash}}}?s=160&amp;d=identicon" title="{{{email}}} - {{{typ}}}" />
                      </span>
                    {{/members}}
                    </div>
            </div>
          </div>
          <div class="participation-graph">
             <!-- chrome and firefox handles this diffrend -->
             <svg class="bars" width="100%" height="40">
                {{#participationBars}}
                  <rect width="{{{width}}}%" height="{{{height}}}%" x="{{{x}}}%" fill="#073642"></rect>
                {{/participationBars}}
             </svg>
          </div>
        </div>
      {{/repos}}
      </div>
  {{/content.slots}}
  </div>
</div>
<footer>
  <img src="./git-report.svg" style="width: 3em; opacity: .33;" />
</footer>
</body>
</html>
