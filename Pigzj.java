import java.io.*;
import static java.lang.Integer.parseInt;
import java.util.concurrent.*;
import java.util.Arrays; //For debugging
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Pigzj
{
    public final static int BLOCK_SIZE = 131072; //131072

	public static File file;
	public static int available_processors = Runtime.getRuntime().availableProcessors();
	public static InputStream in_stream;
	public static int n_thread = 1;
	public static long n_file_bytes;
	
	public static void setup_threads_and_in_stream(String args[]) throws InterruptedException, IOException {
	//Parse arguments for thread number and input stream
        if (args.length == 3)
        {
            if (args[0].equals("-p"))
            {
                n_thread = parseInt(args[1]);
                file = new File(args[2]);
                in_stream = new FileInputStream(file);
                n_file_bytes = file.length();
            }
            else //args[1] == "-p"
            {
                n_thread = parseInt(args[2]);
                file = new File(args[0]);
                in_stream = new FileInputStream(file);
                n_file_bytes = file.length();
            }
        }
        else if (args.length == 1)
        {
            n_thread = available_processors;
            file = new File(args[0]);
            in_stream = new FileInputStream(file);
            n_file_bytes = file.length();
        }
        else //no args given
        {
            n_thread = available_processors;
            in_stream = System.in;
            n_file_bytes = in_stream.available();
        }
	}
	
    public static void main(String args[]) throws InterruptedException, IOException {
        //Arguements parsed, threads setup and input stream setup using file
		setup_threads_and_in_stream(args);
		    
        ByteArrayOutputStream out_stream = new ByteArrayOutputStream(); //Write to out_stream then write to System.out	
		byte[] block_buf = new byte[BLOCK_SIZE];
        int n_block = (n_thread * 2) + 1;
		
		ArrayBlockingQueue<Block> read_compress_queue = new ArrayBlockingQueue<Block>(n_block);
		ArrayBlockingQueue<Block> write_queue = new ArrayBlockingQueue<Block>(n_block);
		
        for(int i=0; i<n_block; i++)
        {
			read_compress_queue.put(new Block());
        }
			
		//Initialize ReadCompressThread for reading into blocks
		ReadCompressThread read_compress_thread = new ReadCompressThread(in_stream, block_buf,
			n_file_bytes, read_compress_queue, write_queue, n_thread, n_block);
		read_compress_thread.start();
		
        //Initialize WriteThread for writing to System.out
        WriteThread write_thread = new WriteThread(out_stream, read_compress_queue, write_queue);
        write_thread.start();
    }
	
	public static void PrintBlocks(ArrayBlockingQueue<Block> queue, String queue_type) 
	{
		if (queue.size() == 0) {
			System.err.println(queue_type + ": empty");
			return;
		}
		System.err.print(queue_type + ": ");
		for (Block block : queue) {
			System.err.print(new String(block.get_block_buf()) + " ");
		}
			System.err.println();
	}
}

