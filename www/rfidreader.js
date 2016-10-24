/*global cordova, module*/

module.exports = {
	echo: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "echo", [args]);
	},
	startRfidListener: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "start", [args]);
	},
	endRfidListener: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "stop", [args]);
	},
	scanInventory: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "scan", [args]);
	},
	readTag: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "read", [args]);
	},
	writeTag: function (args, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "write", [args]);
	}
};
