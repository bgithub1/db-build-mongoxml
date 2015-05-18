package com.billybyte.derivativesetengine.testcases;

import java.util.List;
import java.util.Map;

import com.billybyte.commoncollections.MapFromMap;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;

import junit.framework.TestCase;

public class TestListFromCsv extends TestCase {

	public void testGetList(){
		String csvNameOrPath = "testDataForTestTestListFromCsv.csv";
		Class<?> classInPkgOfResource = this.getClass();
		String colNameOfKey = "shortName";
		Class<String> classOfKey = String.class;
		Class<SecInputsInfo> classOfData= SecInputsInfo.class;
		List<SecInputsInfo> secInputsInfoList =  
				CollectionsStaticMethods.listFromCsv(classOfData, csvNameOrPath,classInPkgOfResource);
		assertEquals(9,secInputsInfoList.size());

		
		Map<String, SecInputsInfo> secInfoMap = 
				new MapFromMap<String, SecInputsInfo>(
						csvNameOrPath,classInPkgOfResource,
						colNameOfKey,classOfKey,classOfData);
		assertEquals(9,secInfoMap.size());

		
	}
}
