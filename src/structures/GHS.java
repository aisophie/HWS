package structures;


import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

/**
 * GHS(Generalized Hirerarchical Structure) strucure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-19
 **/


 public class GHS{
	String assumption;
 	int row;
	int col;
	String[][] matrix;


	/**
	 * Constructor function.
	 *
	 * @param assumption The assumption used for sequences convertion.
	 * @param map The cell map converted by HWS class.
	 **/
	public GHS(String assumption,HashMap<Integer,HashMap<Integer,String>> map){
		this.assumption = assumption;
		row = map.size();
		int cols = 0;
		for(HashMap cMap: map.values()) cols += cMap.size();
		col = cols;
		//matrix
		matrix = new String[row][col];
		for(int i=0;i<row;i++){
			for(int j=0;j<col;j++){
				String cell = map.get(i).get(j);
				//System.out.format("%s,%s,%s\n",i,j,cell);
				matrix[i][j] = (cell==null) ? "ε" : cell;
			}
		}
	}

	/**
	 * Print out the GHS structure.
	 **/
	public void print(){
		for(int i=0;i<this.row;i++){
			String line = StringUtils.join(this.matrix[i],"\t");
			System.out.println(line);
		}
	}


	private void recurse(String[] cxt,int order,int row,int start,int end,ArrayList<String> seqs){
		//the end
		if(row>this.row){
			seqs.add(StringUtils.join(cxt," ")+" "+"<E>");
			return;
		}
		//find head
		int idx = -1;
		for(int i=start;i<end;i++){
			String cell = this.matrix[row][i];
			if(!cell.equals("ε")){
				idx = i;
				break;
			}
		}
		//empty row
		if(idx==-1){
			recurse(cxt,order,row+1,start,end,seqs);
			return;
		}
		//divide
		String head = this.matrix[row][idx];
		seqs.add(StringUtils.join(cxt," ")+" "+head);
		String[] cxtL = Arrays.copyOf(cxt,cxt.length+1);
		String[] cxtR = Arrays.copyOf(cxt,cxt.length+1);
		cxtL = Arrays.copyOfRange(cxtL,Math.max(0,cxtL.length-order+1),cxtL.length);
		cxtR = Arrays.copyOfRange(cxtR,Math.max(0,cxtR.length-order+1),cxtR.length);
		if(!assumption.equals("NGRAM")){
			cxtL[cxtL.length-1] = head+"-L";
			recurse(cxtL,order,row+1,start,idx,seqs);	
			cxtR[cxtR.length-1] = head+"-R";
		}else{//ngram model
			cxtR[cxtR.length-1] = head;
		}
		recurse(cxtR,order,row+1,idx+1,end,seqs);	
	}

	/**
	 * Convert GHS structure to word sequences
	 * @param order The order of word sequences
	 * @return Word sequences
	 **/
	public ArrayList<String> toSeqs(int order){
		ArrayList<String> seqs = new ArrayList<String>();
		recurse(new String[]{"<B>"},order,0,0,this.col,seqs);
		return seqs;
	}

 }
