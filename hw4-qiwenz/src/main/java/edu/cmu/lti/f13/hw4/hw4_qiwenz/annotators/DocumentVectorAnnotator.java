package edu.cmu.lti.f13.hw4.hw4_qiwenz.annotators;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.impl.Util;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f13.hw4.hw4_qiwenz.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_qiwenz.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_qiwenz.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();

    // TO DO: construct a vector of tokens and update the tokenList in CAS
    String docT=docText.toLowerCase();
    //String[] wordsep = docText.split(" ");
    String[] wordsep = docT.split(" ");

    Token[] tempArray = new Token[wordsep.length];
    for (int i = 0; i < wordsep.length; i++) {
      tempArray[i] = new Token(jcas);
      tempArray[i].setText(wordsep[i]);
    }

    // compute the frequency of each token and set it.
    for (int i = 0; i < wordsep.length; i++) {
      int freq = 0;
      for (int j = 0; j < wordsep.length; j++) {
        if (wordsep[i].equals(wordsep[j])) {
          freq++;
        }
      }
      tempArray[i].setFrequency(freq);
      //System.out.println(tempArray[i].getText()+tempArray[i].getFrequency());
    }

    ArrayList<Token> tokenArrayList = new ArrayList(Arrays.asList(tempArray));

    // convert arraylist to FSList using provided "utils"
    FSList tokenList = Utils.fromCollectionToFSList(jcas, tokenArrayList);

    doc.setTokenList(tokenList);
  }

}
