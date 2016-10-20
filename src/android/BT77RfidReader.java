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

import java.util.*;

/**
* This class echoes a string called from JavaScript.
*/
public class BT77RfidReader extends CordovaPlugin {
	RfidReader bt77reader;
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("echo")) {
			String message = args.getString(0);
			this.echo(message, callbackContext, args);
			return true;
		}else if (action.equals("start")){
			System.out.println("start executed");
			this.startRFIDReader();
		}else if (action.equals("scan")){
			
			
			System.out.println("Test3: Start Test3");
			InventoryParameters p = new InventoryParameters();
			System.out.println("Test3: InventoryParameters: "+p);
			/*System.out.println("Test3: InventoryParameters.getCycleCount: "+p.getCycleCount());
			System.out.println("Test3: InventoryParameters.getCountThreshold: "+p.getCountThreshold());
			System.out.println("Test3: InventoryParameters.getRssiThreshold: "+p.getRssiThreshold());
			System.out.println("Test3: InventoryParameters.getEpcInclusionPrefix: "+p.getEpcInclusionPrefix());
			System.out.println("Test3: InventoryParameters.getEpcExclusionPrefix: "+p.getEpcExclusionPrefix());
			*/
            InventoryResult r = this.bt77reader.getInventory(p);
			System.out.println("Test3: InventoryResult: "+r);
			Sy/*stem.out.println("Test3: InventoryResult.getRawResult: "+r.getRawResult());
			System.out.println("Test3: InventoryResult.getInventory: "+r.getInventory());
			*/
			//args = (JSONArray[])r[0];
			//args = new JSONArray(Arrays.asList(r));
			
            OperationStatus s = r.getOperationStatus();
			System.out.println("Test3: OperationStatus: "+s);
			if(args != null && args.length() > 0){
				callbackContext.success("OperationStatus: "+s.toString()+"_-_InventoryParameters:"+p+"_-_InventoryResult: "+r);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
			
			
		}else if (action.equals("read")){
			ReadParameters p = new ReadParameters();

            p.setMemoryBank(TagMemoryBank.USER);
//            p.setEpc("3005FB63AC1F3681EC880468");
			p.setEpc("0066840000000000000010FB");
            p.setOffset(2);
            p.setLength(16);

            ReadResult r = this.bt77reader.readMemoryBank(p);

            OperationStatus s = r.getOperationStatus();
            String data = r.getReadData();
			
			if(data != null && data.length() > 0){
				callbackContext.success("OperationStatus: "+s.toString()+"_-_ReadParameters:"+p+"_-_ReadResult: "+r+"_-_Data: "+data);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
		}else if (action.equals("write")){
			WriteParameters p = new WriteParameters();
			
            p.setMemoryBank(TagMemoryBank.USER);
            p.setEpc("0066840000000000000010FB");
            p.setOffset(2);
            String data = "1337";
			p.setWriteData(data);

            WriteResult r = bt77reader.writeMemoryBank(p);

            OperationStatus s = r.getOperationStatus();
			
			if(data != null && data.length() > 0){
				callbackContext.success("OperationStatus: "+s.toString()+"_-_WriteParameters:"+p+"_-_WriteResult: "+r+"_-_Data: "+data);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
		}else if (action.equals("stop")){
			System.out.println("stop executed");
			this.stopRFIDReader();
		}else{
			return false;
		}
		return true;
	}
	
	private void echo(String message, CallbackContext callbackContext, JSONArray args) {
		if (message != null && message.length() > 0) {
			callbackContext.success(args);
		} else {
			callbackContext.error("Expected one non-empty string argument.");
		}
	}
	
	public static String generateString(Random rng, String characters, int length){
		char[] text = new char[length];
		for (int i = 0; i < length; i++){
			text[i] = characters.charAt(rng.nextInt(characters.length()));
		}
		return new String(text);
	}
	
	private void startRFIDReader(){
		System.out.println("Test1: Start RFIDReader:");
		this.bt77reader = new RfidReader(cordova.getActivity());
		System.out.println("Test1: RFIDReader created");
		System.out.println("Test1: this.bt77reader.open(): " + this.bt77reader.open());
		System.out.println("Test1: this.bt77reader.isBusy(): "+this.bt77reader.isBusy()+"_-_and this.bt77reader.isOpen(): "+this.bt77reader.isOpen());
	}
	
	private void stopRFIDReader(){
		System.out.println("Test2: Stop RFIDReader:");
		System.out.println("Test1: this.bt77reader.close(): " + this.bt77reader.close());
		System.out.println("Test2: this.bt77reader.isBusy(): "+this.bt77reader.isBusy()+"_-_and this.bt77reader.isOpen(): "+this.bt77reader.isOpen());
	}
}
