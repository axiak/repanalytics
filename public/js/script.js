$(function () {
    $("button, input:submit").button();
    $("nav a").click(function () {
        return ($(this).hasClass("enabled"));
    });

    var $crunchtimeLink = $("#crunchtime-link");
    window.log($crunchtimeLink.position().top);
    $crunchtimeLink.css({top: $crunchtimeLink.position().top - 1});
});

















