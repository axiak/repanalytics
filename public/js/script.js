$(function () {
    $("button, input:submit").button();
    $("nav a.disabled").click(function (e) {
        e.preventDefault();
    });

    $("#business-info").validate();


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

    $("#notified").click(function (e) {
        var self = this;
        e.preventDefault();
        $.ajax({
                    url: "/notification/email/",
                    data: {"email": $("#get-notified").val()},
                    type: "POST",
                    success: function (data) {
                        self.disabled = "";
                        $(self).removeClass("disabled");
                        $("#email-preview").replaceWith("<h4>Thank you!</h4>");
                    }
                });

        this.disabled = "disabled";
        $(this).addClass("disabled");
    });
});

















