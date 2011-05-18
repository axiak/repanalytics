var Demo = Demo || {};

Demo.$loading = null;
Demo.$subcontainer = null;
Demo.$formpanel = null;
Demo.$formerrors = null;
Demo.fadeTime = 250;
Demo.searchHref = "/demo/search/";
Demo.selectHref = "/demo/info/";
Demo.$results = null;

Demo.initialize = function () {
  Demo.$results = $("#demo-results");
  Demo.$formerrors = $("#business-info-errors");
  Demo.$subcontainer = $("#subcontainer");
  Demo.$formpanel = $("#demo-form-panel");
  Demo.$loading = $("<p class='loading' style='display:none'>Loading...</p>");
};

Demo.runDemo = function (data) {
  $.ajax({
        url: Demo.searchHref,
        type: "GET",
        data: data,
        success: function (response) {
          Demo.$loading.fadeOut(Demo.fadeTime);
          if (response._1 && response._1 === "notfound") {
            return Demo.notfound(data);
          } else if (response._1 && response._1 === "disambiguate") {
            return Demo.disambiguate(data, response);
          } else {
            return Demo.demoInfo(response);
          }
        }
      });
  if (Modernizr.history) {
    history.pushState(null, null, Demo.searchHref);
    $(window).bind("popstate", Demo.popstate);
  }
  Demo.$subcontainer.append(Demo.$loading);
  Demo.$formpanel.fadeOut(Demo.fadeTime);
  Demo.$formerrors.hide();
  Demo.$loading.fadeIn(Demo.fadeTime);
};

/*
 * The response came back as not finding any businesses.
 */
Demo.notfound = function (inputData) {
  Demo.$formpanel.fadeIn(Demo.fadeTime);
  if ($("#demo-phone").isInputEmpty()) {
    Demo.$formerrors.text("Sorry - your business was not found. Perhaps you could enter a phone number to help us narrow our search?");
  } else {
    Demo.$formerrors.text("Sorry - your business was not found. We're constantly adding new sources. Please try again later.");
  }
  Demo.$formerrors.fadeIn(Demo.fadeTime);
};

/*
 * The response came back as having one or possibly more different "positive" matches to
 * select against.
 */
Demo.disambiguate = function (inputData, response) {
  var resultLength = response._2.length;
  if (resultLength > 1) {
    Demo.$results.html("<p>The following businesses match your search. Please continue by selecting your business.</p>");
  } else {
    Demo.$results.html("<p>The following business matches your search, but wasn't an exact match. Please confirm that this is your business.</p>");
  }

  $("#disambiguate-business").tmpl($.map(response._2, function (_) {return _._2;})).appendTo(Demo.$results);

  $(".business", Demo.$results)
    .mouseenter(function (e) {
      $(this).addClass("highlighted");
    })
    .mouseleave(function (e) {
      $(this).removeClass("highlighted");
    })
    .click(function (e) {
      $.ajax({
        url: Demo.selectHref,
        type: "GET",
        data: {"id": $(this).attr("data-id")},
        success: function (response) {
          Demo.$loading.fadeOut(Demo.fadeTime);
          Demo.demoInfo(response);
        }
      });
    });

  var backLinks = ["This is not my business.", "Neither of these businesses are mine.", "None of these businesses are mine."];

  $("<button id='disambiguate-back'>" + backLinks[Math.min(resultLength - 1, 2)]  + "</a>")
      .appendTo(Demo.$results)
      .click(function (e) {
        e.preventDefault();
        if (Modernizr.history) {
          history.back();
        } else {
          Demo.$formpanel.fadeIn(Demo.fadeTime);
          Demo.$results.fadeOut(Demo.fadeTime);
        }
      })
      .button();

  Demo.$results.fadeIn(Demo.fadeTime);
};

/*
 * We have data for a business. We have to show it now.
 */
Demo.demoInfo = function (response) {

};


Demo.popstate = function (e) {
  if (location.pathname === "/demo/") {
    Demo.$results.hide();
    Demo.$formpanel.show();
    Demo.$loading.hide();
  }
};



$(function () {
  Demo.initialize();
});