window.Demo = window.Demo || {};

Demo.$loading = null;
Demo.$subcontainer = null;
Demo.$formpanel = null;
Demo.$formerrors = null;
Demo.fadeTime = 250;
Demo.searchHref = "/demo/search/";
Demo.selectHref = "/demo/info/";
Demo.$results = null;
Demo.$feedBody = null;
Demo.$feedStats = [0, 0, 0];
Demo.feedMaxSize = 8;

if (!Demo.wsUrl) {
  Demo.wsUrl = '';
}

Demo.initialize = function () {
  Demo.$results = $("#demo-results");
  Demo.$formerrors = $("#business-info-errors");
  Demo.$subcontainer = $("#subcontainer");
  Demo.$formpanel = $("#demo-form-panel");
  Demo.$loading = $("<p class='loading' style='display:none'>Loading...</p>");
  Demo.$subcontainer.append(Demo.$loading);
  google.load("visualization", "1", {packages:["corechart"]});
  $("#login-button").click(function () {
    Demo.secretSquirrel();
  });
  if (Demo.showBusinessId) {
    Demo.$loading.show();
    Demo.showBusinessInfo(Demo.showBusinessId);
  }
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
      Demo.showBusinessInfo($(this).attr("data-id"));

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

Demo.showBusinessInfo = function (id) {
  $.ajax({
        url: Demo.selectHref,
        type: "GET",
        data: {"id": id},
        success: function (response) {
          Demo.$loading.fadeOut(Demo.fadeTime);
          Demo.demoInfo(response);
        }
      });
      Demo.$loading.fadeIn(Demo.fadeTime);
      Demo.$formpanel.hide();
      Demo.$results.hide();
};
/*
 * We have data for a business. We have to show it now.
 */
Demo.demoInfo = function (response) {
  Demo.$results.html('');
  $("#business-report").tmpl([response[0].business]).appendTo(Demo.$results);
  $("#review-tmpl").tmpl(response).appendTo("table.business-reviews tbody:first");
  $("div.chart-tabs").tabs();
  $("h2.subtitle")
    .addClass("my-restaurant")
    .html("My Restaurant - Demo");
  Demo.$feedBody = $("#live-feed-body");
  Demo.$results.addClass("my-restaurant");
  Demo.$feedAggregate = $("#feed-aggregate");
  Demo.$results.fadeIn(Demo.fadeTime,
      function () {
        Demo.drawSentimentGraph(response);
        Demo.drawRatingPie(response);
      }
  );
  if (Demo.$results.height() > 450) {
    Demo.originalDocHeight = $ ("#doc2").height();
  }
  if (Modernizr.history) {
    history.pushState(null, null, "/demo/info/" + response[0].business.id + "/");
    $(window).bind("popstate", Demo.popstate);
  }
  Demo.liveFeed(response[0].business.id);
};

Demo.liveFeedWS = function (id) {
  var socket = new WebSocket(Demo.wsUrl + "?id=" + id);
  socket.onmessage = function (event) {
    console.log(event.data);
  }

};

Demo.liveFeed = function (id, lastTime) {
  if (typeof(lastTime) === "undefined") {
    lastTime = 0;
  }

  $.ajax({
        url: "/demo/poll/",
        data: {id: id,
               lastDate: lastTime,
               feedMaxSize: Demo.feedMaxSize},
        success: function (data) {
          return Demo.liveFeedCallback(data, id, lastTime);
        },
        error: function () {
          setTimeout(function() {Demo.liveFeed(id, lastTime)}, 5000);
        }
        });
};

Demo.liveFeedCallback = function (serverResponse, id, lastTime) {
  var currentRows = Demo.$feedBody.children();
  var totalLength = serverResponse.length + currentRows.length;
  var currentWait = 0;

  if (lastTime != 0) {
    currentWait = 2500;
  }

  if (serverResponse.length > 0) {
    lastTime = serverResponse[0].date;
  }

  for (var i = serverResponse.length - 1; i >= 0; i--) {
    var item = [serverResponse[i]];
    setTimeout(Demo.displayTwitterItem(serverResponse[i], i === 0, id, lastTime),
               currentWait);
    currentWait += 2500;
  }

  if (serverResponse.length === 0) {
    setTimeout(function () {Demo.liveFeed(id, lastTime);}, 1000);
  }
};

Demo.displayTwitterItem = function (item, isLast, businessId, lastTime) { return function () {
  item = [item];
  var itemId = item[0].id = Math.round(Math.random() * 10000);

  $("#feed-item-tmpl").tmpl(item).prependTo(Demo.$feedBody);

  if ($(".feed-item:visible").length >= Demo.feedMaxSize) {
    $(".feed-item:visible:last").fadeOut("slow");
  }

  var $this = $("#review-" + itemId).fadeIn("slow").find(".emote:first");
  var stats = Demo.$feedStats;
  if ($this.hasClass("happy")) {
    stats[0] ++;
  } else if ($this.hasClass("neutral")) {
    stats[1] ++;
  } else {
    stats[2] ++;
  }
  var total = stats[0] + stats[1] + stats[2];
  var percent = (stats[0] / (total));
  percent = Math.round(percent * 10000) / 100;
  Demo.$feedAggregate.text("" + total + " total: " + stats[2] + " bad, " + stats[1] + " neutral, " + stats[0] + " good: " + percent + "% good");
  if (isLast) {
    Demo.liveFeed(businessId, lastTime);
  }
}};

Demo.isSad = function (rating) {
  return rating < 0;
};

Demo.isNeutral = function (rating) {
  return rating == 0;
};

Demo.isHappy = function (rating) {
  return rating > 0;
};



Demo.drawSentimentGraph = function (results) {
  var data = new google.visualization.DataTable();
  data.addColumn("date", "Date");
  data.addColumn("number", "Sentiment");

  var dateData = {};
  var datesList = [];
  $.each(results, function (index, value) {
    if (value.date in dateData) {
      dateData[value.date][0] += value.sentiment;
      dateData[value.date][1] ++;
    } else {
      var dateParts = value.date.split("/");
      dateData[value.date] = [value.sentiment, 1];
      datesList.push([value.date, new Date(~~dateParts[2], (~~dateParts[0]) - 1, ~~dateParts[1])]);
    }
  });

  data.addRows(datesList.length);
  for (var i = datesList.length - 1, j = 0; i >= 0; i--, j++) {
    var dateInfo = datesList[i];
    var currentData = dateData[dateInfo[0]];
    var avgSentiment = currentData[0] / currentData[1];
    data.setValue(j, 0, dateInfo[1]);
    data.setValue(j, 1, Demo.formatSentiment(avgSentiment));
  }
  var chart = new google.visualization.LineChart($("#tab-sentiment")[0]);
  chart.draw(data, {width: 450, height: 220,
        legend: "none", vAxis: {
          title: "Review Sentiment",
          titleTextStyle: {
            color: '#332',
            fontName: "Georgia",
            textSize: '16px'
          }
        },
        chartArea: { width: 360 }});
};



Demo.drawRatingPie = function (results) {
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Rating');
  data.addColumn("number", "Review Popularity");

  var histogram = {};
  var histogramKeys = [];
  $.each(results, function (index, value) {
    if (typeof(value.rating) === "undefined") {
      return;
    }
    if (histogram[value.rating] === undefined) {
      histogramKeys.push(value.rating);
      histogram[value.rating] = 1;
    } else {
      histogram[value.rating] ++;
    }
  });
  var histogramKeysLength = histogramKeys.length;
  histogramKeys.sort();
  data.addRows(histogramKeysLength);
  for (var i = 0; i < histogramKeysLength; i++) {
    var rating = histogramKeys[i];
    data.setValue(i, 0, "" + rating + " star" + (rating == 1 ? "" : "s"));
    data.setValue(i, 1, histogram[rating]);
  }
  var chart = new google.visualization.PieChart($("#tab-ratings")[0]);
  chart.draw(data, {width: 450, height: 220, title: "Rating Breakdown", is3D: true, chartArea: {
        top: 8, left: 45, height: 220, width: 800
      }});
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

  if (Demo.originalDocHeight) {
    $("#doc2").height(Demo.originalDocHeight);
  }
};

Demo.formatSentiment = function (sentiment) {
  return Math.round(sentiment * 100) / 100;
}



$(function () {
  Demo.initialize();
});