$(function () {
  $("button, input:submit").button();
  $("nav a.disabled").click(function (e) {
    e.preventDefault();
  });



  $("#business-info")
      .submit(function (e) {
        e.preventDefault();
        var data = {};
        $.each($(this).serializeArray(), function (key, value) {
          data[value.name] = value.value;
        });
        Demo.runDemo(data);
        e.preventDefault();
      })
      .validate({
        ignoreTitle: true,
        rules: {
          zip: {
            required: true,
            digits: true,
            minlength: 5,
            maxlength: 5
          }
        }
      });


  var $demoName = $("#demo-name:first");
  if ($demoName.length) {
    (function () {
      var cache = {}, lastXhr;
      $demoName.autocomplete({
            minLength: 2,
            delay: 100,
            autoFocus: true,
            change: function (event, ui) {
              if (ui.item) {
                $("#chain").attr({checked: "checked"});
                $("#existingChain").val("true");
              }
            },
            source: function (request, response) {
              var term = request.term;
              if (term in cache) {
                response(cache[term]);
                return;
              }

              lastXhr = $.getJSON("/demo/name/", request, function (data, status, xhr) {
                cache[term] = data;
                if (xhr == lastXhr) {
                  response(data);
                }
              });
            }
          })
    })();
  }

  $("#email-preview").submit(function (e) {
    var self = this;
    e.preventDefault();
    $.ajax({
          url: "/notification/email/",
          data: {"email": $("#get-notified").val(), "locations": $("#num-locations").val()},
          type: "POST",
          success: function (data) {
            self.disabled = "";
            $(self).removeClass("disabled");
            $("#notify-form").fadeOut();
            $("#notify-thanks").fadeIn();
          }
        });

    this.disabled = "disabled";
    $(this).addClass("disabled");
  });

  $("#slideshow").cycle({
        pause: 0,
        speed: 2500
      });
});

