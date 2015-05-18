package com.billybyte.derivativesetengine.run.debundles.fromspring;

//import org.springframework.context.ApplicationContext;
//import org.springframework.context.support.ClassPathXmlApplicationContext;


import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.derivativesetengine.run.debundles.DeBundleQueries;
//import com.billybyte.derivativesetengine.models.DerivativeSetEngine;
//import com.billybyte.derivativesetengine.resources.ResourceClass;
//import com.billybyte.derivativesetengine.run.debundles.DeBundleQueries;
import com.billybyte.dse.DerivativeSetEngine;

public class DeBundleFromSpring {
	private static final String VARBEANS_FILENAME = "varBeans.xml";
	
	private final DerivativeSetEngine derivativeSetEngine;
	/**
	 * 
	 * @param pathOfStringBeansXml
	 * @param beanNameOfDeBundle
	 */
	public DeBundleFromSpring(
			String pathOfStringBeansXml,
			String beanNameOfDeBundle){
		DeBundleQueries deb = Utils.springGetBean(DeBundleQueries.class, pathOfStringBeansXml,  beanNameOfDeBundle);
//		ApplicationContext context = new  FileSystemXmlApplicationContext(pathOfStringBeansXml);
//		Object o = context.getBean(beanNameOfDeBundle);
//		DeBundleQueries deb =
//				(DeBundleQueries)context.getBean(beanNameOfDeBundle);

		this.derivativeSetEngine = deb.getDerivativeSetEngine();

	}
	
	
	/**
	 * 
	 * @param beanNameOfDeBundle (like "deBundleQueries")
	 */
	public DeBundleFromSpring(
			String beanNameOfDeBundle){
		DeBundleQueries deb = Utils.springGetBean(DeBundleQueries.class, VARBEANS_FILENAME,  beanNameOfDeBundle);
//		String path = ResourceClass.class.getPackage().getName().replace(".","/")+"/"+ ResourceClass.VARBEANS_FILENAME;
//		ApplicationContext context = new  ClassPathXmlApplicationContext(
//				path);
//		DeBundleQueries deb =
//				(DeBundleQueries)context.getBean(beanNameOfDeBundle);
		this.derivativeSetEngine = deb.getDerivativeSetEngine();

	}
	
	/**
	 * defaults to varBean file of ResourceClass.VARBEANS_FILENAME and
	 *    xml bean object id of "deBundleQueries"
	 */
	public DeBundleFromSpring(){
		DeBundleQueries deb = Utils.springGetBean(DeBundleQueries.class, VARBEANS_FILENAME,  "deBundleQueries");
		this.derivativeSetEngine = deb.getDerivativeSetEngine();

		
	}

	/**
	 * defaults to varBean file of ResourceClass.VARBEANS_FILENAME and
	 *    xml bean object id of "deBundleQueries"
	 */
	public DeBundleFromSpring(String beanXmlFileName, Boolean placeholder){
		DeBundleQueries deb = Utils.springGetBean(DeBundleQueries.class, beanXmlFileName,  "deBundleQueries");
		this.derivativeSetEngine = deb.getDerivativeSetEngine();
//		String path = ResourceClass.class.getPackage().getName().replace(".","/")+"/"+ beanXmlFileName;
//		ApplicationContext context = new  ClassPathXmlApplicationContext(
//				path);
//		DeBundleQueries deb =
//				(DeBundleQueries)context.getBean("deBundleQueries");
//		this.derivativeSetEngine = deb.getDerivativeSetEngine();

	}

	public DerivativeSetEngine getDerivativeSetEngine() {
		return derivativeSetEngine;
	}
	
}
