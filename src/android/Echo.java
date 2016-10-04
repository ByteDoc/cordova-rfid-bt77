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


//Testing:
import com.sevenid.mobile.reader.api.Epc;

/**
* This class echoes a string called from JavaScript.
*/
public class Echo extends CordovaPlugin {
	RfidReader reader;
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
		}else if (action.equals("start")){
			this.startRFIDReader();
			callbackContext.success("boolean isBusyStart = reader.isBusy(): "+reader.isBusy()+"_-_and boolean isOpenStart = reader.isOpen(): "+reader.isOpen());
		}else if (action.equals("scan")){
			InventoryParameters p = new InventoryParameters();
            InventoryResult r = reader.getInventory(p);
			//args = (JSONArray[])r[0];
			JSONArray test = new JSONArray(Arrays.asList(r));
			args = test;
			if(args != null && args.length() > 0){
				callbackContext.success(args);
			} else {
			callbackContext.error("Scan couldn't be initialized.");
			}
            //OperationStatus s = r.getOperationStatus();
		}else if (action.equals("read")){
			//blabla
		}else if (action.equals("write")){
			// blabla
		}else if (action.equals("end")){
			reader.close();
			callbackContext.success("boolean isOpenEnd = reader.isOpen(): "+reader.isOpen()+"_-_and boolean isBusyEnd = reader.isBusy(): "+reader.isBusy());
			System.out.println("boolean isOpenEnd = reader.isOpen(): "+reader.isOpen());
			System.out.println("boolean isBusyEnd = reader.isBusy(): "+reader.isBusy());
		}else{
			return false;
		}
		reader.close();
		return true;
	}

	private void echo(String message, CallbackContext callbackContext, JSONArray args) {
		if (message != null && message.length() > 0) {
//			callbackContext.success(message);
			callbackContext.success(args);
		} else {
			callbackContext.error("Expected one non-empty string argument.");
		}
	}
	
	private void startRFIDReader(){
		this.reader = new RfidReader();
		this.reader.open();
		System.out.println("boolean isBusyStart = reader.isBusy(): "+this.reader.isBusy());
		System.out.println("boolean isOpenStart = reader.isOpen(): "+this.reader.isOpen());
	}
	
}
