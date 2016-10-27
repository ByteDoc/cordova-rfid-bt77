/*global cordova, module*/

module.exports = {
	echo: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "echo", args);
	},
	startRfidListener: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "startRfidListener", args);
	},
	endRfidListener: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "endRfidListener", args);
	},
	scanInventory: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "scanInventory", args);
	},
	readTag: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "readTag", args);
	},
	writeTag: function (args, successCallback, errorCallback) {
		if (!Array.isArray(args)) args = [args];
		cordova.exec(successCallback, errorCallback, "RfidReader", "writeTag", args);
	}
};
