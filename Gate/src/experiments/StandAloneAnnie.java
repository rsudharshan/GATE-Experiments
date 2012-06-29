/*
 *  StandAloneAnnie.java
 *
 *
 * Copyright (c) 2000-2001, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  hamish, 29/1/2002
 *
 *  $Id: StandAloneAnnie.java,v 1.6 2006/01/09 16:43:22 ian Exp $
 */

package experiments;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.net.*;

import gate.*;
import gate.creole.*;
import gate.util.*;
import gate.corpora.RepositioningInfo;

public class StandAloneAnnie {

	private SerialAnalyserController annieController;

	public void initAnnie() throws GateException {
		Out.prln("Initialising ANNIE...");

		// create a serial analyser controller to run ANNIE with
		annieController = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController",Factory.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_"+ Gate.genSym());
		// load each PR as defined in ANNIEConstants
		for (int i = 0; i < ANNIEConstants.PR_NAMES.length; i++) {
			FeatureMap params = Factory.newFeatureMap(); // use default parameters
			System.out.println("PR : " +ANNIEConstants.PR_NAMES[i] +" loading");
			ProcessingResource pr = (ProcessingResource) Factory.createResource(ANNIEConstants.PR_NAMES[i], params);

			// add the PR to the pipeline controller
			annieController.add(pr);
		} // for each ANNIE PR

		Out.prln("...ANNIE loaded");
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

	public static void main(String args[]) throws GateException, IOException {
		// initialise the GATE library
		Out.prln("Initialising GATE...");
		Gate.init();

		// Load ANNIE plugin
		File gateHome = Gate.getGateHome();
		File pluginsHome = new File(gateHome, "plugins");
		Gate.getCreoleRegister().registerDirectories(
				new File(pluginsHome, "ANNIE").toURL());
		Out.prln("...GATE initialised");

		// initialise ANNIE (this may take several minutes)
		StandAloneAnnie annie = new StandAloneAnnie();
		annie.initAnnie();

		// create a GATE corpus from a directory
		Corpus corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
		File[] files=new File("/home/developer/Downloads/googleresults/txtfilesofclasses/sampletextfiles/").listFiles();
		for(File s:files) {
			URL u=null;
			try{
			u = new URL("file://"+s.getAbsolutePath());
			FeatureMap params = Factory.newFeatureMap();
			params.put("sourceUrl", u);
			params.put("preserveOriginalContent", new Boolean(true));
			params.put("collectRepositioningInfo", new Boolean(true));
			Out.prln("Creating doc for " + u);
			Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
			corpus.add(doc);
			}
			catch(MalformedURLException m)
			{
				System.out.print(u);
				m.printStackTrace();
			}
		} // for each of args

		// tell the pipeline about the corpus and run it
		annie.setCorpus(corpus);
		annie.execute();

		// for each document, get an XML document with the
		// person and location names added
		Iterator iter = corpus.iterator();
		int count = 0;
		String startTagPart_1 = "<span GateID=\"";
		String startTagPart_2 = "\" title=\"";
		String startTagPart_3 = "\" style=\"color:Red;\">";
		String endTag = "</span>";

		while (iter.hasNext()) {
			Document doc = (Document) iter.next();
			AnnotationSet defaultAnnotSet = doc.getAnnotations();
			Set annotTypesRequired = new HashSet();
			annotTypesRequired.add("Token");
			
			//annotTypesRequired.add("Location");
			Set<Annotation> peopleAndPlaces = new HashSet<Annotation>(
					defaultAnnotSet.get(annotTypesRequired));

			FeatureMap features = doc.getFeatures();
			String originalContent = (String) features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
			RepositioningInfo info = (RepositioningInfo) features.get(GateConstants.DOCUMENT_REPOSITIONING_INFO_FEATURE_NAME);
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
					FeatureMap fm=currAnnot.getFeatures();
					System.out.println("Content analysing");
					for(Map.Entry<Object, Object> map:fm.entrySet())
					{   if(map.getKey().equals("category"))
						System.out.println(map.getKey() + "  "+map.getValue() );
					}
					System.out.println();
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
			} // if - should generate
			else if (originalContent != null) {
				Out.prln("OrigContent existing. Generate file...");

				Iterator it = peopleAndPlaces.iterator();
				Annotation currAnnot;
				SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

				while (it.hasNext()) {
					currAnnot = (Annotation)it.next();
					FeatureMap fm=currAnnot.getFeatures();
					System.out.println("Content analysing");
					for(Map.Entry<Object, Object> map:fm.entrySet())
					{ 
						if(map.getKey().equals("category"))
						System.out.println(map.getKey() + "  "+map.getValue() );
					}
					sortedAnnotations.addSortedExclusive(currAnnot);
				} // while

				StringBuffer editableContent=new StringBuffer(originalContent);
				long insertPositionEnd;
				long insertPositionStart;
				// insert annotation tags backward
				Out.prln("Unsorted annotations count: "	+ peopleAndPlaces.size());
				Out.prln("Sorted annotations count: " + sortedAnnotations.size());
				for (int i = sortedAnnotations.size()-1; i>=0; --i) {
					currAnnot = (Annotation) sortedAnnotations.get(i);
					insertPositionStart = currAnnot.getStartNode().getOffset().longValue();
					insertPositionEnd = currAnnot.getEndNode().getOffset().longValue();
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
		} // for each doc
	} // main

	/**
   *
   */
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

