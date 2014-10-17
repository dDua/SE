/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	
	int cmutf = 0;
	String field ;
	
	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	public QryopSlScore(Qryop q, Double weight) {
		this.args.add(q);
	    this.weight = weight;
	  }
	
	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (this.args.size() == 0) {
			System.out
					.println("Null arguments passed to the #AND query operator. Check stemming results...");
		}

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (evaluateUnrankedBoolean(r));
		else if (r instanceof RetrievalModelRankedBoolean)
			return (evaluateRankedBoolean(r));
		else if (r instanceof RetrievalModelTfidfRanked)
			return (evaluateTfidfRanked(r));
		else if (r instanceof RetrievalModelCosineSimilarityRanked)
			return (evaluateCosineSimilarity(r));
		else if (r instanceof RetrievalModelFieldWeightRank)
			return (evaluateFieldRanking(r));
		else if (r instanceof RetrievalModelBM25)
			return (evaluateBM25(r));
		else if (r instanceof RetreivalModelIndri)
			return (evaluateindri(r));
		return null;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateUnrankedBoolean(RetrievalModel r)
			throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) 1.0);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.postings.size(); i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			int termFreq = result.invertedList.postings.get(i).tf;
			
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) (termFreq ));

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models with tf-idf
	 * ranked
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateTfidfRanked(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			int termFreq = result.invertedList.postings.get(i).tf;
			float inversedDocumentFreq = (((RetrievalModelTfidfRanked) r).numDocsInIndex)
					/ (float) result.invertedList.postings.size();
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) (termFreq * Math.log(inversedDocumentFreq)));

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval model with cosine
	 * similarity
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateCosineSimilarity(RetrievalModel r)
			throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			int termFreq = result.invertedList.postings.get(i).tf;

			result.docScores.add(result.invertedList.postings.get(i).docid,
					termFreq);

		}
		return result;
	}

	/**
	 * Evaluate the query operator for best match retrieval model with okapi
	 * BM25
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBM25(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);	
		
		RetrievalModelBM25 rm = (RetrievalModelBM25) r;
		
			// check below for if term is in half of the doc then assign score
			// to 0
			double idfwgt = Math.log((QryEval.READER.numDocs()
					- result.invertedList.df + 0.5)
					/ (result.invertedList.df + 0.5));
			if (idfwgt<0)
				idfwgt=0;
		
			double qtf = 1.0;

			Vector<InvList.DocPosting> posting = new Vector<InvList.DocPosting>();
			posting = result.invertedList.postings;
			for (int j = 0; j < posting.size(); j++) {

				double avglen = QryEval.READER.getSumTotalTermFreq(result.invertedList.field)
						/ (float) QryEval.READER.getDocCount(result.invertedList.field);
				
				double avglend = (QryEval.dls.getDocLength(result.invertedList.field,posting.get(j).docid) / avglen);
				double den = rm.k1 * ((1 - rm.b) + rm.b *avglend);
				double tfwgt = (result.invertedList.postings.get(j).tf )
						/ (result.invertedList.postings.get(j).tf + den);
				double score = tfwgt  * idfwgt;
				
				result.docScores.add(posting.get(j).docid, score);
				
			
		}
		return result;

	}

	/**
	 * Evaluate the query operator for best match retrieval model with Indri
	 * 
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateindri(RetrievalModel r) throws IOException {

		// Evaluate the query argument.
		
		QryResult result = args.get(0).evaluate(r);
		this.cmutf = result.invertedList.ctf;
		this.field = result.invertedList.field;
		RetreivalModelIndri rm = (RetreivalModelIndri) r;			
			
			Vector<InvList.DocPosting> posting = new Vector<InvList.DocPosting>();
			posting = result.invertedList.postings;
			for (int j = 0; j < posting.size(); j++) {
				double tfq_d = posting.get(j).tf;
				//double len_termC = QryEval.READER.getSumTotalTermFreq(result.invertedList.field); 
				double pMLE_qiC = result.invertedList.ctf / (float)QryEval.READER.getSumTotalTermFreq(result.invertedList.field);
				
				double score = rm.lambda*((tfq_d +rm.mu *pMLE_qiC) / (QryEval.dls.getDocLength(result.invertedList.field,posting.get(j).docid) + rm.mu))
						+ (1-rm.lambda)*pMLE_qiC;				
				
				result.docScores.add(posting.get(j).docid, score);	
		}
		return result;

	}

	/**
	 * Evaluate the query operator for boolean retrieval model with 
	 * field ranking
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateFieldRanking(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);
		if (args.get(0) instanceof QryopIlNear) {
			return result;

		} else {
			QryResult titleField = getFieldTerms(r, "title");
			QryResult inlinkField = getFieldTerms(r, "inlink");

			// Each pass of the loop computes a score for one document. Note:
			// If the evaluate operation above returned a score list (which is
			// very possible), this loop gets skipped.

			for (int i = 0; i < result.invertedList.df; i++) {

				int score = result.invertedList.postings.get(i).tf;

				if ((titleField.invertedList.df != 0)
						&& titleField.invertedList
								.containsDocument(result.invertedList.postings
										.get(i).docid)) {
					score = score + 8;
				}

				if ((inlinkField.invertedList.df != 0)
						&& inlinkField.invertedList
								.containsDocument(result.invertedList.postings
										.get(i).docid)) {
					score = score + 16;
				}

				result.docScores.add(result.invertedList.postings.get(i).docid,
						(score));

			}

			result = normalizeScores(result);
			return result;
		}

	}

	/**
	 * Used to bring down the score values to same scale
	 * 
	 * @param result
	 *            the scores to be normalized
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult normalizeScores(QryResult result) {
		double mean = 0.0;
		for (int q = 0; q < result.docScores.scores.size(); q++) {
			mean += result.docScores.scores.get(q).getScore();
		}
		mean = mean / result.docScores.scores.size();

		double standardDevn = 0.0;
		for (int q = 0; q < result.docScores.scores.size(); q++) {
			standardDevn += (result.docScores.scores.get(q).getScore() - mean)
					* (result.docScores.scores.get(q).getScore() - mean);
		}
		standardDevn = Math.sqrt(standardDevn / result.docScores.scores.size());

		ScoreList scorelist = new ScoreList();

		for (int q = 0; q < result.docScores.scores.size(); q++) {
			scorelist
					.add(result.docScores.scores.get(q).getDocID(),
							((result.docScores.scores.get(q).getScore() - mean) / standardDevn) + 100);
			if (((result.docScores.scores.get(q).getScore() - mean) / standardDevn) + 100 < 0) {
				System.out.println("Negative score values");
			}
		}

		result.docScores = scorelist;
		return result;
	}

	/**
	 * Fetched score for the fields to be boosted
	 * 
	 * @param r
	 *            retreival model
	 * @param field
	 *            field name
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult getFieldTerms(RetrievalModel r, String field) {
		QryResult fieldResult = null;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(0) instanceof QryopIlTerm) {
				try {
					fieldResult = new QryopIlTerm(
							((QryopIlTerm) args.get(0)).getTerm(), field)
							.evaluate(r);
				} catch (IOException e) {
					System.out
							.println("Caugh exception in getting field terms for query term");
					e.printStackTrace();
				}
			}
		}
		return fieldResult;
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetreivalModelIndri){
			RetreivalModelIndri rm = (RetreivalModelIndri)r;
			 
		double pMLE_qiC = this.cmutf / (float)QryEval.READER.getSumTotalTermFreq(this.field);		
		double score = rm.lambda*((rm.mu * pMLE_qiC)/ (QryEval.dls.getDocLength(this.field,(int)docid) + rm.mu))
				+ (1-rm.lambda)*pMLE_qiC;	
		return score;
		} else

		return 0.0;
	}

	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}
}
