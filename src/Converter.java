import java.util.HashMap;
import java.util.ArrayList;

import structures.*;

/**
 * HWS(Hierarchical Word Sequance) Converter
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-19
 **/

public class Converter{
	public static String mode;
	public static String assumption;
	public static int order;
	public static HWS hws;

	/**
	 * Constructor function.
	 *
	 * @param mode The mode of conversion of HWS sequences. <b>'ASM'</b>: both training data and test data will be converted by the same assumption; <b>'NST'</b>: thetest data will be converted by an NST trained from the training data. Default mode is 'ASM'.
	 * @param assumption The assumption used for sequences convertion. <b>'NGRAM'</b>: classical n-gram assumption; <b>'FB'</b>: frequency-based HWS assumption; <b>'DB'</b>: dice-coefficient-based HWS assumption; <b>'TB'</b>: t-score-based HWS assumption; <b>'ABS'</b>: abstraction-based HWS assumption. Default assumption is 'FB'.
	 	@param order The order of word sequences. Default order is 3.
	 **/
	public Converter(String mode,String assumption,int order){
		this.mode = mode;
		this.assumption = assumption;
		this.order = order;
	}
	
	/**
	 * Initiation function.Abstract patterns from sentences according certain assuption.
	 *
	 * @param sens raw sentences. For each sentence, words are delimited by space " ".
	 */
	public void init(String[] sens,HashMap extra){
		//init HWS
		if(assumption.equals("NGRAM")) hws = new NGRAM();
		else if(assumption.equals("FB")) hws = new FB();
		else if(assumption.equals("TB")) hws = new TB();
		else if(assumption.equals("DB")) hws = new DB();
		else if(assumption.equals("ABS")) hws = new ABS();
		else hws = new HWS();
		//extra map
		hws.getExtraMap(sens,extra);
	}

	/**
	 * Convert raw sentence to word sequences according to certain assumption.
	 *
	 * @param sen raw sentence. Tokens are delimited by space " ".
	 * @return Word sequences.
	 **/
	public static ArrayList<String> convert(String sen){
		String[] tokens = sen.split(" ");
		HashMap cellMap = hws.toCellMap(tokens);
		GHS ghs = new GHS(assumption,cellMap);
		ArrayList<String> seqs = ghs.toSeqs(order);
		return seqs;
	}

}
