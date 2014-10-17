
public class RetrievalModelBM25 extends RetrievalModel{
	
	double b =0;
	double k1 = 0;
	double k3 = 0;
	
	
	 /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, double value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"RankedBoolean: " +
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
				"RankedBoolean: " +
				parameterName);
	    return false;
	  }
	  
	  public RetrievalModelBM25(double b, double k1, double k3){
		this.b = b;
		this.k1 = k1;
		this.k3 = k3;
	  }
}
