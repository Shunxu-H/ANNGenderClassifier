import java.awt.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class ANN  implements Serializable {
	public ANNLayer[] layers;
	public int test;
	
	/**
	 * Constructor
	 * @param inputNodeCnt number of input node
	 * @param hidenLayerCnt number of hiddenlayer
	 * @param hiddenLayerNodeCnt an array indicating the numbers of nodes each hidden layer has
	 * @param outputNodeCnt number of output nodes
	 */
	public ANN(final int inputNodeCnt, final int[] hiddenLayerNodeCnt, final int outputNodeCnt){
		layers = new ANNLayer[hiddenLayerNodeCnt.length + 2];
		test = 1;
		// constructor output layer
		layers[layers.length - 1] = new ANNLayer(outputNodeCnt, 0, false, null);
		
		// hidden layers
		for (int i = layers.length - 2; i >= 1; i--){
			layers[i] = new ANNLayer(hiddenLayerNodeCnt[i-1], layers[i+1].getNumOfNode(), false, layers[i+1] );
		}
		
		layers[0] = new ANNLayer(inputNodeCnt, layers[1].getNumOfNode(), true, layers[1]);
			
	}
	
	/**
	 * 
	 * @param x: input
	 * @param y: desire vector
	 * @return average change in weights
	 */
	public void train(final float[] x, final float[] y){
		layers[layers.length-1].makeOutputLayer(y);
		
		// feedforward
		layers[0].load(x);
		for (int i = 0; i < layers.length - 1; i++){
			layers[i+1].load(layers[i].getOutput());
		}
		
		// compute error
		// computing from back to start because of backpropagation
		for (int i = layers.length - 1; i >=1 ; i--){
			layers[i].computeError();
		}
		
		ArrayList<Float> dWeight = new ArrayList<Float>();
		// update Weight
		for (int i = 0; i < layers.length - 1; i++){
			layers[i].updateWeight();
		}
		

	}

	public float[] predit(final float[] x){
		// feedforward
		layers[0].load(x);
		for (int i = 0; i < layers.length - 1; i++){
			layers[i+1].load(layers[i].getOutput());
		}
		return layers[layers.length-1].getOutput();
	}
	

	public void saveImg(){
		int imgCnt = 0;
		for (float[] w: layers[0].weight)
		{
			new Picture(w, Main.width, Main.height).save("weight" + imgCnt + ".png");
			imgCnt++;
		}
	}
	
	ANN(String ANN_filename){
		ANN network = null;
		try {	
			FileInputStream file_in = new FileInputStream(ANN_filename + ".net");
			ObjectInputStream in = new ObjectInputStream(file_in);
			network = (ANN)in.readObject();
			in.close();
			file_in.close();
		} catch (IOException i){
			i.printStackTrace();
			System.exit(0);
		} catch (ClassNotFoundException c) {
			System.out.println("Class not found");
			c.printStackTrace();
			System.exit(0);
		} 
		
		this.test = network.test;
		this.layers = network.layers;
	}

	//Save object to a file for testing.
	public void saveNetworkAsFile(String ANN_filename) {

		try {
			FileOutputStream fileOut = new FileOutputStream(ANN_filename + ".net");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
		} catch (IOException i){
			i.printStackTrace();
			System.exit(0);
		}
	}
	
}
