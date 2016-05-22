package structures;

import java.util.HashMap;


/**
 * NGRAM Structure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-14
 */


public class NGRAM extends HWS{

	public void getExtraMap(String[] sens,HashMap loadedMap){
		HashMap empty = new HashMap();
		this.extraMap = empty;
	}

	public HashMap toCellMap(String[] tokens){
		super.initCellMap();
		for(int i=0;i<tokens.length;i++)
			super.putCell(i,i,tokens[i]);
		return this.cellMap;
	}
}
