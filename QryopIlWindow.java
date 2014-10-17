import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class QryopIlWindow extends QryopIl {

	int distance = 0;
	static int cumtf = 0;
	static int cumdf = 0;
	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlWindow(int window, Qryop... q) {
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
						if (Math.abs(comparelistPositions.get(innerIterLenCompare)
								- baselistPositions.get(innerIterLenBase)) < this.distance) {
							if(comparelistPositions.get(innerIterLenCompare) >= baselistPositions.get(innerIterLenBase)){
								matchingPositions.add(comparelistPositions.get(innerIterLenCompare));
							} else {
								matchingPositions.add(Math.max(baselistPositions.get(innerIterLenBase),comparelistPositions.get(innerIterLenCompare)));
							}
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

		return ("#WINDOW/" + this.distance + "( " + result + ")");
	}



}
