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
public class RfidReader extends CordovaPlugin {
	RfidReader reader;
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("start")){
			this.startRFIDReader();
		}else if (action.equals("scan")){
			
			
			System.out.println("Test3: Start Test3");
			InventoryParameters p = new InventoryParameters();
			System.out.println("Test3: InventoryParameters: "+p);
            InventoryResult r = this.reader.getInventory(p);
			System.out.println("Test3: InventoryResult: "+r);
			//args = (JSONArray[])r[0];
			args = new JSONArray(Arrays.asList(r));
			System.out.println("Test3: args = "+args);
			if(args != null && args.length() > 0){
				callbackContext.success(args);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
            OperationStatus s = r.getOperationStatus();
			System.out.println("Test3: OperationStatus: "+s);
			
			
		}else if (action.equals("read")){
			//blabla
		}else if (action.equals("write")){
			// blabla
		}else if (action.equals("stop")){
			this.stopRFIDReader();
		}else{
			return false;
		}
		return true;
	}
	
	private void startRFIDReader(){
		System.out.println("Test1: Start RFIDReader:");
		this.reader = new RfidReader();
		System.out.println("Test1: RFIDReader created");
		System.out.println("Test1: this.reader.open(): " + this.reader.open());
		System.out.println("Test1: this.reader.isBusy(): "+this.reader.isBusy()+"_-_and this.reader.isOpen(): "+this.reader.isOpen())
	}
	
	private void stopRFIDReader(){
		System.out.println("Test2: Stop RFIDReader:");
		System.out.println("Test1: this.reader.close(): " + this.reader.close());
		System.out.println("Test2: this.reader.isBusy(): "+this.reader.isBusy()+"_-_and this.reader.isOpen(): "+this.reader.isOpen())
	}
}
