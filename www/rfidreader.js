/*global cordova, module*/

module.exports = {
	startRfidListener: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "start", [name]);
	},
	endRfidListener: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "stop", [name]);
	},
	scanInventory: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "RfidReader", "scan", [name]);
	}
};
