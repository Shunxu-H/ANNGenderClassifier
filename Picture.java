/**
 * This Object will store the pixels values in a list
 * @author shunxu
 *
 */
import java.util.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Picture {
	/*
	 * MAGIC NUMBER SECTION
	 */
	public int width = 128;
	public int height = 120;
	public static int scaleFactor = 4;
	/*
	 * END OF MAGIC NUMBER SECTION
	 */
	public String fileName;
	private int gender;
	private ArrayList<Integer> pixels = new ArrayList<Integer>();
	
	public Picture(final String fileName, final int gender ){
		File f;
		this.fileName = fileName;
		this.gender = gender;
	    	try{    		
	    		f = new File(fileName);
	    	}catch(Exception e){
	    		System.err.println("Cannot open file: " + fileName);
	    		return;
	    	}
	    	
	    	try(BufferedReader br = new BufferedReader(new FileReader(f))) {
	    	    for(String line; (line = br.readLine()) != null; ) {
	    	        readALine(line);
	    	    }
	    	    // line is not visible here.
	    	}catch(Exception e){
	    		System.err.println(e.getMessage());
	    		return;
	    	}
    	
	}
	

	/**
	 * This takes a list of pixel value and make it a picture, it will skip the first number because we have to skip the bias term 
	 * it will take the absolute values of the data, and normalize them to scale [0 255]
	 * @param data
	 * 					the pixel value
	 * @param width
	 * 					width of the picture
	 * @param height
	 * 					height of the picture
	 */
	public Picture(final float[] data, final int width, final int height){
		gender = Main.UNSPECIFIED_GENDER;
		float max = rangAbsoluteMax(data, 1, data.length);
		for( int i = 1; i < data.length; i++)
			if (data[i] < 0)
				pixels.add(0);
			else
				pixels.add( (int)((data[i]/max)* 255));
		this.width = width;
		this.height = height;
	}
	
	
	/**
	 * 			Find the maxima of the absolute value the array from iniIndex(inclusively) to finalIndex(exclusively)
	 * @param data
	 * 				The data to iterate from 
	 * @param iniIndex
	 * 				Starting index
	 * @param finalIndex
	 * 				The last index to check
	 * @return the maxima of the absolute value in data[] from iniIndex to finalIndex
	 */
	private Float rangAbsoluteMax(final float[] data, final int iniIndex, final int finalIndex){
		if(data.length == 0 || data.length - 1 > finalIndex || iniIndex > finalIndex){
			System.err.println("Empty Array or index out of bound" );
			return null;
		}
		float max = -Float.MAX_VALUE;
		for (int i = iniIndex; i < finalIndex; i++){
			if (Math.abs(data[i]) > max)
				max = Math.abs(data[i]);
		}
		return max;
	}
	
	/**
	 * FILTERING RELATED:
	 */
	public static float[][] getSharpeningKernel(){
		float[][] ret = new float[3][3];
		
		for( int i = 0; i < 3; i++){
			for( int j = 0; j < 3; j++){
				ret[i][j] = -1;
			}
		}
		ret[1][1] = 8;
		return ret;
	}
	
	public static float sum(final float[][] array){
		float sum = 0;
		for(int i = 0; i < array.length; i++)
			for( int j = 0; j < array[i].length; j++)
				sum += array[i][j];
		
		return sum;
	}
	public void applyFilter(final float[][] kernel){
		ArrayList<Float> newPix = new ArrayList<Float>();
		float kernelSum = sum(kernel);
		for (int rowPtr = 0; rowPtr < height; rowPtr++){
			for( int colPtr = 0; colPtr < width; colPtr++){
				float sum = 0;
				for (int i = 0; i < 3; i++){
					for (int j = 0; j < 3; j++){
						sum += (float) kernel[i][j]* getPixelVal(colPtr + i - 1, rowPtr  + j - 1);
					}
				}
				newPix.add(sum);
			}
		}
		
		Float max = Collections.max(newPix);
		for(int i = 0; i < newPix.size(); i++)
			pixels.set(i, (int) (newPix.get(i)/max*(float)255));
		//pixels.set(i, (int) (newPix.get(i)/max*(float)255));
		//pixels = newPix;
	}
	
	private int getPixelVal(final int x, final int y){
		if ( x < 0 || x > width  - 1 || y < 0 || y > height - 1)
			return 1;
		else 
			return pixels.get(y*width + x);
	}
	
	private void setPixelVal(final int newVal, final int x, final int y){
		if ( x < 0 || x > width  - 1 || y < 0 || y > height - 1)
			return;
		pixels.set(y*width + x, newVal);
	}
	
	/**
	 * Save the picture to a .png file, the directory to save in is specified in Main.java (under MAGIC NUMBER SECTION)
	 * @param fileName
	 * 			the file name
	 */
	public void save(final String fileName){
		try{			
			File outputfile = new File(Main.imgDir + fileName);
			ImageIO.write(getImage(), "png", outputfile);
		}catch (Exception e){
			e.getCause();
		}
	}
	
	
	/**
	 * this method normalize pixels value and return the input for Artificial Neural Network
	 * @return input for artificial neural network
	 */
	public float[] getX(){
		float min = min();
		float max = max();
		float d = max - min;
		float[] ret = new float[pixels.size()];
		
		for(int i = 0; i < ret.length; i++){
			float curPixelVal = (float) pixels.get(i);
			ret[i] = (curPixelVal) / 255;
		}
		
		/**
		 * The following way has worse performance
		 */
		/**
		for(int i = 0; i < ret.length; i++){
			float curPixelVal = (float) pixels.get(i);
			ret[i] = (curPixelVal - min) / d;
		}
		 */
		return ret;
	}
	
	public int getGender(){
		return gender;
	}
	
	/**
	 * this method wraps gender in appropricate format for ANN
	 * @return desire vector for artificial neural network
	 */
	public float[] getY(){
		if ( gender == Main.MALE)
		{
			return new float[]{1, 0};
		}
		else if (gender == Main.FEMALE){
			return new float[] {0, 1};
		}
		else {
			System.err.println("Invalid Gender Parameter or geting desire vector from testing set without label");
			return null;
		}
	}
	
	private float min(){
		float min = Float.MAX_VALUE;
		
		for (float f: pixels)
			if (f < min)
				min = f;
		
		return min;
	}
	
	private float max(){
	float max = Float.MIN_VALUE;
		
		for (float f: pixels)
			if (f > max)
				max = f;
		return max;
	}
	
	private void readALine(String str){
		StringTokenizer st = new StringTokenizer(str);
	     while (st.hasMoreTokens()) {
	         pixels.add(Integer.parseInt(st.nextToken(" ")));
	     }
	}
	
	private BufferedImage getImage() {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int x = 0; x < width; x++){
			for( int y = 0; y < height; y++){
				int val = pixels.get(y*width + x);
				image.setRGB(x, y, val + (val << 8) + (val << 16) );
			}
			//image.setRGB(x, y, pixels.get(x*width + y));
		}
		//raster.setPixels(0,0,width,height, rgbArray);
		return image;
	}
//	
//	private static BufferedImage getImageSkipOne(final float[] pixelVal, final int width, final int height) {
//		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//		//float min = minSkipOne(pixelVal);
//		float max = maxSkipOneAbs(pixelVal);
//
//		for ( int x = 0; x < width; x++){
//			for( int y = 0; y < height; y++){
//				int val = (int) ((pixelVal[y*width + x + 1]/max) * 255);
//				image.setRGB(x, y, val + (val << 8) + (val << 16) );
//			}
//			//image.setRGB(x, y, pixels.get(x*width + y));
//		}
//		//raster.setPixels(0,0,width,height, rgbArray);
//		return image;
//	}
	
    public static Image scaleImage(int width, int height, BufferedImage filename) {
        BufferedImage bi;
        try {
            bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) bi.createGraphics();
            g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
            g2d.drawImage(filename, 0, 0, width, height, null);
        } catch (Exception e) {
            return null;
        }
        return bi;
    }
    
//    public static void displayImg(float[] pixelVal){
//		try{
//			generateImg(getImageSkipOne(pixelVal, 128, 120));
//		}catch( Exception e ){
//			e.printStackTrace(System.err);
//		}	
//	}
	
	public void displayImg(){
		try{
			generateImg(getImage());
		}catch( Exception e ){
			e.printStackTrace(System.err);
		}
		
	}
	
	private void generateImg(BufferedImage img) throws MalformedURLException, IOException {
        JFrame frame = new JFrame("Frame with JPanel and background");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //final Image background = getImage();
        final Image background = scaleImage(width*scaleFactor, height*scaleFactor, img);
        //final Image background = new Image();
        final Dimension jpanelDimensions = new Dimension(new ImageIcon(background).getIconWidth(), new ImageIcon(background).getIconHeight());

        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                super.paintComponent(grphcs);
                grphcs.drawImage(background, 0, 0, this);
            }

            @Override
            public Dimension getPreferredSize() {
                return jpanelDimensions;
            }
        });

        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

	public ArrayList<Integer> getPixels(){
		return pixels;
	}
	
}
