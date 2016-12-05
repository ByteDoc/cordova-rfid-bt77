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
import com.sevenid.mobile.reader.api.Epc;
import com.sevenid.mobile.reader.bt77.RfidReader;

import java.util.*;
import android.util.Log;
import java.lang.reflect.*;

public class BT77RfidReader extends CordovaPlugin {
    public enum CordovaAction {
        SCAN_INVENTORY, READ_TAG, WRITE_TAG, START_RFID_LISTENER, STOP_RFID_LISTENER
    }
    private static int EPC_OFFSET = 2;
    private static int EPC_LENGTH = 6;
    RfidReader reader = null;
    int retriesReadWrite = 0;
    int inventoryCyclesForJava = 0;
	int inventoryCountThreshold = 0;
    String epcToRead = "", epcToWrite = "", dataToWrite = "", dataFromReadResult = "";
    // epcString = "", dataString = "";
    JSONObject argsObject;
    JSONArray argsArray;
    CallbackContext callbackContext;
    CordovaAction action;
    

    
    @Override
    public boolean execute(String actionString, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.i("BT77RfidReader", "execute called for action " + actionString);
        this.callbackContext = callbackContext;
        // read argument object, expected as first entry in args array
        try {
            argsArray = args;
            argsObject = args.getJSONObject(0);
            retriesReadWrite = argsObject.getInt("retriesReadWrite");
            inventoryCyclesForJava = argsObject.getInt("inventoryCyclesForJava");
			inventoryCountThreshold = argsObject.getInt("inventoryCountThreshold");
            epcToRead = argsObject.getString("epcToRead");
            epcToWrite = argsObject.getString("epcToWrite");
            dataToWrite = argsObject.getString("dataToWrite");
        } catch (JSONException e){
            Log.e("BT77RfidReader", "Error: JSONException " + e + " was thrown. No or bad argument object supplied!");
            callbackContext.error(e.getMessage());
            return false;
        }
        
        
        try {
            action = CordovaAction.valueOf(actionString);
        } catch (IllegalArgumentException e) {
            Log.e("BT77RfidReader", "Error: JSONException " + e + " was thrown. No valid action supplied!");
            callbackContext.error(e.getMessage());
            return false;
        }
        
        if (actionString.equals("echo")) {
            String message = args.getString(0);
            this.echo(message, callbackContext, args);
            return true;
        }

        switch (action) {

            case START_RFID_LISTENER:
                return startRFIDReader();
            
            case SCAN_INVENTORY:
                return scanInventory();
            
            case READ_TAG:
                return readTag();
            
            case WRITE_TAG:
                return writeTag();
            
            case STOP_RFID_LISTENER:
                return stopRFIDReader();
        }
        
        return false;
    }
    
    private void echo(String message, CallbackContext callbackContext, JSONArray args) {
        if (message != null && message.length() > 0) {
            callbackContext.success(args);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    
	private boolean startRFIDReader(){
		if(reader == null){
			this.reader = new RfidReader(cordova.getActivity());
		}
		if(!this.reader.isBusy() || !this.reader.isOpen()){
			Log.i("BT77RfidReader", "startRFIDReader: this.reader.open(): " + this.reader.open());
		}
		return true;
    }
	
    private boolean stopRFIDReader(){
        if(this.reader.isBusy() && this.reader.isOpen()){
            Log.i("BT77RfidReader", "stopRFIDReader: this.reader.close(): " + this.reader.close());
        }
        return true;
    }
    
    private boolean scanInventory() {
        startRFIDReader();
		
        InventoryParameters p = new InventoryParameters();
        p.setCycleCount(inventoryCyclesForJava);
        p.setCountThreshold(inventoryCountThreshold);

        // DO THE INVENTORY SCANNING ... using the reader, this is the hardware call
        InventoryResult inventoryResult = reader.getInventory(p);
                
        OperationStatus status = inventoryResult.getOperationStatus();
        Log.i("BT77RfidReader", "OperationStatus: " + status.toString());
        if (status != OperationStatus.STATUS_OK) {
            callbackContext.error("Error in scanInventory: " + status.name());
            return false;
        }  
        
        // results in attribute INVENTORY of argsObject, with the format:
        /**
        {
            retriesReadWrite: x,
            inventoryCyclesForJava: y,
            inventory: {                    JSONObject
                epc_id_123: epc_count       Int
                epc_id_456: epc_count       Int
            }
        }
        */
        // read current inventory from argsObject, or create if not existent yet
        JSONObject inventory;
        try{
            inventory = argsObject.getJSONObject("inventory");
        } catch (JSONException e) {
            Log.i("BT77RfidReader", "Creating JSONObject for inventory (" + e + ")");
            inventory = new JSONObject();
            try{
                argsObject.put("inventory", inventory);
            } catch (JSONException e2) {
                Log.e("BT77RfidReader", "Exception: " + e2 + "");
            }
        }
        
        // update inventory with the scan results
        for(int i = 0; i < inventoryResult.getInventory().length; i++){
            Epc currentEpc = inventoryResult.getInventory()[i];
            int epcCount;
            try{
                epcCount = inventory.getInt(currentEpc.getEpc());
            } catch (JSONException e) {
                Log.i("BT77RfidReader", "Creating Int for epcCount (" + e + ")");
                epcCount = 0;
            }
            epcCount += currentEpc.getSeenCount();
            try{
                inventory.put(currentEpc.getEpc(), epcCount);
            } catch (JSONException e) {
                Log.e("BT77RfidReader", "Exception: " + e + "");
            }
            Log.i("BT77RfidReader", "count for epc (" + currentEpc.getEpc() + ") now at (" + epcCount + ")");
        }
        
        
        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        callbackContext.success(argsArray);
        return true;
    }
    
    private boolean readTag() {
        startRFIDReader();
        
        JSONObject readResults;
        try{
            readResults = argsObject.getJSONObject("readResults");
        } catch (JSONException e) {
            Log.i("BT77RfidReader", "Creating JSONObject for results (" + e + ")");
            readResults = new JSONObject();
            try{
                argsObject.put("readResults", readResults);
            } catch (JSONException e2) {
                Log.e("BT77RfidReader", "Exception: " + e2 + "");
            }
        }
		
		Map<String, JSONObject> readResultMap = new HashMap<String, JSONObject>();
		readResultMap.put(TagMemoryBank.RESERVE.toString(), readTagWithMemoryBank(TagMemoryBank.RESERVE));
		readResultMap.put(TagMemoryBank.EPC.toString(), readTagWithMemoryBank(TagMemoryBank.EPC));
		readResultMap.put(TagMemoryBank.TID.toString(), readTagWithMemoryBank(TagMemoryBank.TID));
		readResultMap.put(TagMemoryBank.USER.toString(), readTagWithMemoryBank(TagMemoryBank.USER));
		
		Iterator<Map.Entry<String, JSONObject>>readResultIt = readResultMap.entrySet().iterator();
		while (readResultIt.hasNext()) {
			Map.Entry<String, JSONObject> readResultPair = (Map.Entry)readResultIt.next();
			try{
				readResults.put(readResultPair.getKey(), readResultPair.getValue());
			} catch (JSONException e) {
				Log.e("BT77RfidReader", "Exception: " + e + "");
			}
			readResultIt.remove(); // avoids a ConcurrentModificationException
		}
		
		
        // JSONObject readResultEpc, readResultTid;
        // readResultEpc = readTagWithMemoryBank(TagMemoryBank.EPC);
        // try{
            // readResults.put(TagMemoryBank.EPC.name(), readResultEpc);
        // } catch (JSONException e) {
            // Log.e("BT77RfidReader", "Exception: " + e + "");
        // }
        
        // readResultTid = readTagWithMemoryBank(TagMemoryBank.TID);
        // try{
            // readResults.put(TagMemoryBank.TID.name(), readResultTid);
        // } catch (JSONException e) {
            // Log.e("BT77RfidReader", "Exception: " + e + "");
        // }
		
		// readResultTid = readTagWithMemoryBank(TagMemoryBank.TID);
        // try{
            // readResults.put(TagMemoryBank.TID.name(), readResultTid);
        // } catch (JSONException e) {
            // Log.e("BT77RfidReader", "Exception: " + e + "");
        // }
        
        callbackContext.success(argsArray);
        return true;
    }
    
    private JSONObject readTagWithMemoryBank(TagMemoryBank tagMemoryBank) {
        ReadParameters p = new ReadParameters();
        p.setMemoryBank(tagMemoryBank);
        p.setEpc(epcToRead);
        p.setOffset(EPC_OFFSET);
        p.setLength(EPC_LENGTH);
        p.setRetries(retriesReadWrite);

        Log.i("BT77RfidReader", "ReadParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+")");
        ReadResult readResult = reader.readMemoryBank(p);
        
        OperationStatus status = readResult.getOperationStatus();
        Log.i("BT77RfidReader", "OperationStatus: " + status.toString());
        
        JSONObject result = new JSONObject();
        try{
            result.put("value", readResult.getReadData());
            result.put("status", status.name());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        
        if (status != OperationStatus.STATUS_OK) {
            // stop execution on any error?
            callbackContext.error("Error in readTagWithMemoryBank(" + tagMemoryBank.name() + "): " + status.name());
        }
        
        return result;
    }
    
    private boolean writeTag() {
        
        startRFIDReader();
		
		JSONObject writeResults;
        try{
            writeResults = argsObject.getJSONObject("writeResults");
        } catch (JSONException e) {
            Log.i("BT77RfidReader", "Creating JSONObject for writeResults (" + e + ")");
            writeResults = new JSONObject();
            try{
                argsObject.put("writeResults", writeResults);
            } catch (JSONException e2) {
                Log.e("BT77RfidReader", "Exception: " + e2 + "");
            }
        }
        
        JSONObject writeResultEpc;
        writeResultEpc = writeTagWithMemoryBank(TagMemoryBank.EPC);
        try{
            writeResults.put(TagMemoryBank.EPC.name(), writeResultEpc);
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        
        callbackContext.success(argsArray);
        return true;
    }
	
	private JSONObject writeTagWithMemoryBank(TagMemoryBank tagMemoryBank) {
        WriteParameters p = new WriteParameters();
        p.setMemoryBank(tagMemoryBank);
        p.setEpc(epcToWrite);
        p.setOffset(EPC_OFFSET);
        p.setRetries(retriesReadWrite);
		p.setWriteData(dataToWrite);

        Log.i("BT77RfidReader", "WriteParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+")");
        WriteResult writeResult = reader.writeMemoryBank(p);
        
        OperationStatus status = writeResult.getOperationStatus();
        Log.i("BT77RfidReader", "OperationStatus: " + status.toString());
        
        JSONObject result = new JSONObject();
        try{
            if (status == OperationStatus.STATUS_OK){
				result.put("value", p.getWriteData());
			}
            result.put("status", status.name());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        
        if (status != OperationStatus.STATUS_OK) {
            // stop execution on any error?
            callbackContext.error("Error in writeTagWithMemoryBank(" + tagMemoryBank.name() + "): " + status.name());
        }
        
        return result;
    }

}