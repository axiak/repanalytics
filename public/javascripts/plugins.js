window.log = function(){
  log.history = log.history || [];
  log.history.push(arguments);
  arguments.callee = arguments.callee.caller;  
  if(this.console) console.log( Array.prototype.slice.call(arguments) );
};
(function(b){function c(){}for(var d="assert,count,debug,dir,dirxml,error,exception,group,groupCollapsed,groupEnd,info,log,markTimeline,profile,profileEnd,time,timeEnd,trace,warn".split(","),a;a=d.pop();)b[a]=b[a]||c})(window.console=window.console||{});

(function ($) {
  $.prettyDate = function () {
    var now = new Date();
    var hours = now.getHours(), minutes = now.getMinutes();
    hours = (hours < 10) ? "0" + hours : hours;
    minutes = (minutes < 10) ? "0" + "" + minutes : minutes;
    return "" + (now.getMonth() + 1) + "/" + now.getDate() + "/" + now.getFullYear() + " " + hours + ":" + minutes;
  }

})(jQuery);