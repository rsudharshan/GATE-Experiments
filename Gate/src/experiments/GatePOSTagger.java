package experiments;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.net.*;

import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException;

import experiments.StandAloneAnnie.SortedAnnotationList;

import gate.*;
import gate.creole.*;
import gate.util.*;
import gate.corpora.RepositioningInfo;
import gate.creole.*;



public class GatePOSTagger {

	private static int count=0;
	private SerialAnalyserController annieController;

	public void initAnnie() throws GateException {
		Out.prln("Initialising ANNIE...");
		// create a serial analyser controller to run ANNIE with
		annieController = (SerialAnalyserController) Factory.createResource(
				"gate.creole.SerialAnalyserController",Factory.newFeatureMap(), Factory.newFeatureMap());
		// load each PR as defined in ANNIEConstants
		FeatureMap params = Factory.newFeatureMap(); // use default parameters
		ProcessingResource pr1 = (ProcessingResource) Factory.createResource("gate.opennlp.OpenNlpSentenceSplit", params);
		ProcessingResource pr2 = (ProcessingResource) Factory.createResource("gate.opennlp.OpenNlpTokenizer", params);
		ProcessingResource pr3 = (ProcessingResource) Factory.createResource("gate.opennlp.OpenNlpPOS", params);
		// add the PR to the pipeline controller
				
		annieController.add(pr1);
		annieController.add(pr2);
		annieController.add(pr3);
		Out.prln("OpenNLP POSTagger loaded");
	} // initAnnie()

	/** Tell ANNIE's controller about the corpus you want to run on */
	public void setCorpus(Corpus corpus) {
		annieController.setCorpus(corpus);
	} // setCorpus

	/** Run ANNIE */
	public void execute() throws GateException {
		Out.prln("Running ANNIE...");
		annieController.execute();
		Out.prln("...ANNIE complete");
	} // execute()

	public static void main(String args[])  {
		// Initialize the GATE library
		Out.prln("Initialising GATE...");
		try
		{
		Gate.init();
		// Load ANNIE plugin
		File gateHome = Gate.getGateHome();
		File pluginsHome = new File(gateHome, "plugins");
	    Gate.getCreoleRegister().registerDirectories( new File(Gate.getGateHome().getAbsolutePath() + "/plugins/OpenNLP").toURI().toURL());
		//Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "OpenNLP").toURL());
		Out.prln("...GATE initialised");
		
		// Initialize ANNIE (this may take several minutes)
		GatePOSTagger postag = new GatePOSTagger();
		postag.initAnnie();
		
		Corpus corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
		URL u=new URL("file:///home/developer/Downloads/googleresults/txtfilesofclasses/nlp/nlp11.txt");
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl", u);
		params.put("preserveOriginalContent", new Boolean(true));
		params.put("collectRepositioningInfo", new Boolean(true));
		
		Document sdoc=(Document)Factory.createResource("gate.corpora.DocumentImpl", params);
	    corpus.add(sdoc);
	    postag.setCorpus(corpus);
	    postag.execute();
		Iterator iter = corpus.iterator();

	    String startTagPart_1 = "<span GateID=\"";
		String startTagPart_2 = "\" title=\"";
		String startTagPart_3 = "\" style=\"background:Silver;\">";
		String endTag = "</span>";
		
		while (iter.hasNext()) {
			Document doc = (Document) iter.next();
			AnnotationSet defaultAnnotSet = doc.getAnnotations();
			Set annotTypesRequired = new HashSet();
			annotTypesRequired.add("Person");
			//annotTypesRequired.add("Location");
			Set<Annotation> peopleAndPlaces = new HashSet<Annotation>(
					defaultAnnotSet.get(annotTypesRequired));

			FeatureMap features = doc.getFeatures();
			String originalContent = (String) features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
			RepositioningInfo info = (RepositioningInfo) features
					.get(GateConstants.DOCUMENT_REPOSITIONING_INFO_FEATURE_NAME);

			++count;
			
		File file = new File("StANNIE_" + count + ".HTML");
		Out.prln("File name: '" + file.getAbsolutePath() + "'");
		if (originalContent != null && info != null) {
			Out.prln("OrigContent and reposInfo existing. Generate file...");

			Iterator it = peopleAndPlaces.iterator();
			Annotation currAnnot;
			SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

			while (it.hasNext()) {
				currAnnot = (Annotation) it.next();
				sortedAnnotations.addSortedExclusive(currAnnot);
			} // while

			StringBuffer editableContent = new StringBuffer(originalContent);
			long insertPositionEnd;
			long insertPositionStart;
			// insert annotation tags backward
			Out.prln("Unsorted annotations count: "	+ peopleAndPlaces.size());
			Out.prln("Sorted annotations count: "+ sortedAnnotations.size());
			for (int i = sortedAnnotations.size() - 1; i >= 0; --i) {
				currAnnot = (Annotation) sortedAnnotations.get(i);
				insertPositionStart = currAnnot.getStartNode().getOffset()
						.longValue();
				insertPositionStart = info
						.getOriginalPos(insertPositionStart);
				insertPositionEnd = currAnnot.getEndNode().getOffset()
						.longValue();
				insertPositionEnd = info.getOriginalPos(insertPositionEnd,
						true);
				if (insertPositionEnd != -1 && insertPositionStart != -1) {
					editableContent.insert((int) insertPositionEnd, endTag);
					editableContent.insert((int) insertPositionStart,startTagPart_3);
					editableContent.insert((int) insertPositionStart,currAnnot.getType());
					editableContent.insert((int) insertPositionStart,startTagPart_2);
					editableContent.insert((int) insertPositionStart,currAnnot.getId().toString());
					editableContent.insert((int) insertPositionStart,startTagPart_1);
				} // if
			} // for
			FileWriter writer = new FileWriter(file);
			writer.write(editableContent.toString());
			writer.close();
		} // if - should generate
		else if (originalContent != null) {
			Out.prln("OrigContent existing. Generate file...");
			Iterator it = peopleAndPlaces.iterator();
			Annotation currAnnot;
			SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

			while (it.hasNext()) {
				currAnnot = (Annotation) it.next();
				sortedAnnotations.addSortedExclusive(currAnnot);
			} // while

			StringBuffer editableContent = new StringBuffer(originalContent);
			long insertPositionEnd;
			long insertPositionStart;
			// insert annotation tags backward
			Out.prln("Unsorted annotations count: "	+ peopleAndPlaces.size());
			Out.prln("Sorted annotations count: "   + sortedAnnotations.size());
			for (int i = sortedAnnotations.size() - 1; i >= 0; --i) {
				currAnnot = (Annotation) sortedAnnotations.get(i);
				insertPositionStart = currAnnot.getStartNode().getOffset()
						.longValue();
				insertPositionEnd = currAnnot.getEndNode().getOffset()
						.longValue();
				if (insertPositionEnd != -1 && insertPositionStart != -1) {
					editableContent.insert((int) insertPositionEnd, endTag);
					editableContent.insert((int) insertPositionStart,
							startTagPart_3);
					editableContent.insert((int) insertPositionStart,
							currAnnot.getType());
					editableContent.insert((int) insertPositionStart,
							startTagPart_2);
					editableContent.insert((int) insertPositionStart,
							currAnnot.getId().toString());
					editableContent.insert((int) insertPositionStart,
							startTagPart_1);
				} // if
			} // for

			FileWriter writer = new FileWriter(file);
			writer.write(editableContent.toString());
			writer.close();
		} else {
			Out.prln("Content : " + originalContent);
			Out.prln("Repositioning: " + info);
		}

		String xmlDocument = doc.toXml(peopleAndPlaces, false);
		String fileName = new String("StANNIE_toXML_" + count + ".HTML");
		FileWriter writer = new FileWriter(fileName);
		writer.write(xmlDocument);
		writer.close();

		// do something useful with the XML here!
		// Out.prln("'"+xmlDocument+"'");
	 // for each doc
		}	}
		catch (GateException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			}
	}
	
	

	public static class SortedAnnotationList extends Vector {
		public SortedAnnotationList() {
			super();
		} // SortedAnnotationList

		public boolean addSortedExclusive(Annotation annot) {
			Annotation currAnot = null;

			// overlapping check
			for (int i = 0; i < size(); ++i) {
				currAnot = (Annotation) get(i);
				if (annot.overlaps(currAnot)) {
					return false;
				} // if
			} // for

			long annotStart = annot.getStartNode().getOffset().longValue();
			long currStart;
			// insert
			for (int i = 0; i < size(); ++i) {
				currAnot = (Annotation) get(i);
				currStart = currAnot.getStartNode().getOffset().longValue();
				if (annotStart < currStart) {
					insertElementAt(annot, i);
					/*
					 * Out.prln("Insert start: "+annotStart+" at position: "+i+
					 * " size="+size()); Out.prln("Current start: "+currStart);
					 */
					return true;
				} // if
			} // for

			int size = size();
			insertElementAt(annot, size);
			// Out.prln("Insert start: "+annotStart+" at size position: "+size);
			return true;
		} // addSorted
	} // SortedAnnotationList
} // class StandAloneAnnie
