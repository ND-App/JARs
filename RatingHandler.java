package com.firstapex.fic.rating.services;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.firstapex.aic.common.exception.FASystemException;
import com.firstapex.fic.rating.exception.RatingException;
import com.firstapex.fic.rating.exception.RatingSetupException;
import com.firstapex.fic.rating.invoker.Formula1Invoker;
import com.firstapex.fic.rating.invoker.JDBCUtility;
import com.firstapex.fic.rating.invoker.RatingInvoker;
import com.firstapex.fic.rating.vo.ExcelRatingRow;
import com.firstapex.fic.rating.vo.RatingObject;
import com.firstapex.fic.ruleengine.util.Utils;
import com.firstapex.fic.utils.FICConfigUtils;
import com.firstapex.fic.utils.UtilsConstants;
import com.firstapex.fic.utils.logger.FICLogger;
import com.firstapex.fic.utils.servicelocator.FICServiceLocator;

public class RatingHandler {

	private transient FICLogger log = FICLogger.getLogger(RatingHandler.class);

	private com.firstapex.fic.rating.invoker.RatingInvoker loadSSInvoker() {
		log.debug("->loadSSInvoker");
		RatingInvoker invoker = null;
		Class classObject = null;
		try {
			String impl = FICConfigUtils.getConfigProp(RatingConstants.CONFIG,
					RatingConstants.SSBEAN_INVOKER);
			if (impl.equals("FormulaOneInvoker")) {
				classObject = Class
				.forName("com.firstapex.fic.rating.invoker.Formula1Invoker");
				invoker = (Formula1Invoker) classObject.newInstance();
			/*} else if (impl.equals("ExcelInvoker")) {
				classObject = Class
				.forName("com.firstapex.fic.rating.invoker.ExcelInvoker");
				invoker = (ExcelInvoker) classObject.newInstance();*/
			} else {
				throw new FASystemException(
				"SSBean Implementation is not provided");
			}
		} catch (ClassNotFoundException classNotFoundException) {
			throw new FASystemException("Unable to find the class",
					classNotFoundException);
		} catch (InstantiationException e) {
			throw new FASystemException(
					"Error occured while creating the new instance", e);
		} catch (IllegalAccessException e) {
			throw new FASystemException(
					"Error occured while creating the new instance", e);
		}
		log.debug("->loadSSInvoker");
		
		return invoker;
	}

	public RatingInvoker getSSInvoker(String fileName) throws RatingSetupException {
		log.debug("->getSSInvoker");
		Connection con = null;
		RatingInvoker invoker = null;
		String path = FICConfigUtils.getConfigProp(RatingConstants.CONFIG,
				RatingConstants.SSBEAN_FILEPATH);
		String filePath = path + fileName;
		try {

			if (!new File(filePath).exists()) {
				con = getConnection();
				JDBCUtility utility = new JDBCUtility(con);
				utility.retrieve(fileName, filePath);
			}
			invoker = loadSSInvoker();
			invoker.open(filePath);
		} finally {
			try {
				if (con != null)
					con.close();
			} catch (SQLException e) {
					log.debug("Error occured while closing the connection");
			}
		}
		log.debug("->getSSInvoker");
		return invoker;
	}
	
    public void doRegister(String id, String fileName) throws RatingSetupException  {
    	log.debug("->register");
    	Connection con = getConnection();
    	try {
			JDBCUtility utility = new JDBCUtility(con);
			utility.insert (id,fileName);
    	} finally {
			try {
				if (con != null)
					con.close();
			} catch (SQLException e) {
					log.debug("Error occured while closing the connection");
			}
		}
    	log.debug("->register");
    }
    public void doUnregister(String id) {
    	log.debug("->unRegister");
    	Connection con = null;
		try {
			con = getConnection();
			JDBCUtility utility  = null;
			utility = new JDBCUtility(con);
			
			utility.delete (id);
		} finally {
			if( con != null)
				try {
					con.close();
				} catch (SQLException e) {
					log.debug("Error occured while closing the connection" );
				}
		}
		log.debug("->unRegister");
    }
    
	private Connection getConnection() {
		log.debug("->getConnection");
		InitialContext initCtx = null;
		try {
			String dataSourceName = FICConfigUtils.getConfigProp(RatingConstants.CONFIG,
					RatingConstants.SSBEAN_DATASOURCE);
			DataSource ds = FICServiceLocator.getInstance().getDataSource(
					dataSourceName);
			return ds.getConnection();
		} catch (java.sql.SQLException ne) {
			throw new FASystemException(
					" Exception occured while getting a connection ", ne);
		} finally {
			log.debug("->getConnection");
			try {
				if (initCtx != null)
					initCtx.close();
			} catch (NamingException ne) {
					log.debug("Error occured while closing the IntialContext");
			}
		}
		
	}
	
	public RatingObject doRating(String fileName, RatingObject inputeRatingObject, RatingObject outputRatingObject,String SaveFileName) throws  RatingException, RatingSetupException {
		log.debug("->doRating");
		RatingInvoker invoker = null;
		try {
			invoker  = getSSInvoker(fileName);
			putDataInExcelBook(inputeRatingObject,invoker);
			if(FICConfigUtils.getConfigProp(RatingConstants.CONFIG,RatingConstants.SSBEAN_DEBUG).equals("true")) {
			 invoker.save(SaveFileName);
			}
			outputRatingObject = getDataFromExcelBook(outputRatingObject,invoker);
		} finally {
			if (invoker != null) { 
				invoker.close();
			}
		}
		log.debug("->doRating");
		return outputRatingObject;
	}
	
	
	public void putDataInExcelBook(RatingObject inrate,RatingInvoker invoker) throws RatingException  {
        log.debug("->putDataInExcelBook");
		String values = "";
		ArrayList excelRatingRowList = inrate.getExcelRatingRowList();
		ExcelRatingRow row = null;
		StringBuffer valueArray = null;
		int startRow =0, startColumn = 0, endRow = 0, endColumn = 0, sheetNo = 0, noOfRows = 0;
		String currentCollectionName = "";
		for (int i = 0; i < excelRatingRowList.size(); i++) {
			row = (ExcelRatingRow) excelRatingRowList.get(i);
			log.debug("RatingHandler-->field value :::: "+row.fieldValue);
			values = row.getValues();
		    StringBuffer logMessage = new StringBuffer("Values for Policy  :");
            logMessage.append("\tFor Collection :");
            logMessage.append(row.collectionName).append("\tVALUES :\n");
            logMessage.append(values);
            log.debug(logMessage.toString());
			if (values.trim().length() > 0) {
					if (!(values.equals("\t \t"))) {
						if (values.startsWith("\t"))
							values = values.replaceFirst("\t", " \t");
						if (!currentCollectionName.equals(row.collectionName)) {
							if (! "".equals(currentCollectionName)) {
								invoker.addValueToRange(
										sheetNo,
										startRow,
										startColumn,
										startRow + noOfRows,
										endColumn,
										valueArray.toString());
								log.debug("input data :: "+  valueArray.toString());
								valueArray = null;
							}
							currentCollectionName = row.collectionName;
							startRow = row.startRow + 2;
							startColumn = row.startCol;
							sheetNo = row.sheetNo;
							endColumn = row.endCol;
							noOfRows = -1;
							valueArray = new StringBuffer(20000);
						}
						/*
						invoker.addValueToRange(
							row.sheetNo,
							row.startRow + 2,
							row.startCol,
							row.startRow + 2,
							row.endCol,
							values.toString());
						*/
						valueArray.append(values);
						valueArray.append("\n");
						noOfRows = noOfRows + 1;
						//log.debug("input data :: "+  values.toString());
					}
				}
			
		}
		if(valueArray!=null) {
			if(Utils.isNotEmpty(valueArray.toString())) {
				invoker.addValueToRange(
						sheetNo,
						startRow,
						startColumn,
						startRow + noOfRows,
						endColumn,
						valueArray.toString());
			}
			log.debug("input data :: "+  valueArray.toString());
		}
        log.debug("<-putDataInExcelBook");
	}

	public RatingObject getDataFromExcelBook(RatingObject outputRatingObject,RatingInvoker invoker) throws RatingException {
		log.debug("->getDataFromExcelBook");
	    int rowSize = 0;
	    int sheetNo = 0;
		int startRow = 0;
		int startCol = 0;
		int endRow = 0;
		int endCol = 0;
		ExcelRatingRow newRow = null;
		RatingObject outRate = null;
	    ArrayList excelRatingRowList = outputRatingObject.getExcelRatingRowList();
			for (int i = 0; i < excelRatingRowList.size(); i++) {
				ExcelRatingRow excelRatingRow = (ExcelRatingRow) excelRatingRowList.get(i);
				rowSize = excelRatingRow.getNoOfRows();
				outRate = new RatingObject();
				String[] headers;
				sheetNo = excelRatingRow.sheetNo;
				startRow = excelRatingRow.startRow + 2;
				startCol = excelRatingRow.startCol;
				endRow = excelRatingRow.startRow + 2 + rowSize;
		        endCol = excelRatingRow.endCol;
				headers = excelRatingRow.getColNames();
				
				String[][] rangeValues = invoker.getValueFromRange(sheetNo,
		                startRow, startCol, endRow, endCol);
				if (rangeValues != null && rangeValues.length > 0) {
					for (int x = 0; x < rowSize; x++) {
						newRow = new ExcelRatingRow();
						newRow.collectionName = excelRatingRow.getCollectionName();
						for (int y = 0; y < (endCol - startCol + 1); y++) {
							newRow.fieldValue.put(headers[y], rangeValues[x][y]);
						}
						log.debug("RatingHandler-->out field value :::: "+newRow.fieldValue);
						newRow.setColNames(headers);
						outRate.addRow(newRow);
					}
					excelRatingRow.setOutRatingObject(outRate);
		        }
				log.debug("Range - Sheet:r1,c1:r2,c2" + sheetNo + ":" + startRow
	                    + "," + startCol + ":" + endRow + "," + endCol);
			}
			log.debug("<-getDataFromExcelBook");
		return outputRatingObject;
	}
}
