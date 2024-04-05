import java.io.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.Arrays;

public class ReadCompressThread extends Thread
{
	private InputStream in_stream;
	private byte[] block_buf;
	private long n_file_bytes;
	private int run_id = 0;
	private ArrayBlockingQueue<Block> read_compress_queue;
	private ArrayBlockingQueue<Block> write_queue;
	private int n_thread;
	private int n_block;
	private ThreadPoolExecutor cmp_executor;
	
	public final static int BLOCK_SIZE = 131072; //131072
	
	public ReadCompressThread(InputStream in_stream, byte[] block_buf, long n_file_bytes,
		ArrayBlockingQueue<Block> read_compress_queue, ArrayBlockingQueue<Block> write_queue,
		int n_thread, int n_block)
	{
		this.in_stream = in_stream;
		this.block_buf = block_buf;
		this.n_file_bytes = n_file_bytes;
		this.read_compress_queue = read_compress_queue;
		this.write_queue = write_queue;
		this.n_thread = n_thread;
		this.n_block = n_block;
	}
	
	public void run()
	{
		
		try {
			//Initialize thread pool for compression
			ThreadPoolExecutor cmp_executor = new ThreadPoolExecutor(n_thread,n_thread,60L,
				TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(n_block));
		
			int read_id = 0;
			Block cur_block = read_compress_queue.take();
			int n_bytes_read = in_stream.read(block_buf);
			int total_bytes_read = n_bytes_read;
			if(total_bytes_read != n_file_bytes)
			{
				cur_block.reuse(block_buf, n_bytes_read, false, total_bytes_read, n_file_bytes, read_id);
			}
			else
			{
				cur_block.reuse(block_buf, n_bytes_read, true, total_bytes_read, n_file_bytes, read_id);
			}
			cmp_executor.execute(cur_block);
			write_queue.put(cur_block);
			//Pigzj.PrintBlocks(write_queue, "write_queue from READ");
			block_buf = new byte[BLOCK_SIZE];
			
			while (n_bytes_read > 0)
			{	
				read_id+=1;
				cur_block = read_compress_queue.take();
				n_bytes_read = in_stream.read(block_buf);
				if (n_bytes_read <= 0)
				{
					break; //To avoid deflator array index error
				}
				
				total_bytes_read += n_bytes_read;
				if(total_bytes_read != n_file_bytes)
				{
					cur_block.reuse(block_buf, n_bytes_read, false, total_bytes_read, n_file_bytes, read_id);
				}
				else
				{
					cur_block.reuse(block_buf, n_bytes_read, true, total_bytes_read, n_file_bytes, read_id);
				}
				cmp_executor.execute(cur_block);
				write_queue.put(cur_block);
				//Pigzj.PrintBlocks(write_queue, "write_queue from READ");
				block_buf = new byte[BLOCK_SIZE];
			}

			cmp_executor.shutdown();
			cmp_executor.awaitTermination(60L, TimeUnit.SECONDS);
		} catch (InterruptedException | IOException e) {
            e.printStackTrace();
		}
	}
		
}