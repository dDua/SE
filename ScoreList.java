/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;

public class ScoreList {

	public int cumtf;
	public String field;
	static double matchedScoreValue = 0.0;
  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;
    private double score;

  
    public ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
    
    protected double getScore()
    {
    	return this.score;
    }
    protected int getDocID()
    {
    	return this.docid;
    }
    @Override
    public boolean equals(Object o)
    {
       
       if (((ScoreListEntry)o).getDocID() == (this.docid)){
    	   matchedScoreValue = ((ScoreListEntry)o).getScore();
            	return true;
       }
       else
    	   return false;
        
    }
    @Override
    public int hashCode() {
         return docid;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {	  
    return this.scores.get(n).docid;    
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  
//  public boolean updateDocidScore(int index, int docid, double score){
//	  try
//	  { 
//		  double ms = this.containsElement(docid);
//		  if(ms != -1){
//			double prevScore = ms;
//			double newScore =  score;	
//			this.scores.set(index, new ScoreListEntry(docid, prevScore*newScore));
//		  } else {
//			  this.add(docid, score);
//		  }
//		  return true;
//	  }catch(Exception e){
//		  System.out.println("caught expetion while update docid score");
//		  return false;  
//	  } 
  //}
  
  public boolean updateDocidScore(int index, int docid, double score){
	  try
	  { 	  
			this.scores.set(index, new ScoreListEntry(docid, score));
		 
		  return true;
	  }catch(Exception e){
		  System.out.println("caught expetion while update docid score");
		  return false;  
	  } 
  }
  
  public double containsElement(int docid){
	  try
	  {
		 if( this.scores.contains(new ScoreListEntry(docid, 0.0)))
		  return matchedScoreValue;
		 else
			 return -1.0;
	  }catch(Exception e){
		  System.out.println("caught expetion while update docid score");
		  return -1.0;  
	  }
	  
	  
  }
  
  
}
