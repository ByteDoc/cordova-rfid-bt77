/*global cordova, module*/

module.exports = {
    echo2: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Echo", "echo", [name]);
    }
};
