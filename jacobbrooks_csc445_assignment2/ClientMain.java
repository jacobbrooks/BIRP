
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.FlowLayout;

public class ClientMain{
	public static void main(String[] args){

		String slidingWindow = args[0];
		BIRPClient client = new BIRPClient("altair.cs.oswego.edu", 6145);
		
		client.sendImageRequest("https://pnimg.net/lrep/11/61/sb3406a0b92.jpg");

		long startTime = System.nanoTime();
		if(slidingWindow.equals("true")){
			client.slidingWindow();
		}else{
			client.receiveImage();
		}
		double elapsed = (double) (System.nanoTime() - startTime);

		double elapsedAsSeconds = elapsed / 1000000000.0;
		System.out.println("elapsed: " + elapsedAsSeconds);

		byte[] img = client.getImageBytes();

		System.out.println(img.length);

		try{
			InputStream in = new ByteArrayInputStream(img);
			BufferedImage bufferedImage = ImageIO.read(in);
			JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new FlowLayout());
			frame.getContentPane().add(new JLabel(new ImageIcon(bufferedImage)));
			frame.pack();
			frame.setVisible(true);
		}catch(IOException e){
			e.printStackTrace();
		}

		/*client.sendImageRequest("https://pnimg.net/lrep/11/61/sb3406a0b92.jpg");
		client.slidingWindow();
		img = client.getImageBytes();
		
		System.out.println(img.length);

		try{
			InputStream in = new ByteArrayInputStream(img);
			BufferedImage bufferedImage = ImageIO.read(in);
			JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new FlowLayout());
			frame.getContentPane().add(new JLabel(new ImageIcon(bufferedImage)));
			frame.pack();
			frame.setVisible(true);
		}catch(IOException e){
			e.printStackTrace();
		}*/
	}
}