/*global cordova, module*/

module.exports = {
    echo: function (name, successCallback, errorCallback) {
        /**
         * EinEcho has to match with <feature name="EinEcho"> from the file plugin.xml
         *
         * testeingabe has to match with action.equals("testeingabe") from the *.java-file
         *
         * name are the arguments (JSONArray) that will be sent to the *.java-file
         */
        cordova.exec(successCallback, errorCallback, "EinEcho", "testeingabe", [name]);
    }
};
