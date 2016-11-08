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
	RfidReader reader;
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("echo")) {
			String message = args.getString(0);
			this.echo(message, callbackContext, args);
			return true;
		}else if (action.equals("startRfidListener")){
			System.out.println("start executed");
			this.startRFIDReader();
		}else if (action.equals("scanInventory")){
			System.out.println("SCANINVENTORY - args before: "+args);
			int cycleCount = 0;
			InventoryParameters p = new InventoryParameters();
			
			try{
				cycleCount = args.getJSONObject(0).getInt("cycles");
			}catch(JSONException e){
				if(e.getMessage().contains("java.lang.String cannot be converted to int")){
					callbackContext.error(e.getMessage());
				}
				System.out.println("Error: JSONException " + e + " was thrown. Setting default values.");
				cycleCount = 10;
			}
			
			p.setCycleCount(cycleCount);
			
			
            InventoryResult r = this.reader.getInventory(p);
			
			for(int i = 0; i < r.getInventory().length; i++){
				try{
					System.out.println("found EPC: "+r.getInventory()[i].getEpc());
					int curSeenCtr = args.getJSONObject(0).getInt(r.getInventory()[i].getEpc());
					System.out.println("curSeenCtr for "+i+". entry: "+curSeenCtr);
					args.getJSONObject(0).put(r.getInventory()[i].getEpc(), curSeenCtr+r.getInventory()[i].getSeenCount());
				}catch(JSONException e){
					if(e.getMessage().contains("java.lang.String cannot be converted to int")){
						callbackContext.error(e.getMessage());
					}
					System.out.println("Error: " + e + " was thrown. Creating new value.");
					/** 
					 *	Wenn mehr Parameter übergeben werden sollen, kann das JSONObject auch mehrere JSONObjects beinhalten (z.B. Key: EPC, Value: JSONObject):
					 *	JSONObject currentInventory = new JSONObject();
					 *	currentInventory.put("EPC", r.getInventory()[i].getEpc());
					 *	currentInventory.put("SEENCTR", r.getInventory()[i].getSeenCount());
					 *	args.getJSONObject(0).put(r.getInventory()[i].getEpc(), currentInventory);
					 */
					args.getJSONObject(0).put(r.getInventory()[i].getEpc(), r.getInventory()[i].getSeenCount());
				}
			}
			
			
            OperationStatus s = r.getOperationStatus();
			if(args != null && args.length() > 0){
				System.out.println("SCANINVENTORY - args after: "+args);
				callbackContext.success(args);
			} else if (args.length() == 1){
				callbackContext.error("No results found.");
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
			
			
		}else if (action.equals("readTag")){
			System.out.println("READTEST: args="+args);
			int retries = 0;
			
			for (int n = 0; n < args.length(); n++){
				System.out.println("iteration " + n + " of JSONArray args");
				JSONObject object = args.getJSONObject(n);
				System.out.println("testparam:" + object.get("testparam"));
				// JSONException wird geworfen, wenn .get("") nichts findet
				try{
					retries = object.getInt("retries");
				}catch(JSONException e){
					if(e.getMessage().contains("java.lang.String cannot be converted to int")){
						callbackContext.error(e.getMessage());
					}
					System.out.println("Error: JSONException " + e + " was thrown. Setting default values.");
					retries = 10;
				}
			}
			
			//String teststring = args.get(0).get("testparam");
			//System.out.println("TESTSTRING:"+teststring);
			//System.out.println("NOT INCLUDED:"+args.get(0).get("olp"));
			System.out.println("NOT INCLUDED:"+args.get(0));
			ReadParameters p = new ReadParameters();

			
            p.setMemoryBank(TagMemoryBank.USER);
//            p.setEpc("3005FB63AC1F3681EC880468");
			p.setEpc("0066840000000000000010FB");
            p.setOffset(2);
            p.setLength(16);
			System.out.println("Retries: "+retries);
			p.setRetries(retries);

            ReadResult r = this.reader.readMemoryBank(p);

            OperationStatus s = r.getOperationStatus();
            String data = r.getReadData();
			
			if(data != null && data.length() > 0){
				callbackContext.success("OperationStatus: "+s.toString()+"_-_ReadParameters:"+p+"_-_ReadResult: "+r+"_-_Data: "+data);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
		}else if (action.equals("writeTag")){
			WriteParameters p = new WriteParameters();
			
            p.setMemoryBank(TagMemoryBank.USER);
            p.setEpc("0066840000000000000010FB");
            p.setOffset(2);
            String data = "1337";
			p.setWriteData(data);

            WriteResult r = reader.writeMemoryBank(p);

            OperationStatus s = r.getOperationStatus();
			
			if(data != null && data.length() > 0){
				callbackContext.success("OperationStatus: "+s.toString()+"_-_WriteParameters:"+p+"_-_WriteResult: "+r+"_-_Data: "+data);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
		}else if (action.equals("endRfidListener")){
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
		this.reader = new RfidReader(cordova.getActivity());
		System.out.println("Test1: RFIDReader created");
		System.out.println("Test1: this.reader.open(): " + this.reader.open());
		System.out.println("Test1: this.reader.isBusy(): "+this.reader.isBusy()+"_-_and this.reader.isOpen(): "+this.reader.isOpen());
	}
	
	private void stopRFIDReader(){
		System.out.println("Test2: Stop RFIDReader:");
		System.out.println("Test1: this.reader.close(): " + this.reader.close());
		System.out.println("Test2: this.reader.isBusy(): "+this.reader.isBusy()+"_-_and this.reader.isOpen(): "+this.reader.isOpen());
	}
}
