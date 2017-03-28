/**
 * The starting point of the program
 */

/**
 * TODO:
 * 1. normalize data to -1 ~ 1
 * 2. create labels 
 * 3. bias term
 * 4. 
 */

/**
 * @author shunxu
 * This program will read in a text file representing the pixel intensity of a image and classify the gener of the person in the picture
 */

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

class Main{

	/**
	 * Magic Code Section
	 */
	static String maleDir = "./src/Male/";
	static String femaleDir = "./src/Female/";
	static String testDir = "./src/Test/";
	final static String imgDir =  "./src/Images/";
	final static int width = 128;
	final static int height = 120;
	final static int MAX_TRAINNING_TIME = 1000;
	final static int ERROR_REPORT_FREQUENCY = 1;
	final static float MOMENTUM_CONSTANT = (float) 0.9;
	final static float TERMINATING_THRESHOLD = (float)7.0;
	final static int R_SEED = 8;

	//I have no idea what these should be.
	
	/**
	 * the numbers of node in each layer
	 * { 3, 3, 4 } => 3 layers, the first hidden layer has 3 nodes, second one has 3 nodes and the third one has 4 
	 */
	final static int[] NUM_HIDDEN_NODES = {32};
	final static int NUM_OUTPUT = 2;
	final static int NUM_INPUT = width * height;	
	static Float LEARNING_RATE = (float) 0.5;
	final static int NUM_OF_FOLD = 5;

	final static int MALE = 1 ; // 
	final static int FEMALE = -1; // 
	final static int UNSPECIFIED_GENDER = 0;
	

	/**
	 * End of magic code block
	 */

	final static ArrayList<Picture> data = new ArrayList<Picture>();
	public static void main(String[] args) {

		String ANN_filename = "ANN";
		try{
			if (args[0].equalsIgnoreCase("-train")) {

//				final static String maleDir = "./src/Male/";
//				final static String femaleDir = "./src/Female/";
//				final static String testDir = "./src/Test/";

				maleDir = args[1];
				femaleDir = args[2];
				
				testDir = args[4];
				
				readAllMales();
				readAllFemales();
				trainAll();
			} 
			else if (args[0].equalsIgnoreCase("-test")) {

				System.out.println(args[1]);
				readAllTests();
				ANN ann = new ANN("temp");
				
				//test(data, ann);
				testAll(ann);
				//trainAll();
				
			}
			else if(args[0].equalsIgnoreCase("-fivefold")){
				readAllMales();
				readAllFemales();
				fiveFold();
			}
			else if(args[0].equalsIgnoreCase("-sandBox")){
				readAllMales();
				readAllFemales();
				sharpenImages();
				//data.get(15).displayImg();
				fiveFold();
				
			}
		} 
		catch(IllegalArgumentException iae) {
			System.err.println("Illegal Arguments: " + iae.getMessage());
			System.exit(0);
		} 
		catch(IndexOutOfBoundsException ioobe) {
			System.err.println("Illegal Arguments");
			System.exit(0);
		}
	}
	
	public static void sharpenImages(){
		for (Picture p: data){
			p.applyFilter(Picture.getSharpeningKernel());
		}
	}
	

	static ArrayList<Float> train_acc = new ArrayList<Float>();
	static ArrayList<Float> train_err = new ArrayList<Float>(); 
	
	static ArrayList<Float> test_acc = new ArrayList<Float>();
	static ArrayList<Float> test_err = new ArrayList<Float>(); 

	private static void fiveFold() {
		// TODO Auto-generated method stub
		Collections.shuffle(data, new Random(R_SEED));
		int testSetSize = (int) (data.size() * 0.2);
		int offSet = 0;
		
		for (int foldCnt = 0; foldCnt < 5; foldCnt++){
			ANN ann = new ANN(NUM_INPUT, NUM_HIDDEN_NODES, NUM_OUTPUT);

			for (int i = 0; i < testSetSize; i++){
				Picture temp = data.get(i);
				data.set(i, data.get(offSet+i));
				data.set(offSet + i, temp);
			}

			System.out.println("Fold " + foldCnt + " :");
			train(data.subList(testSetSize, data.size()-1), ann);

			//Picture.displayImg(ann.layers[0].weight[7]);
			//new Picture(ann.layers[0].weight[7], 128, 120).displayImg();

			//System.out.println("Training: ");
			float[] testTraning = test(data.subList(testSetSize, data.size()-1), ann);
			train_acc.add(testTraning[0]);
			train_err.add(testTraning[1]);
			//System.out.println("Testing: ");
			float[] testTesting =test(data.subList(0, testSetSize), ann);
			test_acc.add(testTesting[0]);
			test_err.add(testTesting[1]);
			offSet += testSetSize;
			
		}

		System.out.println("training accuracy is " + getAverage (train_acc) );
		System.out.println("training error is " + getAverage( train_err ));
		
		System.out.println("training accuracy is " + getAverage (test_acc) );
		System.out.println("training error is " + getAverage( test_err ));
		
		
//		ANN ann = new ANN(2, 1, new int[] {2}, 1);
//		ann.train(new float[] { 2, 3 }, new float[] {1});
		
	}


	private static void trainAll() {
		Collections.shuffle(data, new Random(R_SEED));
		
		ANN ann = new ANN(NUM_INPUT, NUM_HIDDEN_NODES, NUM_OUTPUT);

		train(data, ann);

		//Picture.displayImg(ann.layers[0].weight[7]);
		//new Picture(ann.layers[0].weight[7], 128, 120).displayImg();
		ann.saveNetworkAsFile("temp");
		ann.saveImg();
		
	}
	
	private static void testAll(final ANN ann){
		int[] result = new int[data.size()];
		for (int i = 0; i < data.size(); i++){
			float[] curResult = ann.predit(data.get(i).getX());
			
			//System.out.println(data.get(i).getGender() + " " + curResult[1] + " " + curResult[2] );

			System.out.println(  data.get(i).fileName);
			if (curResult[1] > curResult[2] )
				System.out.println("MALE," + curResult[1]);
			else
				System.out.println("FEMALE," + curResult[2]);
		}
		
	}

	private static void train(final List<Picture> trainningSet, final ANN ann) {
		if( trainningSet.size() == 0)
			return;
		
		ArrayList<Float> errors = new ArrayList<Float>();
		for ( int i = 0; i < MAX_TRAINNING_TIME; i++ ){

			LEARNING_RATE = (float) 0.5;
			
			ArrayList<Float> dWeight = new ArrayList<Float>();
			float curError = 0;
			for (int dataPtr = 0; dataPtr < trainningSet.size(); dataPtr++){
				float[] x = trainningSet.get(dataPtr).getX();
				float[] y = trainningSet.get(dataPtr).getY();

				ann.train(x, y);
				float[] result = ann.layers[2].getData();
				curError += ( Math.pow((y[0] - result[1] ), 2.0) + Math.pow((y[1] - result[2] ), 2.0) );
				
			}
			LEARNING_RATE *= (float)0.95 ;
			errors.add(curError);
			if( curError < TERMINATING_THRESHOLD){
//				System.out.println(i + " Final error is: " + Main.getAverage(errors) +  " " +
//						curError + " " + TERMINATING_THRESHOLD);
				break;
			}
			if ( i % ERROR_REPORT_FREQUENCY == 0)
				System.out.println(i + " current error is: " + Main.getAverage(errors));
			// if (Main.getAverage(dWeight) < TERMINATE_THRESHOLD){
			// 	break;
			// }
			test(trainningSet, ann);
			// if (test(trainningSet, ann) > 0.96)
			// 	break;
			// if(i % 20 == 0)
			// 	Picture.displayImg(ann.layers[0].weight[1]);
		}
		//ANN ann = new ANN()
	}

	
	//This function takes the pixels of a file and normalizes them to be between [-1,1]
	private static float[] test(final List<Picture> data, final ANN ann) {
		int[] result = new int[data.size()];
		float errorSum = 0;
		for (int i = 0; i < data.size(); i++){
			float[] curResult = ann.predit(data.get(i).getX());
			
			//System.out.println(data.get(i).getGender() + " " + curResult[1] + " " + curResult[2] );
			if (curResult[1] > curResult[2] && data.get(i).getGender() == MALE){
				result[i] = 1;
				errorSum += ( Math.pow((1 - result[1] ), 2.0) + Math.pow((0 - result[2] ), 2.0) );
			}
			else if (curResult[1] < curResult[2] && data.get(i).getGender() == FEMALE){
				result[i] = 1;
				errorSum += ( Math.pow((0 - result[1] ), 2.0) + Math.pow((1 - result[2] ), 2.0) );
			}
			else
				result[i] = 0;
			
			/**
			 * If we decide to use a single unit output layer
			 * The following code will be used to calculate the accuracy
			 */
//			if ( curResult[1] > 0.5 && data.get(i).getGender() == MALE){
//				result[i] =  1;
//			} // prediction is male
//			else if (curResult[1] < 0.5 && data.get(i).getGender() == FEMALE){
//				//System.out.println("predicting female correctly! " + curResult[1]);
//				result[i] = 1;
//			}
//			else 
//				result[i] = 0;
		}
		
		int sum = IntStream.of(result).sum();
		
		float accuracy = (float)(sum)/(float)result.length;
//		System.out.println("Accuracy is " + accuracy);
//		System.out.println("Error is " + errorSum);
		//acc.add(accuracy);
		//err.add(errorSum);
		
		
		return new float[]{accuracy, errorSum};
	}
	

	private static void readAllMales(){
		ArrayList<String> allFiles = getAllFile(maleDir);
		for( String str: allFiles)
			data.add(new Picture(str, MALE));
	}

	private static void readAllFemales(){
		ArrayList<String> allFiles = getAllFile(femaleDir);
		for(int i = 0; i < allFiles.size(); i++){
			data.add(new Picture(allFiles.get(i), FEMALE));
		}
	}

	private static void readAllTests(){
		ArrayList<String> allFiles = getAllFile(testDir);
		for( String str: allFiles)
			data.add(new Picture(str, 0));
	}
	
	/**
	 * 
	 * @param dirPath the path to the directory containing files of images
	 * @return the absolute path to each image file( text, containing integer values of pixels )
	 */
	public static ArrayList<String> getAllFile(final String dirPath) {
		File f;
		try{    		
			f = new File(dirPath);
		}catch(Exception e){
			System.err.println("Cannot open directory: " + dirPath);
			return null;
		}
		
		if(!f.isDirectory()){
			System.err.println( dirPath + " is not a directory.");
			return null;
		}
		File[] files = f.listFiles();
		ArrayList<String> allFiles = new ArrayList<String>(files.length);
		for ( File file:  files)
			if (file.getName().contains(".txt"))
				allFiles.add(file.getAbsolutePath());
		
		return allFiles;
	}
	
	public static float getAverage(final List<Float> list){
		if (list.isEmpty()){
			return 0;
		}
		else
		{
			float sum = 0;
			for( float f: list)
				sum += f;
			return sum / (float) list.size();
		}
	}
}