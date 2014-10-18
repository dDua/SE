/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import java.lang.Object;

public class QryEval {

	static String usage = "Usage:  java "
			+ System.getProperty("sun.java.command") + " paramFile\n\n";

	// The index file reader is accessible via a global variable. This
	// isn't great programming style, but the alternative is for every
	// query operator to store or pass this value, which creates its
	// own headaches.
	static Qryop finalQuery;
	public static IndexReader READER;
	public static DocLengthStore dls;

	// Create and configure an English analyzer that will be used for
	// query parsing.

	public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
			Version.LUCENE_43);
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	public static class scoreSort implements
			Comparator<ScoreList.ScoreListEntry> {
		public int compare(ScoreList.ScoreListEntry o1,
				ScoreList.ScoreListEntry o2) {
			try {
				if (o1.getScore() < o2.getScore())
					return -1;
				else if (o1.getScore() > o2.getScore())
					return 1;
				else if (o1.getScore() == o2.getScore()) {
					return getExternalDocid(o2.getDocID()).compareToIgnoreCase(
							getExternalDocid(o1.getDocID()));
				} else
					return 0;
			} catch (Exception e) {
				System.out.println("Execption while sorting..");
				return 0;
			}
		}
	}
	
	public static class scoreSortModified implements
	Comparator<scoreArray> {
public int compare(scoreArray o1,
		scoreArray o2) {
	try {
		if (o1.score < o2.score)
			return -1;
		else if (o1.score > o2.score)
			return 1;
		else if (o1.score == o2.score) {
			return o1.externalId.compareToIgnoreCase(
					o2.externalId);
		} else
			return 0;
	} catch (Exception e) {
		System.out.println("Execption while sorting..");
		return 0;
	}
}
}
	public static class scoreArray{
		String externalId;
		int docId;
		double score;
		public scoreArray(String extId, int docid, double score){
			this.externalId = extId;
			this.docId = docid;
			this.score = score;
		}
	}
	

	/**
	 * @param args
	 *            The only argument is the path to the parameter file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// must supply parameter file
		if (args.length < 1) {
			System.err.println(usage);
			System.exit(1);
		}

		// read in the parameter file; one parameter per line in format of
		// key=value
		Map<String, String> params = new HashMap<String, String>();
		Scanner scan = new Scanner(new File(args[0]));
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			params.put(pair[0].trim(), pair[1].trim());
		} while (scan.hasNext());
		scan.close();

		// parameters required for this example to run
		if (!params.containsKey("indexPath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(params
				.get("indexPath"))));
		
		dls = new DocLengthStore(READER);
		
		if (READER == null) {
			System.err.println(usage);
			System.exit(1);
		}		

		RetrievalModel ret = null;
		double mu, lambda, b , k1, k3 ;
		mu=lambda=b=k1=k3=0;
		if (params.containsKey("Indri:mu")) {
			mu = Double.parseDouble(params.get("Indri:mu"));
		}
		if (params.containsKey("Indri:lambda")) {
		   lambda = Double.parseDouble(params.get("Indri:lambda"));
		}
		if (params.containsKey("BM25:b")) {
		   b = Double.parseDouble(params.get("BM25:b"));
		}
		if (params.containsKey("BM25:k_1")) {
		  k1 = Double.parseDouble(params.get("BM25:k_1"));
		}
		if (params.containsKey("BM25:k_3")) {
		 k3 = Double.parseDouble(params.get("BM25:k_3"));
		}
		
		if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("unrankedboolean"))
		{
			ret = new RetrievalModelUnrankedBoolean();
		}else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("tfidfrankedboolean")) {			
			ret = new RetrievalModelTfidfRanked(READER.numDocs());
		} else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("fieldrankedboolean")) {
			ret = new RetrievalModelFieldWeightRank(READER.numDocs());
		} else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("cosinesimrankedboolean")) {
			ret = new RetrievalModelCosineSimilarityRanked(READER.numDocs());
		} else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("bm25")) {
			ret = new RetrievalModelBM25(b,k1,k3);
		}  else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("indri")) {
			ret = new RetreivalModelIndri(mu, lambda);
		} else if(params.get("retrievalAlgorithm").toLowerCase().trim().contains("rankedboolean")) {
			ret = new RetrievalModelRankedBoolean();
		} 
		else {
			ret = new RetrievalModelRankedBoolean();
		}
		
		Qryop parsedQuery1 = parseQuery("#WINDOW/8(heart rate)",ret);
		//Qryop parsedQuery1 = parseQuery("brooks brothers clearance",ret);
		//if(ret instanceof RetreivalModelIndri){
			//parsedQuery1 = multipleRep(parsedQuery1);
		//}
		QryResult result1 = parsedQuery1.evaluate(ret);
		//int id1 = getInternalDocid("clueweb09-en0001-66-14262");
		//int id2 = getInternalDocid("clueweb09-en0011-58-19607");
		BufferedReader reader = null;
		BufferedWriter writer = null;
		
		try {
			reader = new BufferedReader(new FileReader(new File(
					params.get("queryFilePath"))));
			writer = new BufferedWriter(new FileWriter(new File(
					params.get("trecEvalOutputPath"))));
			long timerTicks = 0;
			String q = null;
			long millisStart = 0;
			long millisEnd = 0;
			int noOfQueries = 0;
			while ((q = reader.readLine()) != null) {
				String queryToken[] = q.split(":");
				System.out.println(queryToken[1]);
				Qryop parsedQuery = parseQuery(queryToken[1].trim(),ret);
//				if(ret instanceof RetreivalModelIndri){
//					parsedQuery = multipleRep(parsedQuery);
//				}
				millisStart = System.currentTimeMillis();
				QryResult result = parsedQuery.evaluate(ret);
				millisEnd = System.currentTimeMillis() ;				
				timerTicks += (millisEnd - millisStart);				
				noOfQueries++;
				
				List<scoreArray> finalresults = new ArrayList<scoreArray>();
				
				for (int i =0;i<result.docScores.scores.size();i++){
					finalresults.add(new QryEval.scoreArray(getExternalDocid(result.docScores.getDocid(i)),result.docScores.getDocid(i),result.docScores.getDocidScore(i)));	
				}
				
				Collections.sort(finalresults,
							new QryEval.scoreSortModified());
						
				Collections.reverse(finalresults);
				int j = 1;
				
				int maxLength = (finalresults.size() > 100) ? 100 : finalresults.size();
				for (int i = 0; i < maxLength; i++) {
					String resultLine = queryToken[0] + " Q0 "
							+ finalresults.get(i).externalId
							+ " " + j++ + " "
							+  finalresults.get(i).score+ " run-1\n";
					writer.write(resultLine);
				}
				
			}
			System.out.println(timerTicks);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				writer.close();
			} catch (Exception e) {
			}
		}

		/**
		 * The index is open. Start evaluating queries. The examples below show
		 * query trees for two simple queries. These are meant to illustrate how
		 * query nodes are created and connected. However your software will not
		 * create queries like this. Your software will use a query parser. See
		 * (ry.
		 *
		 * The general pattern is to tokenize the query term (so that it gets
		 * converted to lowercase, stopped, stemmed, etc), create a Term node to
		 * fetch the inverted list, create a Score node to convert an inverted
		 * list to a score list, evaluate the query, and print results.
		 * 
		 * Modify the software so that you read a query from a file, parse it,
		 * and form the query tree automatically.
		 */

		// Later HW assignments will use more RAM, so you want to be aware
		// of how much memory your program uses.

		printMemoryUsage(false);

	}

	/**
	 * Write an error message and exit. This can be done in other ways, but I
	 * wanted something that takes just one statement so that it is easy to
	 * insert checks without cluttering the code.
	 * 
	 * @param message
	 *            The error message to write before exiting.
	 * @return void
	 */
	static void fatalError(String message) {
		System.err.println(message);
		System.exit(1);
	}

	/**
	 * Get the external document id for a document specified by an internal
	 * document id. If the internal id doesn't exists, returns null.
	 * 
	 * @param iid
	 *            The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Finds the internal document id for a document specified by its external
	 * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
	 * an exception.
	 * 
	 * @param externalId
	 *            The external document id of a document.s
	 * @return An internal doc id suitable for finding document vectors etc.
	 * @throws Exception
	 */
	static int getInternalDocid(String externalId) throws Exception {
		Query q = new TermQuery(new Term("externalId", externalId));

		IndexSearcher searcher = new IndexSearcher(QryEval.READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * parseQuery converts a query string into a query tree.
	 * 
	 * @param qString
	 *            A string containing a query.
	 * @param qTree
	 *            A query tree
	 * @throws IOException
	 */
	static Qryop parseQuery(String qString, RetrievalModel r) throws IOException {

		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();

		// Add a default query operator to an unstructured query. This
		// is a tiny bit easier if unnecessary whitespace is removed.

		qString = qString.trim();
		Qryop defaultOperator = null;
		if (r instanceof RetrievalModelTfidfRanked){
			
			defaultOperator = new QryopSlOR();
		}
		else if (r instanceof RetrievalModelBM25){
			
			defaultOperator = new QryopIlSum();
		}
		else if (r instanceof RetreivalModelIndri){
			
			defaultOperator = new QryopSlAnd();
		}else {
			
			defaultOperator = new QryopSlOR();
		}
//		
//		if (qString.charAt(0) != '#') {
//			if (r instanceof RetrievalModelTfidfRanked) {
//				qString = "#or(" + qString + ")";
//				//defaultOperator = new QryopSlOR();
//			} else if (r instanceof RetrievalModelBM25) {
//				qString = "#sum(" + qString + ")";
//				//defaultOperator = new QryopIlSum();
//			} else if (r instanceof RetreivalModelIndri) {
//				qString = "#and(" + qString + ")";
//			//	defaultOperator = new QryopSlAnd();
//			
//		}
		Pattern p = Pattern.compile("#");
	    Matcher m = p.matcher(qString);
	    int hashes = 0;
	    while (m.find()){
	    	hashes +=1;
	    }
	    
	    
		if(!qString.startsWith("(")){
			qString = defaultOperator.toString().replace("(","").replace(")", "").trim()+"(" + qString + ")";
		}
		if(qString.toLowerCase().startsWith("#window") || qString.toLowerCase().startsWith("#near")){
	    	qString = defaultOperator.toString().replace("(","").replace(")", "").trim()+"(" + qString + ")";
	    }
//		if (qString.charAt(0) != '#' && r instanceof RetrievalModelTfidfRanked) {
//				qString = "#or(" + qString + ")";
//				//stack.push(new QryopSlOR());
//			} else if (!qString.toLowerCase().startsWith("#sum") && r instanceof RetrievalModelBM25) {
//				qString = "#sum(" + qString + ")";
//				//defaultOperator = new QryopIlSum();
//			} else if (!qString.toLowerCase().startsWith("#and") && r instanceof RetreivalModelIndri) {
//				qString = "#and(" + qString + ")";
//				//defaultOperator = new QryopSlAnd();
//			}
//			else {
//				qString = "#or(" + qString + ")";
//				//defaultOperator = new QryopSlOR();
//			
//		}
		// Tokenize the query.

		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
				true);
		String token = null;

		// Each pass of the loop processes one token. To improve
		// efficiency and clarity, the query operator on the top of the
		// stack is also stored in currentOp.
//		int nearOccurences = 0;
//		if(qString.toLowerCase().contains("#near")||qString.toLowerCase().contains("#syn"))
//			nearOccurences= 1;
		double weight = -1.0;
		while (tokens.hasMoreTokens()) {

			token = tokens.nextToken();
			Pattern p1 = Pattern.compile("\\d{0,9}\\.\\d{0,9}$");
			Matcher m1 = p1.matcher(token);
			
			if (token.matches("[ ,(\t\n\r]")) {
				// Ignore most delimiters.
			} else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopSlAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				currentOp = new QryopIlSum();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopIlSyn();						
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#or")) {
				currentOp = new QryopSlOR();
				stack.push(currentOp);
			} else if (token.toLowerCase().contains("#near/")) {
				String[] t = token.split("/");
				currentOp =new QryopIlNear(Integer.parseInt(t[1].trim()));
				stack.push(currentOp);				
			} else if (token.toLowerCase().contains("#window/")) {
				String[] t = token.split("/");
				currentOp =new QryopIlWindow(Integer.parseInt(t[1].trim()));
				stack.push(currentOp);
			} else if (token.toLowerCase().contains("#wsum")) {
				currentOp = new QryopSlWSum();
				stack.push(currentOp);
			}else if (token.toLowerCase().contains("#wand")) {
				currentOp = new QryopSlWand();
				stack.push(currentOp);
			} else if(m1.matches() && r instanceof RetreivalModelIndri ){
				weight = Double.parseDouble(token);
				
			}
			else if (token.startsWith(")")) { // Finish current query
												// operator.
				// If the current query operator is not an argument to
				// another query operator (i.e., the stack is empty when it
				// is removed), we're done (assuming correct syntax - see
				// below). Otherwise, add the current operator as an
				// argument to the higher-level operator, and shift
				// processing back to the higher-level operator.

				Qryop temp = stack.pop();
//				if(temp instanceof QryopIlNear || temp instanceof QryopIlSyn){
//					Qryop tempop = new QryopSlScore();
//					tempop.add(temp);
//					currentOp = tempop;					
//				}
				if(qString.toLowerCase().startsWith("#near") && temp instanceof QryopIlNear){
				Qryop tempop = new QryopSlScore();
				tempop.add(temp);
				currentOp = tempop;					
			}
				if(qString.toLowerCase().startsWith("#syn") && temp instanceof QryopIlSyn){
					Qryop tempop = new QryopSlScore();
					tempop.add(temp);
					currentOp = tempop;					
				}

				if (stack.empty() && !tokens.hasMoreTokens())
					break;
				else if (stack.empty() && tokens.hasMoreTokens()){
					currentOp = defaultOperator;
					currentOp.add(temp);
					stack.push(currentOp);	
				}
				else {
					Qryop arg = currentOp;
					currentOp = stack.peek();
					currentOp.add(arg);
				}
			} else {
				//if(weight!=-1.0){
			    	QryopIlTerm t = getFieldTerms(token,weight);		    	
			
			//		QryopIlTerm t = getFieldTerms(token);
					if( t != null)
					{	//currentOp.weight =  weight;
						//currentOp.add(getFieldTerms(token));
						currentOp.add(t);
					}
				
					weight = -1.0;	
					
			}
		}
		return currentOp;
		}

	public static QryopIlTerm getFieldTerms(String token, Double weight) throws IOException
	{
		QryopIlTerm term = null;
		if (token.contains(".url")) {
			term = (new QryopIlTerm(tokenizeQuery(token.trim()
					.split(".url")[0])[0], "url"));
		} else if (token.contains(".keywords")) {
			term = (new QryopIlTerm(tokenizeQuery(token.trim()
					.split(".keywords")[0])[0], "keywords"));
		} else if (token.contains(".title")) {
			term = (new QryopIlTerm(tokenizeQuery(token.trim()
					.split(".title")[0])[0], "title"));
		} else if (token.contains(".inlink")) {
			term = (new QryopIlTerm(tokenizeQuery(token.trim()
					.split(".inlink")[0])[0], "inlink"));
		} else if (token.contains(".body")) {
			term =(new QryopIlTerm(tokenizeQuery(token.trim()
					.split(".body")[0])[0], "body"));
		} else {
			String[] tokenized = tokenizeQuery(token);
			if(tokenized.length > 0)
				term = (new QryopIlTerm(tokenizeQuery(token)[0]));
		}
		
		if(weight>0.0)
			term.weight = weight;
		return term;
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 * 
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 * @return void
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc) {
			runtime.gc();
		}

		System.out
				.println("Memory used:  "
						+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L))
						+ " MB");
	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            Result object generated by {@link Qryop#evaluate()}.
	 * @throws IOException
	 */
	static void printResults(String queryName, QryResult result)
			throws IOException {

		System.out.println(queryName + ":  ");
		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			for (int i = 0; i < result.docScores.scores.size(); i++) {
				System.out.println("\t" + i + ":  "
						+ getExternalDocid(result.docScores.getDocid(i)) + ", "
						+ result.docScores.getDocidScore(i));
			}
		}
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *            String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy",
				new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}
	
	static Qryop multipleRep(Qryop query) throws IOException {

		Qryop temp = query;
		QryopIlTerm car ;
		for(int i=0;i<query.args.size();i++){
			if(temp.args.get(i) instanceof QryopIlTerm){				
				QryopSlWSum eval  = new QryopSlWSum();
				car = new QryopIlTerm(((QryopIlTerm)(temp.args.get(i))).getTerm(), "url");
				QryopSlScore turl  = new QryopSlScore(car,0.1);
				car = new QryopIlTerm(((QryopIlTerm)(temp.args.get(i))).getTerm(), "body");
				QryopSlScore tbody  = new QryopSlScore(car,0.4 );
				car = new QryopIlTerm(((QryopIlTerm)(temp.args.get(i))).getTerm(), "inlink");
				QryopSlScore tinlink  = new QryopSlScore(car,0.3 );
				car = new QryopIlTerm(((QryopIlTerm)(temp.args.get(i))).getTerm(), "title");
				QryopSlScore ttitle  = new QryopSlScore(car, 0.2 );
				eval.add(turl);
				eval.add(tbody);
				eval.add(tinlink);
				eval.add(ttitle);
				query.replace(i, eval);//will fail in NEAR
				//query.add(new QryopSlWSum(turl));
			}else if(temp.args.get(i) instanceof QryopIlNear || temp.args.get(i) instanceof QryopIlSyn){
				QryopSlWSum eval  = new QryopSlWSum();
				List<String> fields = new ArrayList();
				fields.add("url");
				fields.add("body");
				fields.add("inlink");
				fields.add("title");
				QryopIl inter = null;
				double wgt = 0.0;
				for(int k=0;k<fields.size();k++){
					if(temp.args.get(i) instanceof QryopIlNear)
						inter = new QryopIlNear(((QryopIlNear)temp.args.get(i)).distance);
					else 
						inter = new QryopIlSyn();
					for(int j=0;j<temp.args.get(i).args.size();j++){					
						
						if(fields.get(k).trim()=="url")
							wgt = 0.1;
						else if(fields.get(k).trim()=="body")
							wgt = 0.4;
						else if(fields.get(k).trim()=="title")
							wgt = 0.3;
						else if(fields.get(k).trim()=="inlink")
							wgt = 0.2;
						else
							System.out.println("Wrong field");
						inter.add(new QryopIlTerm(((QryopIlTerm)temp.args.get(i).args.get(j)).getTerm(),fields.get(k),wgt ));
						inter.weight = wgt;
					}
					eval.add(inter);
				}
				temp.replace(i, eval);
				
			}else {
				//its not recursive check for that
				temp = temp.args.get(0);
				Qryop newop = multipleRep(temp);
				
				finalQuery.add(newop);
			}
			
		}
		return query;
	}
}
