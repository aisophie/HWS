package structures;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;


/**
 * Frequency-based HWS structure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-14
 */


public class FB extends HWS{

	public void getExtraMap(String[] sens,HashMap loadedMap){
		boolean unique = true;
		HashMap<String,Integer> unigram;
		if(loadedMap!=null){
			unigram = (HashMap<String,Integer>)loadedMap;
		}else{
			unigram = new HashMap<String,Integer>();
		}

		for(String sen:sens){
			String[] tokens = sen.split(" ");
			if(unique){
				Set<String> tSet = new HashSet<String>(Arrays.asList(tokens));
				tokens = tSet.toArray(new String[tSet.size()]);
			}for(String token:tokens){
				if(unigram.containsKey(token)){
					unigram.put(token,unigram.get(token)+1);
				}else{
					unigram.put(token,1);
				}
			}
		}
		this.extraMap = unigram;
	}

	public HashMap toCellMap(String[] tokens){
		super.initCellMap();
		HashMap<Integer,Integer> freqMap = new HashMap<Integer,Integer>();
		for(int i=0;i<tokens.length;i++)
			freqMap.put(i,(Integer)this.extraMap.get(tokens[i]));
		//sort
		ArrayList<Map.Entry<Integer,Integer>> sortMap = new ArrayList<Map.Entry<Integer,Integer>>(freqMap.entrySet());
		Collections.sort(sortMap, new Comparator<Map.Entry<Integer,Integer>>(){
			public int compare(Map.Entry<Integer,Integer> map1, Map.Entry<Integer,Integer> map2){
				return ((map2.getValue() - map1.getValue() == 0) ? 0 : (map2.getValue() - map1.getValue() > 0) ? 1 : - 1);
				}
			});
			int row = -1;
			for(Map.Entry pair:sortMap){
				int idx = (Integer)pair.getKey();
				row += 1;
				super.putCell(row,idx,tokens[idx]);
			}
		return this.cellMap;
	}
}
