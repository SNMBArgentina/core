/*
 * Performs the queries to the server providing a common error management
 * and including some parameters that should always be there, like 'lang'
 */
define([ 'jquery', 'message-bus' ], function($, bus) {
	bus.listen('ajax', function(event, ajaxParams) {
		ajaxParams.error = function(jqXHR) {
			var message = ajaxParams.errorMsg + '. ';
			try {
				var messageObject = $.parseJSON(jqXHR.responseText);
				if (messageObject.hasOwnProperty('message')) {
					message += messageObject.message + '.';
				} else {
					message += 'Unrecognized error from server.';
				}
			} catch (e) {
				// Answer may not be json. Ignore and keep message error from
				// parameters
			}
			bus.send('error', message);
		};

		var xhr = $.ajax(ajaxParams);

		if (ajaxParams.hasOwnProperty('controlCallBack')) {
			ajaxParams.controlCallBack(xhr);
		}
	});
});
