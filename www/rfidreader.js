/*global cordova, module*/

module.exports = {
	echo: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "echo", [name]);
	},
	startRfidListener: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "start", [name]);
	},
	endRfidListener: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "stop", [name]);
	},
	scanInventory: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "scan", [name]);
	},
	readTag: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "read", [name]);
	},
	writeTag: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "write", [name]);
	}
};
