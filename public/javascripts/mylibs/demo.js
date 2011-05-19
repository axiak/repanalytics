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
  google.load("visualization", "1", {packages:["corechart"]});
  $("#login-button").click(function () {
    Demo.secretSquirrel();
  });
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
      Demo.$loading.fadeIn(Demo.fadeTime);
      Demo.$results.hide();

    });

  var backLinks = ["This is not my business.", "Neither of these businesses are mine.", "None of these businesses are mine."];

  $("<button id='disambiguate-back'>" + backLinks[Math.min(resultLength - 1, 2)]  + "</a>")
      .appendTo(Demo.$results)
      .click(function (e) {
        e.preventDefault();
        if (Modernizr.history) {
          history.back();
        } else {
          Demo.returnToFormPanel();
        }
      })
      .button();

  Demo.$results.fadeIn(Demo.fadeTime);
};

/*
 * We have data for a business. We have to show it now.
 */
Demo.demoInfo = function (response) {
  Demo.$results.html('');
  $("#business-report").tmpl(response[0].business).appendTo(Demo.$results);
  $("#review-tmpl").tmpl(response).appendTo("table.business-reviews tbody:first");
  $("div.chart-tabs").tabs();
  $("h2.subtitle")
    .addClass("my-restaurant")
    .html("My Restaurant - Demo");
  Demo.$results.addClass("my-restaurant");
  Demo.$results.fadeIn(Demo.fadeTime,
      function () {
        Demo.drawSentimentGraph(response);
        Demo.drawRatingPie(response);
      }
  );
};

Demo.drawSentimentGraph = function (results) {
  var data = new google.visualization.DataTable();
  data.addColumn("date", "Date");
  data.addColumn("number", "Sentiment");
  data.addRows(results.length);
  $.each(results, function (index, value) {
    var dateParts = value.date.split("/");
    data.setValue(index, 0, new Date(dateParts[2], dateParts[0], dateParts[1]));
    data.setValue(index, 1, value.sentiment);
  });
  var chart = new google.visualization.LineChart($("#tab-sentiment")[0]);
  chart.draw(data, {width: 450, height: 220, title: "Review Sentiment"});
};

Demo.drawRatingPie = function (results) {
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Rating');
  data.addColumn("number", "Review Popularity");

  var histogram = {};
  var histogramLength = 0;
  $.each(results, function (index, value) {
    if (histogram[value.rating] === undefined) {
      histogramLength++;
      histogram[value.rating] = 1;
    } else {
      histogram[value.rating] ++;
    }
  });
  data.addRows(histogramLength);
  var idx = 0;
  $.each(histogram, function (rating, num) {
    data.setValue(idx, 0, "" + rating + " star" + (rating == 1 ? "" : "s"));
    data.setValue(idx, 1, num);
    idx++;
  });
  var chart = new google.visualization.PieChart($("#tab-ratings")[0]);
  chart.draw(data, {width: 450, height: 220, title: "Rating Breakdown"});
};

Demo.secretSquirrel = function () {
  if (!Demo.$formpanel.is(":visible")) {
    return;
  }
  var data = {
    "name": "Cheesecake Factory",
    "address": "100 Cambridgeside Pl",
    "city": "Cambridge",
    "state": "MA",
    "zip": "02138"
  };
  $.each(data, function (key, value) {
    $("#demo-" + key)
        .trigger("focus")
        .val(value)
        .trigger("blur");
  });
  $("#chain").attr({"checked": "checked"});
}


Demo.popstate = function (e) {
  if (location.pathname === "/demo/") {
    Demo.returnToFormPanel();
  }
};

Demo.returnToFormPanel = function () {
  Demo.$results.hide();
  Demo.$formpanel.show();
  Demo.$loading.hide();
  Demo.$results.removeClass("my-restaurant");
  $("h2.subtitle")
    .removeClass("my-restaurant")
    .html("Demo");
};



$(function () {
  Demo.initialize();
});