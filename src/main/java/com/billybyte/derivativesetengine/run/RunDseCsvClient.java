package com.billybyte.derivativesetengine.run;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.inputs.InBlk;
import com.billybyte.queries.ComplexQueryResult;
import com.billybyte.ui.messagerboxes.MessageBox;
import com.thoughtworks.xstream.XStream;

public class RunDseCsvClient {
	private static String FIRSTPART = "http://127.0.0.1:8989/dseinblk";
	private static String ARG_SEP = "?";
	public static void main(String[] args) {
		String response = 
				MessageBox.ConsoleMessage("enter shortnames followed by commas: ");
		//http://127.0.0.1:8989/dseinblk?p1=CL.FUT.NYMEX.USD.201512&p2=LO.FOP.NYMEX.USD.201512.C.60.00&p3=LO.FOP.NYMEX.USD.201512.P.60.00
		String[] shortNames = response.split(",");
		String argString = "";
		for(int i = 0;i < shortNames.length;i++){
			argString += ARG_SEP + "p" + i +  "=" + shortNames[i];
		}
		
		String fullUrl = FIRSTPART + argString;
		
		List<String[]> responseFromHttp = Utils.getCSVData(fullUrl);
		String xml = "";
		for(int i = 1;i<responseFromHttp.size();i++){
			xml += responseFromHttp.get(i)[0];
		}
				
		XStream xs = new XStream();
		
		Map<String, ComplexQueryResult<InBlk>> inBlkMap = (Map<String, ComplexQueryResult<InBlk>>)xs.fromXML(xml);
		
		for(Entry<String, ComplexQueryResult<InBlk>> entry : inBlkMap.entrySet()){
			String sn = entry.getKey();
			ComplexQueryResult<InBlk> cqr = entry.getValue();
			if(!cqr.isValidResult()){
				Utils.prtObErrMess(RunDseCsvClient.class, "Excpetion for shortName: "+ sn);
					Utils.prtObErrMess(RunDseCsvClient.class, cqr.getException().getMessage());
			}else{
				InBlk inblk = cqr.getResult();
				CollectionsStaticMethods.prtListItems(inblk.getDioTypeList());
				
				Utils.prt(cqr.getResult().toString());
			}
		}
	}

}
