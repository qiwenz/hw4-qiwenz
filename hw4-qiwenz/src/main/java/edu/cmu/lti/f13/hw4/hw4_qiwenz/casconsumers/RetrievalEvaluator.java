package edu.cmu.lti.f13.hw4.hw4_qiwenz.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_qiwenz.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_qiwenz.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_qiwenz.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** global dictionary **/
  /**
   * HashSet can automatically judge whether there are duplicates and store only once for each word.
   **/
  /** At first, I try to use ArrayList to build a dictionary and it causes many unknown errors. **/
  public HashSet<String> globalDictionary;

  /** to store all the lines (both questions and answers). **/
  public ArrayList<HashMap> lineList;

  /** to store the scores for each answer **/
  public double[] score;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    globalDictionary = new HashSet<String>();

    lineList = new ArrayList<HashMap>();

    score = new double[200];

  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());

      // Do something useful here

      // create a global dictionary using HashSet
      // HashSet can automatically judge whether there are duplicates and store only once for each word
      // At first, I try to use ArrayList to build a dictionary and it causes many unknown errors
      for (Token token : tokenList) {
        globalDictionary.add(token.getText());
      }

      // extract words and corresponding frequency from tokenList and store them in hashmap
      HashMap<String, Integer> maptemp = new HashMap<String, Integer>();
      for (Token token : tokenList) {
        maptemp.put(token.getText(), token.getFrequency());
      }
      lineList.add(maptemp);
    }

  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure

    /*
     * Iterator<String> iterator=dictionary.iterator(); while(iterator.hasNext()){
     * System.out.println(iterator.next()); }
     */
    // System.out.println(qIdList.size());
    int numberOfQuestions = qIdList.get(qIdList.size() - 1);

    int sequenceOfLine = 0;

    // create two hashmap to store questions and answers
    HashMap<String, Integer> questionMap = new HashMap<String, Integer>();
    HashMap<String, Integer> answerMap = new HashMap<String, Integer>();

    for (HashMap line : lineList) {
      if (relList.get(sequenceOfLine) == 99) {
        questionMap = lineList.get(sequenceOfLine);
      } else {
        answerMap = lineList.get(sequenceOfLine);
        score[sequenceOfLine] = computeCosineSimilarity(questionMap, answerMap);
      }
      sequenceOfLine++;
    }

    // TODO :: compute the rank of retrieved sentences
    int answerNumber = 0;
    int targetQid = 0;

    int[] rank = new int[numberOfQuestions + 1];
    for (int i = 0; i < sequenceOfLine; i++) {
      if (relList.get(i) == 99) {
        answerNumber = 0;
      } else {
        answerNumber++;
        if (relList.get(i) == 1) {
          targetQid = qIdList.get(i);
          rank[targetQid] = 1;
          for (int j = 0; j < sequenceOfLine; j++) {
            if (qIdList.get(j) == targetQid && score[j] > score[i]) {
              rank[targetQid]++;
            }
          }
          System.out.println("Score:" + score[i] + "  Rank=" + rank[targetQid] + "  Rel=1"
                  + " Qid=" + targetQid + " sent" + answerNumber);
        }
      }
    }

    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(rank);
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    // TODO :: compute cosine similarity between two sentences
    int QA = 0;
    int absoluteQ = 0;
    int absoluteA = 0;

    for (String token : globalDictionary) {
      if (queryVector.containsKey(token) && docVector.containsKey(token)) {
        QA = QA + queryVector.get(token) * docVector.get(token);
        absoluteQ = absoluteQ + queryVector.get(token) * queryVector.get(token);
        absoluteA = absoluteA + docVector.get(token) * docVector.get(token);
      } else if (queryVector.containsKey(token)) {
        absoluteQ = absoluteQ + queryVector.get(token) * queryVector.get(token);
      } else if (docVector.containsKey(token)) {
        absoluteA = absoluteA + docVector.get(token) * docVector.get(token);
      }
    }

    cosine_similarity = QA / Math.sqrt(absoluteQ * absoluteA);
    // System.out.println(cosine_similarity);
    return cosine_similarity;

  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(int[] rank) {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
    for (int i = 1; i < rank.length; i++) {
      metric_mrr = metric_mrr + (1 / rank[i]);
    }
    metric_mrr = metric_mrr / (rank.length -1);
    return metric_mrr;
  }

}
