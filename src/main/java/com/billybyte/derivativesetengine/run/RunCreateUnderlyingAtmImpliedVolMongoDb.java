package com.billybyte.derivativesetengine.run;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;


import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commoninterfaces.SettlementDataInterface;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.dse.DerivativeSetEngine;
import com.billybyte.dse.outputs.DerivativeReturnDisplay;
import com.billybyte.marketdata.SecDef;
import com.billybyte.marketdata.SecDefQueryAllMarkets;
import com.billybyte.marketdata.SettlementDataImmute;
import com.billybyte.mongo.MongoDatabaseNames;
import com.billybyte.mongo.MongoXml;
import com.billybyte.ui.messagerboxes.MessageBox;
/**
 * 
 * @author bperlman1
 *
 */
public class RunCreateUnderlyingAtmImpliedVolMongoDb {
	private static final String[] defaultRegexStrings = {
//			"iNG\\.FUT\\.ICE\\.USD\\.201[23456]",	
//			"((NG)|(CL)|(HO)|(RB))\\.FUT\\.NYMEX\\.USD\\.201[23456]",	
//			"((PA)|(PL))\\.FUT\\.NYMEX\\.USD\\.201[23]",
//			"((GC)|(SI)|(HG))\\.FUT\\.COMEX\\.USD\\.201[2345]",
//			"((SB)|(KC)|(CC)|(CT)|(OJ)|(DX))\\.FUT\\.NYBOT\\.USD\\.201[234]",
//			"6[ABCEJS]\\.FUT\\.CME\\.USD\\.201[234]",	
//			"((ES)|(NQ)|(LE)|(HE)|(LS))\\.FUT\\.GLOBEX\\.USD\\.201[23]",	
//			"GE\\.FUT\\.GLOBEX\\.USD\\.20((12)|(13)|(14)|(15)|(16))",	
//			"((ZB)|(ZN)|(ZC)|(ZS)|(ZL)|(ZW)|(ZM))\\.FUT\\.ECBOT\\.USD\\.20((12)|(13))",	
//			"((COIL)|(GOIL))\\.FUT\\.IPE\\.USD\\.20((12)|(13)|(14)|(15)|(16))",	
			"((COIL)|(GOIL))\\.FUT\\.IPE\\.USD\\.20((15)|(17)|(18)|(19))",	
//			"^[ABCD]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[EFGH]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[IJKL]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[MNOP]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[QRST]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[UVWX]((.)|(..)|(...))\\.STK\\.SMART",
//			"^[YT]((.)|(..)|(...))\\.STK\\.SMART",
	};	

	
	private static final BigDecimal MAX_PERC_BTW_SETTLE_AND_OPSTRIKE= new BigDecimal(.1);
	/**
	 * 
	 * @param args 
	 * 	0 boolean showMsgBox
	 *  1 String urlAs - not used
	 *  2 String urlWs  - not used
	 *  3 Integer timeoutValue
	 *  4 TimeUnitType
	 *  5 BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow
	 *  6 String mongoHost
	 *  7 Integer MongoPort
	 *  8 ...  list of regexStrings (8,9,...)
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		// get all command line params
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
		

		
		// ************  CreateMongoSettleImpliedVolDatabase does ALL OF THE WORK ***********************
		CreateMongoSettleImpliedVolDatabase csivd = 
				new CreateMongoSettleImpliedVolDatabase(
						pargs.de, 
				mongoSettleDb,
				sdQuery);
		// create a set of bad names
		Set<String> badNames = new TreeSet<String>();

		// get Settlements of options that will be used whose vols will be
		//    proxies for the "ATM" vol
		Map<String, SettlementDataInterface> atmSettles = 
				csivd.getAtmOptionSettlesForUnderlyings(
						regexStrings, pargs.timeoutValue, 
						pargs.timeUnitType, pargs.maxPercDiffBtwSettleAndOpStrikeToAllow);
		
		// see if some names have not been returned from getting atm options
		// first get all names from all regex strings
		Set<String> allNames = new TreeSet<String>();
		for(String regexString : regexStrings){
			allNames.addAll(mongoSettleDb.getByRegex(regexString).keySet());
		}
		// now see which names did not come back from csivd.getAtmOptionSettlesForUnderlyings
		for(String name : allNames){
			if(!atmSettles.containsKey(name)){
				badNames.add(name);
			}
		}
		// if we should avoid reprocessing stuff that is in the database already
		if(!pargs.processNamesThatAlreadyHaveVols){
			// get rid of names that are already in the database
			Set<String> keySet = atmSettles.keySet();
			Map<String,BigDecimal> alreadyThereMap = 
					pargs.mongoImpliedVolDb.findFromSet(keySet);
			for(String alreadyThereKey : alreadyThereMap.keySet()){
				atmSettles.remove(alreadyThereKey);
			}
		}
		Map<String,DerivativeReturnDisplay> drdMap =
				new TreeMap<String, DerivativeReturnDisplay>(csivd.getAtmImpliedVols(atmSettles));
		CollectionsStaticMethods.prtMapItems(drdMap);
		
		// create implied vol map
		Map<String,BigDecimal> impliedVolMap = new HashMap<String, BigDecimal>();
		for (Entry<String, DerivativeReturnDisplay> entry : drdMap.entrySet()) {
			DerivativeReturnDisplay drd = entry.getValue();
			if(!drd.isValidReturn()){
				badNames.add(entry.getKey());
				continue;
			}
			BigDecimal value = new BigDecimal(
					drd.getValue().doubleValue()).setScale(6,RoundingMode.HALF_EVEN);
			impliedVolMap.put(entry.getKey(), value);
		}

		// write the data
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
			Utils.prtObMess(RunCreateUnderlyingAtmImpliedVolMongoDb.class,MongoDatabaseNames.IMPLIEDVOL_CL + " Has been updated");

		}
		
		
		//*************** END DO FUTURES ATM IMPLIED VOLS ********************************
		Boolean updateVolsForUndersWithNoOptsOverRide  = pargs.updateVolsForUndersWithNoOpts;
		if(pargs.showMsgBox){
			updateVolsForUndersWithNoOptsOverRide = 
					new Boolean(MessageBox.MessageBoxNoChoices("updateVolsForUnderlyingFutWithNoOptions", true));
		}
		if(updateVolsForUndersWithNoOptsOverRide){
			//************** PROCESS FUTURES WITH NO VOL DATA *******************************
			List<String> reject = CreateMongoSettleImpliedVolDatabase.updateVolsForUnderlyingFutWithNoOptions(mongoSettleDb, pargs.mongoImpliedVolDb);
			Utils.prtObErrMess(RunCreateUnderlyingAtmImpliedVolMongoDb.class, "Rejects from processing Futures Vols with no Vol Data:");
			CollectionsStaticMethods.prtListItems(reject);
		//************** END PROCESS FUTURES WITH NO VOL DATA *******************************
		}
		System.exit(0);

	}
	
	static final class ParamArgs {
		final MongoXml<BigDecimal> mongoImpliedVolDb;
		final DerivativeSetEngine de; 
		final Boolean showMsgBox;
		final Integer timeoutValue ;
		final TimeUnit timeUnitType;
		final BigDecimal maxPercDiffBtwSettleAndOpStrikeToAllow ;
		final List<String> regexStringList ;
		final Boolean updateVolsForUndersWithNoOpts ;
		final Boolean processNamesThatAlreadyHaveVols ;
		
		@SuppressWarnings("unchecked")
		ParamArgs(String[] args){
			// *************** get dse from spring beans  ********************************
			Map<String, String> argPairs = 
					Utils.getArgPairsSeparatedByChar(args, "=");
			Map<String,Object> beansMap = 
					DseRunUtils.getBeans(argPairs);
			this.de = DseRunUtils.getDse(beansMap);
			//*************** OPEN OUTPUT MONGO DATABASE ********************************
			this.mongoImpliedVolDb = DseRunUtils.getMongoImpliedVolDb(argPairs);

			//*************** GET ALL OTHER RUNTIME PARAMETERS ********************************
			this.showMsgBox = DseRunUtils.getBooleanParam(argPairs, "showMsgBox", true);
			this.timeoutValue = DseRunUtils.getIntegerParam(argPairs,"timeoutValue",1);
			this.timeUnitType = DseRunUtils.getTimeUnitParam(argPairs,"timeUnitType", TimeUnit.SECONDS);
			this.maxPercDiffBtwSettleAndOpStrikeToAllow = 
					DseRunUtils.getBigDecimalParam(
							argPairs, 
							"maxPercDiffBtwSettleAndOpStrikeToAllow", 
							MAX_PERC_BTW_SETTLE_AND_OPSTRIKE);

			
			// see if the user is suppying a list of regex strings
			//  that determines which shortNames to process
			String regexListXmlFilePath = DseRunUtils.getStringParam(argPairs,"regexListXmlFilePath");
			Class<?> classInPackageWhereFileIsLocated = DseRunUtils.getClassParam(argPairs, "classInPackageWhereFileIsLocated");
			if(regexListXmlFilePath!=null){
				regexStringList = Utils.getXmlData(List.class, classInPackageWhereFileIsLocated, regexListXmlFilePath);
			}else{
				this.regexStringList = null;
			}
			this.updateVolsForUndersWithNoOpts = DseRunUtils.getBooleanParam(argPairs, "updateVolsForUndersWithNoOpts",true);
			this.processNamesThatAlreadyHaveVols = DseRunUtils.getBooleanParam(argPairs, "processNamesThatAlreadyHaveVols",true);
			//*************** END OF GET ALL OTHER RUNTIME PARAMETERS ********************************
			
		}
	}
}
