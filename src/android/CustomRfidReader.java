package org.apache.cordova.plugin;

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

public class CustomRfidReader implements IAbstractReader{
	private static volatile boolean isActive = false;
	private volatile boolean isOpen = false;
	private UhfReader reader;
	private Activity context;
	
	private CustomRfidReader() {}
	
	public CustomRfidReader(Activity c){
		this.context = c;
	}
	
	public boolean open(){
		LicenseManager l = new LicenseManager(this.context);
		if (l.validateLicense()) {
			return waitForReaderInstance(20, 5);
		}
		return false;
	}
	
	public boolean close(){
		this.reader.close();
		isActive = false;
		this.isOpen = false;
		return true;
	}
	
	public boolean isBusy(){
		return (isActive) && (this.isOpen);
	}
	
	public boolean isOpen(){
		return this.isOpen;
	}
	
	public InventoryResult getInventory(InventoryParameters param){
		Log.i("BT77RfidReader", "This is a test if this method will really be overwritten!!!");
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
	
	public ReadResult readMemoryBank(ReadParameters param){
		ReadResult result = new ReadResult();

		this.reader.selectEPC(Tools.HexString2Bytes(param.getEpc()));
		byte[] accessPassword = Tools.HexString2Bytes(param.getPassword());
		for (int i = 0; i < param.getRetries(); i++){
			byte[] oldData = this.reader.readFrom6C(membankToInt(param.getMemoryBank()), param.getOffset(), param.getLength(), accessPassword);
			if ((oldData != null) && (oldData.length > 1)){
				String oldReadResult = Tools.Bytes2HexString(oldData, oldData.length);
				result.setOperationStatus(OperationStatus.STATUS_OK);
				result.setReadData(oldReadResult);
				result.setRawResult(i + 1);
			
				return result;
			}
			try{
				Thread.sleep(50L);
			}catch (InterruptedException localInterruptedException) {}
		}
		result.setOperationStatus(OperationStatus.STATUS_OPERATION_FAIL);
		result.setReadData(null);

		return result;
	}
	
	public WriteResult writeMemoryBank(WriteParameters param){
		WriteResult result = new WriteResult();

		this.reader.selectEPC(Tools.HexString2Bytes(param.getEpc()));
		byte[] accessPassword = Tools.HexString2Bytes(param.getPassword());
		byte[] rawWriteData = Tools.HexString2Bytes(param.getWriteData());
		for (int i = 0; i < param.getRetries(); i++){
			boolean writeRetVal = this.reader.writeTo6C(accessPassword, membankToInt(param.getMemoryBank()), param.getOffset(), rawWriteData.length / 2, rawWriteData);
			if (writeRetVal){
				result.setOperationStatus(OperationStatus.STATUS_OK);
				result.setRawResult(i + 1);

				return result;
			}
			try{
				Thread.sleep(50L);
			} catch (InterruptedException localInterruptedException) {}
		}
		result.setOperationStatus(OperationStatus.STATUS_OPERATION_FAIL);

		return result;
	}
	
	public WriteResult overwriteEpc(String oldEpc, String newEpc){
		if ((oldEpc != null) && (newEpc != null) && (oldEpc.length() == 24) && (newEpc.length() == 24)){
			WriteParameters param = new WriteParameters();
			param.setEpc(oldEpc);
			param.setMemoryBank(TagMemoryBank.EPC);
			param.setOffset(2);
			param.setWriteData(newEpc);
			param.setRetries(10);

			return writeMemoryBank(param);
		}
		WriteResult errorResult = new WriteResult();
		errorResult.setOperationStatus(OperationStatus.PARAMETER_PROBLEM);

		return errorResult;
	}
	
	private boolean survivesFilter(String epc, InventoryParameters param){
		if (epc != null){
			if ((param.getEpcInclusionPrefix() != null) && (!param.getEpcInclusionPrefix().isEmpty()) && (epc.startsWith(param.getEpcInclusionPrefix()))) {
				return true;
			}
			if ((param.getEpcExclusionPrefix() != null) && (!param.getEpcExclusionPrefix().isEmpty()) && (epc.startsWith(param.getEpcInclusionPrefix()))) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	private int membankToInt(TagMemoryBank bank){
		if (TagMemoryBank.RESERVE.equals(bank)) {
			return 0;
		}
		if (TagMemoryBank.EPC.equals(bank)) {
			return 1;
		}
		if (TagMemoryBank.TID.equals(bank)) {
			return 2;
		}
		if (TagMemoryBank.USER.equals(bank)) {
			return 3;
		}
		return -1;
	}
	
	private boolean openReader(int powerValue, int sensitivity){
		this.reader = UhfReader.getInstance();
		if (this.reader != null){
			this.reader.setOutputPower(powerValue);
			this.reader.setSensitivity(sensitivity);
			return true;
		}
		return false;
	}
	
	private boolean waitForReaderInstance(int delay, int count){
		int currentCount = 0;
		while (isActive) {
			try{
				Log.e("RfidReader", "Polling for reader instance...");
				Thread.sleep(delay);
				if (currentCount > count) {
					return false;
				}
				currentCount++;
			} catch (InterruptedException localInterruptedException) {}
		}
		isActive = true;
		this.isOpen = true;
		return openReader(26, 3);
	}
}