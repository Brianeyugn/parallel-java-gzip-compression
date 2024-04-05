import java.util.concurrent.CountDownLatch;
import java.util.zip.Deflater;

public class Block implements Runnable
{
    public final static int BLOCK_SIZE = 131072; //131072
    public final static int DICT_SIZE = 32768; //32768

	//Text Buffers
    private byte[] block_buf;
    private byte[] cmp_block_buf;
    private byte[] dict_buf;
	
	//Data
    private int n_bytes_read;
	private int n_compressed_bytes;
    private boolean has_dict;
	
	private int total_bytes_read;
	
	//Utility + Misc
    private CountDownLatch compress_latch;
    private Deflater compressor;
      
	private int block_id = 0;
	
	private boolean is_last;
	private long file_bytes;

    Block()
    {
        this.block_buf = new byte[BLOCK_SIZE];
        this.cmp_block_buf = new byte[BLOCK_SIZE * 2];
        this.dict_buf = new byte[DICT_SIZE];
		
        this.n_bytes_read = 0;
        this.n_compressed_bytes = 0;
		this.has_dict = false;
		
        this.compress_latch = new CountDownLatch(1);
        this.compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);  
        
        this.total_bytes_read = 0;
		
        this.file_bytes = 0;
		this.is_last = false;
    }

    public synchronized void reuse(byte[] block_buf, int n_bytes_read, boolean is_last, int total_bytes_read, long file_bytes, int block_id)
    {
		this.block_buf = new byte[BLOCK_SIZE];
		this.block_buf = block_buf;
		this.cmp_block_buf = new byte[BLOCK_SIZE * 2];

		//Keep dict_buf same
		//Keep has_dict flag
		
		this.n_bytes_read = n_bytes_read;
		this.is_last = is_last;
		this.compress_latch = new CountDownLatch(1);
		this.compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		this.n_compressed_bytes = 0;
		this.total_bytes_read = total_bytes_read;
		this.file_bytes = file_bytes;
		//System.err.println(new String(this.block_buf));
    }

    public synchronized void compress()
    {
		compressor.reset();

		if (has_dict)
		{
			compressor.setDictionary(dict_buf);
		}
		compressor.setInput(block_buf, 0, n_bytes_read);
				
        if (total_bytes_read == file_bytes)
        {
			compressor.finish();
			this.n_compressed_bytes = compressor.deflate(cmp_block_buf, 0, cmp_block_buf.length, Deflater.SYNC_FLUSH);
        }
        else
        {
            this.n_compressed_bytes = compressor.deflate(cmp_block_buf, 0, cmp_block_buf.length, Deflater.SYNC_FLUSH);
        }
		//System.err.println(new String(block_buf));
		//System.err.println("bytes read" + n_bytes_read);
		
		if(n_bytes_read >= DICT_SIZE)
		{
			System.arraycopy(block_buf, n_bytes_read - DICT_SIZE, dict_buf, 0, DICT_SIZE);
			this.has_dict = true;
		}
		else
		{
			this.has_dict = false;
		}
		compress_latch.countDown();
    }
	
	public synchronized byte [] get_block_buf()
	{
		return this.block_buf;
	}
	public synchronized byte [] get_cmp_block_buf()
	{
		
		return this.cmp_block_buf;
	}
	public synchronized int get_n_bytes_read()
	{
		return this.n_bytes_read;
	}
	public synchronized CountDownLatch get_cmp_latch()
	{
		return compress_latch;
	}
	public synchronized boolean get_is_last()
	{
		return this.is_last;
	}
	public synchronized int get_n_compressed_bytes()
	{
		return this.n_compressed_bytes;
	}
	
    public void run()
    {
        compress();
    }
}
