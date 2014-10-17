import java.io.*;
import java.util.*;
import java.util.regex.*;

public class QryopSlOR extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlOR(Qryop... q) {
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
			return (evaluateTfidfRankedBoolean(r));
		else if (r instanceof RetrievalModelCosineSimilarityRanked)
			return (evaluateRankedCosineSimBoolean(r));
		else if (r instanceof RetrievalModelFieldWeightRank)
			return (evaluateFieldRankBoolean(r));
		else if(r instanceof RetrievalModelRankedBoolean)
			return (evaluateTfidfRankedBoolean(r));
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

		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();

		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;

			mergedSet.addAll(list1);
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		Collections.sort(mergedList, new QryopSlOR.docSort());

		for (int a = 0; a < mergedList.size(); a++) {
			result.docScores.add(mergedList.get(a).getDocID(), 1.0);
		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model which uses
	 * TF-IDF score for ranking the documents
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateTfidfRankedBoolean(RetrievalModel r)
			throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		//create a hash-set to merge all scorelist together as it removes duplicates
		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();

		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;

			mergedSet.addAll(list1);
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		//sort list of docid
		Collections.sort(mergedList, new QryopSlOR.docSort());
		
		//keeps track of ptrs for all query term's inverted lists
		int[] maxIter = new int[this.daatPtrs.size()];
		for (int a = 0; a < mergedList.size(); a++) {

			double docScore = 0.0;
			double docScore1 = 0.0;
			for (int b = 0; b < this.daatPtrs.size(); b++) {

				DaaTPtr ptrb = this.daatPtrs.get(b);
				if (ptrb.scoreList.scores.size() <= maxIter[b]) {
					continue;
				}
				//iteration for docid's on merged list
				if (mergedList.get(a).getDocID() == ptrb.scoreList
						.getDocid(maxIter[b])) {
//					double inverseDocFreqb = Math
//							.log(((RetrievalModelTfidfRanked) r).numDocsInIndex
//									/ (double) ptrb.scoreList.scores.size());
//					double docScoreb = ptrb.scoreList
//							.getDocidScore(ptrb.nextDoc) * inverseDocFreqb;
					double docScoreb = ptrb.scoreList.getDocidScore(ptrb.nextDoc);
					docScore1 = Math.max(docScore1, docScoreb);
					
					docScore=docScore1;
					maxIter[b]++;
				}
			}
			result.docScores.add(mergedList.get(a).getDocID(), docScore);		
		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model using cosine
	 * similarity between the term and document vectors
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateRankedCosineSimBoolean(RetrievalModel r)
			throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		// find the distinct query terms (or features) of the query vector
		double queryVectorValues = 1.0;
		Map<String, Double> termVector = new HashMap<String, Double>();

		for (int j = 0; j < this.daatPtrs.size(); j++) {
			try {
				String query = this.args.toString();
				query = query.toLowerCase().replace("#or", "").replace("[", "")
						.replace("]", "").replace("#score", "")
						.replace("#near", "").replace("#and", "")
						.replace(")", "").replace("(", "").replace(",", "");
				query = query.trim();

				String[] toks = query.split(" ");
				int p = 0;
				while (p < toks.length) {
					if (toks[p].isEmpty() || toks[p].contains("/")) {
						p++;
						continue;
					} else if (termVector.containsKey(toks[p])) {
						termVector.put(toks[p], termVector.get(toks[p])
								+ queryVectorValues);
					} else {
						termVector.put(toks[p], queryVectorValues);
					}
					p++;
				}
			} catch (Exception e) {
				System.out
						.println("Caught exception while finding magnitude of query term");
			}
		}

		// calculate the magnitude of the query vector
		int k = 0;
		double magnitudeQueryVector = 0.0;
		double magnitudeDocumentVector = 0.0;
		while (k < termVector.size()) {
			double kthValue = new ArrayList<Double>(termVector.values()).get(k);
			magnitudeQueryVector += kthValue * kthValue;
			k++;
		}
		magnitudeQueryVector = Math.sqrt(magnitudeQueryVector);

		//create merged list with the help of hashset
		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();

		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;

			mergedSet.addAll(list1);
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		Collections.sort(mergedList, new QryopSlOR.docSort());
		
		int[] maxIter = new int[this.daatPtrs.size()];
		for (int a = 0; a < mergedList.size(); a++) {

			double docScore = 0.0;
			double docScore1 = 0.0;
			for (int b = 0; b < this.daatPtrs.size(); b++) {

				DaaTPtr ptrb = this.daatPtrs.get(b);
				if (ptrb.scoreList.scores.size() <= maxIter[b]) {
					continue;
				}
				if (mergedList.get(a).getDocID() == ptrb.scoreList
						.getDocid(maxIter[b])) {
					double inverseDocFreqb = Math
							.log(((RetrievalModelCosineSimilarityRanked) r).numDocsInIndex
									/ (double) ptrb.scoreList.scores.size());
					double docScoreb = ptrb.scoreList
							.getDocidScore(ptrb.nextDoc) * inverseDocFreqb;
					docScore1 = (docScore1) * magnitudeDocumentVector;
					docScore1 = (docScore1 + docScoreb);
					magnitudeDocumentVector = Math.sqrt(magnitudeDocumentVector
							* magnitudeDocumentVector + inverseDocFreqb
							* inverseDocFreqb);
					docScore1 = docScore1 / magnitudeDocumentVector
							* magnitudeQueryVector;
					docScore = docScore1;
					maxIter[b]++;
				}
			}
			result.docScores.add(mergedList.get(a).getDocID(), docScore);
		}
		freeDaaTPtrs();
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval model giving more
	 * confidence to values in fields like url, inlink, title etc. and finally
	 * taking a max of all the scores
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateFieldRankBoolean(RetrievalModel r)
			throws IOException {

		allocDaaTPtrs(r);
		QryResult result = new QryResult();

		// find the distinct query terms (or features) of the query vector
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

		// calculate the magnitude of the query vector
		int k = 0;
		double magnitudeQueryVector = 0.0;
		double magnitudeDocumentVector = 0.0;
		while (k < termVector.size()) {
			double kthValue = new ArrayList<Double>(termVector.values()).get(k);
			magnitudeQueryVector += kthValue * kthValue;
			k++;
		}

		magnitudeQueryVector = Math.sqrt(magnitudeQueryVector);

		//create the duplicate removed and sorted list from union of all score lists
		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();

		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;

			mergedSet.addAll(list1);
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		Collections.sort(mergedList, new QryopSlOR.docSort());
		int[] maxIter = new int[this.daatPtrs.size()];
		
		//iterate over each docid in the merged list
		for (int a = 0; a < mergedList.size(); a++) {
			double docScore2 = 0.0;
			double docScore = 0.0;
			double docScore1 = 0.0;
			for (int b = 0; b < this.daatPtrs.size(); b++) {

				DaaTPtr ptrb = this.daatPtrs.get(b);
				if (ptrb.scoreList.scores.size() <= maxIter[b]) {
					continue;
				}
				if (mergedList.get(a).getDocID() == ptrb.scoreList
						.getDocid(maxIter[b])) {
					double inverseDocFreqb = Math
							.log(((RetrievalModelFieldWeightRank) r).numDocsInIndex
									/ (double) ptrb.scoreList.scores.size());
					double docScoreb = ptrb.scoreList
							.getDocidScore(ptrb.nextDoc) * inverseDocFreqb;

					//use a combination of docScore1 and docScore2
					docScore2 = Math.max(docScore2, docScoreb);					
					docScore1 = (docScore1) * magnitudeDocumentVector;
					docScore1 = (docScore1 + docScoreb);
					magnitudeDocumentVector = Math.sqrt(magnitudeDocumentVector
							* magnitudeDocumentVector + inverseDocFreqb
							* inverseDocFreqb);
					docScore1 = docScore1 / magnitudeDocumentVector
							* magnitudeQueryVector;

					docScore = docScore1 * magnitudeDocumentVector + docScore2;

					maxIter[b]++;
				}
			}
			result.docScores.add(mergedList.get(a).getDocID(), docScore);
		}
		freeDaaTPtrs();
		return result;
	}

	public static class docSort implements Comparator<ScoreList.ScoreListEntry> {
		public int compare(ScoreList.ScoreListEntry o1,
				ScoreList.ScoreListEntry o2) {
			try {
				if (o1.getDocID() < o2.getDocID())
					return -1;
				else if (o1.getDocID() > o2.getDocID())
					return 1;
				else
					return 0;
			} catch (Exception e) {
				System.out.println("Execption while sorting..");
				return 0;
			}
		}
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
				double newScore = (1 - (Math.pow(((QryopSl)(this.args.get(i))).getDefaultScore(r, docid), (1/(float)termcount))));
				probScore *= newScore;
			}
			return (1-probScore);
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

		return ("#OR( " + result + ")");
	}
}
