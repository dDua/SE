
public class RetreivalModelIndri extends RetrievalModel{
	
	double mu =2500;
	double lambda = 0.4;
	
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
	  
	  public RetreivalModelIndri(double mu, double lambda){
		  this.mu = mu;
		  this.lambda = lambda;
	  }
}


