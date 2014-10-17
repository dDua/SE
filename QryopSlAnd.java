/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlAnd extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlAnd(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param {q} q The query argument (query operator) to append.
	 * @return void
	 * @throws IOException
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluates the query operator, including any child operators and returns
	 * the result.
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
		else if (r instanceof RetrievalModelTfidfRanked)
			return (evaluateTfIdfRanked(r));
		else if (r instanceof RetrievalModelCosineSimilarityRanked)
			return (evaluateCosineSimilarityfRanked(r));
		else if (r instanceof RetrievalModelFieldWeightRank)
			return (evaluateFieldRankBoolean(r));
		else if (r instanceof RetreivalModelIndri)
			return (evaluateIndri(r));
		else if(r instanceof RetrievalModelRankedBoolean)
			return (evaluateTfIdfRanked(r));
		return null;
	}

	/**
	 * Evaluates the query operator for boolean retrieval models, including any
	 * child operators and returns the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateUnrankedBoolean(RetrievalModel r)
			throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		// Sort the arguments so that the shortest lists are first. This
		// improves the efficiency of exact-match AND without changing
		// the result.

		for (int i = 0; i < (this.daatPtrs.size() - 1); i++) {
			for (int j = i + 1; j < this.daatPtrs.size(); j++) {
				if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs
						.get(j).scoreList.scores.size()) {
					ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
					this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
					this.daatPtrs.get(j).scoreList = tmpScoreList;
				}
			}
		}

		// Exact-match AND requires that ALL scoreLists contain a
		// document id. Use the first (shortest) list to control the
		// search for matches.

		// Named loops are a little ugly. However, they make it easy
		// to terminate an outer loop from within an inner loop.
		// Otherwise it is necessary to use flags, which is also ugly.

		Qryop.DaaTPtr ptr0 = this.daatPtrs.get(0);

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
			double docScore = 1.0;

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				Qryop.DaaTPtr ptrj = this.daatPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						break; // ptrj matches ptr0Docid
					}
				}
			}
			result.docScores.add(ptr0Docid, docScore);
			// The ptr0Docid matched all query arguments, so save it.
		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model
	 * which uses TF-IDF score for ranking the documents
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateTfIdfRanked(RetrievalModel r) throws IOException {

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		//sort the lists to get the smallest one first
		for (int i = 0; i < (this.daatPtrs.size() - 1); i++) {
			for (int j = i + 1; j < this.daatPtrs.size(); j++) {
				if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs
						.get(j).scoreList.scores.size()) {
					ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
					this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
					this.daatPtrs.get(j).scoreList = tmpScoreList;
				}
			}
		}
		Qryop.DaaTPtr ptr0 = this.daatPtrs.get(0);

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
			double docScore = Double.MAX_VALUE;

			// Do the other query arguments have the ptr0Docid?

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				Qryop.DaaTPtr ptrj = this.daatPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						//take minimum of tf-idf of all queries 
						docScore = Math.min(docScore, Math.min(
								ptr0.scoreList.getDocidScore(ptr0.nextDoc),
								ptrj.scoreList.getDocidScore(ptrj.nextDoc)));
						break; // ptrj matches ptr0Docid
					}
				}
			}
			result.docScores.add(ptr0Docid, docScore);
			// The ptr0Docid matched all query arguments, so save it.

		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model using 
	 * cosine similarity between the term and document vectors
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateCosineSimilarityfRanked(RetrievalModel r)
			throws IOException {

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		//sort the lists to get the smallest one at index 0
		for (int i = 0; i < (this.daatPtrs.size() - 1); i++) {
			for (int j = i + 1; j < this.daatPtrs.size(); j++) {
				if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs
						.get(j).scoreList.scores.size()) {
					ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
					this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
					this.daatPtrs.get(j).scoreList = tmpScoreList;
				}
			}
		}
		
		//find the distinct query terms (or features) of the query vector
		double queryVectorValue = 1.0;
		Map<String, Double> termVector = new HashMap<String, Double>();

		for (int j = 0; j < this.daatPtrs.size(); j++) {
			try {
				String query = this.toString();
				query = query.toLowerCase().replace("#or", "")
						.replace("#near", "").replace("#and", "")
						.replace("[", "").replace("]", "").replace(")", "")
						.replace("(", "").replace("#score", "")
						.replace(",", "");
				query = query.trim();

				String[] toks = query.split(" ");
				int p = 0;
				while (p < toks.length) {
					if (toks[p].isEmpty() || toks[p].contains("/")) {
						p++;
						continue;
					} else if (termVector.containsKey(toks[p]))
						termVector.put(toks[p], termVector.get(toks[p])
								+ queryVectorValue);
					else
						termVector.put(toks[p], queryVectorValue);
					p++;
				}
			} catch (Exception e) {
				System.out
						.println("Caught exception while finding magnitude of query term");
			}
		}
		//calculate the magnitude of the query vector
		int k = 0;
		double magnitudeQueryVector = 0.0;		
		double magnitudeDocumentVector = 0.0;
		while (k < termVector.size()) {
			double kthValue = new ArrayList<Double>(termVector.values()).get(k);
			magnitudeQueryVector += kthValue * kthValue;
			k++;
		}
		magnitudeQueryVector = Math.sqrt(magnitudeQueryVector);

		Qryop.DaaTPtr ptr0 = this.daatPtrs.get(0);

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
			double docScore = Double.MAX_VALUE;

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				Qryop.DaaTPtr ptrj = this.daatPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						// the values in document vector are given by idf scores
						// the values in query vector are given by tf scores
						double inverseDocFreq0 = Math
								.log(((RetrievalModelCosineSimilarityRanked) r).numDocsInIndex
										/ 1	+ (double) ptr0.scoreList.scores.size());
						double inverseDocFreqj = Math
								.log(((RetrievalModelCosineSimilarityRanked) r).numDocsInIndex
										/ 1 + (double) ptrj.scoreList.scores.size());
						double docScore0 = ptr0.scoreList
								.getDocidScore(ptr0.nextDoc) * inverseDocFreq0;
						double docScorej = ptrj.scoreList
								.getDocidScore(ptrj.nextDoc) * inverseDocFreqj;

						magnitudeDocumentVector += inverseDocFreqj* inverseDocFreqj +
								inverseDocFreq0*inverseDocFreq0;
						docScore = (docScore+docScore0 + docScorej);
						
						break; // ptrj matches ptr0Docid
					}
				}
			}
			magnitudeDocumentVector = Math.sqrt(magnitudeDocumentVector);
			docScore = (docScore/(magnitudeQueryVector*magnitudeDocumentVector));
			result.docScores.add(ptr0Docid, docScore);
			// The ptr0Docid matched all query arguments, so save it.
		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model giving 
	 * more confidence to values in fields like url, inlink, title etc.
	 * and finally taking a max of all the scores
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateFieldRankBoolean(RetrievalModel r)
			throws IOException {

		allocDaaTPtrs(r);
		QryResult result = new QryResult();
		boolean IsStructured = false;
		for (int i = 0; i < (this.daatPtrs.size() - 1); i++) {
			for (int j = i + 1; j < this.daatPtrs.size(); j++) {
				if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs
						.get(j).scoreList.scores.size()) {
					ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
					this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
					this.daatPtrs.get(j).scoreList = tmpScoreList;
				}
			}
		}
		
		//find the distinct query terms (or features) of the query vector
		double queryVectorValue = 1.0;
		Map<String, Double> termVector = new HashMap<String, Double>();

		for (int j = 0; j < this.daatPtrs.size(); j++) {
			try {
				String query = this.toString();
				query = query.toLowerCase().replace("#or", "")
						.replace("#near", "").replace("#and", "")
						.replace("[", "").replace("]", "").replace(")", "")
						.replace("(", "").replace("#score", "")
						.replace(",", "");
				query = query.trim();

				String[] toks = query.split(" ");
				int p = 0;
				while (p < toks.length) {
					if (toks[p].isEmpty() || toks[p].contains("/")) {
						p++;
						continue;
					} else if (termVector.containsKey(toks[p]))
						termVector.put(toks[p], termVector.get(toks[p])
								+ queryVectorValue);
					else
						termVector.put(toks[p], queryVectorValue);
					p++;
				}
			} catch (Exception e) {
				System.out
						.println("Caught exception while finding magnitude of query term");
			}
		}
		
		//calculate the magnitude of the query vector
		int k = 0;
		double magnitudeQueryVector = 0.0;		
		double magnitudeDocumentVector = 0.0;
		while (k < termVector.size()) {
			double kthValue = new ArrayList<Double>(termVector.values())
					.get(k);
			magnitudeQueryVector += kthValue * kthValue;
			k++;
		}

		magnitudeQueryVector = Math.sqrt(magnitudeQueryVector);

		for (int m = 0; m < this.args.size(); m++) {
			try {
				if (this.args.get(m).args.size() > 0
						&& !(this.args.get(m).args.get(0) instanceof QryopSlScore) && !(this.args.get(m).args.get(0) instanceof QryopIlTerm))
					IsStructured = IsStructured || true;
			} catch (Exception e) {
				System.out.println("caught exception while find query type");
			}
		}
		
		Qryop.DaaTPtr ptr0 = this.daatPtrs.get(0);
		double inverseDocFreq0 = Math
				.log(((RetrievalModelFieldWeightRank) r).numDocsInIndex
						/ 1	+ (double) ptr0.scoreList.scores.size());
		double docScore0 = ptr0.scoreList
				.getDocidScore(ptr0.nextDoc) * inverseDocFreq0;
		magnitudeDocumentVector=inverseDocFreq0;
		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
			double docScore = 0.0;
			double docScore1 = docScore0;
			double docScore2 = docScore0;
			

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				Qryop.DaaTPtr ptrj = this.daatPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						
						double inverseDocFreqj = Math
								.log(((RetrievalModelFieldWeightRank) r).numDocsInIndex
										/ 1	+ (double) ptrj.scoreList.scores.size());
						
						double docScorej = ptrj.scoreList
								.getDocidScore(ptrj.nextDoc) * inverseDocFreqj;
						
						if(j != 1)
							docScore1 = (docScore1)*magnitudeDocumentVector;
						else
							docScore1 = (docScore1 + docScorej);
						magnitudeDocumentVector = Math.sqrt(magnitudeDocumentVector*magnitudeDocumentVector +inverseDocFreqj* inverseDocFreqj
								);
						docScore1=docScore1/magnitudeDocumentVector;

						//initial value of docScore = Double.MAX_VALUE which is taken for docScore values hence
						//interferes in docScore1 result for the 0th element so splitting it 
						if (j == 1) {
							docScore2 = docScorej;
						} else {
							docScore2 = Math
									.min(docScore2,	docScorej);
						}
						
						//the results for structured are better if calculated from a different formula than normal AND queries
						if (IsStructured)
							docScore = 3*docScore1*magnitudeDocumentVector + 2*docScore2;
						else {
							docScore = docScore2;
						}
						break; // ptrj matches ptr0Docid
					}
				}
			}
			
			result.docScores.add(ptr0Docid, docScore);
			// The ptr0Docid matched all query arguments, so save it.

		}
		freeDaaTPtrs();
		return result;
	}	

	/**
	 * Evaluates the query operator for best-match retrieval model giving 
	 * using Indri's Bayesian inference networks
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
public QryResult evaluateIndri(RetrievalModel r) throws IOException{
		
		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		Integer[] invListIterator = new Integer[this.args.size()];
				
		//Merge all the list to get de-duped list of all docids that can be present
		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();
		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;
			mergedSet.addAll(list1);
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		Collections.sort(mergedList, new QryopSlOR.docSort());
		
		for (int i =0;i<this.daatPtrs.size();i++){
			invListIterator[i] = 0;
		}

		double termCount =  1/(double)this.args.size();
		
		for (int a = 0; a < mergedList.size(); a++) {
			int docida = mergedList.get(a).getDocID();
			double totalscore = 1.0;
			for (int b = 0; b < this.daatPtrs.size(); b++) {
				
				DaaTPtr ptrb = this.daatPtrs.get(b);	
				if (ptrb.scoreList.scores.size() <= invListIterator[b]) {
					totalscore *= Math.pow(((QryopSl)(this.args.get(b))).getDefaultScore(r, docida), termCount);
					continue;
				}	
				
				int docidb = ptrb.scoreList.getDocid(invListIterator[b]);
				
				if (docida == docidb) {
					
					totalscore *=  Math.pow(ptrb.scoreList.getDocidScore(invListIterator[b]), termCount);
					invListIterator[b]++;
				} else {
					double defaultscore = 1.0;
					defaultscore = Math.pow(((QryopSl)(this.args.get(b))).getDefaultScore(r, docida), termCount);
					totalscore *= defaultscore;					
				}
				
			}
			result.docScores.add(docida, totalscore);
		
		}
		return result;
	}
	
	public double getPrecedingScores(int j, double termCount, RetrievalModel r, int docid){
		double score = 1.0;
		
		try {
		
		for(int k=j ;k>=0;k--){
			score *= Math.pow(((QryopSlScore)(this.args.get(k))).getDefaultScore(r, docid), termCount);
		}
		}catch(Exception e){
			System.out.println("caught exception in getting previosu scores");
			return 0.0;
		}
		return score;
	}
	public double getTermMagnitude(){
		//find the distinct query terms (or features) of the query vector
		double queryVectorValue = 1.0;
		Map<String, Double> termVector = new HashMap<String, Double>();

		for (int j = 0; j < this.daatPtrs.size(); j++) {
			try {
				String query = this.toString();
				query = query.toLowerCase().replace("#or", "")
						.replace("#near", "").replace("#and", "")
						.replace("[", "").replace("]", "").replace(")", "")
						.replace("(", "").replace("#score", "")
						.replace(",", "");
				query = query.trim();

				String[] toks = query.split(" ");
				int p = 0;
				while (p < toks.length) {
					if (toks[p].isEmpty() || toks[p].contains("/")) {
						p++;
						continue;
					} else if (termVector.containsKey(toks[p]))
						termVector.put(toks[p], termVector.get(toks[p])
								+ queryVectorValue);
					else
						termVector.put(toks[p], queryVectorValue);
					p++;
				}
			} catch (Exception e) {
				System.out
						.println("Caught exception while finding magnitude of query term");
			}
		}
		//calculate the magnitude of the query vector
		int k = 0;
		double magnitudeQueryVector = 0.0;		
		while (k < termVector.size()) {
			double kthValue = new ArrayList<Double>(termVector.values())
				.get(k);
			magnitudeQueryVector += kthValue * kthValue;
			k++;
		}
		magnitudeQueryVector = Math.sqrt(magnitudeQueryVector);
		return magnitudeQueryVector;
	}
	/*
	 * Calculate the default score for the specified document if it does not
	 * match the query operator. This score is 0 for many retrieval models, but
	 * not all retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (0.0);
		else if(r instanceof RetreivalModelIndri){
			int termcount = this.args.size();
			double probScore = 1.0;
			for (int i =0;i<termcount; i++){
				double newScore = Math.pow(((QryopSl)(this.args.get(i))).getDefaultScore(r, docid), (1/(float)termcount));
				probScore *= newScore;
			}
			return probScore;
		}
		else
		return 0.0;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#AND( " + result + ")");
	}
}
