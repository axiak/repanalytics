/*
 * jQuery placeholder Plugin
 * version: 0.1
 * @requires: jQuery v1.4.1 or later, Modernizr, and jquery livequery plugin
 *
 * http://docs.jquery.com/Plugins/livequery
 *
 * To use: When you write your input tags, include a title and placeholder attribute as
 * well as a placeholder class. Also include styling for the hasPlaceholder class.
 *
 * E.g.:
 * CSS:
 * .hasPlaceholder { color: #ddd; font-style: italic; }
 *
 * <input type="text" class="placeholder" title="Username" placeholder="Username" name="username">
 */
$.fn.isInputEmpty = function () {
  console.log($.trim($(this).val()));
  return $.trim($(this).val()) === "";
};


(function ($) {

  if (!Modernizr.input.placeholder) {
    $.fn.isInputEmpty = function () {
      var trimmedValue = $.trim($(this).val());
      return (trimmedValue === "" || trimmedValue === $.trim(this.title));
    };
    $(function() {

      var blurInputs = function (original_field, replacement_field, is_password) {
        var thisp = original_field[0];

        var trimmedTitle = $.trim(this.title), trimmedValue = $.trim(this.value);
        if (trimmedTitle !== '' && (trimmedValue === '' || trimmedValue == trimmedTitle)) {
          if (is_password) {
            replacement_field.show();
            original_field.hide();
          } else {
            this.value = this.title;
            $(this).addClass('hasPlaceholder');
          }
        }

      };

      var focusInputs = function (original_field, replacement_field, is_password) {
        if (is_password) {
          replacement_field.hide();
          original_field.show().focus();
        }
        else if ($.trim(this.title) !== '' && $.trim(this.value) === $.trim(this.title)) {
          this.value = '';
          $(this).removeClass('hasPlaceholder');
        }
      };

      $('input.placeholder, textarea.placeholder').livequery(function () {
        var thisp = this;
        var original_field = $(this);
        var replacement_field;
        var is_password = true;

        if (original_field.hasClass("placeholderDummy")) {
          return false;
        }

        original_field.attr("placeholder", '');

        if (original_field.attr("type") === "password") {
          replacement_field = $("<input type='text' class='placeholderDummy' />")
            .attr("class", original_field.attr("class"))
            .addClass("hasPlaceholder")
            .val(original_field.attr("title"))
            .insertAfter(original_field);

          if (original_field.is(":visible")) {
            replacement_field
              .width(original_field.width())
              .height(original_field.height());
          }

        } else {
          replacement_field = original_field;
          is_password = false;
        }

        original_field.blur(function () {
          blurInputs.call(thisp, original_field, replacement_field, is_password);
        });

        replacement_field.focus(function () {
          focusInputs.call(thisp, original_field, replacement_field, is_password);
        });

        blurInputs.call(thisp, original_field, replacement_field, is_password);
      });

    });
  }
})(jQuery);