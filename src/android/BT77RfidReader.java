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
import com.android.hdhe.uhf.reader.Tools;
import com.android.hdhe.uhf.reader.UhfReader;
import com.sevenid.mobile.reader.api.IAbstractReader;
import com.sevenid.mobile.reader.core.LicenseManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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

}