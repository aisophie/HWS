package structures;

import java.util.HashMap;
/*
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
*/

/**
 * Generalized HWS structure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-14
 */


public class HWS{
	public HashMap extraMap;
	HashMap cellMap;
	static double V = 0.0;
	
	public void getExtraMap(String[] sens,HashMap loadedMap){
	}

	public HashMap toCellMap(String[] tokens){
		return cellMap;
	}
	
	protected void initCellMap(){
		cellMap = new HashMap<Integer,HashMap<Integer,String>>();
	}

	protected void putCell(int row,int col,String text){
		HashMap<Integer,String> childMap;
		if(this.cellMap.containsKey(row)) childMap = (HashMap)this.cellMap.get(row);
		else childMap = new HashMap<Integer,String>();
		childMap.put(col,text);
		cellMap.put(row,childMap);
	}
}
