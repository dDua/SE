import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class QryopSlWand extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlWand(Qryop... q) {
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

		if (r instanceof RetreivalModelIndri)
			return (evaluateIndri(r));
		return null;

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
		double totalWgt = 0.0;
		//Merge all the list to get de-duped list of all docids that can be present
		Set<ScoreList.ScoreListEntry> mergedSet = new HashSet<ScoreList.ScoreListEntry>();
		for (int w = 0; w < this.args.size(); w++) {
			List<ScoreList.ScoreListEntry> list1 = this.daatPtrs.get(w).scoreList.scores;
			mergedSet.addAll(list1);
			totalWgt += this.args.get(w).weight;
		}
		List<ScoreList.ScoreListEntry> mergedList = new ArrayList<ScoreList.ScoreListEntry>(
				mergedSet);
		Collections.sort(mergedList, new QryopSlOR.docSort());
		
		for (int i =0;i<this.daatPtrs.size();i++){
			invListIterator[i] = 0;
		}

//		double termCount =  1/(double)this.args.size();
		
		for (int a = 0; a < mergedList.size(); a++) {
			int docida = mergedList.get(a).getDocID();
			double totalscore = 1.0;
			for (int b = 0; b < this.daatPtrs.size(); b++) {
				
				DaaTPtr ptrb = this.daatPtrs.get(b);	
				if (ptrb.scoreList.scores.size() <= invListIterator[b]) {
					totalscore *= Math.pow(((QryopSl)(this.args.get(b))).getDefaultScore(r, docida), (this.args.get(b).weight/totalWgt));
					continue;
				}	
				
				int docidb = ptrb.scoreList.getDocid(invListIterator[b]);
				
				if (docida == docidb) {
					totalscore *=  Math.pow(ptrb.scoreList.getDocidScore(invListIterator[b]),(this.args.get(b).weight/totalWgt));
					//totalscore *=  Math.pow(ptrb.scoreList.getDocidScore(invListIterator[b]), (this.args.get(b).weight/totalWgt));
					invListIterator[b]++;
				} else {
					double defaultscore = 1.0;
					defaultscore = Math.pow(((QryopSl)(this.args.get(b))).getDefaultScore(r, docida), (this.args.get(b).weight/totalWgt));
					totalscore *= defaultscore;					
				}
				
			}
			result.docScores.add(docida, totalscore);
		
		}
		return result;
	}

public double getDefaultScore(RetrievalModel r, long docid)
		throws IOException {

	if (r instanceof RetrievalModelUnrankedBoolean)
		return (0.0);
	else if(r instanceof RetreivalModelIndri){
		int termcount = this.args.size();
		double probScore = 1.0;
		double totalWgt = 0.0;
		for(int k=0;k<this.args.size();k++)
		{
			totalWgt += this.args.get(k).weight;
		}
		for (int i =0;i<termcount; i++){
			double newScore = Math.pow(((QryopSl)(this.args.get(i))).getDefaultScore(r, docid), (this.args.get(i).weight/totalWgt) );
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
	double weight =0.0;
	for (int i = 0; i < this.args.size(); i++){
		weight = this.args.get(i).weight;
		result += this.args.get(i).toString() + " " + weight + " ";
	}
	return ("#WAND( " + result + ")");
}
}
