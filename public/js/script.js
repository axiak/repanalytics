$(function () {
    $("button, input:submit").button();
    $("nav a").click(function () {
        return ($(this).hasClass("enabled"));
    });

    var $crunchtimeLink = $("#crunchtime-link");
    $crunchtimeLink.css({top: $crunchtimeLink.position().top - 1});

    $("#business-info").validate();


    var $demoName = $("#demo-name:first");
    if ($demoName.length) {
        (function () {
            var cache = {}, lastXhr;
            $demoName.autocomplete({
                minLength: 2,
                delay: 100,
                autoFocus: true,
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
});

















