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
		}else if (action.equals("start")){
			System.out.println("start executed");
			this.startRFIDReader();
		}else if (action.equals("scan")){
//			JsonArray args = Json.createArrayBuilder();
//			System.out.println("Test3: Start Test3");
			InventoryParameters p = new InventoryParameters();
			p.setCycleCount(10);
//			System.out.println("Test3: InventoryParameters: "+p);
//			System.out.println("Test3: InventoryParameters.getCycleCount: "+p.getCycleCount());
//			System.out.println("Test3: InventoryParameters.getCountThreshold: "+p.getCountThreshold());
//			System.out.println("Test3: InventoryParameters.getRssiThreshold: "+p.getRssiThreshold());
//			System.out.println("Test3: InventoryParameters.getEpcInclusionPrefix: "+p.getEpcInclusionPrefix());
//			System.out.println("Test3: InventoryParameters.getEpcExclusionPrefix: "+p.getEpcExclusionPrefix());
			
            InventoryResult r = this.reader.getInventory(p);
//			System.out.println("Test3: InventoryResult: "+r);
//			System.out.println("Test3: InventoryResult.getRawResult: "+r.getRawResult());
//			System.out.println("Test3: InventoryResult.getInventory: "+r.getInventory());
			String[] inventory = new String[r.getInventory().length];
			for(int i = 0; i < r.getInventory().length; i++){
				//HashMap currentInventory = new HashMap();
				String[] currentInventory = new String[3];
				currentInventory[0] = "NUMBER:"+ i;
				currentInventory[1] = "EPC:"+ r.getInventory()[i].getEpc();
				currentInventory[2] = "EPCByteArray:"+ r.getInventory()[i].getEpcToByteArray();
				//inventory.add(currentInventory); // Hier Fehler!!!! Evtl nur JSONObject zurückgeben????????????????????????????????????????????????????????????????????????????????????????????????????????????
				inventory[i] = Arrays.toString(currentInventory);
//				args.add(Json.createObjectBuilder()
//					.add("NUMBER", i)
//					.add("EPC", r.getInventory()[i].getEpc())
//					.add("EPCByteArray", r.getInventory()[i].getEpcToByteArray())
//				);
//				InventoryString += "\nInventoryResult r = this.reader.getInventory(p);"+
//				"\nr.getInventory()["+i+"].getEpc: "+r.getInventory()[i].getEpc()+
//				"\nr.getInventory()["+i+"].getSeenCount: "+r.getInventory()[i].getSeenCount()+
//				"\nr.getInventory()["+i+"].getEpcToByteArray: "+r.getInventory()[i].getEpcToByteArray();
//				System.out.println("Test3 InventoryString: "+InventoryString);
			}
//			args.build();
			
			//args = (JSONArray[])r[0];
			//args = new JSONArray(Arrays.asList(r));
			
            OperationStatus s = r.getOperationStatus();
			System.out.println("Test3: OperationStatus: "+s);
			if(inventory != null && inventory.length > 0){
				//callbackContext.success("OperationStatus: "+s.toString()+"_-_InventoryParameters:"+p+"_-_InventoryResult: "+r);
				callbackContext.success(
					Arrays.toString(inventory)
//					"OperationStatus: "+s.toString()+
//					"\nInventoryParameters:"+p+
//					"\nInventoryParameters.getCycleCount: "+p.getCycleCount()+
//					"\nInventoryParameters.getCountThreshold: "+p.getCountThreshold()+
//					"\nInventoryParameters.getRssiThreshold: "+p.getRssiThreshold()+
//					"\nInventoryParameters.getEpcInclusionPrefix: "+p.getEpcInclusionPrefix()+
//					"\nInventoryParameters.getEpcExclusionPrefix: "+p.getEpcExclusionPrefix()+
//					"\nInventoryResult: "+r+
//					"\nInventoryResult.getRawResult: "+r.getRawResult()+
//					"\nInventoryResult.getInventory: "+r.getInventory()+
//					"\nInventoryString: "+InventoryString
				);
			} else {
				callbackContext.error("Scan couldn't be initialized.");
			}
			
			
		}else if (action.equals("read")){
			System.out.println("READTEST: args="+args);
			String teststring = args.get("testparam");
			System.out.println("TESTSTRING:"+teststring);
			System.out.println("NOT INCLUDED:"+args.get("olp"));
			ReadParameters p = new ReadParameters();

			
            p.setMemoryBank(TagMemoryBank.USER);
//            p.setEpc("3005FB63AC1F3681EC880468");
			p.setEpc("0066840000000000000010FB");
            p.setOffset(2);
            p.setLength(16);
			p.setRetries(10);

            ReadResult r = this.reader.readMemoryBank(p);

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

            WriteResult r = reader.writeMemoryBank(p);

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
