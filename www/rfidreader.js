/*global cordova, module*/

myPlugin =  {
	retryCount: 0,
	cycleCount: 0,
	rwSuccessCallback: function(message) {
		myPlugin.successCallback(message);
		myPlugin.retryCount = 0;
	},
	rwErrorCallback: function(message) {
		if (myPlugin.retryCount < myPlugin.retryMax) {
			myPlugin.retryCount++;
			// nochmal ausführen
			cordova.exec(myPlugin.rwSuccessCallback, myPlugin.rwErrorCallback, "RfidReader", "readTag", myPlugin.argsArray);
		} else {
			myPlugin.errorCallback(message);
			myPlugin.retryCount = 0;
		}
	},
	inventorySuccessCallback: function(message) {
		myPlugin.argsArray = message;
		console.log("InventoryScan-maxCycles: "+myPlugin.cycleMax);
		if (myPlugin.cycleCount < myPlugin.cycleMax) {
			console.log("InventoryScan-currentCycle: "+myPlugin.cycleCount);
			myPlugin.cycleCount++;
			// nochmal ausführen
			cordova.exec(myPlugin.inventorySuccessCallback, myPlugin.inventoryErrorCallback, "RfidReader", "scanInventory", myPlugin.argsArray);
		} else if (myPlugin.cycleCount == myPlugin.cycleMax) {
			myPlugin.inventoryAddResults(myPlugin.argsArray);
		}
	},
	inventoryErrorCallback: function(message) {
		myPlugin.errorCallback(message);
		myPlugin.cycleCount = 0;
	},
	inventoryAddResults: function(message) {
		//myPlugin.successCallback(JSON.stringify(message));
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
			if(myPlugin.bWriteTag === true){
				module.exports.writeTag({
					retries: myPlugin.retryMax,
					epc: maxSeenCountEpc
				}, myPlugin.successCallback, myPlugin.errorCallback);
			}else{
				module.exports.readTag({
					retries: myPlugin.retryMax,
					epc: maxSeenCountEpc
				}, myPlugin.successCallback, myPlugin.errorCallback);
			}
		}else{
			myPlugin.inventoryErrorCallback("No results found.");
		}
		
		// shutdown process
		myPlugin.cycleCount = 0;
		module.exports.endRfidListener(null, myPlugin.successCallback, myPlugin.errorCallback);
	},
	getArgsArray: function(args) {
		// args auf erlaubten typ/inhalt prüfen
		// nur ein Object erlaubt, kein Array! 
		if (typeof(args) != "object" || args == null || Array.isArray(args)) args = {};
		return [args];	// Array erstellen
	},
	setPresets: function(args, successCallback, errorCallback) {
		var argsArray = myPlugin.getArgsArray(args);
		var argsObject = argsArray[0];
		console.log("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: "+argsObject);
		
		if(argsObject.retries){
			myPlugin.retryMax = Math.max(1,parseInt(argsObject.retries));
		}else{
			// Default value
			myPlugin.retryMax = 40;
		}
		myPlugin.argsArray = argsArray;
		myPlugin.successCallback = successCallback;
		myPlugin.errorCallback = errorCallback;
	}
};

module.exports = {
	startRfidListener: function (args, successCallback, errorCallback) {
		var argsArray = myPlugin.getArgsArray(args);
		cordova.exec(successCallback, errorCallback, "RfidReader", "startRfidListener", argsArray);
	},
	endRfidListener: function (args, successCallback, errorCallback) {
		var argsArray = myPlugin.getArgsArray(args);
		cordova.exec(successCallback, errorCallback, "RfidReader", "endRfidListener", argsArray);
	},
	scanInventory: function (args, successCallback, errorCallback, bWriteTag) {
		if(bWriteTag !== true){
			bWriteTag = false;
		}
		
		var argsArray = myPlugin.getArgsArray(args);
		var argsObject = argsArray[0];
		
		if(argsObject.cycles){
			myPlugin.cycleMax = Math.max(1,parseInt(argsObject.cycles));
		}else{
			myPlugin.cycleMax = 35;
		}
		if(argsObject.retries){
			myPlugin.retryMax = Math.max(1,parseInt(argsObject.retries));
		}else{
			// Default value
			myPlugin.retryMax = 40;
		}
		if(argsObject.writeTag){
			myPlugin.bWriteTag = (argsObject.writeTag === "true");
		}else{
			myPlugin.bWriteTag = false;
		}
		myPlugin.argsArray = argsArray;
		myPlugin.successCallback = successCallback;
		myPlugin.errorCallback = errorCallback;
		cordova.exec(myPlugin.inventorySuccessCallback, myPlugin.inventoryErrorCallback, "RfidReader", "scanInventory", argsArray);
	},
	readTag: function (args, successCallback, errorCallback) {
		myPlugin.setPresets(args, successCallback, errorCallback);
		//cordova.exec(successCallback, errorCallback, "RfidReader", "readTag", args);
		cordova.exec(myPlugin.rwSuccessCallback, myPlugin.rwErrorCallback, "RfidReader", "readTag", argsArray);
	},
	writeTag: function (args, successCallback, errorCallback) {
		myPlugin.setPresets(args, successCallback, errorCallback);
		//cordova.exec(successCallback, errorCallback, "RfidReader", "writeTag", argsArray);
		cordova.exec(myPlugin.rwSuccessCallback, myPlugin.rwErrorCallback, "RfidReader", "writeTag", argsArray);
	}
};
