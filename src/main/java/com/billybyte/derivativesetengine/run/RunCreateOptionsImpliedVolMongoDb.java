package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JFrame;

import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.derivativesetengine.run.RunCreateUnderlyingAtmImpliedVolMongoDb.ParamArgs;
import com.billybyte.dse.outputs.DerivativeReturnDisplay;
import com.billybyte.marketdata.SecDef;
import com.billybyte.marketdata.SecDefQueryAllMarkets;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;

/**
 * arg0 = boolean - showMsgBox
 * arg1 urlAs
 * arg2 urlWs
 * arg3 outputMongoDbHost
 * arg4 (optional) regexListXmlFilePath
 * arg5 (optional) classInPackageWhereFileIsLocated
 * 
 * @author bperlman1
 *
 */
public class RunCreateOptionsImpliedVolMongoDb {
	
	// ************* regex strings that determine what contracts you will work on
	//    these strings will be applied to the settlement database
	private static final String[] defaultRegexStrings = {
//			"iNG\\.FOP\\.ICE\\.USD\\.201[23456]",	
//			"((LO)|(LNE)|(ON)|(OH)|(OB))\\.FOP\\.NYMEX\\.USD\\.201[23456]",	
//			"((PAO)|(PO))\\.FOP\\.NYMEX\\.USD\\.201[23]",
//			"((OG)|(SO)|(HX))\\.FOP\\.COMEX\\.USD\\.201[2345]",
//			"((SB)|(KC)|(CC)|(CT)|(OJ)|(DX))\\.FOP\\.NYBOT\\.USD\\.201[234]",
//			"ES\\.FOP\\.GLOBEX\\.USD\\.20((12)|(13))((03)|(06)|(09)|(12))",	
//			"NQ\\.FOP\\.GLOBEX\\.USD\\.20((12)|(13))((03)|(06)|(09)|(12))",	
//			"6[ABCEJS]\\.FOP\\.CME\\.USD\\.201[234]",	
//			"((LE)|(HE)|(LS))\\.FOP\\.GLOBEX\\.USD\\.20((12)|(13))((02)|(04)|(06)|(08)|(10)|(12))",	
//			"GE\\.FOP\\.GLOBEX\\.USD\\.20((12)|(13)|(14)|(15)|(16))((03)|(06)|(09)|(12))",	
//			"((OZC)|(OZW))\\.FOP\\.ECBOT\\.USD\\.20((12)|(13))((03)|(05)|(07)|(09)|(12))",	
//			"OZS\\.FOP\\.ECBOT\\.USD\\.20((12)|(13))((01)|(03)|(05)|(07)|(08)|(09)|(11))",	
//			"((OZM)|(OZL))\\.FOP\\.ECBOT\\.USD\\.20((12)|(13))((01)|(03)|(05)|(07)|(08)|(09)|(10)|(12))",	
//			"((OZN)|(OZB))\\.FOP\\.ECBOT\\.USD\\.20((12)|(13))((03)|(06)|(09)|(12))",	
			"((COIL)|(GOIL))\\.FOP\\.IPE\\.USD\\.20((12)|(13)|(14)|(15)|(16))",	
//			"ES\\.FOP\\.GLOBEX\\.USD\\.201.0[124578]",	
//			"ES\\.FOP\\.GLOBEX\\.USD\\.201.1[01]",	
//			"^[AB](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[CD](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[EF](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[GH](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[IJK](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[LM](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[OP](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[QR](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[ST](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[UV](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[WX](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[YZ](()|(.)|(..)|(...))\\.OPT\\.SMART",
//			"^[AB].*\\.OPT\\.SMART",
//			"^[CD].*\\.OPT\\.SMART",
//			"^[EF].*\\.OPT\\.SMART",
//			"^[GH].*\\.OPT\\.SMART",
//			"^[IJK].*\\.OPT\\.SMART",
//			"^[LM].*\\.OPT\\.SMART",
//			"^[OP].*\\.OPT\\.SMART",
//			"^[QR].*\\.OPT\\.SMART",
//			"^[ST].*\\.OPT\\.SMART",
//			"^[UV].*\\.OPT\\.SMART",
//			"^[WX].*\\.OPT\\.SMART",
//			"^[YZ].*\\.OPT\\.SMART",
	};

	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {

		
		ParamArgs pargs = new ParamArgs(args);
		
		// if a regex list was provided, use it
		String[] regexStrings = defaultRegexStrings;
		if(pargs.regexStringList!=null){
			// override regexStrings
			regexStrings = pargs.regexStringList.toArray(new String[]{});
		}
		
		QueryInterface<String, SecDef> sdQuery = new SecDefQueryAllMarkets();

		MongoXml<SettlementDataImmute> mongoSettleDb =
				new MongoXml<SettlementDataImmute>(
						MongoDatabaseNames.DEFAULT_HOST, 
						MongoDatabaseNames.DEFAULT_PORT, 
						MongoDatabaseNames.SETTLEMENT_DB, 
						MongoDatabaseNames.SETTLEMENT_CL);
		
		
		//*************** END OPEN OUTPUT MONGO DATABASE ********************************
		
		

//		QueryInterface<String, SecDef> sdQuery = new SecDefQueryAllMarkets();
		
		//****************** the CreateMongoSettleImpliedVolDatabase class does all of the work ***************
		//  it get's called in the loop below
		CreateMongoSettleImpliedVolDatabase csivd = 
				new CreateMongoSettleImpliedVolDatabase(
				pargs.de, 
				mongoSettleDb,
				sdQuery);
		
		Map<String,BigDecimal> impliedVolMap = new HashMap<String, BigDecimal>();
	
		//******************* MAJOR LOOP IS HERE ********************************
		for(String regexString : regexStrings){
			String[] rArr = {regexString};
			// get option settles
			List<SettlementDataInterface> settles = 
					csivd.getOptionSettlementsFromRegexStrings(rArr);
			// ********** !!!!!! get the implied vols HERE ***********************
			List<DerivativeReturnDisplay> drdList =csivd.getOptionImpliedVols(settles);
			TreeMap<String, DerivativeReturnDisplay> drdMap = 
					new TreeMap<String, DerivativeReturnDisplay>();
			for(DerivativeReturnDisplay drd : drdList){
				drdMap.put(drd.getDerivativeShortName(), drd);
			}
			CollectionsStaticMethods.prtMapItems(drdMap);

			// ************** create implied vol map that will be written to Mongo ***************
			Map<String,BigDecimal> tempImpliedVolMap = new HashMap<String, BigDecimal>();
			
			for (Entry<String, DerivativeReturnDisplay> entry : drdMap.entrySet()) {
				DerivativeReturnDisplay drd = entry.getValue();
				if(!drd.isValidReturn())continue;
				BigDecimal value = new BigDecimal(
						drd.getValue().doubleValue()).setScale(6,RoundingMode.HALF_EVEN);
				tempImpliedVolMap.put(entry.getKey(), value);
			}
			
			 
			// now convert all of the data so that the in-the-money and out-of-the-money options
			//  both have the implied vol of the out-of-the-money option
			Utils.prtObMess(RunCreateOptionsImpliedVolMongoDb.class,"create Itm implieds using Otm implieds" );
			tempImpliedVolMap = csivd.createItmIvFromOtm(tempImpliedVolMap);
			impliedVolMap.putAll(tempImpliedVolMap);
			Utils.prtObMess(RunCreateOptionsImpliedVolMongoDb.class,"finished creating Itm implieds using Otm implieds" );

		}
		
		// *************  write the data *************************
		Boolean saveImpliedVols = true;
		Boolean deleteAllMongoVols = false;
		if(pargs.showMsgBox){
			saveImpliedVols = new Boolean(MessageBox.MessageBoxNoChoices(
					new JFrame(), "Entry true/false", "SAVE IMPLIED VOLS??", "true"));
			if(saveImpliedVols){
				deleteAllMongoVols = new Boolean( MessageBox.MessageBoxNoChoices(
						new JFrame(), "Entry true/false", "DELETE ENTIRE VOL DATABASE BEFORE SAVE??", "false"));
			}
		}
		
		if(saveImpliedVols){
			if(deleteAllMongoVols){
				pargs.mongoImpliedVolDb.deleteAll();
				pargs.mongoImpliedVolDb.writeMap(impliedVolMap);
			}else{
				pargs.mongoImpliedVolDb.multiUpsert(impliedVolMap);
			}
			Utils.prtObMess(RunCreateOptionsImpliedVolMongoDb.class,MongoDatabaseNames.IMPLIEDVOL_CL + " Has been updated");
		}
		

		System.exit(0);
	}
}
