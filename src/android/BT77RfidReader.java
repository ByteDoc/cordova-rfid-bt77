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

public enum CordovaAction {
    SCAN_INVENTORY, READ_TAG, WRITE_TAG, START_RFID_LISTENER, STOP_RFID_LISTENER
}

public class BT77RfidReader extends CordovaPlugin {
    private static int EPC_OFFSET = 2;
    private static int EPC_LENGTH = 6;
    RfidReader reader = null;
    int retriesReadWrite = 0;
    int inventoryCycles = 0;
    String epcToRead = "", epcToWrite = "", dataToWrite = "", dataFromReadResult = "";
    // epcString = "", dataString = "";
    JSONObject argsObject;
    CallbackContext callbackContext;
    CordovaAction action;
    

    
    @Override
    public boolean execute(String actionString, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        // read argument object, expected as first entry in args array
        try {
            argsObject = args.getJSONObject(0);
            retriesReadWrite = argsObject.getInt("retriesReadWrite");
            inventoryCycles = argsObject.getInt("inventoryCycles");
            epcToRead = argsObject.getString("epcToRead");
            epcToWrite = argsObject.getString("epcToWrite");
            dataToWrite = argsObject.getString("dataToWrite");
        } catch (JSONException e){
            System.out.println("Error: JSONException " + e + " was thrown. No or bad argument object supplied!");
            callbackContext.error(e.getMessage());
            return false;
        }
        
        try {
            action = CordovaAction.valueOf(actionString);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: JSONException " + e + " was thrown. No valid action supplied!");
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
                break;
            
            case SCAN_INVENTORY:
                return scanInventory();
                break;
            
            case READ_TAG:
                return readTag();
                break;
            
            case WRITE_TAG:
                return writeTag();
                break;
            
            case STOP_RFID_LISTENER:
                return stopRFIDReader();
                break;
            
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
            System.out.println("startRFIDReader: this.reader.open(): " + this.reader.open());
        }
        return true;
    }
    
    private boolean stopRFIDReader(){
        if(this.reader.isBusy() && this.reader.isOpen()){
            System.out.println("stopRFIDReader: this.reader.close(): " + this.reader.close());
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
        System.out.println("OperationStatus: " + status.toString());
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
            System.out.println("Creating JSONObject for inventory (" + e + ")");
            inventory = new JSONObject();
            try{
                argsObject.put("inventory", inventory);
            } catch (JSONException e2) {
                System.out.println("Exception: " + e2 + "");
            }
        }
        
        // update inventory with the scan results
        for(int i = 0; i < inventoryResult.getInventory().length; i++){
            Epc currentEpc = inventoryResult.getInventory()[i];
            int epcCount;
            try{
                epcCount = inventory.getInt(currentEpc.getEpc());
            } catch (JSONException e) {
                System.out.println("Creating Int for epcCount (" + e + ")");
                epcCount = 0;
            }
            epcCount += currentEpc.getSeenCount();
            try{
                inventory.put(currentEpc.getEpc(), epcCount);
            } catch (JSONException e) {
                System.out.println("Exception: " + e + "");
            }
            System.out.println("count for epc (" + currentEpc.getEpc() + ") now at (" + epcCount + ")");
        }
        
        
        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            System.out.println("Exception: " + e + "");
        }
        callbackContext.success(argsObject);
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

        System.out.println("ReadParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+")");
        ReadResult readResult = this.reader.readMemoryBank(p);

        OperationStatus status = readResult.getOperationStatus();
        System.out.println("OperationStatus: " + status.toString());
        if (status != OperationStatus.STATUS_OK) {
            callbackContext.error("Error in readTag: " + status.name());
            return false;
        }

        try{
            argsObject.put("dataFromReadResult", readResult.getReadData());
        } catch (JSONException e) {
            System.out.println("Exception: " + e + "");
        }
        System.out.println("readResult.getReadData(): "+readResult.getReadData());

        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            System.out.println("Exception: " + e + "");
        }
        callbackContext.success(argsObject);
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

        System.out.println("WriteParameters: Epc("+p.getEpc()+"), Retries("+p.getRetries()+"), WriteData("+p.getWriteData()+"), MemoryBank("+p.getMemoryBank()+")");
        
        WriteResult r = reader.writeMemoryBank(p);

        OperationStatus status = r.getOperationStatus();
        System.out.println("OperationStatus: " + status.toString());
        if (status != OperationStatus.STATUS_OK) {
            callbackContext.error("Error in writeTag: " + status.name());
            return false;
        }
        
        try{
            argsObject.put("status", status.name());
        } catch (JSONException e) {
            System.out.println("Exception: " + e + "");
        }
        callbackContext.success(argsObject);
        return true;
    }
    
    
    
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
