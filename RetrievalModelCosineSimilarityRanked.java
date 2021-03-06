
public class RetrievalModelCosineSimilarityRanked extends RetrievalModelRankedBoolean{
	 
	public int numDocsInIndex = 0; //to calculate scores
		/**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, double value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"Cosine Similarity RankedBoolean: " +
				parameterName);
	    return false;
	  }

	  /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, String value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"Cosine Similarity RankedBoolean: " +
				parameterName);
	    return false;
	  }
	  
	  public RetrievalModelCosineSimilarityRanked(int numberOfDocs)
	  {
		  this.numDocsInIndex = numberOfDocs;
	  }


}

