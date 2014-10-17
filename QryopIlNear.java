import java.io.*;
import java.lang.reflect.Array;
import java.util.regex.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class QryopIlNear extends QryopIl {

	int distance = 0;
	static int cumtf = 0;
	static int cumdf = 0;
	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlNear(int window, Qryop... q) {
		this.distance = window;
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
		result.invertedList.field = new String(
				this.daatPtrs.get(0).invList.field);	

			Vector<Vector<InvList.DocPosting>> positions = new Vector<Vector<InvList.DocPosting>>();

			for (int i = 0; i < this.daatPtrs.size(); i++) {

				positions.add(new Vector<InvList.DocPosting>(this.daatPtrs
						.get(i).invList.postings));
			}
			int docFreq = 0;
			Vector<InvList.DocPosting> finalTermVector = calculateDocScores(positions);

			if (finalTermVector.size() > 0) {
				result.invertedList.postings = finalTermVector;
				result.invertedList.df = cumdf;
				result.invertedList.ctf = cumtf;						
			}
			
		freeDaaTPtrs();
		return result;
	}

	public QryResult normalizeScores(QryResult result)
	{
		double mean = 0.0;
	      for(int q =0;q<result.docScores.scores.size();q++)      {
	    	  mean += result.docScores.scores.get(q).getScore();    	  
	      }
	      mean = mean/result.docScores.scores.size();
	      
	      double standardDevn = 0.0;
	      for(int q =0;q<result.docScores.scores.size();q++)      {
	    	  standardDevn += (result.docScores.scores.get(q).getScore()-mean) * (result.docScores.scores.get(q).getScore()-mean);    	  
	      }
	      standardDevn = Math.sqrt(standardDevn/result.docScores.scores.size());
	      
	      ScoreList scorelist = new ScoreList();
	      
	      for(int q =0;q<result.docScores.scores.size();q++)      {
	    	  scorelist.add(result.docScores.scores.get(q).getDocID(), 
	    			  ((result.docScores.scores.get(q).getScore()-mean)/standardDevn) + 100);
	    	  if(((result.docScores.scores.get(q).getScore()-mean)/standardDevn)+100 < 0)
	    	  {
	    		  System.out.println("Negative score values");
	    	  }
	      }
	      
	      result.docScores = scorelist;
	      return result;
	}
	public Vector<InvList.DocPosting> calculateDocScores(
			Vector<Vector<InvList.DocPosting>> termDistances) {
		// Iterate over each query term in a single doc to find the near
		// distance

		Vector<InvList.DocPosting> baseTermList = termDistances.get(0);
		Vector<InvList.DocPosting> compareTermList = new Vector<InvList.DocPosting>();
		for (int j = 1; j < termDistances.size(); j++) {
			Vector<InvList.DocPosting> intermediateResultList = new Vector<InvList.DocPosting>();
			compareTermList = termDistances.get(j);
			int outerIterLenComp = 0;
			int outerIterLenBase = 0;
			while (outerIterLenBase < baseTermList.size() && outerIterLenComp < compareTermList.size()) {
				if (baseTermList.get(outerIterLenBase).docid < compareTermList
						.get(outerIterLenComp).docid) {
					outerIterLenBase++;
				} else if (baseTermList.get(outerIterLenBase).docid > compareTermList
						.get(outerIterLenComp).docid) {
					outerIterLenComp++;
				} else {
					Vector<Integer> baselistPositions = baseTermList
							.get(outerIterLenBase).positions;
					Vector<Integer> comparelistPositions = compareTermList
							.get(outerIterLenComp).positions;
					List<Integer> matchingPositions = new ArrayList<Integer>();					
					
					int innerIterLenBase = 0;
					int innerIterLenCompare = 0;

					while (innerIterLenBase < baselistPositions.size() && innerIterLenCompare < comparelistPositions.size()) {
						if ((comparelistPositions.get(innerIterLenCompare)
								- baselistPositions.get(innerIterLenBase) <= this.distance)
								&& (comparelistPositions
										.get(innerIterLenCompare)
										- baselistPositions
												.get(innerIterLenBase) > 0)) {
							matchingPositions.add(comparelistPositions
									.get(innerIterLenCompare));
							innerIterLenCompare++;
							innerIterLenBase++;

						}else if (comparelistPositions
								.get(innerIterLenCompare) < baselistPositions
								.get(innerIterLenBase))
							innerIterLenCompare++;
						else if (comparelistPositions.get(innerIterLenCompare) > baselistPositions
								.get(innerIterLenBase))
							innerIterLenBase++;
						else {
							innerIterLenCompare++;
							innerIterLenBase++;
						}

					}
					if (matchingPositions.size() > 0) {
						InvList listResult = new InvList();
						listResult.appendPosting(compareTermList.get(outerIterLenComp).docid, matchingPositions);	
						//cumtf += listResult.ctf;
						cumdf += listResult.df;
						intermediateResultList.add(listResult.getPostings(0));
					}
					outerIterLenBase++;outerIterLenComp++;
				}
			}
			
			 if (j == termDistances.size() - 1) { 
				 return	 intermediateResultList; 
			} else { 
				baseTermList = intermediateResultList; }
			
		}
		return null;
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

		return ("#NEAR/" + this.distance + "( " + result + ")");
	}

	public QryResult findTermVectors(QryResult result) {

		QryResult finalResult = new QryResult();

		try {

			for (int i = 0; i < result.docScores.scores.size(); i++) {

				Vector<Vector<Integer>> termPosPtrs = new Vector<Vector<Integer>>(
						this.args.size()); // List of distance vector(or term
											// position in doc) of each query
											// term

				TermVector tvptr = new TermVector(result.docScores.scores
						.get(i).getDocID(), "body");
				for (int k = 0; k < this.args.size(); k++) {
					String queryText = this.args.get(k).toString();
					if (queryText.contains("("))
						queryText = queryText.substring(
								queryText.indexOf("(") + 1,
								queryText.indexOf(")"));

					if (queryText.contains("."))
						queryText = (queryText.trim().split("\\."))[0];

					int termIndex = Arrays.asList(tvptr.stems).indexOf(
							queryText);
					int len = 0;
					Vector<Integer> nthPositionList = new Vector<Integer>();
					while (len < tvptr.positions.length) {
						if (tvptr.positions[len] == termIndex)
							nthPositionList.add(len);
						len++;
					}

					termPosPtrs.add(nthPositionList);
				}
				
			}
		} catch (Exception e) {
			System.out.println("Caught exception : " + e.getMessage()
					+ " Exiting ...");
		}
		return finalResult;
	}

}
