import java.util.Random;

public class HiddenLayer {
	private float[] data;
	private float[][] weight;
	private float[] dError;
	
	public HiddenLayer(final int numOfNodes, final int numOfNodes_nextLayer){
		data = new float[numOfNodes + 1];
		weight= new float[numOfNodes + 1][numOfNodes_nextLayer + 1];
		dError = new float[numOfNodes + 1];
		data[0] = 1;
		initializeWeight();
	}
	
	/**
	 * Initialize all the weight to -0.5 ~ 0.5
	 */
	private void initializeWeight(){
		Random random = new Random();
		for (int i = 0; i < weight.length; i++){
			for (int j = 0; j < weight.length; j++){
				weight[i][j] = (float) (random.nextFloat() - 0.5);
			}
		}
	}
	
	/*
	 * This function load pixels to the data array
	 * It normalizes the data within [0, 1]
	 */
	public void load(final int[] input){
		// normalize data first
		if ( input.length != data.length - 1){
			System.err.println("input Data length mismatch");
			return;
		}
		
		for( int i = 0; i < input.length; i++){
			data[i+1] = (float) (input[i] / 255.0);
		}
	}
	
	
}
