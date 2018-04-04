
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
		BIRPClient client = new BIRPClient("altair.cs.oswego.edu", 6145);
		client.sendImageRequest("https://pnimg.net/lrep/11/61/sb3406a0b92.jpg");
		client.receiveImage();
		byte[] img = client.getImageBytes();

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
	}
}