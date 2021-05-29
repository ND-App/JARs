package com.firstapex.fic.rating.services;

import java.util.HashMap;

import com.firstapex.fic.rating.exception.RatingException;
import com.firstapex.fic.rating.exception.RatingSetupException;
import com.firstapex.fic.rating.vo.RatingObject;

public interface RatingService {
	public RatingObject doRating(String fileName, RatingObject inputeRatingObject, RatingObject outputRatingObject,String SaveFileName)
		throws RatingException, RatingSetupException;
	
	public HashMap doRegister(String id, String sourceFileName,boolean isRegisterRequired) throws  RatingSetupException;
	
	public void doUnregister(String id);
}
