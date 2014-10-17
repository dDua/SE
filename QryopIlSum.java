import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


public class QryopIlSum extends QryopSl {

	int numdocs = 0;
	long sumTotalTF = 0;
	double avglen = 0.0;

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlSum(Qryop... q) {
		
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
		int[] maxIter = new int[this.daatPtrs.size()];
		int recentlyAddedDocid = -1;
		Map<String, Integer> termVector = new HashMap<String, Integer>();
		for (int k = 0; k < this.args.size(); k++) {
			String key = this.args.get(k).toString();
			if (termVector.containsKey(key))
				termVector.put(key, (int)termVector.get(key) + 1);
			else
				termVector.put(key, 1);

		}
		for (int a = 0; a < mergedList.size(); a++) {
			for (int b = 0; b < this.daatPtrs.size(); b++) {
								
				DaaTPtr ptrb = this.daatPtrs.get(b);
							
				double qtf = termVector.get(this.args.get(b).toString());
				
				if (ptrb.scoreList.scores.size() <= maxIter[b]) {
					continue;
				}
				
				int docida = mergedList.get(a).getDocID();
				int docidb = ptrb.scoreList.getDocid(maxIter[b]);
				double userwgt = ((((RetrievalModelBM25)r).k3 + 1) * qtf) / (((RetrievalModelBM25)r).k3 + qtf);
				
				if (docida == docidb) {
					if (docidb == recentlyAddedDocid) {
						double prevScore = result.docScores.getDocidScore(a);
						double newScore =  ptrb.scoreList.getDocidScore(maxIter[b])*userwgt;
						result.docScores.updateDocidScore(a, docida, newScore+prevScore);
					}else {
						result.docScores.add(docida, ptrb.scoreList.getDocidScore(maxIter[b])*userwgt);
						recentlyAddedDocid = docida;
					}
					maxIter[b]++;
				}
			}

		}

		freeDaaTPtrs();
		return result;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SUM" + "( " + result + ")");
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

		return 0.0;
	}

}
