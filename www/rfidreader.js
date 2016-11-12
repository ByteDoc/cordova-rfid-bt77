/*global cordova, module*/

var RfidReaderPlugin = (function () {
    "use strict";
    
    var CORDOVA_PLUGIN_NAME = "RfidReader";
    var CORDOVA_ACTION_SCAN_INVENTORY = "SCAN_INVENTORY";
    var CORDOVA_ACTION_READ_TAG = "READ_TAG";
    var CORDOVA_ACTION_WRITE_TAG = "WRITE_TAG";
    var CORDOVA_ACTION_START_RFID_LISTENER = "START_RFID_LISTENER";
    var CORDOVA_ACTION_STOP_RFID_LISTENER = "STOP_RFID_LISTENER";

    var defaultValues = {
        inventoryCycles: 30,
        seenCountForFind: 5,
        seenCountAdvantageForFind: 5,
        retriesReadWrite: 20
    };
    // further attributes for args object
    //   epcToRead: epc
    //   epcToWrite: epc
    //   dataToWrite: epc
    //   dataFromReadResult: epc
    var valueLimits = {
        maxInventoryCycles: 50,
        maxSeenCountForFind: 5,
        maxSeenCountAdvantageForFind: 5,
        maxRetriesReadWrite: 35
    };
    var argsObject = {};
    var argsArray = [];
    var retryCount = 0;
    var cycleCount = 0;
    var seenCountForFind = 0;
    var seenCountAdvantageForFind = 0;

    //var successCallback, errorCallback, inventoryProcessCallback;
    
    /**
     * ensure that needed values are set in the argsObject
     * and set default values if initial or bad value ...
     */
    function checkArgsObject() {
        argsObject = argsArray[0];
        argsObject.inventoryCycles = Math.min(
            Math.max(1,parseInt(argsObject.inventoryCycles)),
            valueLimits.maxInventoryCycles);
        if (isNaN(argsObject.inventoryCycles)){
            argsObject.inventoryCycles = defaultValues.inventoryCycles;
        }
        argsObject.retriesReadWrite = Math.min(
            Math.max(1,parseInt(argsObject.retriesReadWrite)),
            valueLimits.maxRetriesReadWrite);
        if (isNaN(argsObject.retriesReadWrite)){
            argsObject.retriesReadWrite = defaultValues.retriesReadWrite;
        }
        seenCountForFind = Math.min(
            Math.max(1,parseInt(argsObject.seenCountForFind)),
            valueLimits.maxSeenCountForFind);
        if (isNaN(seenCountForFind)){
            seenCountForFind = defaultValues.seenCountForFind;
        }
        seenCountAdvantageForFind = Math.min(
            Math.max(1,parseInt(argsObject.seenCountAdvantageForFind)),
            valueLimits.maxSeenCountAdvantageForFind);
        if (isNaN(seenCountAdvantageForFind)){
            seenCountAdvantageForFind = defaultValues.seenCountAdvantageForFind;
        }
    }
    function getArgsArray(args) {
        // args auf erlaubten typ/inhalt prüfen
        // nur ein Object erlaubt, kein Array!
        if (typeof(args) != "object" || args === null || Array.isArray(args)) {
            args = {};
        }
        return [args];  // Array erstellen
    }
    function init(args, successCallback, errorCallback) {
        argsArray = getArgsArray(args);
        checkArgsObject();
        successCallback = successCallback;
        errorCallback = errorCallback;
        cycleCount = 0;
        retryCount = 0;
    }
    function shutdown(argsArray, errorCallback) {
        if (typeof(errorCallback) != "function") {
            errorCallback = errorCallback;
        }
        cordova.exec(
            emptyCallback,
            errorCallback,
            CORDOVA_PLUGIN_NAME,
            CORDOVA_ACTION_END_RFID_LISTENER,
            argsArray);
    }
    function emptyCallback() {

    }


    /**
     * === scanInventory ===
     *   - callback: RfidReaderPlugin.inventoryProcessCallback
     *   - read details for a given Epc
     */
    function cordovaExecScanInventory() {
        cordova.exec(
            inventoryCycleSuccessCallback,
            inventoryCycleErrorCallback,
            CORDOVA_PLUGIN_NAME,
            CORDOVA_ACTION_SCAN_INVENTORY,
            argsArray
        );
    }
    function inventoryCycleSuccessCallback(argsArray) {
        argsArray = argsArray;
        argsObject = argsArray[0];


        // CHECK FOR BEST FIND  ... #TODOX

        if (cycleCount < argsObject.inventoryCycles) {
            cycleCount = cycleCount + 1;
            console.log("scanInventory ... starting another cycle: " + cycleCount +
                " (max: " + argsObject.inventoryCycles + ")");
            cordovaExecScanInventory();
        } else {
            inventoryProcessCallback();
        }
    }
    function inventoryCycleErrorCallback(message) {
        errorCallback(message);
        //shutdown(message, emptyCallback);
        shutdown(message);
    }
    function readBestTagFromInventory() {
        var epc = getBestEpcFromInventory(argsArray);

        // set EPC into argsArray
        argsObject.epcToRead = epc;
        argsArray[0] = argsObject;

        cordovaExecReadTag();
    }
    function getBestEpcFromInventory() {
        console.log("getBestEpcFromInventory ... processing results ...");
        var maxSeenCountEpc = null;
        var maxSeenCountValue = -1;
        Object.keys(argsObject.inventory).forEach(function(epc) {
            var seenCount = argsObject.inventory[epc];
            console.log("Inventory-Entry: epc("+epc+"), seenCount("+seenCount+")");
            if (seenCount > maxSeenCountValue) {
                maxSeenCountEpc = epc;
                maxSeenCountValue = seenCount;
            }
        });
        /* WOLFGANG TODO:
             - check OneNote for "Notepad++", install JSLintNpp
             - check https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
             - check http://www.jslint.com/help.html#forin
             - remove this code afterwards
        for (var epc in argsObject.inventory) {
            var seenCount = argsObject.inventory[epc];
            console.log("Inventory-Entry: epc("+epc+"), seenCount("+seenCount+")");
            if (seenCount > maxSeenCountValue) {
                maxSeenCountEpc = epc;
                maxSeenCountValue = seenCount;
            }
        } */
        return maxSeenCountEpc;
    }




    /**
     * === readTag ===
     *   - callback: successCallback/errorCallback
     *     (application submitted callback function)
     *   - read details for a given Epc
     */
    function cordovaExecReadTag() {
        cordova.exec(
            readRetrySuccessCallback,
            readRetryErrorCallback,
            CORDOVA_PLUGIN_NAME,
            CORDOVA_ACTION_READ_TAG,
            argsArray);
    }
    function readRetrySuccessCallback(message) {
        successCallback(message);
        shutdown(message);
    }
    function readRetryErrorCallback(message) {
        if (retryCount < argsObject.retriesReadWrite) {
            retryCount++;
            console.log("readTag ... starting another cycle: " + retryCount +
                " (max: " + argsObject.retriesReadWrite + ")");
            cordovaExecReadTag();

        } else {
            errorCallback(message);
            shutdown(message);
        }
    }


    /**
     * === writeTag ===
     *   - callback: successCallback/errorCallback
     *     (application submitted callback function)
     *   - write new Epc for a given Epc
     */
    function cordovaExecWriteTag() {
        cordova.exec(
            writeRetrySuccessCallback,
            writeRetryErrorCallback,
            CORDOVA_PLUGIN_NAME,
            CORDOVA_ACTION_WRITE_TAG,
            argsArray);
    }
    function writeRetrySuccessCallback(message) {
        successCallback(message);
        shutdown(message);
    }
    function writeRetryErrorCallback(message) {
        if (retryCount < argsObject.retriesReadWrite) {
            retryCount++;
            console.log("writeTag ... starting another cycle: " + retryCount +
                " (max: " + argsObject.retriesReadWrite + ")");
            cordovaExecWriteTag();

        } else {
            errorCallback(message);
            shutdown(message);
        }
    }

    /**
     * NOT NEEDED ANYMORE?!?
    inventoryAddResults: function(message) {
        //successCallback(JSON.stringify(message));
        // "message" must be an existing object
        var argsObject = message[0];
        if (argsObject === null || typeof argsObject !== "object" || Array.isArray(argsObject)){
            argsObject = {};
            console.error("Java-Callback mit ungültigem ARGS/MESSAGE-Inhalt!");
        }

        // get EPC from the most tagged RFID-tag
        var maxSeenCountEpc = null;
        var maxSeenCountValue = -1;
        for (var epc in argsObject.inventory) {
            var seenCount = argsObject.inventory[epc];
            console.log("Inventory-Entry: epc("+epc+"), seenCount("+seenCount+")");
            if (seenCount > maxSeenCountValue) {
                maxSeenCountEpc = epc;
                maxSeenCountValue = seenCount;
            }
        }

        // start reading the RFID-tag by using EPC from the most tagged RFID-tag
        if (maxSeenCountEpc !== null){
            if(data != ""){
                module.exports.writeTag({
                    retries: retryMax,
                    epc: maxSeenCountEpc,
                    data: data
                }, successCallback, errorCallback);
            }else{
                module.exports.readTag({
                    retries: retryMax,
                    epc: maxSeenCountEpc
                }, successCallback, errorCallback);
            }
        }else{
            inventoryErrorCallback("No results found.");
        }

        // shutdown process
        cycleCount = 0;
        module.exports.endRfidListener(null, successCallback, errorCallback);
    },
    */

    
    /**
     *  PUBLIC FUNCTIONS for the plugin
     */
    function scanAndReadBestTag (args, successCallback, errorCallback) {
        // init the plugin class
        init(args, successCallback, errorCallback);
        // set the necessary follow-up action ... (because scan and read are separate API calls)
        inventoryProcessCallback = readBestTagFromInventory;
        // ... before initiating the scan
        cordovaExecScanInventory();
    }
    function readTag (args, successCallback, errorCallback) {
        // init the plugin class
        init(args, successCallback, errorCallback);
        // call the readTag API
        cordovaExecReadTag();
    }
    function writeTag (args, successCallback, errorCallback) {
        // init the plugin class
        init(args, successCallback, errorCallback);
        // call the writeTag API
        cordovaExecWriteTag();
    }
    // calls only for test purposes, should not be necessary to be called by applications
    function startRfidListener (args, successCallback, errorCallback) {
        var argsArray = getArgsArray(args);
        cordova.exec(successCallback, errorCallback, CORDOVA_PLUGIN_NAME, CORDOVA_ACTION_START_RFID_LISTENER, argsArray);
    }
    function endRfidListener (args, successCallback, errorCallback) {
        var argsArray = getArgsArray(args);
        cordova.exec(successCallback, errorCallback, CORDOVA_PLUGIN_NAME, CORDOVA_ACTION_END_RFID_LISTENER, argsArray);
    }
    
    return {
        scanAndReadBestTag: scanAndReadBestTag,
        readTag: readTag,
        writeTag: writeTag,
        startRfidListener: startRfidListener,
        endRfidListener: endRfidListener
    };


}());

module.exports = {
    scanAndReadBestTag: RfidReaderPlugin.scanAndReadBestTag,
    readTag: RfidReaderPlugin.readTag,
    writeTag: RfidReaderPlugin.writeTag,
    // calls only for test purposes, should not be necessary to be called by applications
    startRfidListener: RfidReaderPlugin.startRfidListener,
    endRfidListener: RfidReaderPlugin.endRfidListener
    
    /**
    scanAndReadBestTag: function(args, successCallback, errorCallback) {
        // init the plugin class
        RfidReaderPlugin.init(args, successCallback, errorCallback);
        // set the necessary follow-up action ... (because scan and read are separate API calls)
        RfidReaderPlugin.inventoryProcessCallback = RfidReaderPlugin.readBestTagFromInventory;
        // ... before initiating the scan
        RfidReaderPlugin.cordovaExecScanInventory();
    },
    readTag: function (args, successCallback, errorCallback) {
        // init the plugin class
        RfidReaderPlugin.init(args, successCallback, errorCallback);
        // call the readTag API
        RfidReaderPlugin.cordovaExecReadTag();
    },
    writeTag: function (args, successCallback, errorCallback) {
        // init the plugin class
        RfidReaderPlugin.init(args, successCallback, errorCallback);
        // call the writeTag API
        RfidReaderPlugin.cordovaExecWriteTag();
    },

    // calls only for test purposes, should not be necessary to be called by applications
    startRfidListener: function (args, successCallback, errorCallback) {
        var argsArray = RfidReaderPlugin.getArgsArray(args);
        cordova.exec(successCallback, errorCallback, CORDOVA_PLUGIN_NAME, "startRfidListener", argsArray);
    },
    endRfidListener: function (args, successCallback, errorCallback) {
        var argsArray = RfidReaderPlugin.getArgsArray(args);
        cordova.exec(successCallback, errorCallback, CORDOVA_PLUGIN_NAME, "endRfidListener", argsArray);
    }
    */


    /**
     * old method, remove when NEW scanInventory is implemented
    scanInventory: function (args, successCallback, errorCallback) {
        // deprecated, only TEST method
        var argsArray = RfidReaderPlugin.getArgsArray(args);
        var argsObject = argsArray[0];

        if(argsObject.cycles){
            RfidReaderPlugin.cycleMax = Math.max(1,parseInt(argsObject.cycles));
        }else{
            RfidReaderPlugin.cycleMax = 35;
        }
        if(argsObject.retries){
            RfidReaderPlugin.retryMax = Math.max(1,parseInt(argsObject.retries));
        }else{
            // Default value
            RfidReaderPlugin.retryMax = 40;
        }
        if(argsObject.data){
            RfidReaderPlugin.data = argsObject.data;
        }else{
            RfidReaderPlugin.data = "";
        }
        RfidReaderPlugin.argsArray = argsArray;
        RfidReaderPlugin.successCallback = successCallback;
        RfidReaderPlugin.errorCallback = errorCallback;
        cordova.exec(RfidReaderPlugin.inventoryCycleSuccessCallback, RfidReaderPlugin.inventoryCycleErrorCallback, "RfidReader", "scanInventory", argsArray);
    }
    */
};


