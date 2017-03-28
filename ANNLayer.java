import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * TODO: 1. testing feedForward 2.
 * 
 * @author shunxu
 *
 */

public class ANNLayer implements Serializable {
	private float[] data;
	public float[][] weight;
	private float[][] momentum;
	private float[] dError;
	private ANNLayer nextLayer;
	private boolean isInputLayer;
	private float[] d;


	float stiff = 1;

	/**
	 * Constructor
	 * 
	 * @param numOfNodes
	 *            number of Nodes
	 * @param numOfNodes_nextLayer
	 *            number of nodes in the next layer
	 * @param isInput
	 *            if it will be an input layer
	 * @param nextLayer
	 *            a reference to next layer, null if it is output layer
	 */
	public ANNLayer(final int numOfNodes, final int numOfNodes_nextLayer, final boolean isInput, ANNLayer nextLayer) {
		data = new float[numOfNodes + 1];
		weight = new float[numOfNodes_nextLayer][numOfNodes + 1];
		momentum = new float[numOfNodes_nextLayer][numOfNodes + 1];
		dError = new float[numOfNodes];
		this.nextLayer = nextLayer;
		data[0] = 1;
		isInputLayer = isInput;
		initializeWeight();
		d = null;
	}


	public void makeOutputLayer(final float[] _d) {
		if (_d.length != data.length - 1) {
			System.err.println("Length of desire vector invalid");
			return;
		}

		nextLayer = null;
		d = new float[_d.length];
		for (int i = 0; i < _d.length; i++) {
			d[i] = _d[i];
		}
	}

	/**
	 * 
	 * @return if this layer is output layer
	 */
	public boolean isOutputLayer() {
		return d != null;
	}

	/**
	 * 
	 * @return if this layer a input layer
	 */
	public boolean isInputLayer() {
		return isInputLayer;
	}

	/**
	 * compute error for hidden layer, this function should not be called for
	 * input layer
	 */
	public void computeError() {
		if (isInputLayer()) {
			System.err.println("Error: error cannot be computed for input layer");
			return;
		}

		if (isOutputLayer()) {
			for (int i = 0; i < dError.length; i++) {
				dError[i] = data[i + 1] * (1 - data[i + 1]) * (d[i] - data[i + 1]);
//				dError[i] =  (d[i] - data[i + 1]);
//				if( d[0] == 0 && data[1] < 0.5)
//					System.out.println("predicting female correctly! " + data[1]);
			}
		} else {
			final float[] errorInNextLayer = nextLayer.getdError();
			for (int i = 0; i < dError.length; i++) {
				dError[i] = data[i + 1] * (1 - data[i + 1]) * dot(getColumn(weight, i + 1), errorInNextLayer);
			}
		} 
	} 
	
	private float getAverage(final float[] array){
		if(array.length == 0){
			return 0;
		}
		else{
			float sum = 0;
			for ( float f:array)
				sum += Math.abs( f );
			return sum / (float)array.length;
		}
		
	}

	public void updateWeight() {
		final float[] nextLayerError = nextLayer.getdError();
		final float[] nextLayerData = nextLayer.getData();
		for (int dataPtr = 0; dataPtr < data.length; dataPtr++) {
			for (int weightPtr = 0; weightPtr < weight.length; weightPtr++) {
//				float step = LEARNING_RATE * nextLayerError[weightPtr] * (nextLayerData[weightPtr+1])*(1- nextLayerData[weightPtr+1])*data[dataPtr];
//				momentum[weightPtr][dataPtr] = Main.MOMENTUM_CONSTANT * momentum[weightPtr][dataPtr] - step;
//
//				weight[weightPtr][dataPtr] = weight[weightPtr][dataPtr] - momentum[weightPtr][dataPtr];
				float step = Main.LEARNING_RATE * nextLayerError[weightPtr] * (nextLayerData[weightPtr+1])*(1- nextLayerData[weightPtr+1])*data[dataPtr];

				weight[weightPtr][dataPtr] = weight[weightPtr][dataPtr] + step;
			}
		}
	}
	
	
	public float[] getdError() {
		return dError;
	}

	public float[] getData() {
		return data;
	}
	/**
	 * The result will be the data for next layer
	 */
	public float[] getOutput() {

		// data = new float[numOfNodes + 1];
		// weight= new float[numOfNodes_nextLayer][numOfNodes + 1];
		if (isOutputLayer()) {
			return data;
		} else {

			float[] ret = new float[nextLayer.getNumOfNode()];

			for (int i = 0; i < nextLayer.getNumOfNode(); i++) {
				ret[i] = Sigmoid(dot(data, weight[i]));
			}
			return ret;
		}

	}

	/**
	 * return the number of nodes - 1, -1 to not count the bias term
	 */
	public int getNumOfNode() {
		// TODO Auto-generated method stub
		return data.length - 1;
	}

	/**
	 * Initialize all the weight to -0.5 ~ 0.5
	 */
	private void initializeWeight() {
		Random random = new Random();
		for (int i = 0; i < weight.length; i++) {
			for (int j = 0; j < data.length; j++) {
				weight[i][j] = (float) (random.nextFloat() - 0.5);
				momentum[i][j] = (float) (random.nextFloat() - 0.5);
				if(weight[i][j] == 0)
					j--;
			}
		}
//		for (int i = 0; i < weight.length; i++) {
//			for (int j = 0; j < data.length; j++) {
//				weight[i][j] = (float) i * data.length + j + 1;
//			}
//		}
	}

	/*
	 * Please normalize the data first before laoding the data
	 */
	public void load(final float[] input) {
		if (input.length != data.length - 1) {
			System.err.println("input Data length mismatch");
			return;
		}
		data[0] = 1;
		for (int i = 0; i < input.length; i++) {
			data[i + 1] = input[i];
		}
	}

	private float[] getColumn(final float[][] array, final int index ){
		if (index >= array[0].length){
			System.err.println("Index invalid");
			return null;
		}
		float[] ret = new float[array.length];
		for (int i = 0; i < array.length; i++){
			ret[i] = array[i][index];
		}
		return ret;
	}
	
	/**
	 * comput the dot product of v1 and v2,
	 * 
	 * @param v1
	 * @param v2
	 * @return null if (|v1| != |v2|) OR (|v1| == 0 OR |v2| == 0) dot products
	 *         of v1 and v2 otherwise
	 */
	public Float dot(final float[] v1, final float[] v2) {
		if (v1 == null || v2 == null || (v1.length != v2.length) || v1.length == 0 || v2.length == 0) {
			System.err.println("dot(): Parameters invalid.");
			return null;
		}
		float sum = (float) 0.0;
		for (int i = 0; i < v1.length; i++)
			sum += v1[i] * v2[i];

		return sum;
	}

	/**
	 * calculate the sigmoid function
	 * 
	 * @param n
	 *            the power of e
	 * @return
	 */
	public float Sigmoid(final float n) {
		return (float) (1.0 / (1 + Math.pow(Math.E, -n*stiff)));
	}

}
