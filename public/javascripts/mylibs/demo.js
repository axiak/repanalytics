var Demo = Demo || {};

Demo.$loading = null;
Demo.$subcontainer = null;
Demo.$formpanel = null;
Demo.$formerrors = null;
Demo.fadeTime = 250;

Demo.initialize = function () {
  Demo.$formerrors = $("#business-info-errors");
  Demo.$subcontainer = $("#subcontainer");
  Demo.$formpanel = $("#demo-form-panel");
  Demo.$loading = $("<p class='loading' style='display:none'>Loading...</p>");
};

Demo.runDemo = function (data) {
  $.ajax({
        url: "/demo/search/",
        type: "GET",
        data: data,
        success: function (response) {
          Demo.$loading.fadeOut(Demo.fadeTime);
          if (response._1 && response._1 === "notfound") {
            return Demo.notfound(data);
          } else if (response._1 && response._1 === "disambiguate") {
            return Demo.disambiguate(data, response);
          } else {
            return Demo.demoInfo(data, response);
          }
        }
      });
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
  console.log(response._2);
};

/*
 * We have data for a business. We have to show it now.
 */
Demo.demoInfo = function (inputData, response) {

};

/*
 * Given a business object, return an img jquery object of
 * the map.
 */
Demo.mapImg = function (business, height, width) {
  if (typeof(width) === "undefined") {
    width = 128;
  }
  if (typeof(height) === "undefined") {
    height = 128;
  }
  var $img = $("<img width='" + width + "' height='" + height + "' border=0 class='business-map' />");
  $img.attr({"src": "http://maps.google.com/maps/api/staticmap?center=" + business.latitude + "," + business.longitude +
                    "&zoom=14&size=" + width + "x" + height + "&maptype=roadmap&markers=color:blue%7Clabel:" + encodeURIComponent(business.name) +
                    "%7C" + business.latitude + "," + business.longitude + "&sensor=false"});
  return $img;
};

$(function () {
  Demo.initialize();
});