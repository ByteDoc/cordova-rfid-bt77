package org.apache.cordova.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sevenid.mobile.reader.api.operationresult.InventoryResult;
import com.sevenid.mobile.reader.api.operationresult.OperationStatus;
import com.sevenid.mobile.reader.api.operationresult.ReadResult;
import com.sevenid.mobile.reader.api.operationresult.WriteResult;
import com.sevenid.mobile.reader.api.parameters.InventoryParameters;
import com.sevenid.mobile.reader.api.parameters.ReadParameters;
import com.sevenid.mobile.reader.api.parameters.TagMemoryBank;
import com.sevenid.mobile.reader.api.parameters.WriteParameters;
import com.sevenid.mobile.reader.bt77.RfidReader;

/**
* This class echoes a string called from JavaScript.
*/
public class Echo extends CordovaPlugin {

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		/**
		 * testeingabe has to match with
		 * cordova.exec(successCallback, errorCallback, "EinEcho", "testeingabe", [name]);
		 * from the *.js-file
		 */
		if (action.equals("testeingabe")) {
			String message = args.getString(0);
			// Start Testing:
			/*int i = 1, j = 2;
			if(i != j){
				throw new ArithmeticException("this doesn't work"); 
			}
			*/// End Testing
			this.echo(message, callbackContext, args);
			return true;
		}
		return false;
	}

	private void echo(String message, CallbackContext callbackContext, JSONArray args) {
		if (message != null && message.length() > 0) {
//			callbackContext.success(message);
			callbackContext.success(args);
		} else {
			callbackContext.error("Expected one non-empty string argument.");
		}
	}
	
}
