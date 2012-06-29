package experiments;

import gate.Corpus;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.ExtensionFileFilter;
import gate.util.GateException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class GateExperiment {
	/**
	 * @param arguments
	 * @throws GateException
	 */
	public static void main(String[] args) {
		//
		try {
			Gate.init();

			Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR).toURI().toURL());
			// create a serial analyser controller to run ANNIE with 
			SerialAnalyserController annieController = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController",Factory.newFeatureMap(), Factory.newFeatureMap(),"ANNIE");
			// load each PR as defined in ANNIEConstants
			for (int i = 0; i < ANNIEConstants.PR_NAMES.length; i++) {
				// use default parameters
				FeatureMap params = Factory.newFeatureMap();
				ProcessingResource pr = (ProcessingResource) Factory
						.createResource(ANNIEConstants.PR_NAMES[i], params);
				// add the PR to the pipeline controller
				annieController.add(pr);
			} // for each ANNIE PR

			// Tell ANNIE’s controller about the corpus you want to run on
			Corpus corpus;
			try {
				corpus = Factory.newCorpus("MyDocs");
				File directory = new File(
						"/home/developer/dataset_rev/freshrevs/train/pos/");
				ExtensionFileFilter filter = new ExtensionFileFilter(
						"Text files", "txt");
				URL url = directory.toURL();
				corpus.populate(url, filter, null, false);
				annieController.setCorpus(corpus);
			} catch (ResourceInstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// makes an IOException
				e.printStackTrace();
			}
		} catch (GateException ex) {
			Logger.getLogger(GateExperiment.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}