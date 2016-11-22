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

import com.sevenid.mobile.reader.bt77.*;
import android.app.Activity;
import android.util.Log;
import com.android.hdhe.uhf.reader.Tools;
import com.android.hdhe.uhf.reader.UhfReader;
import com.sevenid.mobile.reader.api.Epc;
import com.sevenid.mobile.reader.api.IAbstractReader;
import com.sevenid.mobile.reader.api.operationresult.InventoryResult;
import com.sevenid.mobile.reader.api.operationresult.OperationStatus;
import com.sevenid.mobile.reader.api.operationresult.ReadResult;
import com.sevenid.mobile.reader.api.operationresult.WriteResult;
import com.sevenid.mobile.reader.api.parameters.InventoryParameters;
import com.sevenid.mobile.reader.api.parameters.ReadParameters;
import com.sevenid.mobile.reader.api.parameters.TagMemoryBank;
import com.sevenid.mobile.reader.api.parameters.WriteParameters;
import com.sevenid.mobile.reader.core.LicenseManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class BT77RfidReader extends CordovaPlugin {
    public enum CordovaAction {
        SCAN_INVENTORY, SCAN_INVENTORY_TWO, READ_TAG, WRITE_TAG, START_RFID_LISTENER, STOP_RFID_LISTENER
    }
    private static int EPC_OFFSET = 2;
    private static int EPC_LENGTH = 6;
    RfidReader reader = null;
    int retriesReadWrite = 0;
    int inventoryCycles = 0;
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
            inventoryCycles = argsObject.getInt("inventoryCycles");
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
				
			case SCAN_INVENTORY_TWO:
				return scanInventoryTwo();
            
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
            this.reader = new RfidReader(cordova.getActivity()){
				Field fReader = this.reader.getClass().getDeclaredField("reader");
				fReader.setAccessible(true);
				UhfReader uhfreader = (UhfReader) fReader.get(this.reader);
				
				@Override
				public InventoryResult getInventory(InventoryParameters param){
					System.out.println("This is a test if this method will really be overwritten!!!");
					InventoryResult result = new InventoryResult();
					
					HashMap<String, Epc> unfilteredInventory = new HashMap();
					for (int i = 0; i < param.getCycleCount(); i++){
						List<byte[]> currentInventory = uhfreader.inventoryRealTime();
						if ((currentInventory != null) && (!currentInventory.isEmpty())) {
							for (byte[] epc : currentInventory) {
								if ((epc != null) && (epc.length > 0)){
									String epcStr = Tools.Bytes2HexString(epc, epc.length);
									if (survivesFilter(epcStr, param)){
										Epc old = (Epc)unfilteredInventory.get(epcStr);
										if (old == null) {
											unfilteredInventory.put(epcStr, new Epc(epcStr));
										} else {
											old.incrementSeenCount();
										}
									}
								}
							}
						}
						try{
							Thread.sleep(50L);
						} catch (InterruptedException localInterruptedException1) {}
					}
					List<Epc> thresholdFilteredEpcList = new ArrayList();
					for (Iterator i = unfilteredInventory.entrySet().iterator(); i.hasNext();){
						Map.Entry currentEntry = (Map.Entry)i.next();
						Epc currentEpc = (Epc)currentEntry.getValue();
						if (currentEpc.getSeenCount() >= param.getCountThreshold()) {
							thresholdFilteredEpcList.add(currentEpc);
						}
					}
					Epc[] a = new Epc[thresholdFilteredEpcList.size()];
					result.setInventory((Epc[])thresholdFilteredEpcList.toArray(a));
					result.setOperationStatus(OperationStatus.STATUS_OK);
					
					return result;
				}
			};
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
        p.setCycleCount(inventoryCycles);

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
            inventoryCycles: y,
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
	
	private boolean scanInventoryTwo() {
        startRFIDReader();

        InventoryParameters p = new InventoryParameters();
        p.setCycleCount(inventoryCycles);
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
            inventoryCycles: y,
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
            epcCount = currentEpc.getSeenCount();
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

        ReadParameters p = new ReadParameters();
        p.setMemoryBank(TagMemoryBank.EPC);
        p.setEpc(epcToRead);
        p.setOffset(EPC_OFFSET);
        p.setLength(EPC_LENGTH);
        p.setRetries(retriesReadWrite);

        Log.i("BT77RfidReader", "ReadParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+")");
        ReadResult readResult = reader.readMemoryBank(p);

        OperationStatus status = readResult.getOperationStatus();
        Log.i("BT77RfidReader", "OperationStatus: " + status.toString());
        if (status != OperationStatus.STATUS_OK) {
            callbackContext.error("Error in readTag: " + status.name());
            return false;
        }

        try{
            argsObject.put("dataFromReadResult", readResult.getReadData());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        Log.i("BT77RfidReader", "readResult.getReadData(): "+readResult.getReadData());

        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        callbackContext.success(argsArray);
        return true;
    }
    
    private boolean writeTag() {
        
        startRFIDReader();

        WriteParameters p = new WriteParameters();
        
        p.setMemoryBank(TagMemoryBank.EPC);
        p.setEpc(epcToWrite);
        p.setOffset(EPC_OFFSET);
        p.setRetries(retriesReadWrite);
        p.setWriteData(dataToWrite);

        Log.i("BT77RfidReader", "WriteParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+"), WriteData("+p.getWriteData()+"), MemoryBank("+p.getMemoryBank()+")");
        
        WriteResult r = reader.writeMemoryBank(p);

        OperationStatus status = r.getOperationStatus();
        Log.i("BT77RfidReader", "OperationStatus: " + status.toString());
        if (status != OperationStatus.STATUS_OK) {
            callbackContext.error("Error in writeTag: " + status.name());
            return false;
        }
        
        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            Log.e("BT77RfidReader", "Exception: " + e + "");
        }
        callbackContext.success(argsArray);
        return true;
    }
	
/* 	public InventoryResult getInventory(InventoryParameters param){
		System.out.println("This is a test if this method will really be overwritten!!!");
		return reader.getInventory(param);
	} */
	
/* 	private void inventoryAdvantageReached(){
		Log.e("inventoryAdvantageReached ... checking current inventory ...");
		String maxSeenCountEpc = secondMostSeenCountEpc = null;
		int maxSeenCountValue = secondMostSeenCountValue = -1;
		for (int i = 0; i < args.length(); i++){
			
			
		}
		
		
	}
        Object.keys(argsObject.inventory).forEach(function (epc) {
            var seenCount = argsObject.inventory[epc];
            debugLog("Inventory-Entry: epc(" + epc + "), seenCount(" + seenCount + ")");
            if (seenCount > maxSeenCountValue) {
                maxSeenCountEpc = epc;
                maxSeenCountValue = seenCount;
            }
        });
        Object.keys(argsObject.inventory).forEach(function (epc) {
            if (epc == maxSeenCountEpc) {
                return;     // do not use the epc already in first place with highest seenCount
            }
            var seenCount = argsObject.inventory[epc];
            debugLog("Inventory-Entry: epc(" + epc + "), seenCount(" + seenCount + ")");
            if (seenCount > maxSeenCountValue) {
                secondMostSeenCountEpc = epc;
                secondMostSeenCountValue = seenCount;
            }
        });
        if (maxSeenCountValue - secondMostSeenCountValue >= seenCountAdvantageForFind) {
            return true;
        }
        return false; */
    
    
    
    /**
    REMOVE IF NOT NEEDED
    public static String generateString(Random rng, String characters, int length){
        char[] text = new char[length];
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    
    private void getParameters(JSONArray args, CallbackContext callbackContext, int retriesReadWriteDefault, boolean bWriteData) throws JSONException {
        if(bWriteData != true){
            bWriteData = false;
        }

        for (int n = 0; n < args.length(); n++){
            System.out.println("iteration " + n + " of JSONArray" +args);
            JSONObject object = args.getJSONObject(n);
            // JSONException wird geworfen, wenn .get("") nichts findet
            try{
                this.retriesReadWrite = object.getInt("retriesReadWrite");
            }catch(JSONException e){
                if(e.getMessage().contains("java.lang.String cannot be converted to int")){
                    callbackContext.error(e.getMessage());
                }
                System.out.println("Error: JSONException " + e + " was thrown. Setting default values.");
                this.retriesReadWrite = retriesReadWriteDefault;
            }
            try{
                this.epcString = object.getString("epc");
                if(bWriteData == true){
                    this.dataString = object.getString("data");
                }
            }catch(JSONException e){
                callbackContext.error(e.getMessage() + "" + args);
            }
        }
    }
    */
}

/* class CustomRfidReader extends RfidReader{
	Field field = RfidReader.class.getDeclaredField("reader");
	field.setAccessible(true);
	
	@Override
	public InventoryResult getInventory(InventoryParameters param){
		System.out.println("This is a test if this method will really be overwritten!!!");
		InventoryResult result = new InventoryResult();
		
		HashMap<String, Epc> unfilteredInventory = new HashMap();
		for (int i = 0; i < param.getCycleCount(); i++){
			List<byte[]> currentInventory = this.reader.inventoryRealTime();
			if ((currentInventory != null) && (!currentInventory.isEmpty())) {
				for (byte[] epc : currentInventory) {
					if ((epc != null) && (epc.length > 0)){
						String epcStr = Tools.Bytes2HexString(epc, epc.length);
						if (survivesFilter(epcStr, param)){
							Epc old = (Epc)unfilteredInventory.get(epcStr);
							if (old == null) {
								unfilteredInventory.put(epcStr, new Epc(epcStr));
							} else {
								old.incrementSeenCount();
							}
						}
					}
				}
			}
			try{
				Thread.sleep(50L);
			} catch (InterruptedException localInterruptedException1) {}
		}
		List<Epc> thresholdFilteredEpcList = new ArrayList();
		for (Iterator i = unfilteredInventory.entrySet().iterator(); i.hasNext();){
			Map.Entry currentEntry = (Map.Entry)i.next();
			Epc currentEpc = (Epc)currentEntry.getValue();
			if (currentEpc.getSeenCount() >= param.getCountThreshold()) {
				thresholdFilteredEpcList.add(currentEpc);
			}
		}
		Epc[] a = new Epc[thresholdFilteredEpcList.size()];
		result.setInventory((Epc[])thresholdFilteredEpcList.toArray(a));
		result.setOperationStatus(OperationStatus.STATUS_OK);
		
		return result;
	}
} */