package org.apache.cordova.plugin;

import com.sevenid.mobile.reader.*
import android.app.Activity;
import android.util.Log;
import java.util.*

public class CustomRfidReader extends RfidReader{
	public InventoryResult getInventory(InventoryParameters param){
		System.out.println("This is a test if this method will really be overwritten!!!")
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
}